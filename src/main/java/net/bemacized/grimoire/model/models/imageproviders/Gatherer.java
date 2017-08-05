package net.bemacized.grimoire.model.models.imageproviders;

import net.bemacized.grimoire.model.models.Card;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Gatherer extends ImageProvider {

	@Override
	public String getUrl(Card card) {
		String url = String.format("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=%s&type=card", card.getMultiverseid());
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.connect();
			if (con.getResponseCode() != 200) return null;
			// Check for cardback
			return (DigestUtils.sha1Hex(con.getInputStream()).equals("cbfa599596c820cdf9a12a07f35917ae1ec20573")) ? null : url;
		} catch (IOException ignored) {
			return null;
		}
	}
}
