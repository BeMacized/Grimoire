package net.bemacized.grimoire.model.models.storeAPIs;

import com.google.gson.JsonObject;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import org.bson.types.ObjectId;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.oid.MongoObjectId;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

public abstract class StoreAPI {

	protected final Logger LOG = Logger.getLogger(this.getClass().getName());

	private static final long PRICE_TIMEOUT = 21600 * 1000;

	public abstract String getStoreName();

	public abstract String getStoreId();

	public abstract String[] supportedLanguages();

	public abstract void updateSets(JsonObject setDictionary) throws UnknownStoreException, StoreAuthException, StoreServerErrorException;

	protected abstract StoreCardPriceRecord getPriceFresh(Card card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException;

	public StoreCardPriceRecord getPrice(Card card) throws StoreServerErrorException, StoreAuthException, UnknownStoreException, StoreSetUnknownException, LanguageUnsupportedException {
		// Check language support
		if (Arrays.stream(supportedLanguages()).noneMatch(lang -> lang.equalsIgnoreCase(card.getLanguage())))
			throw new LanguageUnsupportedException(card.getLanguage());

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

	public class LanguageUnsupportedException extends Exception {

		private String language;

		LanguageUnsupportedException(String language) {
			this.language = language;
		}

		public String getLanguage() {
			return language;
		}
	}
}
