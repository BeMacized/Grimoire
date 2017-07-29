package net.bemacized.grimoire.utils;


import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LinkShortener {

	private static final Logger LOG = Logger.getLogger(LinkShortener.class.getName());

	public static String shorten(String link) {
		final String GOOGLE_KEY = System.getenv("GOOGLE_KEY");
		if (GOOGLE_KEY == null || GOOGLE_KEY.isEmpty()) {
			LOG.warning("Environment variable GOOGLE_KEY has not been set! Cannot shorten links.");
			return link;
		}
		String responseText;
		int responseCode;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("https://www.googleapis.com/urlshortener/v1/url?key=" + GOOGLE_KEY).openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);
			OutputStream out = con.getOutputStream();
			out.write(new JSONObject() {{
				put("longUrl", link);
			}}.toString().getBytes());
			out.close();
			con.connect();
			responseCode = con.getResponseCode();
			responseText = (con.getResponseCode() >= 200 && con.getResponseCode() < 300) ? IOUtils.toString(con.getInputStream()) : IOUtils.toString(con.getErrorStream());
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "An error occurred while shortening a link", e);
			return link;
		}

		switch (responseCode) {
			case 401:
			case 403:
				LOG.warning("Could not authenticate with Google!");
				break;
			case 200:
				return new JSONObject(responseText).getString("id");
			default:
				if (responseCode < 500 || responseCode >= 600)
					LOG.severe("Google gave an unknown response: " + responseCode);
				else LOG.warning("Google encountered an internal server error.");
				break;
		}
		return link;

	}

}
