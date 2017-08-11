package net.bemacized.grimoire.model.models;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dependency {

	private static final Logger LOG = Logger.getLogger(Dependency.class.getName());
	private static final String FILE_STORAGE = "dependency_cache";

	private String id;
	private List<String> sources;
	private Type type;
	private byte[] data;
	private String encoding;

	public Dependency(String id, List<String> sources, Type type, String encoding) {
		this.id = id;
		this.sources = new ArrayList<>(sources);
		this.type = type;
		this.data = null;
		this.encoding = encoding;
	}

	public String getId() {
		return id;
	}

	public List<String> getSources() {
		return sources;
	}

	public Type getType() {
		return type;
	}

	public String getString() {
		// Attempt retrieval if needed
		if (data == null && !retrieve()) return null;
		try {
			return IOUtils.toString(data, encoding);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not get string from dependency.", e);
		}
		return null;
	}

	public byte[] getBinary() {
		if (data == null && !retrieve()) return null;
		return data;
	}

	public boolean retrieve() {
		LOG.info(getId() + ": Retrieving dependency file...");
		if (loadFromCache()) {
			LOG.info(getId() + ": Retrieved dependency from cache.");
			return true;
		}
		LOG.info(getId() + ": Not in cache. Retrieving dependency from web...");
		for (String source : sources) {
			try {
				HttpURLConnection con = (HttpURLConnection) new URL(source).openConnection();
				con.setRequestMethod("GET");
				con.connect();
				if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
					data = IOUtils.toByteArray(con.getInputStream());
					break;
				}
			} catch (IOException e) {
				LOG.log(Level.SEVERE, getId() + ": Could not retrieve dependency from the web.", e);
			}
		}
		if (data != null) {
			LOG.info(getId() + ": Retrieved dependency from web.");
			cacheData();
		}
		return data != null;
	}

	public void release() {
		data = null;
	}

	private File getFileReference() {
		return new File(FILE_STORAGE + File.separator + getId());
	}

	private boolean loadFromCache() {
		File file = getFileReference();
		// Check if exists in cache
		if (!file.exists()) return false;
		// Load from cache
		try {
			this.data = FileUtils.readFileToByteArray(file);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, getId() + ": Could not load from cache", e);
			return false;
		}
		return true;
	}

	private void cacheData() {
		if (data == null) return;
		File file = getFileReference();
		// Delete old copy
		if (file.exists()) file.delete();
		// Assert directory existence
		new File(FILE_STORAGE).mkdirs();
		// Write data to file
		try {
			FileUtils.writeByteArrayToFile(file, data);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, getId() + ": Could not cache dependency data", e);
		}
	}

	public enum Type {
		TEXT,
		BINARY
	}
}
