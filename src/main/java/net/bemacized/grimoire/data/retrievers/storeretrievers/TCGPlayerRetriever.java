package net.bemacized.grimoire.data.retrievers.storeretrievers;

import com.google.gson.JsonObject;
import net.bemacized.grimoire.data.models.card.MtgCard;
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
		rawSetDictionary.getAsJsonArray("data").forEach(setMap -> {
			// Check if we know the store set name
			if (!setMap.getAsJsonObject().has(getStoreId())) return;
			// Get the set code
			String setCode;
			if (setMap.getAsJsonObject().has("scryfall"))
				setCode = setMap.getAsJsonObject().get("scryfall").getAsJsonObject().get("code").getAsString();
			else if (setMap.getAsJsonObject().has("mtgjson"))
				setCode = setMap.getAsJsonObject().get("mtgjson").getAsJsonObject().get("code").getAsString();
			else return;
			// Get the store set name
			String setName = setMap.getAsJsonObject().get(getStoreId()).getAsJsonObject().get("name").getAsString();
			// Store it in local dictionary
			setDictionary.put(setCode, setName);
		});
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
	protected StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreDisabledException {
		if (TCG_HOST == null || TCG_KEY == null) throw new StoreDisabledException();

		// First fetch TCG set name
		String storeSetName = setDictionary.get(card.getSet().getCode());
		if (storeSetName == null)
			storeSetName = card.getSet().getName();

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
				System.currentTimeMillis(),
				this.getStoreId(),
				new HashMap<String, String>() {{
					put("Low", formatPrice(doc.getDocumentElement().getElementsByTagName("lowprice").item(0).getTextContent()));
					put("Average", formatPrice(doc.getDocumentElement().getElementsByTagName("avgprice").item(0).getTextContent()));
					put("High", formatPrice(doc.getDocumentElement().getElementsByTagName("hiprice").item(0).getTextContent()));
					put("Average Foil", formatPrice(doc.getDocumentElement().getElementsByTagName("foilavgprice").item(0).getTextContent()));
				}}
		);
	}

	private String formatPrice(String price) {
		if (price == null || price.isEmpty()) price = "0";
		try {
			if (Double.parseDouble(price) <= 0) price = "N/A";
			else price = "$" + price;
		} catch (Exception e) {
			price = "N/A";
		}
		return price;
	}

}
