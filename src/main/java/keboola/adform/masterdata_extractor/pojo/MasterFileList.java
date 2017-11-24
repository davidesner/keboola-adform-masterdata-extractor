/*
 */
package keboola.adform.masterdata_extractor.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class MasterFileList {
    @JsonProperty("files")
    private List<MasterFile> files;

    @JsonCreator
    public MasterFileList(@JsonProperty("files") List<MasterFile> files) {
        this.files = files;
        Collections.sort(files);
    }

    /**
     * Returns sublist of files created after(>=) given date
     *
     * @param date - the boundary date
     * @param filePrefix - prefix of files to retrieve
     * @return
     */
    public List<MasterFile> getFilesSince(Date date, String filePrefix) {
        int startIndex = Collections.binarySearch(files, new MasterFile(date), new MasterFileListComparator());
        if (startIndex < 0) {
            startIndex = -startIndex - 1;
        }

        try {
            return getSublistByPrefix(filePrefix, files.subList(0, startIndex));

        } catch (Exception ex) {

            return null;
        }
    }

    /**
     * Returns sublist of files created in range: <date_from,date_to)
     * @param date
     *
     * _fro
     *
     * m
     * @param date_to
     * @param filePrefix
     * @return
     */
    public List<MasterFile> getFilesSince(Date date_from, Date date_to, String filePrefix) {
        int startIndex = Collections.binarySearch(files, new MasterFile(date_from), new MasterFileListComparator());
        int endIndex = Collections.binarySearch(files, new MasterFile(date_to), new MasterFileListComparator());
        if (startIndex < 0) {
            startIndex = -startIndex - 1;
        }
        if (endIndex < 0) {
            endIndex = -endIndex - 1;
        } else {
            //if the value is found,exclude
            endIndex++;
        }
        try {
            return getSublistByPrefix(filePrefix, files.subList(endIndex, (startIndex)));

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    /**
     * Retrieves sublist of files matching given prefix. (Case sensitive)
     *
     * @param prefix - searched prefix
     * @param search - List of MasterFile objects to search in
     * @return
     */
    private List<MasterFile> getSublistByPrefix(String prefix, List<MasterFile> search) {
        List<MasterFile> list = new ArrayList<MasterFile>();
        for (MasterFile file : search) {
            if (file.getPrefix().equals(prefix)) {
                list.add(file);
            }
        }
        return list;
    }

    public List<MasterFile> getFiles() {
        return files;
    }

    public void setFiles(List<MasterFile> files) {
        this.files = files;
    }
}
