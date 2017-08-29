package net.bemacized.grimoire.data.models.card;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.text.WordUtils;
import org.jongo.marshall.jackson.oid.MongoId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal"/*, "unused"*/, "WeakerAccess"})
public class MtgCard {

	private static final Logger LOG = Logger.getLogger(MtgCard.class.getName());
	public static final String COLLECTION = "MtgCards";

	@MongoId
	private String scryfallId = "UNKNOWN";

	@Nonnull
	private String language = "English";
	private int multiverseid = -1;

	// Transient values
	private transient ScryfallCard scryfallCard;
	private transient ScryfallSet scryfallSet;
	private transient MtgJsonCard mtgJsonCard;
	private transient String imageUrl;
	private String printedText;
	private String printedType;

	public MtgCard() {
	}

	public MtgCard(String scryfallId) throws ScryfallRetriever.ScryfallRequest.UnknownResponseException, ScryfallRetriever.ScryfallRequest.NoResultException, ScryfallRetriever.ScryfallRequest.ScryfallErrorException {
		this.scryfallId = scryfallId;
		initialize();
	}

	public MtgCard(@Nonnull ScryfallCard scryfallCard) {
		this.scryfallCard = scryfallCard;
		this.scryfallId = scryfallCard.getId();
		try {
			initialize();
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
			LOG.log(Level.SEVERE, "An unknown error occurred with Scryfall", e);
		} catch (ScryfallRetriever.ScryfallRequest.NoResultException ignored) {
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
			LOG.log(Level.SEVERE, "An error occurred with Scryfall", e);
		}
	}

	public MtgCard(@Nonnull ScryfallCard scryfallCard, @Nonnull MtgJsonCard mtgJsonCard) throws ScryfallRetriever.ScryfallRequest.UnknownResponseException, ScryfallRetriever.ScryfallRequest.NoResultException, ScryfallRetriever.ScryfallRequest.ScryfallErrorException {
		this(scryfallCard);
		this.mtgJsonCard = mtgJsonCard;
		initialize();
	}

	private void initialize() throws ScryfallRetriever.ScryfallRequest.UnknownResponseException, ScryfallRetriever.ScryfallRequest.NoResultException, ScryfallRetriever.ScryfallRequest.ScryfallErrorException {
		// Retrieve scryfall card if needed
		if (scryfallCard == null)
			scryfallCard = ScryfallRetriever.getCardByScryfallId(scryfallId);
		// Retrieve scryfall set
		scryfallSet = ScryfallRetriever.getSet(scryfallCard.getSet());
		// Get the multiverse id from the mtg json card if it exists already (For foreign cards)
		multiverseid = mtgJsonCard == null ? scryfallCard.getMultiverseId() : mtgJsonCard.getMultiverseid();
		// Get the language from the mtg json card if it exists already (For foreign cards)
		if (mtgJsonCard != null)
			language = mtgJsonCard.getLanguage();
		// Fetch a mtg json card instance if we don't have one yet.
		if (getMultiverseId() > 0 && mtgJsonCard == null)
			mtgJsonCard = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getCardByMultiverseId(multiverseid);
	}

	@Nullable
	public String getName() {
		if (mtgJsonCard != null) return mtgJsonCard.getName();
		if (scryfallCard == null) return null;
		return scryfallCard.getName();
	}

	public int getMultiverseId() {
		return multiverseid;
	}

	public String[] getColorIdentity() {
		if (scryfallCard == null) return new String[0];
		return scryfallCard.getColorIdentity();
	}

	public ScryfallSet getSet() {
		return scryfallSet;
	}

	public List<MtgCard> getAllPrintings() throws ScryfallRetriever.ScryfallRequest.UnknownResponseException, ScryfallRetriever.ScryfallRequest.ScryfallErrorException {
		try {
			return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(String.format("++!\"%s\"", this.getName()));
		} catch (ScryfallRetriever.ScryfallRequest.NoResultException e) {
			return new ArrayList<MtgCard>() {{
				add(MtgCard.this);
			}};
		}
	}

	@Nonnull
	public String getLanguage() {
		return language;
	}

	@Nullable
	public String getGathererUrl() {
		return (getMultiverseId() > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + getMultiverseId() : null;
	}

	@Nullable
	public String getScryfallUrl() {
		// Fallback to gatherer for foreign cards
		if (!getLanguage().equalsIgnoreCase("English") || scryfallCard == null) return getGathererUrl();
		return scryfallCard.getScryfallUri();
	}

	@Nullable
	public String getMagicCardsInfoUrl() {
		// Fallback to gatherer for foreign cards
		if (!getLanguage().equalsIgnoreCase("English")) return getGathererUrl();
		try {
			return "http://magiccards.info/query?q=!" + URLEncoder.encode(getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			return "http://magiccards.info/query?q=!" + getName();
		}
	}

	@Nullable
	public ScryfallCard getScryfallCard() {
		return scryfallCard;
	}

	public MtgJsonCard.Ruling[] getRulings() {
		return mtgJsonCard == null ? new MtgJsonCard.Ruling[0] : mtgJsonCard.getRulings();
	}

	@Nullable
	public String getImageUrl() {
		if (imageUrl == null) imageUrl = Grimoire.getInstance().getCardProvider().getImageRetriever().findUrl(this);
		return imageUrl;
	}

	public MessageEmbed getEmbed(Guild guild, GuildPreferences guildPreferences) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setColor(MTGUtils.colorIdentitiesToColor(getColorIdentity()));
		if (guildPreferences.showThumbnail()) eb.setThumbnail(getImageUrl());
		eb.setTitle(getName(), guildPreferences.getCardUrl(this));
		if (guildPreferences.showManaCost())
			eb.appendDescription((getManaCost() == null || getManaCost().isEmpty()) ? "" : Grimoire.getInstance().getEmojiParser().parseEmoji(getManaCost(), guild));
		if (guildPreferences.showConvertedManaCost()) {
			String cmc = getCmc();
			try {
				cmc = new DecimalFormat("##.###").format(Double.parseDouble(cmc));
			} catch (NumberFormatException e) {
			}
			eb.appendDescription(" **(" + cmc + ")**");
		}
		eb.appendDescription("\n");
		if (guildPreferences.showPowerToughness()) {
			String pat = MTGUtils.parsePowerAndToughness(getPower(), getToughness());
			if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		}
		if (guildPreferences.showCardType() && getType() != null)
			eb.appendDescription(getType() + "\n\n");
		if (guildPreferences.showOracleText() && getPrintedText() != null)
			eb.appendDescription(Grimoire.getInstance().getEmojiParser().parseEmoji(getPrintedText(), guild) + "\n");
		if (guildPreferences.showFlavorText() && getFlavorText() != null)
			eb.appendDescription("\n_" + getFlavorText() + "_");
		if (guildPreferences.showLegalFormats()) {
			String formats = String.join(", ", getLegalities().entrySet().parallelStream().filter(e -> e.getValue().equals(ScryfallCard.Legality.LEGAL)).map(s -> s.getKey().substring(0, 1).toUpperCase() + s.getKey().substring(1).toLowerCase()).collect(Collectors.toList()));
			if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		}
		if (guildPreferences.showPrintedRarities()) {
			String rarities = null;
			try {
				rarities = String.join(", ", getAllPrintings().parallelStream().map(c -> c.getRarity().toString()).distinct().map(r -> WordUtils.capitalize(r.toLowerCase())).collect(Collectors.toList()));
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
				LOG.log(Level.WARNING, "Scryfall gave an error when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: " + e.getError().getDetails(), true);
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
				LOG.log(Level.WARNING, "Scryfall gave an unknown response when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: An unknown error occurred.", true);
			}
			if (rarities != null && !rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		}
		if (guildPreferences.showPrintings()) {
			String printings = "";
			try {
				printings = String.join(", ",
						new String[]{"**" + getSet().getName() + " (" + getSet().getCode() + ")**", String.join(", ", getAllPrintings().parallelStream().filter(card -> !getSet().getCode().equalsIgnoreCase(card.getSet().getCode())).map(card -> card.getSet().getCode()).collect(Collectors.toList()))}).trim();
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
				LOG.log(Level.WARNING, "Scryfall gave an error when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: " + e.getError().getDetails(), true);
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
				LOG.log(Level.WARNING, "Scryfall gave an unknown response when trying to receive printings for a card embed.", e);
				eb.addField("Rarities", "Could not retrieve rarities: An unknown error occurred.", true);
			}
			if (printings.endsWith(",")) printings = printings.substring(0, printings.length() - 1);
			if (!printings.isEmpty()) eb.addField("Printings", printings, true);
		}
		if (guildPreferences.showMiscProperties()) {
			if (getLoyalty() != null) eb.addField("Loyalty", getLoyalty(), true);
			if (getHandModifier() != null && getLifeModifier() != null)
				eb.addField("Vanguard Hand/Life Modifiers", getHandModifier() + "/" + getLifeModifier(), true);
		}

		// Return result
		return eb.build();
	}

	@Nullable
	public String getManaCost() {
		if (scryfallCard == null) return null;
		return scryfallCard.getManaCost();
	}

	@Nullable
	public String getPower() {
		if (scryfallCard == null) return null;
		return scryfallCard.getPower();
	}

	@Nullable
	public String getToughness() {
		if (scryfallCard == null) return null;
		return scryfallCard.getToughness();
	}

	@Nullable
	public String getType() {
		if (language.equalsIgnoreCase("English") || getMultiverseId() < 1) return scryfallCard.getTypeLine();
		if (printedType != null && !printedType.isEmpty()) return printedType;
		try {
			printedType = Jsoup.parse(new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=true&multiverseid=" + getMultiverseId()), 5000)
					.getElementsByClass("cardComponentTable")
					.first()
					.getElementsByTag("td")
					.first()
					.select("[id$=_typeRow]")
					.first()
					.getElementsByClass("value")
					.first()
					.text()
					.trim();
			if (printedType.isEmpty()) printedType = null;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not retrieve foreign type for card with multiverseid " + getMultiverseId(), e);
		}
		return (printedType == null) ? scryfallCard.getTypeLine() : printedType;
	}

	@Nullable
	public String getPrintedText() {
		if (language.equalsIgnoreCase("English") || getMultiverseId() < 1) return scryfallCard.getOracleText();
		if (printedText != null && !printedText.isEmpty()) return printedText;
		try {
			printedText = String.join("\n",
					Jsoup.parse(new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=true&multiverseid=" + getMultiverseId()), 5000)
							.getElementsByClass("cardComponentTable")
							.first()
							.getElementsByTag("td")
							.first()
							.getElementsByClass("cardtextbox")
							.stream()
							.map(Element::text)
							.map(String::trim)
							.collect(Collectors.toList())
			);
			if (printedText.isEmpty()) printedText = null;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not retrieve foreign text for card with multiverseid " + getMultiverseId(), e);
		}
		return (printedText == null) ? scryfallCard.getOracleText() : printedText;
	}

	@Nullable
	public String getFlavorText() {
		return scryfallCard.getFlavorText();
	}

	public Map<String, ScryfallCard.Legality> getLegalities() {
		if (scryfallCard == null) return new HashMap<>();
		return scryfallCard.getLegalities();
	}

	@Nullable
	public String getLoyalty() {
		if (scryfallCard == null) return null;
		return scryfallCard.getLoyalty();
	}

	@Nullable
	public String getHandModifier() {
		if (scryfallCard == null) return null;
		return scryfallCard.getHandModifier();
	}

	@Nullable
	public String getLifeModifier() {
		if (scryfallCard == null) return null;
		return scryfallCard.getLifeModifier();
	}

	public ScryfallCard.Rarity getRarity() {
		if (scryfallCard == null) return ScryfallCard.Rarity.UNKNOWN;
		return scryfallCard.getRarity();
	}

	@Nullable
	public String getCmc() {
		if (scryfallCard == null) return null;
		return scryfallCard.getCMC();
	}

	public MtgJsonCard.ForeignName[] getForeignNames() {
		if (mtgJsonCard == null) return new MtgJsonCard.ForeignName[0];
		return mtgJsonCard.getForeignNames();
	}

	@Nullable
	public String getTokenColor() {
		return MTGUtils.colourIdToName(getColorIdentity().length > 0 ? getColorIdentity()[0] : null);
	}

	@Nullable
	public String getNumber() {
		if (scryfallCard == null) return null;
		return scryfallCard.getCollectorNumber();
	}

	public String getScryfallId() {
		return scryfallId;
	}

	public void updateScryfall() {
		ScryfallCard card = null;
		try {
			card = ScryfallRetriever.getCardByScryfallId(getScryfallId());
		} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.UnknownResponseException e) {
			LOG.log(Level.WARNING, "Could not refresh scryfall card because of an unknown error", e);
		} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.NoResultException e) {
			LOG.log(Level.WARNING, "Could not refresh scryfall card because it could not be found", e);
		} catch (net.bemacized.grimoire.data.retrievers.ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
			LOG.log(Level.WARNING, "Could not refresh scryfall card because scryfall returned an error", e);
		}
		if (card != null) this.scryfallCard = card;
	}
}
