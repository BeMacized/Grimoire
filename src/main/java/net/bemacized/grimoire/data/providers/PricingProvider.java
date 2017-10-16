package net.bemacized.grimoire.data.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.storeretrievers.*;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jongo.MongoCollection;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
			if (System.getenv("MCM_HOST") != null && System.getenv("MCM_TOKEN") != null && System.getenv("MCM_SECRET") != null)
				add(new MagicCardMarketRetriever(
						System.getenv("MCM_HOST"),
						System.getenv("MCM_TOKEN"),
						System.getenv("MCM_SECRET"),
						setDictionary
				));
			if (System.getenv("TCG_HOST") != null && System.getenv("TCG_KEY") != null)
				add(new TCGPlayerRetriever(
						System.getenv("TCG_HOST"),
						System.getenv("TCG_KEY"),
						setDictionary
				));
			add(new MTGGoldfishRetriever());
			add(new ScryfallPriceRetriever());
		}};
	}

	public List<StoreCardPrice> getPricing(MtgCard card) {
		return getPricing(card, null);
	}

	public List<StoreCardPrice> getPricing(MtgCard card, @Nullable List<Class<? extends StoreRetriever>> retrievers) {

		return stores.parallelStream()
				.filter(store -> retrievers == null || retrievers.size() == 0 || retrievers.parallelStream().anyMatch(r -> r.isInstance(store)))
				.map(store -> {
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
						LOG.log(Level.WARNING, "Store produced error", e);
						return new StoreCardPrice(StoreCardPriceStatus.SERVER_ERROR, card, store.getStoreName(), store.getStoreId(), null);
					} catch (StoreRetriever.StoreAuthException e) {
						LOG.log(Level.WARNING, "Store produced AUTH error", e);
						return new StoreCardPrice(StoreCardPriceStatus.AUTH_ERROR, card, store.getStoreName(), store.getStoreId(), null);
					} catch (StoreRetriever.UnknownStoreException e) {
						LOG.log(Level.WARNING, "Store produced unknown error", e);
						return new StoreCardPrice(StoreCardPriceStatus.UNKNOWN_ERROR, card, store.getStoreName(), store.getStoreId(), null);
					} catch (StoreRetriever.LanguageUnsupportedException e) {
						return new StoreCardPrice(StoreCardPriceStatus.LANGUAGE_UNSUPPORTED, card, store.getStoreName(), store.getStoreId(), null);
					}
				}).collect(Collectors.toList());
	}

	public MessageEmbed getPricingEmbed(MtgCard card, GuildPreferences guildPreferences) {
		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm z");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		EmbedBuilder embed = new EmbedBuilder();

		embed.setTitle("Pricing: " + card.getName(), guildPreferences.getCardUrl(card));
		embed.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));

		// Check layout
		switch (guildPreferences.getPricePresentationMode()) {
			case "ALL_MARKETS": {
				List<Class<? extends StoreRetriever>> stores = new ArrayList<>();
				if (guildPreferences.enabledMagicCardMarketStore()) stores.add(MagicCardMarketRetriever.class);
				if (guildPreferences.enabledTCGPlayerStore()) stores.add(TCGPlayerRetriever.class);
				if (guildPreferences.enabledMTGGoldfishStore()) stores.add(MTGGoldfishRetriever.class);
				if (guildPreferences.enabledScryfallStore()) stores.add(ScryfallPriceRetriever.class);
				List<StoreCardPrice> pricing = getPricing(card, stores);
				embed.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
				pricing.forEach(storeprice -> {
					switch (storeprice.getStatus()) {
						case SUCCESS:
							StoreRetriever.Currency targetCurrency = StoreRetriever.Currency.get(guildPreferences.getPricingCurrencyMode());
							List<Map.Entry<String, StoreRetriever.Price>> prices = storeprice.getRecord()
									.getPrices()
									.entrySet()
									.parallelStream()
									.sorted(Comparator.comparing(Map.Entry::getKey))
									.map(price -> {
										StoreRetriever.Price priceVal = targetCurrency == null || !targetCurrency.isConvertable() || !price.getValue().getCurrency().isConvertable()
												? price.getValue()
												: (price.getValue().convertTo(targetCurrency));
										if (priceVal != null) price.setValue(priceVal);
										return price;
									})
									.filter(Objects::nonNull)
									.collect(Collectors.toList());

							// Scryfall exception
//							if (targetCurrency != null && storeprice.getStoreId().equals(new ScryfallPriceRetriever().getStoreId()))
//								prices = prices.parallelStream()
//										.map(p -> new AbstractMap.SimpleEntry<>(p.getValue().getCurrency() == targetCurrency ? "Average" : p.getKey(), p.getValue()))
//										.filter(StreamUtils.distinctByKey(AbstractMap.SimpleEntry::getKey))
//										.collect(Collectors.toList());

							List<StoreRetriever.Currency> convertedFrom = prices.parallelStream().map(p -> p.getValue().getConvertedFrom()).filter(Objects::nonNull).distinct().collect(Collectors.toList());

							String priceText = prices.isEmpty()
									? "No results."
									: String.join("\n", prices.parallelStream().map(price -> price.getKey() + ": **" + price.getValue().toString() + "**").collect(Collectors.toList()))
									+ "\n**Last updated: **" + sdf.format(new Date(storeprice.getRecord().getTimestamp()))
									+ (storeprice.getRecord().getUrl() == null ? "" : String.format("\n[`[%s]`](%s)", "Store Page", storeprice.getRecord().getUrl()))
									+ (convertedFrom.isEmpty() ? "" : "\n_Converted from " + StringUtils.reverse(StringUtils.reverse(String.join(", ", convertedFrom.parallelStream().map(StoreRetriever.Currency::getAbbr).collect(Collectors.toList()))).replaceFirst(",", "& ")) + "_");
							embed.addField(storeprice.getStoreName(), priceText, true);
							break;
						case UNKNOWN_ERROR:
							embed.addField(storeprice.getStoreName(), "An unknown error occurred.", true);
							break;
						case CARD_UNKNOWN:
							embed.addField(storeprice.getStoreName(), "No results.", true);
							break;
						case AUTH_ERROR:
							embed.addField(storeprice.getStoreName(), "Could not authenticate.", true);
							break;
						case SERVER_ERROR:
							embed.addField(storeprice.getStoreName(), "Store is having server problems.", true);
							break;
						case LANGUAGE_UNSUPPORTED:
							embed.addField(storeprice.getStoreName(), "Language not supported.", true);
							break;
					}
				});
				break;
			}
			case "SCRYFALL_ONE": {
				List<StoreCardPrice> pricing = getPricing(card, Stream.of(ScryfallPriceRetriever.class).collect(Collectors.toList()));
				pricing.parallelStream().filter(p -> p.getStoreId().equals(new ScryfallPriceRetriever().getStoreId())).findFirst().ifPresent(storeprice -> {
					switch (storeprice.getStatus()) {
						case SUCCESS:
							List<String> prices = new ArrayList<>();
							if (guildPreferences.getPricingCurrencyMode().equals("DEFAULT"))
								storeprice.getRecord().getPrices().forEach((key, value) -> prices.add(value.toString()));
							else {
								StoreRetriever.Currency targetCurrency = StoreRetriever.Currency.valueOf(guildPreferences.getPricingCurrencyMode());
								Supplier<Stream<StoreRetriever.Price>> getPrices = () -> storeprice
										.getRecord().getPrices().values().parallelStream()
										.sorted(Comparator.comparing(o -> o.getCurrency().name()));
								StoreRetriever.Price price = getPrices.get()
										.filter(p -> p.getCurrency() == targetCurrency)
										.findFirst().orElseGet(() -> {
											StoreRetriever.Price source = getPrices.get().filter(p -> p.getCurrency().isConvertable()).findFirst().orElse(null);
											if (source == null) return null;
											return source.convertTo(targetCurrency);
										});
								if (price != null) prices.add(price.toString(true));
							}
							String priceString = prices.isEmpty() ? "No pricing available." : String.join(" • ", prices);
							embed.addField(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()), priceString, true);
							break;
						case UNKNOWN_ERROR:
							embed.addField("", "An unknown error occurred.", true);
							break;
						case CARD_UNKNOWN:
							embed.addField("", "No results for card.", true);
							break;
						case AUTH_ERROR:
							embed.addField("", "Could not authenticate.", true);
							break;
						case SERVER_ERROR:
							embed.addField("", "Scryfall is having server issues. Please try again later.", true);
							break;
						case LANGUAGE_UNSUPPORTED:
							embed.addField("", "Card language not supported.", true);
							break;
					}
				});
				break;
			}
			case "SCRYFALL_ALL": {
				try {
					if (card.getTypeLine().contains("Basic Land")) {
						embed.addField("", "Due to the fact that **basic lands** are in every set, I cannot give a price overview in the currently selected price presentation mode.\n\nIf you want to check pricing for basic lands, please configure a different price presentation mode in the [Dashboard](" + Grimoire.WEBSITE + "/dashboard).", true);
						break;
					}
					card.getAllPrintings(-1).forEach(c -> {
						List<StoreCardPrice> pricing = getPricing(c, Stream.of(ScryfallPriceRetriever.class).collect(Collectors.toList()));
						pricing.parallelStream().filter(p -> p.getStoreId().equals(new ScryfallPriceRetriever().getStoreId())).findFirst().ifPresent(storeprice -> {
							switch (storeprice.getStatus()) {
								case SUCCESS:
									List<String> prices = new ArrayList<>();
									if (guildPreferences.getPricingCurrencyMode().equals("DEFAULT"))
										storeprice.getRecord().getPrices().forEach((key, value) -> prices.add(value.toString()));
									else {
										StoreRetriever.Currency targetCurrency = StoreRetriever.Currency.valueOf(guildPreferences.getPricingCurrencyMode());
										Supplier<Stream<StoreRetriever.Price>> getPrices = () -> storeprice
												.getRecord().getPrices().values().parallelStream()
												.sorted(Comparator.comparing(o -> o.getCurrency().name()));
										StoreRetriever.Price price = getPrices.get()
												.filter(p -> p.getCurrency() == targetCurrency)
												.findFirst().orElseGet(() -> {
													StoreRetriever.Price source = getPrices.get().filter(p -> p.getCurrency().isConvertable()).findFirst().orElse(null);
													if (source == null) return null;
													return source.convertTo(targetCurrency);
												});
										if (price != null) prices.add(price.toString(true));
									}
									String priceString = prices.isEmpty() ? "No pricing available." : String.join(" • ", prices);
									embed.addField(String.format("%s (%s)", c.getSet().getName(), c.getSet().getCode()), priceString, false);
									break;
								case UNKNOWN_ERROR:
									embed.addField("", "An unknown error occurred.", true);
									break;
								case CARD_UNKNOWN:
									embed.addField("", "No results for card.", true);
									break;
								case AUTH_ERROR:
									embed.addField("", "Could not authenticate.", true);
									break;
								case SERVER_ERROR:
									embed.addField("", "Scryfall is having server issues. Please try again later.", true);
									break;
								case LANGUAGE_UNSUPPORTED:
									embed.addField("", "Card language not supported.", true);
									break;
							}
						});
					});
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
					LOG.log(Level.WARNING, "Scryfall Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
					LOG.log(Level.SEVERE, "Unknown Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				}
				break;
			}
			case "MTGO_ONE": {
				if (card.getTypeLine().contains("Basic Land")) {
					embed.addField("", "Due to the fact that **basic lands** are in every set, I cannot give a price overview in the currently selected price presentation mode.\n\nIf you want to check pricing for basic lands, please configure a different price presentation mode in the [Dashboard](" + Grimoire.WEBSITE + "/dashboard).", true);
					break;
				}
				try {
					List<MtgCard> prints = card.getAllPrintings(-1);
					prints.remove(card);
					prints.add(0, card);
					for (MtgCard c : prints) {
						List<Class<? extends StoreRetriever>> stores = new ArrayList<>();
						if (guildPreferences.enabledMTGGoldfishStore()) stores.add(MTGGoldfishRetriever.class);
						if (guildPreferences.enabledScryfallStore()) stores.add(ScryfallPriceRetriever.class);

						List<StoreCardPrice> pricing = getPricing(c, stores);

						final Map<StoreCardPrice, Boolean> prices = new HashMap<>();
						pricing.forEach(storeprice -> {
							switch (storeprice.getStatus()) {
								case SUCCESS:
									prices.put(storeprice, storeprice.getRecord().getPrices().get("MTGO") != null);
									break;
							}
						});
						if (prices.values().contains(true)) {
							embed.setDescription(String.format("%s (%s)", c.getSet().getName(), c.getSet().getCode()));
							if (!c.getScryfallId().equalsIgnoreCase(card.getScryfallId()))
								embed.appendDescription(String.format("\n(There was no MTGO pricing for the **%s** print. Showing the **%s** print instead.)", card.getSet().getCode(), c.getSet().getCode()));
							prices.entrySet().parallelStream().filter(e -> e.getValue()).forEachOrdered(e -> {
								String priceText = e.getKey().getRecord().getPrices().get("MTGO").toString()
										+ "\n**Last updated: **" + sdf.format(new Date(e.getKey().getRecord().getTimestamp()))
										+ (e.getKey().getRecord().getUrl() == null ? "" : String.format("\n[`[%s]`](%s)", "Store Page", e.getKey().getRecord().getUrl()));
								embed.addField(e.getKey().getStoreName(), priceText, true);
							});
							break;
						}
					}
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
					LOG.log(Level.WARNING, "Scryfall Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
					LOG.log(Level.SEVERE, "Unknown Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				}
				if (embed.getFields().isEmpty())
					embed.setDescription("There are no prices available.");
				break;
			}
			case "MTGO_ALL": {
				if (card.getTypeLine().contains("Basic Land")) {
					embed.addField("", "Due to the fact that **basic lands** are in every set, I cannot give a price overview in the currently selected price presentation mode.\n\nIf you want to check pricing for basic lands, please configure a different price presentation mode in the [Dashboard](" + Grimoire.WEBSITE + "/dashboard).", true);
					break;
				}
				try {
					card.getAllPrintings(-1).forEach(c -> {
						List<Class<? extends StoreRetriever>> stores = new ArrayList<>();
						if (guildPreferences.enabledMTGGoldfishStore()) stores.add(MTGGoldfishRetriever.class);
						if (guildPreferences.enabledScryfallStore()) stores.add(ScryfallPriceRetriever.class);

						List<StoreCardPrice> pricing = getPricing(c, stores);

						final Map<StoreCardPrice, Boolean> prices = new HashMap<>();
						pricing.forEach(storeprice -> {
							switch (storeprice.getStatus()) {
								case SUCCESS:
									prices.put(storeprice, storeprice.getRecord().getPrices().get("MTGO") != null);
									break;
							}
						});
						if (prices.values().contains(true))
							embed.addField(c.getSet().getName() + " (" + c.getSet().getCode() + ")", String.join("\n", prices.entrySet().parallelStream().filter(Map.Entry::getValue).map(e ->
											e.getKey().getStoreName() + ": **" + e.getKey().getRecord().getPrices().get("MTGO").toString() + "**").collect(Collectors.toList())),
									true);
					});
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
					LOG.log(Level.WARNING, "Scryfall Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
					LOG.log(Level.SEVERE, "Unknown Error occurred when getting all printings", e);
					embed.setDescription("Could not retrieve all printings for \"" + card.getName() + "\". The error has been logged. Please try again later!");
				}
				if (embed.getFields().isEmpty())
					embed.setDescription("There are no prices available.");
				break;
			}
		}
		return embed.build();
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
		UNKNOWN_ERROR,
		CARD_UNKNOWN,
		LANGUAGE_UNSUPPORTED
	}
}