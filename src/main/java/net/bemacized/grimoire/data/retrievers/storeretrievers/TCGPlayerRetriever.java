package net.bemacized.grimoire.data.retrievers.storeretrievers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.MtgCard;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TCGPlayerRetriever extends StoreRetriever {

	private String TCG_KEY;
	private String TCG_HOST;

	private Map<String, String> setDictionary;

	public TCGPlayerRetriever(String TCG_HOST, String TCG_KEY, JsonObject rawSetDictionary) {
		this.TCG_KEY = TCG_KEY;
		this.TCG_HOST = TCG_HOST;
		setDictionary = new HashMap<>();
		rawSetDictionary.keySet().stream()
				.map(setcode -> {
					JsonObject obj = new JsonParser().parse(rawSetDictionary.getAsJsonObject(setcode).toString()).getAsJsonObject();
					obj.addProperty("code", setcode);
					return obj;
				})
				.filter(obj -> obj.has(getStoreId()))
				.filter(obj -> !obj.get(getStoreId()).getAsString().isEmpty())
				.forEach(obj -> setDictionary.put(obj.get("code").getAsString(), obj.get(getStoreId()).getAsString()));
	}

	@Override
	public String getStoreName() {
		return "TCGPlayer.com";
	}

	@Override
	public String getStoreId() {
		return "tcgplayer";
	}

	@Override
	public String[] supportedLanguages() {
		return new String[]{"English"};
	}

	@Override
	public long timeout() {
		return 1000 * 60 * 60 * 6; // 6 hours
	}

	@Override
	protected StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException {
		// First fetch TCG set name
		final String storeSetName = setDictionary.get(card.getSet().getCode());
		if (storeSetName == null)
			throw new StoreSetUnknownException();

		// Construct endpoint URL
		String endpoint;
		try {
			endpoint = String.format(
					"http://%s/x3/phl.asmx/p?pk=%s&p=%s%s",
					TCG_HOST,
					URLEncoder.encode(TCG_KEY, "UTF-8"),
					URLEncoder.encode(card.getName(), "UTF-8"),
					"&s=" + URLEncoder.encode(storeSetName, "UTF-8")
			);
		} catch (UnsupportedEncodingException e) {
			LOG.log(Level.SEVERE, "Could not construct endpoint url for price fetching", e);
			return null;
		}

		// Make request
		String responseBody;
		int responseCode;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
			connection.connect();
			responseCode = connection.getResponseCode();
			try {
				responseBody = IOUtils.toString((responseCode == 200) ? connection.getInputStream() : connection.getErrorStream());
			} catch (Exception e) {
				responseBody = "";
			}
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not fetch response", e);
			throw new UnknownStoreException();
		}

		// Handle errors
		switch (responseCode) {
			case 401:
			case 403:
				LOG.severe("Received a " + responseCode + " response while fetching card pricing.");
				throw new StoreAuthException();
			case 404:
				return null;
			case 500:
			case 503:
				LOG.severe("Received a " + responseCode + " response while fetching card pricing.");
				throw new StoreServerErrorException();
			case 200:
				break;
			default:
				LOG.severe("Received an UNKNOWN " + responseCode + " response while fetching card pricing.");
				throw new UnknownStoreException();
		}

		// If no result, return null
		if (responseBody.trim().equalsIgnoreCase("Product not found.")) return null;

		// Parse result
		Document doc;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(responseBody));
			doc = db.parse(is);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOG.log(Level.SEVERE, "Could not parse response", e);
			throw new UnknownStoreException();
		}

		// Return null if we don't have a link
		if (doc.getDocumentElement().getElementsByTagName("link").getLength() == 0) return null;

		return new StoreCardPriceRecord(
				card.getName(),
				card.getSet().getCode(),
				doc.getDocumentElement().getElementsByTagName("link").item(0).getTextContent(),
				"$",
				System.currentTimeMillis(),
				this.getStoreId(),
				new HashMap<String, Double>() {{
					put("Low", Double.valueOf(doc.getDocumentElement().getElementsByTagName("lowprice").item(0).getTextContent()));
					put("Average", Double.valueOf(doc.getDocumentElement().getElementsByTagName("avgprice").item(0).getTextContent()));
					put("High", Double.valueOf(doc.getDocumentElement().getElementsByTagName("hiprice").item(0).getTextContent()));
					put("Average Foil", Double.valueOf(doc.getDocumentElement().getElementsByTagName("foilavgprice").item(0).getTextContent()));
				}}
		);
	}

}
