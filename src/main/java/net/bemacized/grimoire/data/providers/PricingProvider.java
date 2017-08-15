package net.bemacized.grimoire.data.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.MtgCard;
import net.bemacized.grimoire.data.retrievers.storeretrievers.MagicCardMarketRetriever;
import net.bemacized.grimoire.data.retrievers.storeretrievers.StoreRetriever;
import net.bemacized.grimoire.data.retrievers.storeretrievers.TCGPlayerRetriever;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.io.IOUtils;
import org.jongo.MongoCollection;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PricingProvider {

	private final static String RECORDS_COLLECTION = "PricingRecords";
	private final static Logger LOG = Logger.getLogger(PricingProvider.class.getName());

	private List<StoreRetriever> stores;

	public PricingProvider() {
		JsonObject setDictionary;
		try {
			setDictionary = new JsonParser().parse(IOUtils.toString(getClass().getResourceAsStream("/SetDictionary.json"))).getAsJsonObject();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load SetDictionary.json. Pricing won't be available.", e);
			this.stores = new ArrayList<>();
			return;
		}
		this.stores = new ArrayList<StoreRetriever>() {{
			add(new MagicCardMarketRetriever(
					System.getenv("MCM_HOST"),
					System.getenv("MCM_TOKEN"),
					System.getenv("MCM_SECRET"),
					setDictionary
			));
			add(new TCGPlayerRetriever(
					System.getenv("TCG_HOST"),
					System.getenv("TCG_KEY"),
					setDictionary
			));
		}};
	}

	public List<StoreCardPrice> getPricing(MtgCard card) {

		return stores.parallelStream().map(store -> {
			// Check DB
			MongoCollection priceRecords = Grimoire.getInstance().getDBManager().getJongo().getCollection(RECORDS_COLLECTION);
			StoreRetriever.StoreCardPriceRecord record = priceRecords.findOne(String.format(
					"{cardName: %s, setCode: %s, storeId: %s}",
					JSONObject.quote(card.getName()),
					JSONObject.quote(card.getSet().getCode()),
					JSONObject.quote(store.getStoreId())
			)).as(StoreRetriever.StoreCardPriceRecord.class);

			// If present and within the timeout limit, return the db record
			if (record != null && System.currentTimeMillis() - record.getTimestamp() < store.timeout())
				return new StoreCardPrice(StoreCardPriceStatus.SUCCESS, card, store.getStoreName(), store.getStoreId(), record);

			// If not we get a fresh price
			try {
				StoreRetriever.StoreCardPriceRecord price = store.retrievePrice(card);
				if (price != null)
					Grimoire.getInstance().getDBManager().getJongo().getCollection(RECORDS_COLLECTION).save(price);
				return new StoreCardPrice((price == null) ? StoreCardPriceStatus.CARD_UNKNOWN : StoreCardPriceStatus.SUCCESS, card, store.getStoreName(), store.getStoreId(), price);
			} catch (StoreRetriever.StoreServerErrorException e) {
				return new StoreCardPrice(StoreCardPriceStatus.SERVER_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreRetriever.StoreAuthException e) {
				return new StoreCardPrice(StoreCardPriceStatus.AUTH_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreRetriever.UnknownStoreException e) {
				return new StoreCardPrice(StoreCardPriceStatus.UNKNOWN_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreRetriever.StoreSetUnknownException e) {
				return new StoreCardPrice(StoreCardPriceStatus.SET_UNSUPPORTED, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreRetriever.LanguageUnsupportedException e) {
				return new StoreCardPrice(StoreCardPriceStatus.LANGUAGE_UNSUPPORTED, card, store.getStoreName(), store.getStoreId(), null);
			}
		}).collect(Collectors.toList());
	}

	public MessageEmbed getPricingEmbed(MtgCard card) {
		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm z");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		// Get pricing
		List<StoreCardPrice> pricing = getPricing(card);
		// Build embeds
		EmbedBuilder priceEmbed = new EmbedBuilder();
		priceEmbed.setTitle("Pricing: " + card.getName(), card.getGathererUrl());
		priceEmbed.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		priceEmbed.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		pricing.forEach(storeprice -> {
			DecimalFormat formatter = new DecimalFormat("#.00");
			String priceText = "N/A";
			switch (storeprice.getStatus()) {
				case SUCCESS:
					priceText = String.join("\n", storeprice.getRecord().getPrices().entrySet().parallelStream().sorted(Comparator.comparing(Map.Entry::getKey)).map(price -> String.format(
							"%s: **%s%s**",
							price.getKey(),
							(price.getValue() > 0) ? storeprice.getRecord().getCurrency() : "",
							(price.getValue() > 0) ? formatter.format(price.getValue()) : "N/A"
					)).collect(Collectors.toList()));
					priceText += "\n**Last updated: **" + sdf.format(new Date(storeprice.getRecord().getTimestamp()));
					priceText += String.format("\n[`[%s]`](%s)", "Store Page", storeprice.getRecord().getUrl());
					break;
				case UNKNOWN_ERROR:
					priceText = "An unknown error occurred.";
					break;
				case CARD_UNKNOWN:
					priceText = "No results.";
					break;
				case AUTH_ERROR:
					priceText = "Could not authenticate.";
					break;
				case SET_UNSUPPORTED:
					priceText = "Set not supported.";
					break;
				case SERVER_ERROR:
					priceText = "Store is having server problems.";
					break;
				case LANGUAGE_UNSUPPORTED:
					priceText = "Language not supported.";
					break;
			}
			priceEmbed.addField(storeprice.getStoreName(), priceText, true);
		});

		return priceEmbed.build();
	}

	public class StoreCardPrice {
		private StoreCardPriceStatus status;
		private MtgCard card;
		private String storeName;
		private String storeId;
		private StoreRetriever.StoreCardPriceRecord record;

		public StoreCardPrice(StoreCardPriceStatus status, MtgCard card, String storeName, String storeId, @Nullable StoreRetriever.StoreCardPriceRecord record) {
			this.status = status;
			this.card = card;
			this.storeName = storeName;
			this.storeId = storeId;
			this.record = record;
		}

		public StoreCardPriceStatus getStatus() {
			return status;
		}

		public MtgCard getCard() {
			return card;
		}

		public String getStoreName() {
			return storeName;
		}

		public String getStoreId() {
			return storeId;
		}

		public StoreRetriever.StoreCardPriceRecord getRecord() {
			return record;
		}
	}

	public enum StoreCardPriceStatus {
		SUCCESS,
		SERVER_ERROR,
		AUTH_ERROR,
		SET_UNSUPPORTED,
		UNKNOWN_ERROR,
		CARD_UNKNOWN,
		LANGUAGE_UNSUPPORTED
	}
}