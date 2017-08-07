package net.bemacized.grimoire.model.models.storeAPIs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
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
import java.util.HashMap;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

public class MagicCardMarketAPI extends StoreAPI {

	private final String MCM_HOST;
	private final String MCM_TOKEN;
	private final String MCM_SECRET;

	public MagicCardMarketAPI(String MCM_HOST, String MCM_TOKEN, String MCM_SECRET) {
		this.MCM_HOST = MCM_HOST;
		this.MCM_TOKEN = MCM_TOKEN;
		this.MCM_SECRET = MCM_SECRET;
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

	@SuppressWarnings("Duplicates")
	@Override
	public void updateSets(JsonObject setDictionary) throws UnknownStoreException, StoreAuthException, StoreServerErrorException {
		setDictionary.keySet().stream()
				.map(setcode -> {
					JsonObject obj = new JsonParser().parse(setDictionary.getAsJsonObject(setcode).toString()).getAsJsonObject();
					obj.addProperty("code", setcode);
					return obj;
				})
				.filter(obj -> obj.has(getStoreId()))
				.filter(obj -> !obj.get(getStoreId()).getAsString().isEmpty())
				.forEach(obj -> Grimoire.getInstance().getSets().getByCode(obj.get("code").getAsString()).setStoreSetName(getStoreId(), obj.get(getStoreId()).getAsString()));
		LOG.info(getStoreName() + ": Loaded " + Grimoire.getInstance().getSets().getSets().parallelStream().filter(s -> s.getStoreSetName(getStoreId()) != null).count() + " sets.");
	}

	@Override
	protected StoreCardPriceRecord getPriceFresh(Card card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException {
		// First fetch MCM set name
		if (card.getSet().getStoreSetName(getStoreId()) == null)
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
		).parallel().filter(p -> (((JSONObject) p).getString("expansion").equalsIgnoreCase(card.getSet().getStoreSetName(getStoreId())))).findFirst().orElse(null);
		// Return null if we didn't find anything
		if (product == null) return null;

		// Return the found data
		return new StoreCardPriceRecord(
				card.getName(),
				card.getSet().getCode(),
				"https://www.magiccardmarket.eu" + product.getString("website"),
				"€",
				System.currentTimeMillis(),
				this.getStoreId(),
				new HashMap<String, Double>() {{
					put("Low", product.getJSONObject("priceGuide").getDouble("LOW"));
					put("Avg. Sell Price", product.getJSONObject("priceGuide").getDouble("SELL"));
					put("Low Foil", product.getJSONObject("priceGuide").getDouble("LOWFOIL"));
					put("Average", product.getJSONObject("priceGuide").getDouble("AVG"));
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

	private static String generateRandomString(int length) {
		Random rng = new Random();
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		char[] text = new char[length];
		for (int i = 0; i < length; i++) text[i] = characters.charAt(rng.nextInt(characters.length()));
		return new String(text);
	}

}
