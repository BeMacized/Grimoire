package net.bemacized.grimoire.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class AppProperties extends Properties {

	private final static Logger LOG = Logger.getLogger(AppProperties.class.getName());
	private static AppProperties instance;

	public static AppProperties getInstance() {
		if (instance == null) instance = new AppProperties();
		return instance;
	}

	private AppProperties() {
		super();
		try {
			System.out.println(IOUtils.toString(getClass().getResourceAsStream("/app.properties")));
			InputStream is = getClass().getResourceAsStream("/app.properties");
			this.load(is);
			is.close();
		} catch (IOException ioe) {
			LOG.severe("Could not load app properties.");
			System.exit(1);
		}
	}

	public String getVersion() {
		return this.getProperty("version");
	}

}
