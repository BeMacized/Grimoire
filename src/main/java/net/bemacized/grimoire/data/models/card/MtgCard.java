package net.bemacized.grimoire.data.models.card;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.GathererRetriever;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal"/*, "unused"*/, "WeakerAccess"})
public class MtgCard {

	private static final Logger LOG = Logger.getLogger(MtgCard.class.getName());

	private int multiverseId;
	private String name;
	private String manacost;
	private String cmc;
	private String language;
	private String typeLine;
	private ScryfallCard.Rarity rarity;
	private ScryfallSet set;
	private String scryfallId;
	private String[] colorIdentity = new String[0];
	private HashMap<String, ScryfallCard.Legality> legalities = new HashMap<>();
	private String text;
	private String power;
	private String toughness;
	private String flavorText;
	private String loyalty;
	private String vgHandModifier;
	private String vgLifeModifier;
	private String scryfallUrl;
	private String number;
	private MtgJsonCard.Ruling[] rulings;
	private MtgJsonCard.ForeignName[] foreignNames;
	private Supplier<MtgCard> otherSideSupplier;
	private ScryfallCard.Layout layout;
	private String scryfallImageUrl;

	private transient String imageUrl;
	private transient String printedText;
	private transient String printedTypeLine;
	private transient MtgCard otherSide;

	MtgCard(int multiverseId, String name, String manacost, String cmc, String language, String typeLine, ScryfallCard.Rarity rarity, ScryfallSet set, String scryfallId, String[] colorIdentity, HashMap<String, ScryfallCard.Legality> legalities, String text, String power, String toughness, String flavorText, String loyalty, String vgHandModifier, String vgLifeModifier, String scryfallUrl, String number, MtgJsonCard.Ruling[] rulings, MtgJsonCard.ForeignName[] foreignNames, Supplier<MtgCard> otherSideSupplier, ScryfallCard.Layout layout, String scryfallImageUrl) {
		this.multiverseId = multiverseId;
		this.name = name;
		this.manacost = manacost;
		this.cmc = cmc;
		this.language = language;
		this.typeLine = typeLine;
		this.rarity = rarity;
		this.set = set;
		this.scryfallId = scryfallId;
		this.colorIdentity = colorIdentity;
		this.legalities = legalities;
		this.text = text;
		this.power = power;
		this.toughness = toughness;
		this.flavorText = flavorText;
		this.loyalty = loyalty;
		this.vgHandModifier = vgHandModifier;
		this.vgLifeModifier = vgLifeModifier;
		this.scryfallUrl = scryfallUrl;
		this.number = number;
		this.rulings = rulings;
		this.foreignNames = foreignNames;
		this.otherSideSupplier = otherSideSupplier;
		this.layout = layout;
		this.scryfallImageUrl = scryfallImageUrl;
	}

	//
	// Standard Getters
	//

	@Nullable
	public MtgCard getOtherSide() {
		if (otherSide == null) otherSide = otherSideSupplier.get();
		return otherSide;
	}

	public ScryfallCard.Layout getLayout() {
		return layout;
	}

	public MtgJsonCard.ForeignName[] getForeignNames() {
		return foreignNames;
	}

	public MtgJsonCard.Ruling[] getRulings() {
		return rulings;
	}

	public String getNumber() {
		return number;
	}

	public String getScryfallId() {
		return scryfallId;
	}

	public int getMultiverseId() {
		return multiverseId;
	}

	public String getName() {
		return name;
	}

	public String getManacost() {
		return manacost;
	}

	public String getCmc() {
		return cmc;
	}

	public String getLanguage() {
		return language;
	}

	public String getTypeLine() {
		// Return printed type line if available
		if (printedTypeLine != null) return printedTypeLine;
		// Attempt fetching printed type line
		printedTypeLine = fetchPrintedTypeLine();
		if (printedTypeLine != null) return printedTypeLine;
		// Fall back to given type line otherwise
		return typeLine;
	}

	public ScryfallCard.Rarity getRarity() {
		return rarity;
	}

	public ScryfallSet getSet() {
		return set;
	}

	public String[] getColorIdentity() {
		return colorIdentity;
	}

	public HashMap<String, ScryfallCard.Legality> getLegalities() {
		return legalities;
	}

	public String getText() {
		// Return printed text if available
		if (printedText != null) {
			return printedText;
		}
		// Attempt fetching printed text
		printedText = fetchPrintedText();
		if (printedText != null) {
			return printedText;
		}
		// Fall back to given text otherwise
		return text;
	}

	public String getPower() {
		return power;
	}

	public String getToughness() {
		return toughness;
	}

	public String getFlavorText() {
		return flavorText;
	}

	public String getLoyalty() {
		return loyalty;
	}

	public String getVgHandModifier() {
		return vgHandModifier;
	}

	public String getVgLifeModifier() {
		return vgLifeModifier;
	}

	public String getScryfallImageUrl() {
		return scryfallImageUrl;
	}

	//
	// Utility Getters
	//

	public List<MtgJsonCard> getAllMtgJsonPrintings() {
		List<MtgJsonCard> cards = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getCardsByName(name);
		if (cards.isEmpty()) return cards;
		if (!language.equals("English")) {
			MtgJsonCard c = cards.get(0).getAllLanguages().parallelStream().filter(ca -> ca.getLanguage().equals("English")).findFirst().orElse(null);
			if (c == null)
				return cards.parallelStream().filter(ca -> ca.getLanguage().equals(language)).collect(Collectors.toList());
			cards = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getCardsByName(c.getName());
			if (cards.isEmpty()) return cards;
		}
		return cards;
	}

	public List<MtgCard> getAllPrintings(int maxResults) throws ScryfallRetriever.ScryfallRequest.UnknownResponseException, ScryfallRetriever.ScryfallRequest.ScryfallErrorException {
		try {
			return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(String.format("++!\"%s\"", name), maxResults);
		} catch (ScryfallRetriever.ScryfallRequest.NoResultException e) {
			return new ArrayList<MtgCard>() {{
				add(MtgCard.this);
			}};
		}
	}

	@Nullable
	public String getTokenColor() {
		return MTGUtils.colourIdToName(getColorIdentity().length > 0 ? getColorIdentity()[0] : null);
	}

	@Nullable
	public String getGathererUrl() {
		return (multiverseId > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId : null;
	}

	@Nullable
	public String getScryfallUrl() {
		// Fallback to gatherer for foreign cards
		if (!language.equals("English")) return getGathererUrl();
		return scryfallUrl;
	}

	@Nullable
	public String getMagicCardsInfoUrl() {
		// Fallback to gatherer for foreign cards
		if (!language.equals("English")) return getGathererUrl();
		try {
			return "http://magiccards.info/query?q=!" + URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			return "http://magiccards.info/query?q=!" + name;
		}
	}

	@Nullable
	public String getImageUrl(GuildPreferences guildPreferences) {
		if (imageUrl == null)
			imageUrl = Grimoire.getInstance().getCardProvider().getImageRetriever().findUrl(this, guildPreferences);
		return imageUrl;
	}

	public MessageEmbed getEmbed(Guild guild, GuildPreferences guildPreferences) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setColor(MTGUtils.colorIdentitiesToColor(colorIdentity));
		if (guildPreferences.showThumbnail()) eb.setThumbnail(getImageUrl(guildPreferences));
		eb.setTitle(name, guildPreferences.getCardUrl(this));
		if (guildPreferences.showManaCost())
			eb.appendDescription((manacost == null || manacost.isEmpty()) ? "" : Grimoire.getInstance().getEmojiParser().parseEmoji(manacost, guild));
		if (guildPreferences.showConvertedManaCost()) {
			String cmcStr;
			try {
				cmcStr = new DecimalFormat("##.###").format(Double.parseDouble(cmc));
				eb.appendDescription(" **(" + cmcStr + ")**");
			} catch (NumberFormatException e) {
			}
		}
		eb.appendDescription("\n");
		if (guildPreferences.showPowerToughness()) {
			String pat = MTGUtils.parsePowerAndToughness(power, toughness);
			if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		}
		if (guildPreferences.showCardType() && getTypeLine() != null)
			eb.appendDescription(getTypeLine() + "\n\n");
		if (guildPreferences.showOracleText() && getText() != null)
			eb.appendDescription(Grimoire.getInstance().getEmojiParser().parseEmoji(getText(), guild) + "\n");
		if (guildPreferences.showFlavorText() && getFlavorText() != null)
			eb.appendDescription("\n_" + getFlavorText() + "_");
		if (guildPreferences.showLegalFormats()) {
			String formats = String.join(", ", legalities.entrySet().parallelStream().filter(e -> e.getValue().equals(ScryfallCard.Legality.LEGAL)).map(s -> s.getKey().substring(0, 1).toUpperCase() + s.getKey().substring(1).toLowerCase()).collect(Collectors.toList()));
			if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		}
		if (guildPreferences.showPrintedRarities() && !typeLine.contains("Basic Land")) {
			String rarities = null;
			try {
				if (guildPreferences.disableScryfallPrintChecks()) {
					rarities = String.join(", ", getAllMtgJsonPrintings().parallelStream().map(c -> c.getRarity().toString()).distinct().map(r -> WordUtils.capitalize(r.toLowerCase()).replaceAll("_", "")).collect(Collectors.toList()));
				} else {
					rarities = String.join(", ", getAllPrintings(-1).parallelStream().map(c -> c.rarity.toString()).distinct().map(r -> WordUtils.capitalize(r.toLowerCase())).collect(Collectors.toList()));
				}
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
				LOG.log(Level.WARNING, "Scryfall gave an error when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: " + e.getError().getDetails(), true);
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
				LOG.log(Level.WARNING, "Scryfall gave an unknown response when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: An unknown error occurred.", true);
			}
			if (rarities != null && !rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		}
		if (guildPreferences.showPrintings() && !typeLine.contains("Basic Land")) {
			String printings = "";
			try {
				if (guildPreferences.disableScryfallPrintChecks()) {
					printings = String.join(", ",
							new String[]{"**" + set.getName() + " (" + set.getCode() + ")**", String.join(", ", getAllMtgJsonPrintings().parallelStream().filter(card -> !set.getCode().equalsIgnoreCase(card.getSetCode())).map(MtgJsonCard::getSetCode).collect(Collectors.toList()))}).trim();
				} else {
					printings = String.join(", ",
							new String[]{"**" + set.getName() + " (" + set.getCode() + ")**", String.join(", ", getAllPrintings(-1).parallelStream().filter(card -> !set.getCode().equalsIgnoreCase(card.set.getCode())).map(card -> card.set.getCode()).collect(Collectors.toList()))}).trim();
				}
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
				LOG.log(Level.WARNING, "Scryfall gave an error when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: " + e.getError().getDetails(), true);
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
				LOG.log(Level.WARNING, "Scryfall gave an unknown response when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: An unknown error occurred.", true);
			}
			if (printings.endsWith(",")) printings = printings.substring(0, printings.length() - 1);
			if (!printings.isEmpty())
				eb.addField("Printings", printings.length() <= 1024 ? printings : "Too many printings to list.", true);
			if (printings.length() > 1024)
				LOG.log(Level.SEVERE, "PRINTINGS EXCEEDS 1024 CHARS FOR : " + toString() + "\n\n" + printings);
		}
		if (guildPreferences.showMiscProperties()) {
			if (loyalty != null) eb.addField("Loyalty", loyalty, true);
			if (vgHandModifier != null && vgLifeModifier != null)
				eb.addField("Vanguard Hand/Life Modifiers", vgHandModifier + "/" + vgLifeModifier, true);
		}

		// Return result
		return eb.build();
	}

	public boolean sameCardAs(MtgCard mtgCard) {
		if (mtgCard.getName().equalsIgnoreCase(getName())) return true;
		return (getOtherSide() != null && getOtherSide().getName().equalsIgnoreCase(mtgCard.getName()));
	}

	//
	// Internal utilities
	//

	private String fetchPrintedText() {
		if (getLanguage().equals("English")) return text;
		GathererRetriever.GathererData data = GathererRetriever.getGathererData(getMultiverseId(), getName());
		return (data == null) ? null : data.getText();
	}

	private String fetchPrintedTypeLine() {
		if (getLanguage().equals("English")) return typeLine;
		GathererRetriever.GathererData data = GathererRetriever.getGathererData(getMultiverseId(), getName());
		return (data == null) ? null : data.getTypeLine();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("multiverseId", multiverseId)
				.append("name", name)
				.append("manacost", manacost)
				.append("cmc", cmc)
				.append("language", language)
				.append("typeLine", typeLine)
				.append("rarity", rarity)
				.append("set", set)
				.append("scryfallId", scryfallId)
				.append("colorIdentity", colorIdentity)
				.append("legalities", legalities)
				.append("text", text)
				.append("power", power)
				.append("toughness", toughness)
				.append("flavorText", flavorText)
				.append("loyalty", loyalty)
				.append("vgHandModifier", vgHandModifier)
				.append("vgLifeModifier", vgLifeModifier)
				.append("scryfallUrl", scryfallUrl)
				.append("number", number)
				.append("rulings", rulings)
				.append("foreignNames", foreignNames)
				.append("imageUrl", imageUrl)
				.append("printedText", printedText)
				.append("tokenColor", getTokenColor())
				.append("gathererUrl", getGathererUrl())
				.append("magicCardsInfoUrl", getMagicCardsInfoUrl())
				.toString();
	}

}
