package eu.europa.ec.eurostat.los.codes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.opencsv.CSVReader;

public class CodelistMaker {
	private static Logger logger = LogManager.getLogger(CodelistMaker.class);
	private static final String NUTS = "nuts";

	private static final String[] NUTS_CSV = { "src/main/resources/data/tourism-nuts-nace-r2-fr.csv",
												"src/main/resources/data/tourism-degurba-fr.csv",
												"src/main/resources/data/tourism-partner-fr.csv",
												"src/main/resources/data/tourism-terrtypo-fr.csv"
											};

	private static final String RDF_DIRECTORY = "src/main/resources/rdf/cl-";

	private static Workbook wb = null;

	private final static String EXCEL_FILE_NAME = "src/main/resources/data/tourism-fr-dsd-1.xls";

	private final static String BASE_URI_NUTS = "http://ec.europa.eu/nuts/";
	private final static String BASE_URI_NACE_R2 = "http://id.insee.fr/codes/nafr2/groupes/";

	public static void main(String[] args) throws IOException {
		wb = new HSSFWorkbook(new FileInputStream(EXCEL_FILE_NAME));

		generateDsdCodeListForPartner();
		generateDsdCodeListForNaceR2();

		Iterator<Sheet> sheetsIterator = wb.iterator();
		while (sheetsIterator.hasNext()) {
			Sheet sheet = (Sheet) sheetsIterator.next();
			String clTag = normalize(sheet.getSheetName().trim().toLowerCase());
			if (!StringUtils.containsIgnoreCase(clTag, "Scope") && !StringUtils.containsIgnoreCase(clTag, "DSD")
					&& sheet.getRow(0).getLastCellNum() == 2) {
				logger.info(String.format("import code list normal (feuille %s - nb cell %d)", sheet.getSheetName(),
						sheet.getRow(0).getLastCellNum()));
				Model codelistModel = createMultipleLevelsConceptScheme(clTag, sheet, 1);
				RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + clTag + ".ttl"), codelistModel, Lang.TURTLE);
			}
		}
		importNuts();
	}

	private static void generateDsdCodeListForPartner() throws FileNotFoundException {
		Sheet partnerSheet = wb.getSheet("PARTNER");
		String clTag = "partner";
		Model codelistModel = createMultipleLevelsConceptScheme(clTag, partnerSheet, 3);
		RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + clTag + ".ttl"), codelistModel, Lang.TURTLE);
	}

	private static void generateDsdCodeListForNaceR2() throws FileNotFoundException {
		Sheet sheet = wb.getSheet("NACE_R2");
		String clTag = "nace_r2";
		Model codelistModel = createMultipleLevelsConceptScheme(clTag, sheet, 2);
		// Compl�ments
		codelistModel.setNsPrefix("foaf", FOAF.getURI());
		for (ResIterator iterator = codelistModel.listResourcesWithProperty(SKOS.broader); iterator.hasNext();) {
			Resource item = iterator.next();
			String groupe = item.getProperty(SKOS.notation).getLiteral().getString().substring(1);
			groupe = groupe.substring(0, 2) + "." + groupe.substring(2, 3);
			item.addProperty(FOAF.focus, BASE_URI_NACE_R2 + groupe.toLowerCase());
		}

		RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + clTag + ".ttl"), codelistModel, Lang.TURTLE);
	}

	private static void importNuts() throws FileNotFoundException {
		logger.info("importNuts");
		Set<String> nuts = new HashSet<>();
		for (String nutFilename : NUTS_CSV) {
			nuts.addAll(readNutsFromFile(nutFilename));
		}
		Model codelistModel = createNutsConceptScheme(nuts);
		RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY + "nuts.ttl"), codelistModel, Lang.TURTLE);

	}

	private static Set<String> readNutsFromFile(String nomFichier) {
		Set<String> nuts = new HashSet<>();
		try (FileReader fileReader = new FileReader(nomFichier)) {
			CSVReader csvReader = new CSVReader(fileReader);
			nuts = csvReader.readAll().stream().skip(1).map(line -> line[2]).collect(Collectors.toSet());
			csvReader.close();
		} catch (IOException e) {
			logger.error(e);
		}
		return nuts;
	}

	private static Model createNutsConceptScheme(Set<String> nutsValues) {
		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdfs", RDFS.getURI());
		clModel.setNsPrefix("owl", OWL.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());
		clModel.setNsPrefix("foaf", FOAF.getURI());
		clModel.setNsPrefix("los-codes", Configuration.CODES_BASE_URI);
		clModel.setNsPrefix("los-concepts", Configuration.CONCEPTS_BASE_URI);

		// Codelist code and name should be on the first line
		// Create the concept scheme and the associated concept
		String conceptName = "NUTS";
		String clURI = Configuration.CODES_BASE_URI + NUTS;

		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(NUTS));

		Resource concept = clModel.createResource(Configuration.CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName),
				OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
		concept.addProperty(SKOS.prefLabel, clModel.createLiteral(NUTS, "en"));
		concept.addProperty(SKOS.notation, clModel.createLiteral(NUTS, "en"));
		scheme.addProperty(RDFS.seeAlso, concept);
		concept.addProperty(RDFS.seeAlso, scheme);

		nutsValues.stream().forEach(nut -> addResourceForNutsValue(nut, clModel, scheme, concept));

		return clModel;
	}

	private static void addResourceForNutsValue(String itemCode, Model clModel, Resource scheme, Resource concept) {
		String parentURI = Configuration.CODES_BASE_URI + NUTS + "/" + itemCode;
		Resource item = clModel.createResource(parentURI, concept);
		item.addProperty(RDF.type, SKOS.Concept);
		item.addProperty(SKOS.notation, itemCode);
		item.addProperty(SKOS.inScheme, scheme);
		item.addProperty(FOAF.focus, BASE_URI_NUTS + itemCode.toLowerCase());
	}

	public static Model createMultipleLevelsConceptScheme(String clTag, Sheet clSheet, int numberOfLevels) {

		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdfs", RDFS.getURI());
		clModel.setNsPrefix("owl", OWL.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());
		clModel.setNsPrefix("los-codes", Configuration.CODES_BASE_URI);
		clModel.setNsPrefix("los-concepts", Configuration.CONCEPTS_BASE_URI);

		String clURI = Configuration.CODES_BASE_URI + clTag;

		// Codelist code and name should be on the first line
		// Create the concept scheme and the associated concept for the first level
		String conceptName = normalize(clTag);
		String clName = clTag + " label";

		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, clName, "en");
		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));

		Resource concept = clModel.createResource(Configuration.CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName),
				OWL.Class);
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
			while (currentRow.getCell(level * 2) == null || currentRow.getCell(level * 2).toString().trim().isEmpty()) {
				level++;
			}

			String itemCode = currentRow.getCell(level * 2).toString().trim();
			String currentUri = clURI + "/" + itemCode;
			Resource newResource = clModel.createResource(currentUri, concept);
			newResource.addProperty(RDF.type, SKOS.Concept);
			newResource.addProperty(SKOS.notation, itemCode);
			if (currentRow.getCell(level * 2 + 1) != null) {
				String itemName = currentRow.getCell(level * 2 + 1).toString().trim();
				newResource.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "en"));
			}
			newResource.addProperty(SKOS.inScheme, scheme);

			if (level > 0) {
				newResource.addProperty(SKOS.broader, parentUriByLevel.get(level - 1));
				itemParentByLevel.get(level - 1).addProperty(SKOS.narrower, currentUri);
			} else if (numberOfLevels > 1) {
				newResource.addProperty(SKOS.topConceptOf, concept);
			}

			parentUriByLevel.add(level, currentUri);
			itemParentByLevel.add(level, newResource);

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
