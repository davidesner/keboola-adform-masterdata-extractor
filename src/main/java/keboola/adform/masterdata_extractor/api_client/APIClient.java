/*
 */
package keboola.adform.masterdata_extractor.api_client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import keboola.adform.masterdata_extractor.pojo.MasterFile;
import keboola.adform.masterdata_extractor.pojo.MasterFileList;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class APIClient {

    private Exception apiException;
    private static final String BASE_URL = "https://api.adform.com";
    //private static final String BASE_URL = "http://localhost:8888";
    private static final String LOGIN_URL_PATH = "/Services/Security/Login";
    private static final String MD_FILES_URL_PATH = "/v1/buyer/masterdata/files/";
    private static final String DOWNLOAD_F_URL_PATH = "/v1/buyer/masterdata/download/";
    private final String userName;
    private final String password;
    private final String masterDataListID;
    private final Logger logger;
	private BasicCookieStore cookieStore;
	
	private static final int MAX_RETRIES = 15;
	private static final long RETRY_INTERVAL = 5000;
	private static final int[] RETRY_STATUS_CODES = {504};

    public APIClient(String userName, String password, String masterDataListID, Logger log) {
        this.userName = userName;
        this.password = password;
        this.masterDataListID = masterDataListID;
        this.logger = log;
    }

    public Exception getApiException() {
        return apiException;
    }

    private void setApiException(Exception apiException) {
        this.apiException = apiException;
    }

    public void authenticate() throws ClientException {
        try (CloseableHttpClient client = getHttpClient(null);){
            HttpPost httpPost = new HttpPost(BASE_URL + LOGIN_URL_PATH);

            String credsJson = "{\"Username\":\"" + userName + "\",\"Password\":\"" + password + "\"}";

            StringEntity requestEntity = new StringEntity(
                    credsJson, "UTF-8");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(requestEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            //check response code
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
            	client.close();
            	response.close();
                throw new ClientException("Authentization error. Http Response code:" + statusCode + " - " + response.getStatusLine().getReasonPhrase());
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONObject resp = new JSONObject(result.toString());
            setHttpContext(resp.getString("Ticket"));  
            
            
            response.close();
        } catch (IOException ex) {
            throw new ClientException("Authentication failed! " + ex.getMessage());
        }
    }

    private void setHttpContext(String authTicket) {    	
    	BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("authTicket", authTicket);
        cookie.setPath("/");
        cookie.setDomain("api.adform.com");         
        cookieStore.addCookie(cookie);
        this.cookieStore = cookieStore;
    }

    private  BasicCookieStore getCookieStore() {
		return cookieStore;
	}

	public void setCookieStore(BasicCookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	public MasterFileList retrieveFileList() throws ClientException {
        try {
            if (cookieStore == null) {
                authenticate();
            }
            HttpClient client = getHttpClient(getCookieStore());
            

            HttpGet httpGet = new HttpGet(BASE_URL + MD_FILES_URL_PATH + masterDataListID);
            httpGet.setHeader("Accept", "application/json");
            
            
            //disable redirects
            RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
            httpGet.setConfig(requestConfig);

            HttpResponse response = client.execute(httpGet);

            //check response code
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientException("Authentization error. Http Response code:" + statusCode + " - " + response.getStatusLine().getReasonPhrase());
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            ObjectMapper mapper = new ObjectMapper();
            //JSON to POJO
            MasterFile[] files = mapper.readValue(result.toString(), MasterFile[].class);            
            return new MasterFileList(Arrays.asList(files));
        } catch (IOException ex) {		
            logger.error("retrieveFileList ERROR! " + ex.getMessage(), ex);
            return null;
        }

    }

    /**
     *
     * @param fileUrl - URL of the file to download
     * @param downloadPath - path where the file is to be downloaded
     * @param lastRun - indicates it is a last run and API exception should be
     * set if fail response code is returned	
     * @return
     * @throws ClientException
     */
    public boolean downloadFile(String fileId, String downloadPath, boolean lastRun) throws ClientException {
        try {
            FileOutputStream fos = null;
            CloseableHttpClient client = getHttpClient(getCookieStore());
            HttpGet httpget = new HttpGet(BASE_URL + DOWNLOAD_F_URL_PATH + masterDataListID + "/" + fileId);
            //disable redirects
            RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
            httpget.setConfig(requestConfig);

            HttpResponse response = client.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                if (lastRun) {
                    setApiException(new Exception("Failed to retrieve file id: " + fileId + " Server returned response code: " + statusCode));
                    return false;
                }
                if (statusCode == 302) {
                    //invalid ticket
                    authenticate();
                    return false;
                } else {
                    return false;
                }
            }
            HttpEntity entity = response.getEntity();
            //download file
            InputStream is = entity.getContent();
            String filePath = downloadPath;
            File newFile = new File(filePath);
            //create all non exists folders            
            new File(newFile.getParent()).mkdirs();
            fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[8192];
            int inByte;
            while ((inByte = is.read(buffer)) != -1) {
                fos.write(buffer, 0, inByte);
            }
            is.close();
            fos.close();
            return true;
        } catch (ClientProtocolException ex) {
            setApiException(ex);
            return false;
        } catch (IOException ex) {
            setApiException(ex);
            return false;
        } catch (Exception ex) {
            setApiException(ex);
            return false;
        }
    }

	private CloseableHttpClient getHttpClient(BasicCookieStore cookieStore) {
		HttpClientBuilder builder = HttpClientBuilder.create()
				.setRetryHandler(getRetryHandler(MAX_RETRIES)).setServiceUnavailableRetryStrategy(
						getServiceUnavailableRetryStrategy(MAX_RETRIES, RETRY_STATUS_CODES));
		if (cookieStore != null) {
			builder = builder.setDefaultCookieStore(cookieStore);
		}
		return builder.build();
	}

    private HttpRequestRetryHandler getRetryHandler(int maxRetryCount){
        return (exception, executionCount, context) -> {

            logger.warn("Retrying for: " + executionCount + ". time");

            if (executionCount >= maxRetryCount) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
            	return true;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        };
    }
    
    private ServiceUnavailableRetryStrategy getServiceUnavailableRetryStrategy(final int maxRetryCount, int[] allowedCodes){
    	return new ServiceUnavailableRetryStrategy() {
            @Override
            public boolean retryRequest(
                    final HttpResponse response, final int executionCount, final HttpContext context) {
                int statusCode = response.getStatusLine().getStatusCode();
                String urlPath = context.getAttribute("http.request").toString();
                urlPath = urlPath.substring(0, urlPath.indexOf("["));
                logger.warn(urlPath + " returned " + response.getStatusLine() + " Retrying for: " + executionCount + ". time");
                return Arrays.stream(allowedCodes).anyMatch(i -> i==statusCode) && executionCount < maxRetryCount;
            }

            @Override
            public long getRetryInterval() {
                return RETRY_INTERVAL;
            }
        };
    }
}
