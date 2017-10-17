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

    private String prefix;
    private boolean inlineCardReferencesEnabled;
    private boolean inlinePriceReferencesEnabled;
    private boolean showCardType;
    private boolean showPowerToughness;
    private boolean showManaCost;
    private boolean showConvertedManaCost;
    private boolean showThumbnail;
    private boolean showOracleText;
    private boolean showLegalFormats;
    private boolean showPrintedRarities;
    private boolean showPrintings;
    private boolean showMiscProperties;
    private boolean showFlavorText;
    private String getTitleService;
    private String getPricePresentationMode;
    private String getPricingCurrencyMode;
    private boolean enabledMagicCardMarketStore;
    private boolean enabledTCGPlayerStore;
    private boolean enabledMTGGoldfishStore;
    private boolean enabledScryfallStore;
    private String preferredLanguage;
    private boolean showPriceOnCard;
    private boolean removeCommandCalls;
    private boolean disableNonEnglishCardQueries;
    private boolean disableLoadingMessages;
    private boolean disableImageVerification;
    private boolean disableScryfallPrintChecks;
    private boolean preferLQImages;
    private boolean showCanlanderPoints;
    private boolean showAuslanderPoints;
    private boolean showColorIdentity;

    public GuildPreferences() {
    }

    public GuildPreferences(GuildPreferences original) {
        this.prefix = original.prefix;
        this.inlineCardReferencesEnabled = original.inlineCardReferencesEnabled;
        this.inlinePriceReferencesEnabled = original.inlinePriceReferencesEnabled;
        this.showCardType = original.showCardType;
        this.showPowerToughness = original.showPowerToughness;
        this.showManaCost = original.showManaCost;
        this.showConvertedManaCost = original.showConvertedManaCost;
        this.showThumbnail = original.showThumbnail;
        this.showOracleText = original.showOracleText;
        this.showLegalFormats = original.showLegalFormats;
        this.showPrintedRarities = original.showPrintedRarities;
        this.showPrintings = original.showPrintings;
        this.showMiscProperties = original.showMiscProperties;
        this.showFlavorText = original.showFlavorText;
        this.getTitleService = original.getTitleService;
        this.getPricePresentationMode = original.getPricePresentationMode;
        this.getPricingCurrencyMode = original.getPricingCurrencyMode;
        this.enabledMagicCardMarketStore = original.enabledMagicCardMarketStore;
        this.enabledTCGPlayerStore = original.enabledTCGPlayerStore;
        this.enabledMTGGoldfishStore = original.enabledMTGGoldfishStore;
        this.enabledScryfallStore = original.enabledScryfallStore;
        this.preferredLanguage = original.preferredLanguage;
        this.showPriceOnCard = original.showPriceOnCard;
        this.removeCommandCalls = original.removeCommandCalls;
        this.disableNonEnglishCardQueries = original.disableNonEnglishCardQueries;
        this.disableLoadingMessages = original.disableLoadingMessages;
        this.disableImageVerification = original.disableImageVerification;
        this.disableScryfallPrintChecks = original.disableScryfallPrintChecks;
        this.preferLQImages = original.preferLQImages;
        this.showCanlanderPoints = original.showCanlanderPoints;
        this.showAuslanderPoints = original.showAuslanderPoints;
        this.showColorIdentity = original.showColorIdentity;
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

        this.prefix = getMultiValue(0, getValue(0));
        if (this.prefix.equals("@")) this.prefix = "@" + Grimoire.BOT_NAME + " ";
        this.inlineCardReferencesEnabled = getValue(1) == 1;
        this.inlinePriceReferencesEnabled = getValue(2) == 1;
        this.showCardType = getValue(3) == 1;
        this.showPowerToughness = getValue(4) == 1;
        this.showManaCost = getValue(5) == 1;
        this.showConvertedManaCost = getValue(6) == 1;
        this.showThumbnail = getValue(7) == 1;
        this.showOracleText = getValue(8) == 1;
        this.showLegalFormats = getValue(9) == 1;
        this.showPrintedRarities = getValue(10) == 1;
        this.showPrintings = getValue(11) == 1;
        this.showMiscProperties = getValue(12) == 1;
        this.showFlavorText = getValue(16) == 1;
        this.getTitleService = getMultiValue(13, getValue(13));
        this.getPricePresentationMode = getMultiValue(14, getValue(14));
        this.getPricingCurrencyMode = getMultiValue(15, getValue(15));
        this.enabledMagicCardMarketStore = getValue(17) == 1;
        this.enabledTCGPlayerStore = getValue(18) == 1;
        this.enabledMTGGoldfishStore = getValue(19) == 1;
        this.enabledScryfallStore = getValue(20) == 1;
        this.preferredLanguage = getMultiValue(23, getValue(23));
        this.showPriceOnCard = getValue(21) == 1;
        this.removeCommandCalls = getValue(22) == 1;
        this.disableNonEnglishCardQueries = getValue(25) == 1;
        this.disableLoadingMessages = getValue(26) == 1;
        this.disableImageVerification = getValue(27) == 1;
        this.disableScryfallPrintChecks = getValue(28) == 1;
        this.preferLQImages = getValue(29) == 1;
        this.showCanlanderPoints = getValue(30) == 1;
        this.showAuslanderPoints = getValue(31) == 1;
        this.showColorIdentity = getValue(32) == 1;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean areInlineCardReferencesEnabled() {
        return inlineCardReferencesEnabled;
    }

    public boolean areInlinePriceReferencesEnabled() {
        return inlinePriceReferencesEnabled;
    }

    public boolean showCardType() {
        return showCardType;
    }

    public boolean showPowerToughness() {
        return showPowerToughness;
    }

    public boolean showManaCost() {
        return showManaCost;
    }

    public boolean showConvertedManaCost() {
        return showConvertedManaCost;
    }

    public boolean showThumbnail() {
        return showThumbnail;
    }

    public boolean showOracleText() {
        return showOracleText;
    }

    public boolean showLegalFormats() {
        return showLegalFormats;
    }

    public boolean showPrintedRarities() {
        return showPrintedRarities;
    }

    public boolean showPrintings() {
        return showPrintings;
    }

    public boolean showMiscProperties() {
        return showMiscProperties;
    }

    public boolean showFlavorText() {
        return showFlavorText;
    }

    public String getTitleService() {
        return getTitleService;
    }

    public String getPricePresentationMode() {
        return getPricePresentationMode;
    }

    public String getPricingCurrencyMode() {
        return getPricingCurrencyMode;
    }

    public boolean enabledMagicCardMarketStore() {
        return enabledMagicCardMarketStore;
    }

    public boolean enabledTCGPlayerStore() {
        return enabledTCGPlayerStore;
    }

    public boolean enabledMTGGoldfishStore() {
        return enabledMTGGoldfishStore;
    }

    public boolean enabledScryfallStore() {
        return enabledScryfallStore;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public boolean showPriceOnCard() {
        return showPriceOnCard;
    }

    public boolean removeCommandCalls() {
        return removeCommandCalls;
    }

    public boolean disableNonEnglishCardQueries() {
        return disableNonEnglishCardQueries;
    }

    public boolean disableLoadingMessages() {
        return disableLoadingMessages;
    }

    public boolean disableImageVerification() {
        return disableImageVerification;
    }

    public boolean disableScryfallPrintChecks() {
        return disableScryfallPrintChecks;
    }

    public boolean preferLQImages() {
        return preferLQImages;
    }

    public boolean showCanlanderPoints() {
        return showCanlanderPoints;
    }

    public boolean showAuslanderPoints() {
        return showAuslanderPoints;
    }

    public boolean showColorIdentity() {
        return showColorIdentity;
    }

    public String getGuildId() {
        return guildId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setInlineCardReferencesEnabled(boolean inlineCardReferencesEnabled) {
        this.inlineCardReferencesEnabled = inlineCardReferencesEnabled;
    }

    public void setInlinePriceReferencesEnabled(boolean inlinePriceReferencesEnabled) {
        this.inlinePriceReferencesEnabled = inlinePriceReferencesEnabled;
    }

    public void setShowCardType(boolean showCardType) {
        this.showCardType = showCardType;
    }

    public void setShowPowerToughness(boolean showPowerToughness) {
        this.showPowerToughness = showPowerToughness;
    }

    public void setShowManaCost(boolean showManaCost) {
        this.showManaCost = showManaCost;
    }

    public void setShowConvertedManaCost(boolean showConvertedManaCost) {
        this.showConvertedManaCost = showConvertedManaCost;
    }

    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
    }

    public void setShowOracleText(boolean showOracleText) {
        this.showOracleText = showOracleText;
    }

    public void setShowLegalFormats(boolean showLegalFormats) {
        this.showLegalFormats = showLegalFormats;
    }

    public void setShowPrintedRarities(boolean showPrintedRarities) {
        this.showPrintedRarities = showPrintedRarities;
    }

    public void setShowPrintings(boolean showPrintings) {
        this.showPrintings = showPrintings;
    }

    public void setShowMiscProperties(boolean showMiscProperties) {
        this.showMiscProperties = showMiscProperties;
    }

    public void setShowFlavorText(boolean showFlavorText) {
        this.showFlavorText = showFlavorText;
    }

    public void setGetTitleService(String getTitleService) {
        this.getTitleService = getTitleService;
    }

    public void setGetPricePresentationMode(String getPricePresentationMode) {
        this.getPricePresentationMode = getPricePresentationMode;
    }

    public void setGetPricingCurrencyMode(String getPricingCurrencyMode) {
        this.getPricingCurrencyMode = getPricingCurrencyMode;
    }

    public void setEnabledMagicCardMarketStore(boolean enabledMagicCardMarketStore) {
        this.enabledMagicCardMarketStore = enabledMagicCardMarketStore;
    }

    public void setEnabledTCGPlayerStore(boolean enabledTCGPlayerStore) {
        this.enabledTCGPlayerStore = enabledTCGPlayerStore;
    }

    public void setEnabledMTGGoldfishStore(boolean enabledMTGGoldfishStore) {
        this.enabledMTGGoldfishStore = enabledMTGGoldfishStore;
    }

    public void setEnabledScryfallStore(boolean enabledScryfallStore) {
        this.enabledScryfallStore = enabledScryfallStore;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setShowPriceOnCard(boolean showPriceOnCard) {
        this.showPriceOnCard = showPriceOnCard;
    }

    public void setRemoveCommandCalls(boolean removeCommandCalls) {
        this.removeCommandCalls = removeCommandCalls;
    }

    public void setDisableNonEnglishCardQueries(boolean disableNonEnglishCardQueries) {
        this.disableNonEnglishCardQueries = disableNonEnglishCardQueries;
    }

    public void setDisableLoadingMessages(boolean disableLoadingMessages) {
        this.disableLoadingMessages = disableLoadingMessages;
    }

    public void setDisableImageVerification(boolean disableImageVerification) {
        this.disableImageVerification = disableImageVerification;
    }

    public void setDisableScryfallPrintChecks(boolean disableScryfallPrintChecks) {
        this.disableScryfallPrintChecks = disableScryfallPrintChecks;
    }

    public void setPreferLQImages(boolean preferLQImages) {
        this.preferLQImages = preferLQImages;
    }

    public void setShowCanlanderPoints(boolean showCanlanderPoints) {
        this.showCanlanderPoints = showCanlanderPoints;
    }

    public void setShowAuslanderPoints(boolean showAuslanderPoints) {
        this.showAuslanderPoints = showAuslanderPoints;
    }

    public void setShowColorIdentity(boolean showColorIdentity) {
        this.showColorIdentity = showColorIdentity;
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
                .append("inlineCardReferencesEnabled", inlineCardReferencesEnabled)
                .append("inlinePriceReferencesEnabled", inlinePriceReferencesEnabled)
                .append("showCardType", showCardType)
                .append("showPowerToughness", showPowerToughness)
                .append("showManaCost", showManaCost)
                .append("showConvertedManaCost", showConvertedManaCost)
                .append("showThumbnail", showThumbnail)
                .append("showOracleText", showOracleText)
                .append("showLegalFormats", showLegalFormats)
                .append("showPrintedRarities", showPrintedRarities)
                .append("showPrintings", showPrintings)
                .append("showMiscProperties", showMiscProperties)
                .append("showFlavorText", showFlavorText)
                .append("titleService", getTitleService)
                .append("pricePresentationMode", getPricePresentationMode)
                .append("pricingCurrencyMode", getPricingCurrencyMode)
                .toString();
    }
}
