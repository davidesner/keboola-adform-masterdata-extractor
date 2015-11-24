/*
 */
package keboola.adform.masterdata_extractor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class KBCParameters {

    private final static String[] REQUIRED_FIELDS = {"user", "pass", "mdListUrl", "bucket", "prefixes", "daysInterval"};
    private final Map<String, Object> parametersMap;
    private Date date_to;
    @JsonProperty("user")
    private String user;
    @JsonProperty("#pass")
    private String pass;
    @JsonProperty("mdListUrl")
    private String mdListUrl;
    @JsonProperty("daysInterval")
    private int daysInterval;
    //end date of fetched interval in format: 05-10-2015 21:00
    @JsonProperty("dateTo")
    private String dateTo;
    @JsonProperty("bucket")
    private String bucket;
    @JsonProperty("prefixes")
    private ArrayList<String> prefixes;
    @JsonProperty("metaFiles")
    private ArrayList<String> metaFiles;

    public KBCParameters() {
        parametersMap = new HashMap<String, Object>();

    }

    @JsonCreator
    public KBCParameters(@JsonProperty("user") String user, @JsonProperty("#pass") String pass,
            @JsonProperty("mdListUrl") String mdListUrl, @JsonProperty("daysInterval") int daysInterval,
            @JsonProperty("dateTo") String dateTo, @JsonProperty("bucket") String bucket,
            @JsonProperty("prefixes") ArrayList<String> prefixes, @JsonProperty("metaFiles") ArrayList<String> metaFiles) throws ParseException {
        parametersMap = new HashMap<String, Object>();
        this.user = user;
        this.pass = pass;
        this.mdListUrl = mdListUrl;
        this.daysInterval = daysInterval;
        this.dateTo = dateTo;
        if (dateTo != null) {
            setDate_to(dateTo);
        }

        this.bucket = bucket;
        this.prefixes = prefixes;
        this.metaFiles = metaFiles;
        //set param map
        parametersMap.put("user", user);
        parametersMap.put("pass", pass);
        parametersMap.put("mdListUrl", mdListUrl);
        parametersMap.put("daysInterval", mdListUrl);
        parametersMap.put("dateTo", dateTo);
        parametersMap.put("bucket", bucket);
        parametersMap.put("prefixes", prefixes);
    }

    /**
     * Returns list of required fields missing in config
     *
     * @return
     */
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<String>();
        for (int i = 0; i < REQUIRED_FIELDS.length; i++) {
            Object value = parametersMap.get(REQUIRED_FIELDS[i]);
            if (value == null) {
                missing.add(REQUIRED_FIELDS[i]);
            }
        }

        if (missing.isEmpty()) {
            return null;
        }
        return missing;
    }

    public ArrayList<String> getMetaFiles() {
        return metaFiles;
    }

    public void setMetaFiles(ArrayList<String> metaFiles) {
        this.metaFiles = metaFiles;
    }

    private void setDate_to(String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        this.date_to = format.parse(dateString);

    }

    public Date getDate_to() {
        return date_to;
    }

    public Map<String, Object> getParametersMap() {
        return parametersMap;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public ArrayList<String> getPrefixes() {
        return prefixes;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public void setPrefixes(ArrayList<String> prefixes) {
        this.prefixes = prefixes;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getMdListUrl() {
        return mdListUrl;
    }

    public void setMdListUrl(String mdListUrl) {
        this.mdListUrl = mdListUrl;
    }

    public int getDaysInterval() {
        return daysInterval;
    }

    public void setDaysInterval(int daysInterval) {
        this.daysInterval = daysInterval;
    }
}
