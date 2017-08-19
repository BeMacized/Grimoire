package net.bemacized.grimoire.data.retrievers.storeretrievers;

import net.bemacized.grimoire.data.models.card.MtgCard;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jongo.marshall.jackson.oid.MongoId;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class StoreRetriever {

	protected final Logger LOG = Logger.getLogger(this.getClass().getName());

	public abstract String getStoreName();

	public abstract String getStoreId();

	public abstract String[] supportedLanguages();

	public abstract long timeout();

	protected abstract StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, StoreDisabledException;

	public StoreCardPriceRecord retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, LanguageUnsupportedException, StoreDisabledException {
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
		private long timestamp;
		private String storeId;
		private Map<String, String> prices;

		public StoreCardPriceRecord() {
		}

		public StoreCardPriceRecord(String cardName, String setCode, @Nullable String url, long timestamp, String storeId, Map<String, String> prices) {
			this.cardName = cardName;
			this.setCode = setCode;
			this.url = url;
			this.timestamp = timestamp;
			this.storeId = storeId;
			this.prices = new HashMap<String, String>() {{
				for (Entry<String, String> entry : prices.entrySet())
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

		public long getTimestamp() {
			return timestamp;
		}

		public String getStoreId() {
			return storeId;
		}

		public Map<String, String> getPrices() {
			return prices;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("_id", _id)
					.append("cardName", cardName)
					.append("setCode", setCode)
					.append("url", url)
					.append("timestamp", timestamp)
					.append("storeId", storeId)
					.append("prices", String.join(", ", prices.entrySet().parallelStream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList())))
					.toString();
		}
	}

	public class StoreDisabledException extends Exception {

	}

	public class StoreAuthException extends Exception {
	}

	public class StoreServerErrorException extends Exception {
	}

	public class UnknownStoreException extends Exception {
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
