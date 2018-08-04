/*
 */
package keboola.adform.masterdata_extractor.config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class KBCParameters {

    private final static String[] REQUIRED_FIELDS = {"user", "pass", "mdListId", "bucket", "prefixes", "daysInterval"};
    private final Map<String, Object> parametersMap;
    private Date date_to;
    @JsonProperty("user")
    private String user;
    @JsonProperty("#pass")
    private String pass;
    
    @JsonProperty("mdListId")
    private String mdListId;

    @JsonProperty("daysInterval")
    private int daysInterval;
    @JsonProperty("hoursInterval")
    private int hoursInterval;

    @JsonProperty("alwaysGetMeta")
    private boolean alwaysGetMeta;
    
    //end date of fetched interval in format: 05-10-2015 21:00
    @JsonProperty("dateTo")
    private String dateTo;
    @JsonProperty("bucket")
    private String bucket;
    @JsonProperty("srcCharset")
    private String srcCharset;
    @JsonProperty("prefixes")
    private ArrayList<String> prefixes;
    @JsonProperty("metaFiles")
    private ArrayList<String> metaFiles;

    public KBCParameters() {
        parametersMap = new HashMap<String, Object>();

    }

    @JsonCreator
    public KBCParameters(@JsonProperty("user") String user, @JsonProperty("#pass") String pass,
    		 @JsonProperty("mdListId") String mdListId,
            @JsonProperty("mdListUrl") String mdListUrl, @JsonProperty("daysInterval") int daysInterval,
            @JsonProperty("dateTo") String dateTo, @JsonProperty("bucket") String bucket, @JsonProperty("srcCharset") String srcCharset,
            @JsonProperty("prefixes") ArrayList<String> prefixes, @JsonProperty("metaFiles") ArrayList<String> metaFiles,
            @JsonProperty("hoursInterval") Integer hoursInterval, @JsonProperty("alwaysGetMeta") boolean alwaysGetMeta) throws ParseException {
        parametersMap = new HashMap<String, Object>();
        this.user = user;
        this.pass = pass;
        this.mdListId = mdListId;
        //legacy backward compatibilty
        if (this.mdListId == null && mdListUrl != null) {
        	this.mdListId = mdListUrl.substring(mdListUrl.lastIndexOf("/") + 1);
        }
        this.daysInterval = daysInterval;
        this.hoursInterval = hoursInterval;
        this.dateTo = !"".equals(dateTo) ? dateTo : null;
        if (this.dateTo != null) {
            setDate_to(dateTo);
        }

        this.alwaysGetMeta = alwaysGetMeta;
        this.srcCharset = StringUtils.isEmpty(srcCharset) ? "UTF-8" : srcCharset;
        this.bucket = bucket;
        this.prefixes = prefixes;
        this.metaFiles = metaFiles;
        //set param map
        parametersMap.put("user", user);
        parametersMap.put("pass", pass);
        parametersMap.put("mdListId", this.mdListId);
        parametersMap.put("daysInterval", daysInterval);
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

    public int getDaysInterval() {
        return daysInterval;
    }

    public void setDaysInterval(int daysInterval) {
        this.daysInterval = daysInterval;
    }

	public String getMdListId() {
		return mdListId;
	}

	public void setMdListId(String mdListId) {
		this.mdListId = mdListId;
	}

	public String getSrcCharset() {
		return srcCharset;
	}

	public int getHoursInterval() {
		return hoursInterval;
	}

	public boolean isAlwaysGetMeta() {
		return alwaysGetMeta;
	}
	
	

	
    
}
