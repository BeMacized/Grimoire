package net.bemacized.grimoire.pricing.apis;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.pricing.SetDictionary;
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

public class TCGPlayerAPI extends StoreAPI {

	private String TCG_KEY;
	private String TCG_HOST;

	public TCGPlayerAPI(String TCG_HOST, String TCG_KEY) {
		this.TCG_KEY = TCG_KEY;
		this.TCG_HOST = TCG_HOST;
	}

	@Override
	public String getStoreName() {
		return "TCGPlayer.com";
	}

	@Override
	public String getStoreId() {
		return "TCGP";
	}

	@Override
	public String[] supportedLanguages() {
		return new String[]{"English"};
	}

	@Override
	public void updateSetDictionary(SetDictionary setDictionary) throws UnknownStoreException, StoreAuthException, StoreServerErrorException {
		Map<String, String> map = this.parseSetDictionary();
		map.forEach((key, value) -> setDictionary.getItem(key).setStoreSetName(getStoreId(), value));
		LOG.info(getStoreName() + ": Loaded " + map.entrySet().parallelStream().filter(e -> e.getValue() != null).count() + " sets.");
	}

	@Override
	protected StoreCardPriceRecord getPriceFresh(Card card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException {
		// First fetch TCG set name
		final SetDictionary.SetDictionaryItem setDictionaryItem = Grimoire.getInstance().getPricingManager().getSetDictionary().getItem(card.getSet().getCode());
		if (setDictionaryItem.getStoreSetName(getStoreId()) == null)
			throw new StoreSetUnknownException();

		// Construct endpoint URL
		String endpoint;
		try {
			endpoint = String.format(
					"http://%s/x3/phl.asmx/p?pk=%s&p=%s%s",
					TCG_HOST,
					URLEncoder.encode(TCG_KEY, "UTF-8"),
					URLEncoder.encode(card.getName(), "UTF-8"),
					"&s=" + URLEncoder.encode(setDictionaryItem.getStoreSetName(getStoreId()), "UTF-8")
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
