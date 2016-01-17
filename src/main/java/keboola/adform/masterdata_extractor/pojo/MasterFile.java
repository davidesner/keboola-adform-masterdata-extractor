/*
 */
package keboola.adform.masterdata_extractor.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class MasterFile implements Comparable<MasterFile> {

    @JsonProperty("name")
    private String name;
    @JsonProperty("path")
    private String path;
    @JsonProperty("absolutePath")
    private String absolutePath;
    @JsonProperty("size")
    private Double size;
    @JsonProperty("created")
    private String created;
    @JsonIgnore
    private Date creationTime;
    @JsonIgnore
    private final String prefix;
    @JsonIgnore
    private String localAbsolutePath;

    /**
     * Helper constructor to perform binary search in list
     *
     * @param date - date to search
     */
    public MasterFile(Date date) {
        creationTime = date;
        prefix = "";
    }

    @JsonCreator
    public MasterFile(@JsonProperty("path") String path, @JsonProperty("absolutePath") String absolutePath,
            @JsonProperty("size") Double size, @JsonProperty("created") String created, @JsonProperty("name") String name) {
        this.path = path;
        this.absolutePath = absolutePath;
        this.size = size;
        this.created = created;
        this.name = name;
        setCreationTime(created);

        //set prefic of the file
        int i = name.indexOf("_");
        if (i > 0) {
            this.prefix = name.substring(0, i);
        } else {
            i = name.indexOf(".");
            if (i > 0) {
                this.prefix = name.substring(0, i);
            } else {
                this.prefix = "";
            }
        }
    }

    public MasterFile(MasterFile f) {
        this.name = f.getName();
        this.path = f.getPath();
        this.absolutePath = f.getAbsolutePath();
        this.size = f.getSize();
        this.created = f.getCreated();
        this.creationTime = f.getCreationTime();
        this.prefix = f.getPrefix();
        this.localAbsolutePath = f.getLocalAbsolutePath();
    }

    public String getLocalAbsolutePath() {
        return localAbsolutePath;
    }

    public void setLocalAbsolutePath(String localAbsolutePath) {
        this.localAbsolutePath = localAbsolutePath;
    }

    private void setCreationTime(String created) {
        try {

            SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.ENGLISH);
            this.creationTime = format.parse(created);
        } catch (ParseException ex) {
            Logger.getLogger(MasterFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    //TODO: implement function removeFilesBefore date
    public void removeFilesBefore(Date dateTime) {
    }

    @Override
    public int compareTo(MasterFile file2) {
        if (this.getCreationTime().before(file2.getCreationTime())) {
            return 1;
        } else {
            return -1;
        }
    }
}
