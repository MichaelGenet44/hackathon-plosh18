package eu.europa.ec.eurostat.los.codes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class CodelistMaker {

	private static final String RDF_DIRECTORY = "src/main/resources/rdf/cl-";

	private static Workbook wb = null;

	public final static String EXCEL_FILE_NAME = "src/main/resources/data/tourism-fr-dsd-1.xls";
	public final static String BASE_URI = "http://id.linked-open-statistics.org/";
	public final static String CODES_BASE_URI = BASE_URI + "codes/";
	public final static String CONCEPTS_BASE_URI = BASE_URI + "concepts/";

	public static void main(String[] args) throws IOException {

		wb = new HSSFWorkbook(new FileInputStream(EXCEL_FILE_NAME));

		generateDsdCodeListForPartner();
		generateDsdCodeListForNaceR2();
	}

	
	private static void generateDsdCodeListForPartner() throws FileNotFoundException {
		Sheet partnerSheet = wb.getSheet("PARTNER");
		String clTag="partner";
		Model codelistModel = createMultipleLevelsConceptScheme(clTag, partnerSheet, 3);
		RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + clTag + ".ttl"), codelistModel, Lang.TURTLE);
	}

	private static void generateDsdCodeListForNaceR2() throws FileNotFoundException {
		Sheet sheet = wb.getSheet("NACE_R2");
		String clTag="nace_r2";
		Model codelistModel = createMultipleLevelsConceptScheme(clTag, sheet, 2);
		RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + clTag + ".ttl"), codelistModel, Lang.TURTLE);
	}


	
	public static Model createMultipleLevelsConceptScheme(String clTag, Sheet clSheet, int numberOfLevels) {

		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdfs", RDFS.getURI());
		clModel.setNsPrefix("owl", OWL.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());
		clModel.setNsPrefix("los-codes", CODES_BASE_URI);
		clModel.setNsPrefix("los-concepts", CONCEPTS_BASE_URI);

		String clURI = CODES_BASE_URI + clTag;
		
		// Codelist code and name should be on the first line
		// Create the concept scheme and the associated concept for the first level
		String conceptName = normalize(clTag);
		String clName = clTag + " label";

		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, clName, "en");
		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
		
		Resource concept = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
		concept.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "en"));
		concept.addProperty(SKOS.notation, clModel.createLiteral(clTag, "en"));
		scheme.addProperty(RDFS.seeAlso, concept);
		concept.addProperty(RDFS.seeAlso, scheme);

		
			
		// Iterate through lines (skipping the first one) to add the codes
		Iterator<Row> rowIterator = clSheet.rowIterator();
		rowIterator.next();
		List<String> parentUriByLevel = new ArrayList<>(numberOfLevels);
		List<Resource> itemParentByLevel = new ArrayList<>(numberOfLevels);
		
		while (rowIterator.hasNext()) {
			Row currentRow = rowIterator.next();
			
			int level = 0;
			while (currentRow.getCell(level * 2).toString().trim().isEmpty()) {
				level++;
			}
			
			String itemCode = currentRow.getCell(level * 2).toString().trim();
			String itemName = currentRow.getCell(level * 2 + 1).toString().trim();
			String currentUri = clURI + "/" + itemCode;
			Resource newResource = clModel.createResource(currentUri, concept);
			newResource.addProperty(RDF.type, SKOS.Concept);
			newResource.addProperty(SKOS.notation, itemCode);
			newResource.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
			newResource.addProperty(SKOS.inScheme, scheme);
			
			if (level > 0) {
				newResource.addProperty(SKOS.broader, parentUriByLevel.get(level - 1));
				itemParentByLevel.get(level - 1).addProperty(SKOS.narrower, currentUri);
			}
			else {
				newResource.addProperty(SKOS.topConceptOf, concept);
			}
			
			parentUriByLevel.add(level, currentUri);
			itemParentByLevel.add(level, newResource);
		}

		return clModel;
	}
	
		
//	public static Model createTwoLevelsConceptScheme(String clTag, Sheet clSheet) {
//
//		Model clModel = ModelFactory.createDefaultModel();
//		clModel.setNsPrefix("rdfs", RDFS.getURI());
//		clModel.setNsPrefix("owl", OWL.getURI());
//		clModel.setNsPrefix("skos", SKOS.getURI());
//		clModel.setNsPrefix("los-codes", CODES_BASE_URI);
//		clModel.setNsPrefix("los-concepts", CONCEPTS_BASE_URI);
//
//		String clURI = CODES_BASE_URI + clTag;
//		
//		// Codelist code and name should be on the first line
//		// Create the concept scheme and the associated concept for the first level
//		String conceptName = normalize(clTag);
//		String clName = clTag + " label";
//
//		Resource schemeLevel1 = clModel.createResource(clURI, SKOS.ConceptScheme);
//		schemeLevel1.addProperty(SKOS.prefLabel, clName, "en");
//		schemeLevel1.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
//		
//		Resource conceptLevel1 = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
//		conceptLevel1.addProperty(RDF.type, RDFS.Class);
//		conceptLevel1.addProperty(RDFS.subClassOf, SKOS.Concept);
//		conceptLevel1.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "en"));
//		conceptLevel1.addProperty(SKOS.notation, clModel.createLiteral(clTag, "en"));
//		schemeLevel1.addProperty(RDFS.seeAlso, conceptLevel1);
//		conceptLevel1.addProperty(RDFS.seeAlso, schemeLevel1);
//
//		
//			
//		// Iterate through lines (skipping the first one) to add the codes
//		Iterator<Row> rowIterator = clSheet.rowIterator();
//		rowIterator.next();
//		String parentURI = null;
//		Resource itemParent = null;
//		while (rowIterator.hasNext()) {
//			Row currentRow = rowIterator.next();
//			String itemCode = currentRow.getCell(0).toString().trim();
//			// Si première colonne vide : on est sur un level 1
//			if (!itemCode.isEmpty()) {
//				String itemName = currentRow.getCell(1).toString().trim();
//				parentURI = clURI + "/" + itemCode;
//				itemParent = clModel.createResource(parentURI, conceptLevel1);
//				itemParent.addProperty(RDF.type, SKOS.Concept);
//				itemParent.addProperty(SKOS.notation, itemCode);
//				itemParent.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
//				itemParent.addProperty(SKOS.inScheme, schemeLevel1);
//			}
//			else {
//				// Sinon on est sur un level 2
//				itemCode = currentRow.getCell(2).toString().trim();
//				String itemName = currentRow.getCell(3).toString().trim();
//				String childUri = clURI + "/" + itemCode;
//				Resource item = clModel.createResource(clURI + "/" + itemCode, conceptLevel1);
//				item.addProperty(RDF.type, SKOS.Concept);
//				item.addProperty(SKOS.notation, itemCode);
//				item.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
//				item.addProperty(SKOS.inScheme, schemeLevel1);
//				item.addProperty(SKOS.broader, parentURI);
//				itemParent.addProperty(SKOS.narrower, childUri);
//			}
//		}
//
//		return clModel;
//	}
//	
//	
//	public static Model createPartnerConceptScheme(String clTag, Sheet clSheet) {
//
//		Model clModel = ModelFactory.createDefaultModel();
//		clModel.setNsPrefix("rdfs", RDFS.getURI());
//		clModel.setNsPrefix("owl", OWL.getURI());
//		clModel.setNsPrefix("skos", SKOS.getURI());
//		clModel.setNsPrefix("los-codes", CODES_BASE_URI);
//		clModel.setNsPrefix("los-concepts", CONCEPTS_BASE_URI);
//
//		// Codelist code and name should be on the first line
//		// Create the concept scheme and the associated concept
//		String conceptName = normalize(clSheet.getRow(0).getCell(0).toString());
//		String clName = clSheet.getRow(0).getCell(1).toString();
//		String clURI = CODES_BASE_URI + clTag;
//		
//		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
//		scheme.addProperty(SKOS.prefLabel, clName, "en");
//		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
//		
//		Resource concept = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
//		concept.addProperty(RDF.type, RDFS.Class);
//		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
//		concept.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "en"));
//		concept.addProperty(SKOS.notation, clModel.createLiteral(clTag, "en"));
//		scheme.addProperty(RDFS.seeAlso, concept);
//		concept.addProperty(RDFS.seeAlso, scheme);
//
//		
//		// Create the concept scheme and the associated concept for the second level
//		conceptName = normalize(clSheet.getRow(0).getCell(2).toString());
//		clName = clSheet.getRow(0).getCell(3).toString();
//		
//		Resource schemeLevel2 = clModel.createResource(clURI, SKOS.ConceptScheme);
//		schemeLevel2.addProperty(SKOS.prefLabel, clName, "en");
//		schemeLevel2.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
//		
//		Resource conceptLevel2 = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
//		conceptLevel2.addProperty(RDF.type, RDFS.Class);
//		conceptLevel2.addProperty(RDFS.subClassOf, SKOS.Concept);
//		conceptLevel2.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "en"));
//		conceptLevel2.addProperty(SKOS.notation, clModel.createLiteral(clTag, "en"));
//		schemeLevel2.addProperty(RDFS.seeAlso, conceptLevel2);
//		conceptLevel2.addProperty(RDFS.seeAlso, schemeLevel2);
//		
//		
//		// Iterate through lines (skipping the first one) to add the codes
//		Iterator<Row> rowIterator = clSheet.rowIterator();
//		rowIterator.next();
//		String parentURI = null;
//		while (rowIterator.hasNext()) {
//			Row currentRow = rowIterator.next();
//			String itemCode = currentRow.getCell(0).toString().trim();
//			String itemName = currentRow.getCell(1).toString().trim();
//			if (!itemCode.isEmpty()) {
//				parentURI = clURI + "/" + itemCode;
//				Resource item = clModel.createResource(parentURI, concept);
//				item.addProperty(RDF.type, SKOS.Concept);
//				item.addProperty(SKOS.notation, itemCode);
//				item.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
//				item.addProperty(SKOS.inScheme, scheme);
//			}
//			else {
//				itemCode = currentRow.getCell(2).toString().trim();
//				itemName = currentRow.getCell(3).toString().trim();
//				Resource item = clModel.createResource(clURI + "/" + itemCode, conceptLevel2);
//				item.addProperty(RDF.type, SKOS.Concept);
//				item.addProperty(SKOS.notation, itemCode);
//				item.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
//				item.addProperty(SKOS.inScheme, schemeLevel2);
//				item.addProperty(SKOS.broader, parentURI);
//			}
//		}
//
//		return clModel;
//	}
//	
	
	public static Model createConceptScheme(String clTag, Sheet clSheet) {

		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdfs", RDFS.getURI());
		clModel.setNsPrefix("owl", OWL.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());
		clModel.setNsPrefix("los-codes", CODES_BASE_URI);
		clModel.setNsPrefix("los-concepts", CONCEPTS_BASE_URI);

		// Codelist code and name should be on the first line
		// Create the concept scheme and the associated concept
		String conceptName = normalize(clSheet.getRow(0).getCell(0).toString());
		String clName = clSheet.getRow(0).getCell(1).toString();
		String clURI = CODES_BASE_URI + clTag;
		
		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, clName, "fr");
		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
		
		Resource concept = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
		concept.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "fr"));
		concept.addProperty(SKOS.notation, clModel.createLiteral(clTag, "fr"));
		scheme.addProperty(RDFS.seeAlso, concept);
		concept.addProperty(RDFS.seeAlso, scheme);

		// Iterate through lines (skipping the first one) to add the codes
		Iterator<Row> rowIterator = clSheet.rowIterator();
		rowIterator.next();
		while (rowIterator.hasNext()) {
			Row currentRow = rowIterator.next();
			String itemCode = currentRow.getCell(0).toString().trim();
			String itemName = currentRow.getCell(1).toString().trim();
			Resource item = clModel.createResource(clURI + "/" + itemCode, concept);
			item.addProperty(RDF.type, SKOS.Concept);
			item.addProperty(SKOS.notation, itemCode);
			item.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "fr"));
			item.addProperty(SKOS.inScheme, scheme);
		}

		return clModel;
	}

	private static String normalize(String original) {

		String normalForm = Normalizer.normalize(original, Normalizer.Form.NFD);
		normalForm = normalForm.replaceAll(" ", "-");
		normalForm = normalForm.replaceAll("[^\\p{ASCII}]", "");

		return normalForm;
	}
}
