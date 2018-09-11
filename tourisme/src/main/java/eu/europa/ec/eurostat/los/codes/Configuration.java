package eu.europa.ec.eurostat.los.codes;

public class Configuration {

	public final static String BASE_URI = "http://id.linked-open-statistics.org/";

	public static String componentURI(String componentType, String conceptCode) { // Type should be 'attribute', 'dimension' or 'measure' but no control is made
		return "http://id.insee.fr/meta/" + componentType + "/" + conceptCode.toLowerCase();
	}
	
	/** Naming constants and methods for other components */
	public static String codeItemURI(String conceptCode, String itemCode) {
		return "http://id.insee.fr/codes/" + conceptCode.toLowerCase() + "/" + itemCode;
	}
	
	public static String observationURI(String params) {
		return BASE_URI + "observation/" + params;	
	}
}
