package net.bemacized.grimoire.model.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.Dependency;
import net.bemacized.grimoire.model.models.storeAPIs.MagicCardMarketAPI;
import net.bemacized.grimoire.model.models.storeAPIs.StoreAPI;
import net.bemacized.grimoire.model.models.storeAPIs.TCGPlayerAPI;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PricingManager {

	private final static Logger LOG = Logger.getLogger(PricingManager.class.getName());

	private List<StoreAPI> stores;

	public PricingManager() {
		this.stores = new ArrayList<StoreAPI>() {{
			add(new MagicCardMarketAPI(
					System.getenv("MCM_HOST"),
					System.getenv("MCM_TOKEN"),
					System.getenv("MCM_SECRET")
			));
			add(new TCGPlayerAPI(
					System.getenv("TCG_HOST"),
					System.getenv("TCG_KEY")
			));
		}};
	}

	public List<StoreCardPrice> getPricing(Card card) {
		return stores.parallelStream().map(store -> {
			try {
				StoreAPI.StoreCardPriceRecord price = store.getPrice(card);
				return new StoreCardPrice((price == null) ? StoreCardPriceStatus.CARD_UNKNOWN : StoreCardPriceStatus.SUCCESS, card, store.getStoreName(), store.getStoreId(), price);
			} catch (StoreAPI.StoreServerErrorException e) {
				return new StoreCardPrice(StoreCardPriceStatus.SERVER_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreAPI.StoreAuthException e) {
				return new StoreCardPrice(StoreCardPriceStatus.AUTH_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreAPI.UnknownStoreException e) {
				return new StoreCardPrice(StoreCardPriceStatus.UNKNOWN_ERROR, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreAPI.StoreSetUnknownException e) {
				return new StoreCardPrice(StoreCardPriceStatus.SET_UNSUPPORTED, card, store.getStoreName(), store.getStoreId(), null);
			} catch (StoreAPI.LanguageUnsupportedException e) {
				return new StoreCardPrice(StoreCardPriceStatus.LANGUAGE_UNSUPPORTED, card, store.getStoreName(), store.getStoreId(), null);
			}
		}).collect(Collectors.toList());
	}

	public MessageEmbed getPricingEmbed(Card card) {
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

	public void init() {
		// Retrieve set dictionary
		Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("SET_DICTIONARY");
		d.retrieve();
		if (!d.retrieve()) {
			LOG.severe("Could not load set dictionary.");
			return;
		}
		String json = d.getString();
		d.release();

		// Parse set dictionary
		final JsonObject obj = new JsonParser().parse(json).getAsJsonObject();

		this.stores.forEach(store -> {
			try {
				store.updateSets(obj);
			} catch (StoreAPI.StoreAuthException e) {
				LOG.log(Level.SEVERE, "Authentication error occurred with " + store.getStoreName() + " while updating set names", e);
			} catch (StoreAPI.StoreServerErrorException e) {
				LOG.log(Level.WARNING, "Server error occurred with " + store.getStoreName() + " while updating set names", e);
			} catch (StoreAPI.UnknownStoreException e) {
				LOG.log(Level.SEVERE, "Unknown error occurred with " + store.getStoreName() + " while updating set names", e);
			}
		});
	}

	public class StoreCardPrice {
		private StoreCardPriceStatus status;
		private Card card;
		private String storeName;
		private String storeId;
		private StoreAPI.StoreCardPriceRecord record;

		public StoreCardPrice(StoreCardPriceStatus status, Card card, String storeName, String storeId, StoreAPI.StoreCardPriceRecord record) {
			this.status = status;
			this.card = card;
			this.storeName = storeName;
			this.storeId = storeId;
			this.record = record;
		}

		public StoreCardPriceStatus getStatus() {
			return status;
		}

		public Card getCard() {
			return card;
		}

		public String getStoreName() {
			return storeName;
		}

		public String getStoreId() {
			return storeId;
		}

		public StoreAPI.StoreCardPriceRecord getRecord() {
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