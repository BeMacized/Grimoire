package net.bemacized.grimoire.data.retrievers.storeretrievers;

import com.ritaja.xchangerate.api.CurrencyConverter;
import com.ritaja.xchangerate.api.CurrencyConverterBuilder;
import com.ritaja.xchangerate.api.CurrencyNotSupportedException;
import com.ritaja.xchangerate.endpoint.EndpointException;
import com.ritaja.xchangerate.service.ServiceException;
import com.ritaja.xchangerate.storage.StorageException;
import com.ritaja.xchangerate.util.Strategy;
import net.bemacized.grimoire.data.models.card.MtgCard;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jongo.marshall.jackson.oid.MongoId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class StoreRetriever {

	protected static final Logger LOG = Logger.getLogger(StoreRetriever.class.getName());

	private final static CurrencyConverter CONVERTER = new CurrencyConverterBuilder()
			.strategy(Strategy.YAHOO_FINANCE_FILESTORE)
			.buildConverter();

	public abstract String getStoreName();

	public abstract String getStoreId();

	public abstract String[] supportedLanguages();

	public abstract long timeout();

	protected abstract StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException;

	public StoreCardPriceRecord retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException, LanguageUnsupportedException {
		if (Arrays.stream(supportedLanguages()).parallel().noneMatch(l -> l.equalsIgnoreCase(card.getLanguage())))
			throw new LanguageUnsupportedException(card.getLanguage());
		return _retrievePrice(card);
	}

	public static class Price {
		private double value;
		@Nonnull
		private Currency currency;
		@Nullable
		private Currency convertedFrom;

		public Price() {
		}

		public Price(double value, @Nonnull Currency currency) {
			this.value = value;
			this.currency = currency;
		}

		public Price(double value, @Nonnull Currency currency, @Nonnull Currency convertedFrom) {
			this(value, currency);
			this.convertedFrom = convertedFrom;
		}


		public double getValue() {
			return value;
		}

		public Currency getCurrency() {
			return currency;
		}

		@Nullable
		public Currency getConvertedFrom() {
			return convertedFrom;
		}

		@Nullable
		public Price convertTo(@Nonnull Currency convertTo) {
			if (!this.currency.isConvertable())
				throw new IllegalArgumentException("You cannot convert to or from " + this.currency.getName());
			if (!convertTo.isConvertable())
				throw new IllegalArgumentException("You cannot convert to or from " + convertTo.getName());
			try {
				return new Price(CONVERTER.convertCurrency(
						new BigDecimal(value),
						com.ritaja.xchangerate.util.Currency.valueOf(currency.getAbbr()),
						com.ritaja.xchangerate.util.Currency.valueOf(convertTo.getAbbr())
				).doubleValue(), convertTo, this.currency);
			} catch (CurrencyNotSupportedException | ServiceException | EndpointException e) {
				return null;
			} catch (StorageException e) {
				LOG.log(Level.SEVERE, "Could not store currency conversion data", e);
				return null;
			}
		}

		@Override
		public String toString() {
			return toString(false);
		}

		public String toString(boolean showConversion) {
			return (!currency.getSymbolPosition() ? currency.getSymbol() : "")
					+ new DecimalFormat(currency.getFormat()).format(value)
					+ (currency.getSymbolPosition() ? " " + currency.getSymbol() : "")
					+ (convertedFrom != null && showConversion ? " (Converted from " + convertedFrom.getAbbr() + ")" : "");
		}
	}

	public enum Currency {
		TIX("TIX", "TIX", "TIX", true, false, "#0.00"),
		EUR("€", "EUR", "Euros", false, true, "#0.00"),
		USD("$", "USD", "US Dollars", false, true, "#0.00"),
		JPY("¥", "JPY", "Japanese Yen", false, true, "#0"),
		GBP("£", "GBP", "Great British Pounds", false, true, "#0.00");

		private String symbol;
		private String abbr;
		private String name;
		private boolean symbolPosition; //false=before, true=after
		private boolean convertable;
		private String format;

		Currency(String symbol, String abbr, String name, boolean symbolPosition, boolean convertable, String format) {
			this.symbol = symbol;
			this.abbr = abbr;
			this.name = name;
			this.symbolPosition = symbolPosition;
			this.convertable = convertable;
			this.format = format;
		}

		public String getSymbol() {
			return symbol;
		}

		public String getAbbr() {
			return abbr;
		}

		public String getName() {
			return name;
		}

		public boolean getSymbolPosition() {
			return symbolPosition;
		}

		public boolean isConvertable() {
			return convertable;
		}

		public String getFormat() {
			return format;
		}

		public static Currency get(String pricingCurrencyMode) {
			try {
				return valueOf(pricingCurrencyMode);
			} catch (Exception e) {
			}
			return null;
		}
	}

	public static class StoreCardPriceRecord {

		@MongoId
		private String _id;

		private String cardName;
		private String setCode;
		private String url;
		private long timestamp;
		private String storeId;
		private Map<String, Price> prices;

		public StoreCardPriceRecord() {
		}

		public StoreCardPriceRecord(String cardName, String setCode, @Nullable String url, long timestamp, String storeId, Map<String, Price> prices) {
			this.cardName = cardName;
			this.setCode = setCode;
			this.url = url;
			this.timestamp = timestamp;
			this.storeId = storeId;
			this.prices = new HashMap<String, Price>() {{
				for (Entry<String, Price> entry : prices.entrySet())
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

		public Map<String, Price> getPrices() {
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
					.append("prices", prices)
					.toString();
		}
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
