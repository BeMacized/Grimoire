package net.bemacized.grimoire.data.retrievers.storeretrievers;

import net.bemacized.grimoire.data.models.MtgCard;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jongo.marshall.jackson.oid.MongoId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class StoreRetriever {

	protected final Logger LOG = Logger.getLogger(this.getClass().getName());

	private static final long PRICE_TIMEOUT = 21600 * 1000;

	public abstract String getStoreName();

	public abstract String getStoreId();

	public abstract String[] supportedLanguages();

	public abstract long timeout();

	protected abstract StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException;

	public StoreCardPriceRecord retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreSetUnknownException, LanguageUnsupportedException {
		if (Arrays.stream(supportedLanguages()).parallel().noneMatch(l -> l.equalsIgnoreCase(card.getLanguage())))
			throw new LanguageUnsupportedException(card.getLanguage());
		return _retrievePrice(card);
	}

	public static class StoreCardPriceRecord {

		@MongoId
		private String _id;

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
			this.prices = new HashMap<String, Double>() {{
				for (Entry<String, Double> entry : prices.entrySet())
					put(entry.getKey().replaceAll("[^a-zA-Z0-9_ ]", ""), entry.getValue());
			}};
			this._id = generateId();
		}

		private String generateId() {
			return DigestUtils.sha1Hex(cardName + setCode + storeId);
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

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("_id", _id)
					.append("cardName", cardName)
					.append("setCode", setCode)
					.append("url", url)
					.append("currency", currency)
					.append("timestamp", timestamp)
					.append("storeId", storeId)
					.append("prices", String.join(", ", prices.entrySet().parallelStream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList())))
					.toString();
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
