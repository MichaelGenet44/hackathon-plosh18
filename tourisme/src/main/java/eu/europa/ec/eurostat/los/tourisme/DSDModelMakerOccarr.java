package eu.europa.ec.eurostat.los.tourisme;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;

/**
 * The <code>DSDModelMaker</code> class creates the Data Cube Data Structure Definition for the POP5 data set.
 * 
 * @author Franck
 */
public class DSDModelMakerOccarr {

	private static final String MEASURE_DESCRIPTION = "An arrival is defined as a person (tourist) who arrives at a tourist accommodation establishment and checks in or arrives at non-rented accommodation. But in the scope of the Regulation concerning European statistics on tourism, this variable is not collected for the latter type of accommodation.\r\n" + 
			"\r\n" + 
			"Statistically there is not much difference if, instead of arrivals, departures are counted. No age limit is applied: children are counted as well as adults, even in the case when the overnight stays of children might be free of charge. Arrivals are registered by country of residence of the guest and by month. The arrivals of same-day visitors spending only a few hours during the day (no overnight stay, the date of arrival and departure are the same) at the establishment are excluded from accommodation statistics.";

	private static Logger logger = LogManager.getLogger(DSDModelMakerOccarr.class);

	private static Workbook wb = null;
	
	/** Names of the Excel files containing the TOURISM data and metadata
	 * The source file is at https://github.com/LOS-ESSnet/Paris-Hackathon/blob/master/data/tourism-fr.md */
	public final static String TOURISM_DATA_FILE_NAME = "src/main/resources/data/tourism-nuts-nace-r2-fr.csv";
	public final static String TOURISM_METADATA_FILE_NAME = "src/main/resources/data/tourism-fr-dsd-1.xls";
	
	/** Prefix mappings */
	public static Map<String, String> DSD_PREFIXES = new HashMap<String, String>();
	static {
		DSD_PREFIXES.put("qb", DataCubeOntology.getURI());
		DSD_PREFIXES.put("rdfs", RDFS.getURI());
		DSD_PREFIXES.put("dc", DC.getURI());		
		DSD_PREFIXES.put("dc", DCTerms.getURI());		
		DSD_PREFIXES.put("xsd", XSD.getURI());
	}
	
	private static Map<String, String> CONCEPT_SCHEME_PREFIXES = new HashMap<String, String>();
	static {
		CONCEPT_SCHEME_PREFIXES.put("rdfs", RDFS.getURI());
		CONCEPT_SCHEME_PREFIXES.put("owl", OWL.getURI());
		CONCEPT_SCHEME_PREFIXES.put("skos", SKOS.getURI());
	}


	private static final String BASE_URI = "http://id.linked-open-statistics.org/plosh/temp5/meta/";
	private static final String TOURISME_URI = BASE_URI + "demo/tourism/";

	public static final String POP_MEASURE_ID = "TOURISM_OCCARR";
	public static final String POP_MEASURE_URI = "http://id.insee.fr/meta/mesure/tourism_occarr";
	public static final String POP_MEASURE_NAME = "Arrivals of residents and non-residents";



	/** Naming constants and methods for other components */
	public static String conceptSchemeURI(String conceptCode, String conceptName) {
		return "http://id.linked-open-statistics.org/codes/" + conceptName;
	}

	public static String codeConceptURI(String conceptCode, String conceptName) {
		return "http://id.linked-open-statistics.org/concepts/" + StringUtils.capitalize(conceptName);
	}

	// A very basic implementation for now
	public static String conceptURI(String conceptCode) {
		return "http://id.linked-open-statistics.org/concepts/" + conceptCode;
	}

	public static String componentURI(String componentType, String conceptCode) { // Type should be 'attribute', 'dimension' or 'measure' but no control is made
		return "http://id.insee.fr/meta/" + componentType + "/" + conceptCode.toLowerCase();
	}

	public static String dsdURI(String dsdId) {
		return TOURISME_URI + "dsd/" + dsdId;	
	}
	
	
	

	public static void main(String[] args) throws Exception {
		wb = new HSSFWorkbook(new FileInputStream(TOURISM_METADATA_FILE_NAME));

//		String dsdName = "nuts-nacer2-occarr";
//		String sheetName = "DSD-tourism_nuts_nace_r2";
		String dsdName = "degurba-occarr";
		String sheetName = "DSD-tourism-degurba";
//		String dsdName = "terrtypo-occarr";
//		String sheetName = "DSD-tourism-terrtypo";
//		String dsdName = "partner-occarr";
//		String sheetName = "DSD-tourism-partner";
		
		Model tourismeNutsNacer2 = getTourismeNutsNacer2Model(sheetName,dsdName);
		RDFDataMgr.write(new FileOutputStream("src/main/resources/rdf/dsd-tourism-"+dsdName+".ttl"), tourismeNutsNacer2, Lang.TURTLE);
	}

	
	

	/**
	 * Returns a Jena Model containing the DSD and its components (including concepts and code lists).
	 * 
	 * @return The Data Cube data structure definition as a Jena model.
	 */
	public static Model getTourismeNutsNacer2Model(String sheetName,String dsdName) { // TODO Distinguish COM and/or ARM ?
		Model tourismeNutsNacer2Model = ModelFactory.createDefaultModel();
		tourismeNutsNacer2Model.setNsPrefixes(DSD_PREFIXES);

		// Creation of the DSD
		Resource tourisme = tourismeNutsNacer2Model.createResource(dsdURI(dsdName), DataCubeOntology.DataStructureDefinition);
		tourisme.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral("Tourism industries  -  Annual occupancy of tourist accommodation establishments - Nights spent by residents and non-residents", "en"));
		tourisme.addProperty(DC.description, tourismeNutsNacer2Model.createLiteral("Nights by NUTS, NACE_R2 and Country of residence", "en"));
		tourisme.addProperty(DCTerms.identifier, tourismeNutsNacer2Model.createLiteral("DSD-TOURISM-"+dsdName.toUpperCase(), "fr"));
		logger.info("Creating DSD " + tourisme.getURI());

		Sheet feuilleDSD = wb.getSheet(sheetName);
		Iterator<Row> rowIterator = feuilleDSD.rowIterator();
		rowIterator.next();
		rowIterator.next();
		while (rowIterator.hasNext()) {
			Row currentRow = rowIterator.next();
			String conceptCode = currentRow.getCell(0).toString().trim();
			String conceptName = conceptCode.toLowerCase();
			String role = currentRow.getCell(2).toString().trim();
			String conceptAlreadyExisting = currentRow.getCell(3).toString().trim();
			
			// ??? String sdmxBroaderConcept = Configuration.getSDMXBroaderConcept(conceptCode);
			
			// Create the dimension property
			if ("Dimension".equals(role)) {
				if (conceptAlreadyExisting.isEmpty()) {
					Resource concept = tourismeNutsNacer2Model.createResource(conceptURI(conceptCode), OWL.Class);
					concept.addProperty(RDF.type, RDFS.Class);
					concept.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(conceptName, "fr"));
					conceptAlreadyExisting = conceptURI(conceptCode);
				}
				
				Resource dimensionProperty = tourismeNutsNacer2Model.createResource(componentURI("dimension", conceptCode), DataCubeOntology.DimensionProperty).addProperty(RDF.type, DataCubeOntology.CodedProperty);
				dimensionProperty.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(conceptName, "fr"));
				dimensionProperty.addProperty(DataCubeOntology.concept, tourismeNutsNacer2Model.createResource(conceptAlreadyExisting));
				dimensionProperty.addProperty(DCTerms.identifier, tourismeNutsNacer2Model.createLiteral(conceptCode, "fr"));
				dimensionProperty.addProperty(RDFS.range, tourismeNutsNacer2Model.createResource(codeConceptURI(conceptCode, conceptName)));
				dimensionProperty.addProperty(DataCubeOntology.codeList, tourismeNutsNacer2Model.createResource(conceptSchemeURI(conceptCode, conceptName)));
				// Attach the dimension property to the DSD through anonymous ComponentSpecification
				tourisme.addProperty(DataCubeOntology.component, tourismeNutsNacer2Model.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.dimension, dimensionProperty));
			}
			
			
			if ("Observation attribute".equals(role)) {
				if (conceptAlreadyExisting.isEmpty()) {
					Resource concept = tourismeNutsNacer2Model.createResource(conceptURI(conceptCode), OWL.Class);
					concept.addProperty(RDF.type, RDFS.Class);
					concept.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(conceptName, "fr"));
					conceptAlreadyExisting = conceptURI(conceptCode);
				}
				
				Resource attributeProperty = tourismeNutsNacer2Model.createResource(componentURI("attribute", conceptCode), DataCubeOntology.AttributeProperty).addProperty(RDF.type, DataCubeOntology.CodedProperty);
				attributeProperty.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(conceptName, "fr"));
				attributeProperty.addProperty(DataCubeOntology.concept, tourismeNutsNacer2Model.createResource(conceptAlreadyExisting));
				attributeProperty.addProperty(DCTerms.identifier, tourismeNutsNacer2Model.createLiteral(conceptCode, "fr"));
				attributeProperty.addProperty(RDFS.range, tourismeNutsNacer2Model.createResource(codeConceptURI(conceptCode, conceptName)));
				attributeProperty.addProperty(DataCubeOntology.codeList, tourismeNutsNacer2Model.createResource(conceptSchemeURI(conceptCode, conceptName)));
				// Attach the dimension property to the DSD through anonymous ComponentSpecification
				tourisme.addProperty(DataCubeOntology.component, tourismeNutsNacer2Model.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.attribute, attributeProperty).addProperty(DataCubeOntology.componentAttachment, DataCubeOntology.Observation));
			}

		}

		// Create and attach the measure property
		Resource concept = tourismeNutsNacer2Model.createResource(conceptURI(POP_MEASURE_ID), OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(POP_MEASURE_NAME, "en"));
		concept.addProperty(DC.description, tourismeNutsNacer2Model.createLiteral(MEASURE_DESCRIPTION, "en"));
		
		
		Resource measureProperty = tourismeNutsNacer2Model.createResource(POP_MEASURE_URI, DataCubeOntology.MeasureProperty); // The component is not coded
		//measureProperty.addProperty(RDFS.subPropertyOf, tourismeNutsNacer2Model.createResource(SDMX_OBS_VALUE_MEASURE_URI));
		measureProperty.addProperty(RDFS.label, tourismeNutsNacer2Model.createLiteral(POP_MEASURE_NAME, "en"));
		measureProperty.addProperty(DCTerms.identifier, tourismeNutsNacer2Model.createLiteral(POP_MEASURE_ID, "en"));
		measureProperty.addProperty(DataCubeOntology.concept, tourismeNutsNacer2Model.createResource(conceptURI(POP_MEASURE_ID)));
		measureProperty.addProperty(RDFS.range, XSD.xint);
		tourisme.addProperty(DataCubeOntology.component, tourismeNutsNacer2Model.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.measure, measureProperty));

		
		Resource dimensionProperty = tourismeNutsNacer2Model.createResource(DataCubeOntology.DimensionProperty)
				.addProperty(RDF.type, "http://purl.org/linked-data/sdmx/2009/dimension#timePeriod");
		// Attach the dimension property to the DSD through anonymous ComponentSpecification
		tourisme.addProperty(DataCubeOntology.component, tourismeNutsNacer2Model.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.dimension, dimensionProperty));
		
		return tourismeNutsNacer2Model;
	}
}
