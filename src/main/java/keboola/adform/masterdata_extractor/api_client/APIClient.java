/*
 */
package keboola.adform.masterdata_extractor.api_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import keboola.adform.masterdata_extractor.Extractor;
import keboola.adform.masterdata_extractor.pojo.MasterFileList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class APIClient {

    private Exception apiException;
    private static final String LOGIN_URL = "https://api.adform.com/Services/Security/Login";
    private final String userName;
    private final String password;
    private final String masterDataListUrl;
    private String AUTH_TICKET;

    public APIClient(String userName, String password, String masterDataListUrl) {
        this.userName = userName;
        this.password = password;
        this.masterDataListUrl = masterDataListUrl;
    }

    public Exception getApiException() {
        return apiException;
    }

    private void setApiException(Exception apiException) {
        this.apiException = apiException;
    }

    public void authenticate() throws ClientException {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(LOGIN_URL);

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
                throw new ClientException("Authentization error. Http Response code:" + statusCode + " - " + response.getStatusLine().getReasonPhrase());
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONObject resp = new JSONObject(result.toString());
            AUTH_TICKET = resp.getString("Ticket");
            client.close();


        } catch (IOException ex) {
            Logger.getLogger(Extractor.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public MasterFileList retrieveFileList() throws ClientException {
        try {
            if (AUTH_TICKET == null) {
                authenticate();
            }

            CloseableHttpClient client = HttpClients.createDefault();


            HttpGet httpGet = new HttpGet(masterDataListUrl + "?render=json&authTicket=" + AUTH_TICKET);
            httpGet.setHeader("Accept", "application/json");

            //disable redirects
            RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
            httpGet.setConfig(requestConfig);

            CloseableHttpResponse response = client.execute(httpGet);

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
            MasterFileList fileList = mapper.readValue(result.toString(), MasterFileList.class);
            return fileList;
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    public boolean downloadFile(String fileUrl, String downloadPath) throws ClientException {
        try {
            FileOutputStream fos = null;
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpget = new HttpGet(fileUrl + "?authTicket=" + AUTH_TICKET);
            HttpResponse response = client.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                if (statusCode == 302) {
                    //invalid ticket
                    authenticate();
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
        }
    }
}
