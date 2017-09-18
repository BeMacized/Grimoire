package net.bemacized.grimoire.data.retrievers.imageproviders;

import net.bemacized.grimoire.data.models.card.MtgCard;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ImageService {

	protected static final Logger LOG = Logger.getLogger(ImageService.class.getName());

	public abstract String getUrl(MtgCard card, boolean checkAvailability);

	protected boolean imageAvailable(String url) {
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
