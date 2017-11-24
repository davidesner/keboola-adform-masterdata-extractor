/*
 */
package keboola.adform.masterdata_extractor.pojo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class MasterFile implements Comparable<MasterFile> {

	@JsonProperty("id")
	private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("setup")
    private String setup;
    @JsonProperty("size")
    private Double size;
    @JsonProperty("createdAt")
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
    public MasterFile(@JsonProperty("id") String id, @JsonProperty("setup") String setup, 
            @JsonProperty("size") Double size, @JsonProperty("createdAt") String created, @JsonProperty("name") String name) {
    	this.id = id;
        this.setup = setup;
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
    	this.id = f.getId();
        this.name = f.getName();
        this.setup = f.getSetup();
        this.size = f.getSize();
        this.created = f.getCreated();
        this.localAbsolutePath = f.localAbsolutePath;
        this.creationTime = f.getCreationTime();
        this.prefix = f.getPrefix();
    }

    private void setCreationTime(String created) {
         this.creationTime = javax.xml.bind.DatatypeConverter.parseDateTime(created).getTime();
       
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

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
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
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    public String getLocalAbsolutePath() {
		return localAbsolutePath;
	}

	public void setLocalAbsolutePath(String localAbsolutePath) {
		this.localAbsolutePath = localAbsolutePath;
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
