package net.bemacized.grimoire.data.models.preferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jongo.marshall.jackson.oid.MongoId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class GuildPreferences {

	@MongoId
	private String guildId;
	private Map<Integer, Integer> preferences;
	private long timestamp;

	public GuildPreferences() {
	}

	public GuildPreferences(@Nullable String guildId, @Nullable String permissionString) {
		this.guildId = guildId;
		this.timestamp = System.currentTimeMillis();
		if (permissionString == null)
			permissionString = Grimoire.getInstance().getGuildPreferenceProvider().getDefaultPreferenceString();
		try {
			preferences = new HashMap<>();
			for (int i = 0; i < permissionString.length(); i += 4)
				preferences.put(Integer.valueOf(permissionString.substring(i, i + 2), 16), Integer.valueOf(permissionString.substring(i + 2, i + 4), 16));
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid permission string supplied");
		}
		if (!isValid()) throw new IllegalArgumentException("Invalid permission string supplied");
	}

	@Nonnull
	public String getPrefix() {
		String prefix = getMultiValue(0, getValue(0));
		if (prefix.equals("@")) prefix = "@" + Grimoire.BOT_NAME + " ";
		return prefix;
	}

	public boolean inlineCardReferencesEnabled() {
		return getValue(1) == 1;
	}

	public boolean inlinePriceReferencesEnabled() {
		return getValue(2) == 1;
	}

	public boolean showCardType() {
		return getValue(3) == 1;
	}

	public boolean showPowerToughness() {
		return getValue(4) == 1;
	}

	public boolean showManaCost() {
		return getValue(5) == 1;
	}

	public boolean showConvertedManaCost() {
		return getValue(6) == 1;
	}

	public boolean showThumbnail() {
		return getValue(7) == 1;
	}

	public boolean showOracleText() {
		return getValue(8) == 1;
	}

	public boolean showLegalFormats() {
		return getValue(9) == 1;
	}

	public boolean showPrintedRarities() {
		return getValue(10) == 1;
	}

	public boolean showPrintings() {
		return getValue(11) == 1;
	}

	public boolean showMiscProperties() {
		return getValue(12) == 1;
	}

	public boolean showFlavorText() {
		return getValue(16) == 1;
	}

	@Nonnull
	public String getTitleService() {
		return getMultiValue(13, getValue(13));
	}

	@Nonnull
	public String getPricePresentationMode() {
		return getMultiValue(14, getValue(14));
	}

	@Nonnull
	public String getPricingCurrencyMode() {
		return getMultiValue(15, getValue(15));
	}

	public boolean enabledMagicCardMarketStore() {
		return getValue(17) == 1;
	}

	public boolean enabledTCGPlayerStore() {
		return getValue(18) == 1;
	}

	public boolean enabledMTGGoldfishStore() {
		return getValue(19) == 1;
	}

	public boolean enabledScryfallStore() {
		return getValue(20) == 1;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getGuildId() {
		return guildId;
	}

	public String getPreferredLanguage() {
		return getMultiValue(23, getValue(23));
	}

	public boolean showPriceOnCard() {
		return getValue(21) == 1;
	}

	public boolean removeCommandCalls() {
		return getValue(22) == 1;
	}

	public boolean disableNonEnglishCardQueries() {
		return getValue(25) == 1;
	}

	public boolean disableLoadingMessages() {
		return getValue(26) == 1;
	}

	public boolean disableImageVerification() {
		return getValue(27) == 1;
	}

	public boolean disableScryfallPrintChecks() {
		return getValue(28) == 1;
	}

	public boolean preferLQImages() {
		return getValue(29) == 1;
	}

	@Nullable
	public String getCardUrl(MtgCard card) {
		switch (getTitleService()) {
			case "GATHERER":
				return card.getGathererUrl();
			case "SCRYFALL":
				return card.getScryfallUrl();
			case "MAGICCARDSINFO":
				return card.getMagicCardsInfoUrl();
			default:
				return card.getGathererUrl();
		}
	}

	private int getValue(int id) {
		if (preferences.containsKey(id)) return preferences.get(id);
		JsonArray defaultPreferences = Grimoire.getInstance().getGuildPreferenceProvider().getDefaultPreferences();
		if (defaultPreferences == null)
			throw new IllegalStateException("Cannot get default value without default preferences");
		JsonElement preference = StreamSupport.stream(defaultPreferences.spliterator(), false).filter(p -> p.getAsJsonObject().get("id").getAsInt() == id).findFirst().orElse(null);
		if (preference == null) throw new IllegalArgumentException("Invalid preference id supplied");
		int value = preference.getAsJsonObject().get("default").getAsInt();
		preferences.put(id, value);
		return value;
	}

	@Nonnull
	private String getMultiValue(int id, int valueIndex) {
		JsonArray defaultPreferences = Grimoire.getInstance().getGuildPreferenceProvider().getDefaultPreferences();
		if (defaultPreferences == null)
			throw new IllegalStateException("Cannot get multi value without default preferences");
		JsonElement preference = StreamSupport.stream(defaultPreferences.spliterator(), false).filter(p -> p.getAsJsonObject().get("id").getAsInt() == id).findFirst().orElse(null);
		if (preference == null) throw new IllegalArgumentException("Invalid preference id supplied");
		JsonObject preferenceObj = preference.getAsJsonObject();
		if (!preferenceObj.get("type").getAsString().equals("MULTI"))
			throw new IllegalArgumentException("Preference is not of type MULTI");
		JsonArray options = preferenceObj.getAsJsonArray("options");
		if (valueIndex >= options.size() || valueIndex < 0)
			throw new IllegalArgumentException("Value index provided is out of range");
		return options.get(valueIndex).getAsJsonArray().get(1).getAsString();
	}

	private boolean isValid() {
		JsonArray defaultPreferences = Grimoire.getInstance().getGuildPreferenceProvider().getDefaultPreferences();
		if (defaultPreferences == null)
			throw new IllegalStateException("Cannot determine validity without default preferences");
		if (preferences.isEmpty()) return false;
//		if (preferences.keySet().stream().anyMatch(id -> StreamSupport.stream(defaultPreferences.spliterator(), false).noneMatch(p -> p.getAsJsonObject().get("id").getAsInt() == id)))
//			return false;
		for (JsonElement preference : defaultPreferences) {
			JsonObject obj = preference.getAsJsonObject();
			int id = obj.get("id").getAsInt();
			if (!preferences.containsKey(id)) continue;
			int maxValue;
			switch (obj.get("type").getAsString()) {
				case "SWITCH":
					maxValue = 2;
					break;
				case "MULTI":
					maxValue = obj.get("options").getAsJsonArray().size();
					break;
				default:
					maxValue = -1;
					break;
			}
			int value = preferences.get(id);
			if (value >= maxValue) return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("guildId", guildId)
				.append("preferences", preferences)
				.append("timestamp", timestamp)
				.append("prefix", getPrefix())
				.append("inlineCardReferencesEnabled", inlineCardReferencesEnabled())
				.append("inlinePriceReferencesEnabled", inlinePriceReferencesEnabled())
				.append("showCardType", showCardType())
				.append("showPowerToughness", showPowerToughness())
				.append("showManaCost", showManaCost())
				.append("showConvertedManaCost", showConvertedManaCost())
				.append("showThumbnail", showThumbnail())
				.append("showOracleText", showOracleText())
				.append("showLegalFormats", showLegalFormats())
				.append("showPrintedRarities", showPrintedRarities())
				.append("showPrintings", showPrintings())
				.append("showMiscProperties", showMiscProperties())
				.append("showFlavorText", showFlavorText())
				.append("titleService", getTitleService())
				.append("pricePresentationMode", getPricePresentationMode())
				.append("pricingCurrencyMode", getPricingCurrencyMode())
				.toString();
	}
}
