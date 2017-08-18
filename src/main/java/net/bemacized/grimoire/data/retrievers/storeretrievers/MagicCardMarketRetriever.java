package net.bemacized.grimoire.data.retrievers.storeretrievers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.MtgCard;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

public class MagicCardMarketRetriever extends StoreRetriever {

	private final String MCM_HOST;
	private final String MCM_TOKEN;
	private final String MCM_SECRET;

	private Map<String, String> setDictionary;

	public MagicCardMarketRetriever(String MCM_HOST, String MCM_TOKEN, String MCM_SECRET, JsonObject rawSetDictionary) {
		this.MCM_HOST = MCM_HOST;
		this.MCM_TOKEN = MCM_TOKEN;
		this.MCM_SECRET = MCM_SECRET;
		setDictionary = new HashMap<>();
		rawSetDictionary.getAsJsonArray("data").forEach(setMap -> {
			// Check if we know the store set name
			if (!setMap.getAsJsonObject().has(getStoreId())) return;
			// Get the set code
			String setCode;
			if (setMap.getAsJsonObject().has("scryfall")) setCode = setMap.getAsJsonObject().get("scryfall").getAsJsonObject().get("code").getAsString();
			else if (setMap.getAsJsonObject().has("mtgjson"))  setCode = setMap.getAsJsonObject().get("mtgjson").getAsJsonObject().get("code").getAsString();
			else return;
			// Get the store set name
			String setName = setMap.getAsJsonObject().get(getStoreId()).getAsJsonObject().get("name").getAsString();
			// Store it in local dictionary
			setDictionary.put(setCode, setName);
		});
	}

	@Override
	public String getStoreName() {
		return "MagicCardMarket.eu";
	}

	@Override
	public String getStoreId() {
		return "magiccardmarket";
	}

	@Override
	public String[] supportedLanguages() {
		return new String[]{"English"};
	}

	@Override
	public long timeout() {
		return 1000 * 60 * 60 * 6; // 6 hours in ms
	}

	@Override
	protected StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException {
		// First fetch MCM set name
		final String storeSetName = setDictionary.get(card.getSet().getCode());
		if (storeSetName == null)
			throw new StoreSetUnknownException();

		// Construct endpoint URL
		String endpoint;
		try {
			endpoint = String.format("https://%s/ws/v1.1/output.json/products/%s/1/1/true", MCM_HOST, URLEncoder.encode(card.getName(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOG.log(Level.SEVERE, "Could not construct endpoint url for price fetching", e);
			return null;
		}

		// Make request
		String responseBody;
		int responseCode;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
			String authHeader = getAuthorizationHeader("GET", endpoint);
			connection.addRequestProperty("Authorization", authHeader);
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

		// Parse response
		JSONObject response = new JSONObject(responseBody);
		// Find matching product
		JSONObject product = (JSONObject) StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
						response.getJSONArray("product").iterator(),
						Spliterator.ORDERED),
				false
		).parallel().filter(p -> (((JSONObject) p).getString("expansion").equalsIgnoreCase(storeSetName))).findFirst().orElse(null);
		// Return null if we didn't find anything
		if (product == null) return null;

		DecimalFormat formatter = new DecimalFormat("#.00");
		// Return the found data
		return new StoreCardPriceRecord(
				card.getName(),
				card.getSet().getCode(),
				"https://www.magiccardmarket.eu" + product.getString("website"),
				System.currentTimeMillis(),
				this.getStoreId(),
				new HashMap<String, String>() {{
					put("Low", formatPrice(formatter.format(product.getJSONObject("priceGuide").getDouble("LOW"))));
					put("Avg. Sell Price", formatPrice(formatter.format(product.getJSONObject("priceGuide").getDouble("SELL"))));
					put("Low Foil", formatPrice(formatter.format(product.getJSONObject("priceGuide").getDouble("LOWFOIL"))));
					put("Average", formatPrice(formatter.format(product.getJSONObject("priceGuide").getDouble("AVG"))));
				}}
		);
	}

	private String getAuthorizationHeader(String httpMethod, String realm) {
		// Define main parameters
		final String oauthVersion = "1.0";
		final String oauthConsumerKey = MCM_TOKEN;
		final String oauthToken = "";
		final String oauthSignatureMethod = "HMAC-SHA1";
		final String oauthTimestamp = String.valueOf(System.currentTimeMillis() / 1000L);
		final String oauthNonce = generateRandomString(12);

		// Construct signature
		try {
			String baseString = String.format("%s&%s&", httpMethod.toUpperCase(), URLEncoder.encode(realm, "UTF-8"));
			String paramString = String.format("%s=%s&", "oauth_consumer_key", URLEncoder.encode(oauthConsumerKey, "UTF-8")) +
					String.format("%s=%s&", "oauth_nonce", URLEncoder.encode(oauthNonce, "UTF-8")) +
					String.format("%s=%s&", "oauth_signature_method", URLEncoder.encode(oauthSignatureMethod, "UTF-8")) +
					String.format("%s=%s&", "oauth_timestamp", URLEncoder.encode(oauthTimestamp, "UTF-8")) +
					String.format("%s=%s&", "oauth_token", URLEncoder.encode(oauthToken, "UTF-8")) +
					String.format("%s=%s", "oauth_version", URLEncoder.encode(oauthVersion, "UTF-8"));
			baseString += URLEncoder.encode(paramString, "UTF-8");
			final String signingKey = URLEncoder.encode(MCM_SECRET, "UTF-8") + "&";

			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec secret = new SecretKeySpec(signingKey.getBytes(), mac.getAlgorithm());
			mac.init(secret);
			byte[] digest = mac.doFinal(baseString.getBytes());
			final String oauthSignature = DatatypeConverter.printBase64Binary(digest);

			return "OAuth " +
					String.format("%s=\"%s\", ", "realm", realm) +
					String.format("%s=\"%s\", ", "oauth_version", oauthVersion) +
					String.format("%s=\"%s\", ", "oauth_timestamp", oauthTimestamp) +
					String.format("%s=\"%s\", ", "oauth_nonce", oauthNonce) +
					String.format("%s=\"%s\", ", "oauth_consumer_key", oauthConsumerKey) +
					String.format("%s=\"%s\", ", "oauth_token", oauthToken) +
					String.format("%s=\"%s\", ", "oauth_signature_method", oauthSignatureMethod) +
					String.format("%s=\"%s\"", "oauth_signature", oauthSignature);
		} catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException e) {
			LOG.log(Level.SEVERE, "Could not construct authorization header for price fetching", e);
			return null;
		}
	}

	private String formatPrice(String price) {
		if (price == null || price.isEmpty()) price = "0";
		try {
			if (Double.parseDouble(price) <= 0) price = "N/A";
			else price = "€" + price;
		} catch (Exception e) {
			price = "N/A";
		}
		return price;
	}

	private static String generateRandomString(int length) {
		Random rng = new Random();
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		char[] text = new char[length];
		for (int i = 0; i < length; i++) text[i] = characters.charAt(rng.nextInt(characters.length()));
		return new String(text);
	}

}
