package net.bemacized.grimoire.pricing.apis;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.pricing.SetDictionary;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.oid.MongoObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StoreAPI {

	protected final Logger LOG = Logger.getLogger(this.getClass().getName());

	private static final long PRICE_TIMEOUT = 21600 * 1000;

	public abstract String getStoreName();

	public abstract String getStoreId();

	public abstract void updateSetDictionary(SetDictionary setDictionary) throws UnknownStoreException, StoreAuthException, StoreServerErrorException;

	protected abstract StoreCardPriceRecord getPriceFresh(Card card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException;

	public StoreCardPriceRecord getPrice(Card card) throws StoreServerErrorException, StoreAuthException, UnknownStoreException, StoreSetUnknownException {
		// Fetch record
		MongoCollection collection = Grimoire.getInstance().getDBManager().getJongo().getCollection("StoreCardPrices");
		StoreCardPriceRecord record = collection.findOne(String.format(
				"{cardName: %s, setCode: %s, storeId: %s}",
				JSONObject.quote(card.getName()),
				JSONObject.quote(card.getSet().getCode()),
				JSONObject.quote(getStoreId())
		)).as(StoreCardPriceRecord.class);

		// If the record has expired remove it
		if (record != null && System.currentTimeMillis() - record.getTimestamp() >= PRICE_TIMEOUT) {
			collection.remove(record.getId());
			record = null;
		}

		// Fetch fresh record if needed and save it
		if (record == null) {
			record = getPriceFresh(card);
			if (record != null) collection.save(record);
		}
		return record;
	}

	public static class StoreCardPriceRecord {

		@MongoObjectId
		private ObjectId id;
		private String cardName;
		private String setCode;
		private String url;
		private String currency;
		private long timestamp;
		private String storeId;
		private Map<String, Double> prices;


		public StoreCardPriceRecord() {
		}

		public StoreCardPriceRecord(String cardName, String setCode, String url, String currency, long timestamp, String storeId, Map<String, Double> prices) {
			this.cardName = cardName;
			this.setCode = setCode;
			this.url = url;
			this.currency = currency;
			this.timestamp = timestamp;
			this.storeId = storeId;
			this.prices = prices;
		}

		public ObjectId getId() {
			return id;
		}

		public String getCardName() {
			return cardName;
		}

		public String getSetCode() {
			return setCode;
		}

		public String getUrl() {
			return url;
		}

		public String getCurrency() {
			return currency;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getStoreId() {
			return storeId;
		}

		public Map<String, Double> getPrices() {
			return prices;
		}
	}

	public class StoreAuthException extends Exception {
	}

	public class StoreServerErrorException extends Exception {
	}

	public class UnknownStoreException extends Exception {
	}

	public class StoreSetUnknownException extends Exception {
	}

	protected Map<String, String> parseSetDictionary() {
		// Load config
		String configText;
		try {
			configText = IOUtils.toString(getClass().getResourceAsStream("/set_dictionary_" + getStoreId().toLowerCase() + ".txt"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load set dictionary!", e);
			return new HashMap<>();
		}

		// Parse config
		Map<String, String> map = new HashMap<>();
		Pattern pattern = Pattern.compile("[^=\\n\\r]+[=].*?(?=[#])?");
		for (String s : configText.split("[\r\n]")) {
			Matcher matcher = pattern.matcher(s.trim());
			if (matcher.find()) {
				String setcode = matcher.group().split("[=]")[0];
				String name = s.substring(setcode.length() + 1);
				map.put(setcode, (name.isEmpty()) ? null : name);
			}
		}
		return map;
	}
}
