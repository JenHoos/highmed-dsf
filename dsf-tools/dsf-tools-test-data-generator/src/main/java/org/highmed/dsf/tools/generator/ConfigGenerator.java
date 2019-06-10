package org.highmed.dsf.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.highmed.dsf.tools.generator.CertificateGenerator.CertificateFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

	private Properties javaTestFhirConfigProperties;
	private Properties dockerTestFhirConfigProperties;

	private Properties readProperties(Path propertiesFile)
	{
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(propertiesFile);
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		catch (IOException e)
		{
			logger.error("Error while reading properties from " + propertiesFile.toString(), e);
			throw new RuntimeException(e);
		}
		return properties;
	}

	private void writeProperties(Path propertiesFiles, Properties properties)
	{
		try (OutputStream out = Files.newOutputStream(propertiesFiles);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			properties.store(writer, "Generated by test-data-generator");
		}
		catch (IOException e)
		{
			logger.error("Error while writing properties to " + propertiesFiles.toString(), e);
			throw new RuntimeException(e);
		}
	}

	public void modifyJavaTestServerConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles testClient = clientCertificateFilesByCommonName.get("test-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path javaTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/java-test-fhir-config.properties");
		javaTestFhirConfigProperties = readProperties(javaTestFhirConfigTemplateFile);
		javaTestFhirConfigProperties.setProperty("org.highmed.fhir.local-user.thumbprints",
				testClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/java-test-fhir-config.properties"), javaTestFhirConfigProperties);
	}

	public void modifyDockerTestServerConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles testClient = clientCertificateFilesByCommonName.get("test-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-fhir-config.properties");
		dockerTestFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerTestFhirConfigProperties.setProperty("org.highmed.fhir.local-user.thumbprints",
				testClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-fhir-config.properties"), dockerTestFhirConfigProperties);
	}

	public void copyJavaTestFhirServerConfigProperties()
	{
		Path javaTestConfigPropertiesFile = Paths.get("../../dsf-fhir/dsf-fhir-server-jetty/conf/config.properties");
		logger.info("Copying config.properties to {}", javaTestConfigPropertiesFile);
		writeProperties(javaTestConfigPropertiesFile, javaTestFhirConfigProperties);
	}

	public void copyDockerTestFhirServerConfigProperties()
	{
		Path dockerTestConfigPropertiesFile = Paths.get("../../dsf-docker-test-setup/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerTestConfigPropertiesFile);
		writeProperties(dockerTestConfigPropertiesFile, dockerTestFhirConfigProperties);
	}
}
