package eu.europa.ec.eurostat.los.codes;

public class Configuration {

	public final static String BASE_URI = "http://id.linked-open-statistics.org/";
	public final static String CODES_BASE_URI = BASE_URI + "codes/";
	public final static String CONCEPTS_BASE_URI = BASE_URI + "concepts/";

	public static String componentURI(String componentType, String conceptCode) { // Type should be 'attribute', 'dimension' or 'measure' but no control is made
		return  BASE_URI + "meta/" + componentType + "/" + conceptCode.toLowerCase();
	}
	
	/** Naming constants and methods for other components */
	public static String codeItemURI(String conceptCode, String itemCode) {
		return  BASE_URI + "codes/" + conceptCode.toLowerCase() + "/" + itemCode;
	}
	
	public static String observationURI(String params) {
		return BASE_URI + "observation/" + params;	
	}
}
