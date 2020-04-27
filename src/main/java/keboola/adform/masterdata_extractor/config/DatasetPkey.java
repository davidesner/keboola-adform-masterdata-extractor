/*
 */
package keboola.adform.masterdata_extractor.config;

import java.text.ParseException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class DatasetPkey {

	public enum ReportType {
		Click, Impression, Trackingpoint, Event
	}

	private final String dataset;

	private final String[] pkey;

	public DatasetPkey(@JsonProperty("dataset") String dataset, @JsonProperty("pkey") String[] pkey)
			throws ParseException {

		this.pkey = pkey;
		this.dataset = dataset;

	}

	private boolean isValidType(String type) {
		for (ReportType c : ReportType.values()) {
			if (c.name().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public String getDataset() {
		return dataset;
	}

	public String[] getPkey() {
		return pkey;
	}

}
