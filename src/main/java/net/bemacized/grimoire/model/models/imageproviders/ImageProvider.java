package net.bemacized.grimoire.model.models.imageproviders;

import net.bemacized.grimoire.model.models.Card;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ImageProvider {

	protected static final Logger LOG = Logger.getLogger(ImageProvider.class.getName());

	public abstract String getUrl(Card card);

	protected boolean imageAvailable(String url){
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.connect();
			return (con.getResponseCode() == 200);
		} catch (MalformedURLException e) {
			LOG.log(Level.SEVERE, "Could not parse image url", e);
		} catch (IOException ignored) {
		}
		return false;
	}
}
