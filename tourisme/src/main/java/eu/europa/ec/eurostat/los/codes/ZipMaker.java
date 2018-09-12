package eu.europa.ec.eurostat.los.codes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZipMaker {
	private static Logger logger = LogManager.getLogger(ZipMaker.class);

	private static final String RDF_DIRECTORY = "src/main/resources/rdf/";

	public static void main(String[] args) throws IOException {
		zip("nuts-nacer2", "occarr");
		zip("nuts-nacer2", "occni");

		zip("degurba", "occarr");
		zip("degurba", "occni");

		zip("partner", "occarr");
		zip("partner", "occni");

		zip("terrtypo", "occarr");
		zip("terrtypo", "occni");
	}

	private static void zip(String type, String mesure) throws IOException {
		logger.info(String.format("zip de %s %s", type, mesure));

		
		List<Path> atrackFileNames = Files.list(Paths.get(RDF_DIRECTORY)).filter(s -> filtrer(s, type, mesure))
				.collect(Collectors.toList());
		ZipWriter zw = new ZipWriter();
		zw.createZip(atrackFileNames, RDF_DIRECTORY + type + "-" + mesure+".zip");

	}

	private static boolean filtrer(Path s, String type, String mesure) {
		logger.info(s.getFileName());
		logger.info(StringUtils.containsIgnoreCase(s.getFileName().toString(), type));
		logger.info(StringUtils.containsIgnoreCase(s.getFileName().toString(), mesure));
		return !FilenameUtils.isExtension(s.getFileName().toString(), "zip") && (s.getFileName().toString().startsWith("cl") || (StringUtils.containsIgnoreCase(s.getFileName().toString(), type)
				&& StringUtils.containsIgnoreCase(s.getFileName().toString(), mesure)));
	}

}
