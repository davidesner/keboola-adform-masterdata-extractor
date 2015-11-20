/*
 */
package keboola.adform.masterdata_extractor.pojo;

import java.util.Comparator;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class MasterFileListComparator implements Comparator<MasterFile> {

    public int compare(MasterFile o1, MasterFile o2) {
        return o1.compareTo(o2);
    }
}
