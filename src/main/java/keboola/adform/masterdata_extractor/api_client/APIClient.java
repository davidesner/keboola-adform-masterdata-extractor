/*
 */
package keboola.adform.masterdata_extractor.api_client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import keboola.adform.masterdata_extractor.Extractor;
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
    private static final String LOGIN_URL_PATH = "/Services/Security/Login";
    private static final String MD_FILES_URL_PATH = "/v1/buyer/masterdata/files/";
    private static final String DOWNLOAD_F_URL_PATH = "/v1/buyer/masterdata/download/";
    private final String userName;
    private final String password;
    private final String masterDataListID;
    private String AUTH_TICKET;
	private BasicCookieStore cookieStore;

    public APIClient(String userName, String password, String masterDataListID) {
        this.userName = userName;
        this.password = password;
        this.masterDataListID = masterDataListID;
    }

    public Exception getApiException() {
        return apiException;
    }

    private void setApiException(Exception apiException) {
        this.apiException = apiException;
    }

    public void authenticate() throws ClientException {
        try (CloseableHttpClient client = HttpClients.createDefault();){
            HttpPost httpPost = new HttpPost(BASE_URL + LOGIN_URL_PATH);

            String credsJson = "{\"Username\":\"" + userName + "\",\"Password\":\"" + password + "\"}\"";

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
            Logger.getLogger(Extractor.class
                    .getName()).log(Level.SEVERE, null, ex);
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

            HttpClient client = HttpClientBuilder.create().setDefaultCookieStore(getCookieStore()).build();
            

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
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
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
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(getCookieStore()).build();
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
}
