package org.highmed.fhir.db;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.PropertyResourceBundle;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class FhirDbMigrationMain
{
	private static final Logger logger = LoggerFactory.getLogger(FhirDbMigrationMain.class);

	public static void main(String[] args) throws Exception
	{
		// if (args.length != 1 || !("-dev".equals(args[0]) ||
		// "-prod".equals(args[0])))
		// {
		// logger.error("Argument -dev or -prod expected");
		// System.exit(1);
		// }
		//
		// String context = args[0];
		// context = context.substring(1, context.length());
		//
		// logger.info("Executing with context {}", context);
		//
		// Contexts contexts = new Contexts(context);

		Contexts contexts = new Contexts();

		Path propertiesPath = Paths.get("db.properties");
		if (!Files.isReadable(propertiesPath))
		{
			System.err.println("Properties file not readable: " + propertiesPath.toAbsolutePath().toString());
			System.exit(1);
		}

		InputStream in = Files.newInputStream(propertiesPath);

		PropertyResourceBundle dbProperties = new PropertyResourceBundle(
				new InputStreamReader(in, Charset.forName("UTF-8")));

		Class.forName(dbProperties.getString("db.driver"));

		BasicDataSource dataSource = null;
		try
		{
			dataSource = new BasicDataSource();

			dataSource.setDriverClassName(dbProperties.getString("db.driver"));
			dataSource.setUrl(dbProperties.getString("db.url"));
			dataSource.setUsername(dbProperties.getString("db.migration-username"));
			dataSource.setPassword(dbProperties.getString("db.migration-password"));

			try (Connection connection = dataSource.getConnection())
			{
				Database database = DatabaseFactory.getInstance()
						.findCorrectDatabaseImplementation(new JdbcConnection(connection));
				Liquibase liquibase = new Liquibase("db/db.changelog.xml", new ClassLoaderResourceAccessor(), database);

				ChangeLogParameters changeLogParameters = liquibase.getChangeLogParameters();
				changeLogParameters.set("liquibase_user", dbProperties.getString("db.migration-username"));
				changeLogParameters.set("server_users_group", dbProperties.getString("db.server_users_group"));
				changeLogParameters.set("server_user", dbProperties.getString("db.server_user"));
				changeLogParameters.set("server_user_password", dbProperties.getString("db.server_user_password"));

				logger.info("Executing DB migration ...");
				liquibase.update(contexts);
				logger.info("Executing DB migration [Done]");
			}
		}
		finally
		{
			if (dataSource != null)
				dataSource.close();
		}
	}
}
