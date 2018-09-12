package eu.europa.ec.eurostat.los.codes;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReaderHeaderAware;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;

public class DataCubeMaker {
	private static Logger logger = LogManager.getLogger(DataCubeMaker.class);

	private static final String DATA_CSV = "src/main/resources/data/tourism-nuts-nace-r2-fr.csv";

	private static final String RDF_DIRECTORY_DS = "src/main/resources/rdf/ds-";

	public static void main(String[] args) throws IOException {
		traiterUneMeasure("occ_arr");
		traiterUneMeasure("occ_ni");
	}

	private static void traiterUneMeasure(String measure) {
		try (FileReader fileReader = new FileReader(DATA_CSV)) {
	
			Model tourismDepModel = ModelFactory.createDefaultModel();
			tourismDepModel.setNsPrefix("xsd", XSD.getURI());
			tourismDepModel.setNsPrefix("qb", DataCubeOntology.getURI());

			tourismDepModel.setNsPrefix("dim", "http://id.linked-open-statistics.org/meta/dimension/");
			tourismDepModel.setNsPrefix("mes", "http://id.linked-open-statistics.org/meta/mesure/");
			tourismDepModel.setNsPrefix("att", "http://id.linked-open-statistics.org/meta/attribute/");
			tourismDepModel.setNsPrefix("sdmxdim", "http://purl.org/linked-data/sdmx/2009/dimension#");
			
			Resource tourismDataSet = tourismDepModel.createResource(Configuration.BASE_URI + "dataSet/tourism-nuts-nacer2-occ-arr", DataCubeOntology.DataSet);
	
			// Dimensions and measure
			Property cresidDimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "C_RESID"));
			Property nutsDimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "NUTS"));
			Property nacer2DimensionProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "NACE_R2"));
			Property timePeriodDimensionProperty = tourismDepModel.createProperty("http://purl.org/linked-data/sdmx/2009/dimension#timePeriod");
			Property measureProperty = tourismDepModel.createProperty(Configuration.componentURI("mesure", "MEASURE"));
			//Property observationProperty = tourismDepModel.createProperty(Configuration.componentURI("dimension", "OBS_VALUE"));

			Property obsStatusAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "OBS_STATUS"));
			Property confStatusAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "CONF_STATUS"));
			Property unitAttributProperty = tourismDepModel.createProperty(Configuration.componentURI("attribute", "UNIT"));
			
			CSVReaderHeaderAware reader = new CSVReaderHeaderAware(fileReader);
			Map<String, String> nextLine;
			while ((nextLine = reader.readMap()) != null) {
				if (measure.equals(nextLine.get("MEASURE"))) {
					//traiterLigneArrivee(nextLine,tourismDepModel,tourismDataSet);
					traiterLigne(tourismDepModel, tourismDataSet, cresidDimensionProperty, nutsDimensionProperty,
							nacer2DimensionProperty, timePeriodDimensionProperty, measureProperty,
							obsStatusAttributProperty, confStatusAttributProperty, unitAttributProperty, nextLine);
				} 
			}
			reader.close();
			RDFDataMgr.write(new FileOutputStream(RDF_DIRECTORY_DS + "tourism-nuts-nacer2-"+StringUtils.remove(measure, "_")+".ttl"), tourismDepModel, Lang.TURTLE);
			
		
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static void traiterLigne(Model tourismDepModel, Resource tourismDataSet,
			Property cresidDimensionProperty, Property nutsDimensionProperty, Property nacer2DimensionProperty,
			Property timePeriodDimensionProperty, Property measureProperty, Property obsStatusAttributProperty,
			Property confStatusAttributProperty, Property unitAttributProperty, Map<String, String> nextLine) {
		Resource observation = tourismDepModel.createResource(Configuration.observationURI(nextLine.get("OBS_VALUE")), DataCubeOntology.Observation);
		observation.addProperty(DataCubeOntology.dataSet, tourismDataSet);
		

		// Add dimension values
		
		Resource cResidResource = tourismDepModel.createResource(Configuration.codeItemURI("C_RESID", nextLine.get("C_RESID")));
		observation.addProperty(cresidDimensionProperty, cResidResource);
		Resource nutsResource = tourismDepModel.createResource(Configuration.codeItemURI("NUTS", nextLine.get("NUTS")));
		observation.addProperty(nutsDimensionProperty, nutsResource);
		Resource nacer2Resource = tourismDepModel.createResource(Configuration.codeItemURI("NACE_R2", nextLine.get("NACE_R2")));
		observation.addProperty(nacer2DimensionProperty, nacer2Resource);
		observation.addProperty(timePeriodDimensionProperty, tourismDepModel.createTypedLiteral(nextLine.get("TIME_PERIOD"), XSDDatatype.XSDgYear));
		Resource obsStatusResource = tourismDepModel.createResource(Configuration.codeItemURI("OBS_STATUS", nextLine.get("OBS_STATUS")));
		observation.addProperty(obsStatusAttributProperty, obsStatusResource);
		Resource confStatusResource = tourismDepModel.createResource(Configuration.codeItemURI("CONF_STATUS", nextLine.get("CONF_STATUS")));
		observation.addProperty(confStatusAttributProperty, confStatusResource);
		Resource unitResource = tourismDepModel.createResource(Configuration.codeItemURI("UNIT", nextLine.get("UNIT")));
		observation.addProperty(unitAttributProperty, unitResource);
		// Add measure
		observation.addProperty(measureProperty, tourismDepModel.createTypedLiteral(Integer.valueOf(nextLine.get("OBS_VALUE"))));
	}

	
}
