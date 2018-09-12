package eu.europa.ec.eurostat.los.codes;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReaderHeaderAware;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;

public class DataCubeMaker {
	private static Logger logger = LogManager.getLogger(DataCubeMaker.class);

	private static final String RDF_DIRECTORY_DS = "src/main/resources/rdf/ds-";

	public static void main(String[] args) throws IOException {

		traiterUneMeasure("nuts-nacer2", "occ_arr", "src/main/resources/data/tourism-nuts-nace-r2-fr.csv");
		traiterUneMeasure("nuts-nacer2", "occ_ni", "src/main/resources/data/tourism-nuts-nace-r2-fr.csv");

		traiterUneMeasure("degurba", "occ_arr", "src/main/resources/data/tourism-degurba-fr.csv");
		traiterUneMeasure("degurba", "occ_ni", "src/main/resources/data/tourism-degurba-fr.csv");

		traiterUneMeasure("partner", "occ_arr", "src/main/resources/data/tourism-partner-fr.csv");
		traiterUneMeasure("partner", "occ_ni", "src/main/resources/data/tourism-partner-fr.csv");

		traiterUneMeasure("terrtypo", "occ_arr", "src/main/resources/data/tourism-terrtypo-fr.csv");
		traiterUneMeasure("terrtypo", "occ_ni", "src/main/resources/data/tourism-terrtypo-fr.csv");
	}

	private static void traiterUneMeasure(String typeDsd, String measure, String csvFile) {
		logger.info(String.format("traiterUneMeasure %s %s %s",typeDsd, measure, csvFile));
		try (FileReader fileReader = new FileReader(csvFile)) {

			Model tourismDepModel = ModelFactory.createDefaultModel();
			tourismDepModel.setNsPrefix("xsd", XSD.getURI());
			tourismDepModel.setNsPrefix("qb", DataCubeOntology.getURI());

			tourismDepModel.setNsPrefix("dim", "http://id.insee.fr/meta/dimension/");
			tourismDepModel.setNsPrefix("mes", "http://id.insee.fr/meta/mesure/");
			tourismDepModel.setNsPrefix("att", "http://id.insee.fr/meta/attribute/");
			tourismDepModel.setNsPrefix("sdmxdim", "http://purl.org/linked-data/sdmx/2009/dimension#");

			String dataSetName = typeDsd + "-" + StringUtils.remove(measure, "_");
			Resource tourismDataSet = tourismDepModel.createResource(
					Configuration.BASE_URI + "dataSet/tourism-" + dataSetName, DataCubeOntology.DataSet);

			Model modelDsd = ModelFactory.createDefaultModel();
			modelDsd.read("src/main/resources/rdf/dsd-tourism-" + dataSetName + ".ttl");

			// Dimensions and measure
//			Property cresidDimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "C_RESID"));
//			Property nutsDimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "NUTS"));
//			Property nacer2DimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "NACE_R2"));
//			Property timePeriodDimensionProperty = tourismDepModel.createProperty("http://purl.org/linked-data/sdmx/2009/dimension#timePeriod");
//			Property measureProperty = tourismDepModel.createProperty(Configuration.componentURI("mesure", "MEASURE"));
//			//Property observationProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "OBS_VALUE"));
//
//			Property obsStatusAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "OBS_STATUS"));
//			Property confStatusAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "CONF_STATUS"));
//			Property unitAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "UNIT"));

			CSVReaderHeaderAware reader = new CSVReaderHeaderAware(fileReader);
			Map<String, String> nextLine;
			while ((nextLine = reader.readMap()) != null) {
				if (measure.equals(nextLine.get("MEASURE"))) {
					// traiterLigneArrivee(nextLine,tourismDepModel,tourismDataSet);
					traiterLigne(tourismDepModel, tourismDataSet, nextLine, modelDsd);
				}
			}
			reader.close();
			RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY_DS + "tourism-" + dataSetName + ".ttl"),
					tourismDepModel, Lang.TURTLE);

		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static void traiterLigne(Model tourismDepModel, Resource tourismDataSet, Map<String, String> nextLine,
			Model modelDsd) {
		Resource observation = tourismDepModel.createResource(Configuration.observationURI(nextLine.get("OBS_VALUE")),
				DataCubeOntology.Observation);
		observation.addProperty(DataCubeOntology.dataSet, tourismDataSet);

//		ResIterator attribIt = modelDsd.listResourcesWithProperty(DataCubeOntology.attribute);
//		ResIterator dimIt = modelDsd.listResourcesWithProperty(DataCubeOntology.dimension);
		// ResIterator measureIt =
		// modelDsd.listResourcesWithProperty(DataCubeOntology.measure);

		NodeIterator measureIt = modelDsd.listObjectsOfProperty(DataCubeOntology.measure);
		while (measureIt.hasNext()) {
			RDFNode mes = measureIt.next();
			logger.info(mes.toString());
			logger.info(mes.asResource().getLocalName());
			logger.info(mes.asResource().getURI());
			logger.info(mes.asResource().getNameSpace());
			// Property dimensionProperty =
			// tourismDepModel.createProperty(Configuration.componentURI("dimension",
			// dim.asLiteral().getString()));
		}
		NodeIterator attrIt = modelDsd.listObjectsOfProperty(DataCubeOntology.attribute);
		while (attrIt.hasNext()) {
			RDFNode att = attrIt.next();
			String localNameUC = att.asResource().getLocalName().toUpperCase();
			logger.info(localNameUC);

			Resource resource = tourismDepModel
					.createResource(Configuration.codeItemURI(localNameUC, nextLine.get(localNameUC)));
			Property property = tourismDepModel.createProperty(Configuration.componentURI("attribute", localNameUC));
			observation.addProperty(property, resource);

		}
		ResIterator dimIt = modelDsd.listResourcesWithProperty(RDF.type, DataCubeOntology.DimensionProperty);
		while (dimIt.hasNext()) {
			Resource dim = dimIt.next();
			logger.info(dim.toString());
			if (StringUtils.isNoneEmpty(dim.getLocalName())) {

				String localNameUC = dim.getLocalName().toUpperCase();
				logger.info(localNameUC);
				Resource resource = tourismDepModel
						.createResource(Configuration.codeItemURI(localNameUC, nextLine.get(localNameUC)));
				Property property = tourismDepModel
						.createProperty(Configuration.componentURI("dimension", localNameUC));

				observation.addProperty(property, resource);
			} else {
				logger.info("timePeriodProperty");
				Property timePeriodProperty = tourismDepModel
						.createProperty("http://purl.org/linked-data/sdmx/2009/dimension#timePeriod");
				observation.addProperty(timePeriodProperty,
						tourismDepModel.createTypedLiteral(nextLine.get("TIME_PERIOD"), XSDDatatype.XSDgYear));
			}

		}
		Property measureProperty = tourismDepModel.createProperty(Configuration.componentURI("mesure", "MEASURE"));

		// Add measure
		observation.addProperty(measureProperty,
				tourismDepModel.createTypedLiteral(Integer.valueOf(nextLine.get("OBS_VALUE"))));
	}

}
