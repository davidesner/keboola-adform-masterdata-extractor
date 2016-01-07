/*
 */
package keboola.adform.masterdata_extractor;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class ExtractorException extends Exception {

    int severity;

    public ExtractorException(String message) {
        super(message);
        severity = 2;
    }

    public ExtractorException(String message, int severity) {
        super(message);
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

}
