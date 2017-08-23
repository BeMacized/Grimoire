package net.bemacized.grimoire.data.models.card;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"SpellCheckingInspection", "WeakerAccess"})
public class MtgCard {

	private static final Logger LOG = Logger.getLogger(MtgCard.class.getName());
	public static final String COLLECTION = "MtgCards";

	@MongoId
	private String _id;

	// Parent Objects
	@Nonnull
	private ScryfallCard scryfallCard;
	@Nullable
	private MtgJsonCard mtgJsonCard;

	// Extra data
	@Nonnull
	private String language;

	// Foreign Data
	@Nullable
	private String foreignName;
	private int foreignMultiverseId = -1;
	private MtgJsonCard.ForeignName[] foreignNames;

	// Type data
	private String[] types;
	private String[] supertypes;
	private String[] subtypes;

	// Transient data
	private transient String imageUrl;
	private transient String printedText;

	public MtgCard() {
	}

	public MtgCard(@Nonnull ScryfallCard scryfallCard, @Nullable MtgJsonCard mtgJsonCard) {
		this.scryfallCard = scryfallCard;
		this.mtgJsonCard = mtgJsonCard;
		this.language = "English";
		this._id = generateId();
	}

	public MtgCard(MtgCard mtgCard, MtgJsonCard.ForeignName foreignName) {
		this(mtgCard.scryfallCard, mtgCard.mtgJsonCard);
		if (foreignName.getLanguage().equalsIgnoreCase("English"))
			throw new IllegalArgumentException("Do not use the language constructor for the default language: English.");
		this.foreignName = foreignName.getName();
		this.foreignMultiverseId = foreignName.getMultiverseid();
		this.language = foreignName.getLanguage();
		this.foreignNames = Stream.concat(
				Arrays.stream(mtgCard.getForeignNames())
						.filter(n -> !n.getLanguage().equals(foreignName.getLanguage())),
				(!foreignName.getLanguage().equalsIgnoreCase("English")) ? Stream.of(new MtgJsonCard.ForeignName(
						"English",
						mtgCard.getName(),
						mtgCard.getMultiverseid()
				)) : Stream.of()
		).collect(Collectors.toList()).toArray(new MtgJsonCard.ForeignName[0]);
		this._id = generateId();
	}

	@SuppressWarnings("ConstantConditions")
	public MtgSet getSet() {
		MtgSet set = Grimoire.getInstance().getCardProvider().getSetByCode(scryfallCard.getSet());
		if (set == null) {
			System.out.println("CANNOT FIND " + scryfallCard.getSet());
		}
		return set;
	}

	@Nonnull
	public String getName() {
		// If we are a foreign card, provide our own name instead of the scryfall name
		return (foreignName == null) ? scryfallCard.getName() : foreignName;
	}

	@Nonnull
	public Rarity getRarity() {
		return (mtgJsonCard != null)
				? Rarity.getByMtgJsonRarity(mtgJsonCard.getRarity())
				: Rarity.getByScryfallRarity(scryfallCard.getRarity());
	}

	@Nonnull
	public String getLanguage() {
		return language;
	}

	public MtgJsonCard.ForeignName[] getForeignNames() {
		// If we are a foreign card we have our own list
		if (foreignNames != null) return foreignNames;
		// Otherwise fallback to the mtgjson parent
		return (mtgJsonCard != null) ? mtgJsonCard.getForeignNames() : new MtgJsonCard.ForeignName[0];
	}

	public int getMultiverseid() {
		// If we are a foreign card, return that instead of the scryfall provided one
		return (foreignMultiverseId > 0) ? foreignMultiverseId : scryfallCard.getMultiverseId();
	}

	public void assertValidity() {
		assert scryfallCard != null;
		assert language != null;
		assert getSet() != null;
		assert getRarity() != Rarity.UNKNOWN;
	}

	public Layout getLayout() {
		Layout layout = Layout.getByScryfallLayout(scryfallCard.getLayout());
		if (layout == null) {
			if (mtgJsonCard != null) layout = Layout.getByMtgJsonLayout(mtgJsonCard.getLayout());
			else layout = Layout.UNKNOWN;
		}
		return layout;
	}

	@Nullable
	public String getImageUrl() {
		if (imageUrl == null) imageUrl = Grimoire.getInstance().getCardProvider().getImageRetriever().findUrl(this);
		return imageUrl;
	}

	@Nullable
	public String getNumber() {
		return scryfallCard.getCollectorNumber();
	}

	@Nonnull
	public String getScryfallId() {
		return scryfallCard.getId();
	}

	public String[] getColorIdentity() {
		return scryfallCard.getColorIdentity();
	}

	@Nullable
	public String getReleaseDate() {
		return getSet().getReleaseDate();
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
			eb.appendDescription("\n_\"" + getFlavorText() + "\"_");
		if (guildPreferences.showLegalFormats()) {
			String formats = String.join(", ", getLegalities().entrySet().parallelStream().filter(e -> e.getValue().equals(ScryfallCard.Legality.LEGAL)).map(s -> s.getKey().substring(0, 1).toUpperCase() + s.getKey().substring(1).toLowerCase()).collect(Collectors.toList()));
			if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		}
		if (guildPreferences.showPrintedRarities()) {
			String rarities = String.join(", ", new CardProvider.SearchQuery().hasName(getName()).parallelStream().map(c -> c.getRarity().toString()).distinct().collect(Collectors.toList()));
			if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		}
		if (guildPreferences.showPrintings()) {
			String printings = String.join(", ",
					new String[]{"**" + getSet().getName() + " (" + getSet().getCode() + ")**", String.join(", ", getPrintings().parallelStream().filter(set -> !getSet().getCode().equalsIgnoreCase(set.getCode())).map(MtgSet::getCode).collect(Collectors.toList()))}).trim();
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

	public Map<String, ScryfallCard.Legality> getLegalities() {
		return scryfallCard.getLegalities();
	}

	public List<MtgSet> getPrintings() {
		return new CardProvider.SearchQuery().hasName(getName()).inLanguage("English").distinctSets().parallelStream().map(MtgCard::getSet).collect(Collectors.toList());
	}

	@Nullable
	public String getPower() {
		return scryfallCard.getPower();
	}

	@Nullable
	public String getToughness() {
		return scryfallCard.getToughness();
	}

	@Nullable
	public String getManaCost() {
		return scryfallCard.getManaCost();
	}

	public String getCmc() {
		return scryfallCard.getCMC();
	}

	@Nullable
	public String getType() {
		return scryfallCard.getTypeLine();
	}

	@Nullable
	public String getText() {
		return scryfallCard.getOracleText();
	}

	@Nullable
	public String getLoyalty() {
		return scryfallCard.getLoyalty();
	}

	@Nullable
	public String getFlavorText() {
		return scryfallCard.getFlavorText();
	}

	@Nullable
	public String getLifeModifier() {
		return scryfallCard.getLifeModifier();
	}

	@Nullable
	public String getHandModifier() {
		return scryfallCard.getHandModifier();
	}

	@Nonnull
	public MtgJsonCard.Ruling[] getRulings() {
		return (mtgJsonCard == null) ? new MtgJsonCard.Ruling[0] : mtgJsonCard.getRulings();
	}

	@Nullable
	public String getTokenColor() {
		return MTGUtils.colourIdToName(getColorIdentity().length > 0 ? getColorIdentity()[0] : null);
	}

	@Nullable
	public String getPrintedText() {
		if (language.equalsIgnoreCase("English") || getMultiverseid() < 1) return getText();
		if (printedText != null && !printedText.isEmpty()) return printedText;
		try {
			printedText = String.join("\n",
					Jsoup.parse(new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=true&multiverseid=" + getMultiverseid()), 5000)
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
			LOG.log(Level.WARNING, "Could not retrieve foreign text for card with multiverseid " + getMultiverseid(), e);
		}
		return (printedText == null) ? getText() : printedText;
	}

	@Nullable
	public MtgJsonCard getMtgJsonCard() {
		return mtgJsonCard;
	}

	public String[] getSupertypes() {
		if (mtgJsonCard != null) return mtgJsonCard.getSupertypes();
		if (supertypes != null) return supertypes;
		if (getType() == null || getType().isEmpty()) return new String[0];
		supertypes = Grimoire.getInstance().getCardProvider().getAllSupertypes().parallelStream()
				.filter(type -> getType().toLowerCase().contains(type))
				.collect(Collectors.toList())
				.toArray(new String[0]);
		save();
		return this.supertypes;
	}

	public String[] getSubtypes() {
		if (mtgJsonCard != null) return mtgJsonCard.getSubtypes();
		if (subtypes != null) return subtypes;
		if (getType() == null || getType().isEmpty()) return new String[0];
		subtypes = Grimoire.getInstance().getCardProvider().getAllSubtypes().parallelStream()
				.filter(type -> getType().toLowerCase().contains(type))
				.collect(Collectors.toList())
				.toArray(new String[0]);
		save();
		return this.subtypes;
	}

	public String[] getTypes() {
		if (mtgJsonCard != null) return mtgJsonCard.getTypes();
		if (types != null) return types;
		if (getType() == null || getType().isEmpty()) return new String[0];
		types = Grimoire.getInstance().getCardProvider().getAllTypes().parallelStream()
				.filter(type -> getType().toLowerCase().contains(type))
				.collect(Collectors.toList())
				.toArray(new String[0]);
		save();
		return this.types;
	}

	public void save() {
		if (language.equalsIgnoreCase("English"))
			Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).save(this);
	}

	@Nonnull
	public ScryfallCard getScryfallCard() {
		return scryfallCard;
	}

	public void updateScryfall() {
		ScryfallCard c = ScryfallRetriever.retrieveCard(this.scryfallCard.getId());
		if (c != null) {
			this.scryfallCard = c;
			this.save();
		}
	}

	@Nullable
	public String getGathererUrl() {
		return (getMultiverseid() > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + getMultiverseid() : null;
	}

	@Nullable
	public String getScryfallUrl() {
		// Fallback to gatherer for foreign cards
		if (!getLanguage().equalsIgnoreCase("English")) return getGathererUrl();

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

	@SuppressWarnings("unused")
	public enum Layout {
		UNKNOWN(MtgJsonCard.Layout.UNKNOWN, ScryfallCard.Layout.UNKNOWN),
		NORMAL(MtgJsonCard.Layout.NORMAL, ScryfallCard.Layout.NORMAL),
		SPLIT(MtgJsonCard.Layout.SPLIT, ScryfallCard.Layout.SPLIT),
		FLIP(MtgJsonCard.Layout.FLIP, ScryfallCard.Layout.FLIP),
		TRANSFORM(MtgJsonCard.Layout.DOUBLE_FACED, ScryfallCard.Layout.TRANSFORM),
		MELD(MtgJsonCard.Layout.MELD, ScryfallCard.Layout.MELD),
		LEVELER(MtgJsonCard.Layout.UNKNOWN, ScryfallCard.Layout.LEVELER),
		PLANE(MtgJsonCard.Layout.PLANE, ScryfallCard.Layout.PLANE),
		PHENOMENON(MtgJsonCard.Layout.PHENOMENON, ScryfallCard.Layout.PHENOMENON),
		SCHEME(MtgJsonCard.Layout.SCHEME, ScryfallCard.Layout.SCHEME),
		VANGUARD(MtgJsonCard.Layout.VANGUARD, ScryfallCard.Layout.VANGUARD),
		TOKEN(MtgJsonCard.Layout.TOKEN, ScryfallCard.Layout.TOKEN),
		AFTERMATH(MtgJsonCard.Layout.AFTERMATH, null),
		EMBLEM(null, ScryfallCard.Layout.EMBLEM);

		static Layout getByScryfallLayout(ScryfallCard.Layout layout) {
			return Arrays.stream(values()).parallel().filter(l -> layout.equals(l.scryfallLayout)).findFirst().orElse(UNKNOWN);
		}

		static Layout getByMtgJsonLayout(MtgJsonCard.Layout layout) {
			return Arrays.stream(values()).parallel().filter(l -> layout.equals(l.mtgjsonLayout)).findFirst().orElse(UNKNOWN);
		}

		private MtgJsonCard.Layout mtgjsonLayout;
		private ScryfallCard.Layout scryfallLayout;

		Layout(MtgJsonCard.Layout mtgjsonLayout, ScryfallCard.Layout scryfallLayout) {
			this.mtgjsonLayout = mtgjsonLayout;
			this.scryfallLayout = scryfallLayout;
		}

		@Override
		public String toString() {
			return WordUtils.capitalize(name().replaceAll("_", " ").toLowerCase()) + " Card";
		}
	}

	public enum Rarity {
		UNKNOWN(ScryfallCard.Rarity.UNKNOWN, MtgJsonCard.Rarity.UNKNOWN),
		COMMON(ScryfallCard.Rarity.COMMON, MtgJsonCard.Rarity.COMMON),
		UNCOMMON(ScryfallCard.Rarity.UNCOMMON, MtgJsonCard.Rarity.UNCOMMON),
		RARE(ScryfallCard.Rarity.RARE, MtgJsonCard.Rarity.RARE),
		MYTHIC_RARE(ScryfallCard.Rarity.MYTHIC, MtgJsonCard.Rarity.MYTHIC_RARE),
		SPECIAL(null, MtgJsonCard.Rarity.SPECIAL),
		BASIC_LAND(null, MtgJsonCard.Rarity.BASIC_LAND);

		static Rarity getByScryfallRarity(ScryfallCard.Rarity rarity) {
			if (rarity == null) return Rarity.UNCOMMON;
			return Arrays.stream(values()).parallel().filter(r -> rarity == r.scryfallRarity).findFirst().orElse(UNKNOWN);
		}

		static Rarity getByMtgJsonRarity(MtgJsonCard.Rarity rarity) {
			if (rarity == null) return Rarity.UNCOMMON;
			return Arrays.stream(values()).parallel().filter(r -> rarity == r.mtgjsonRarity).findFirst().orElse(UNKNOWN);
		}

		private ScryfallCard.Rarity scryfallRarity;
		private MtgJsonCard.Rarity mtgjsonRarity;

		Rarity(ScryfallCard.Rarity scryfallRarity, MtgJsonCard.Rarity mtgjsonRarity) {
			this.scryfallRarity = scryfallRarity;
			this.mtgjsonRarity = mtgjsonRarity;
		}

		@Override
		public String toString() {
			return WordUtils.capitalize(name().replaceAll("_", " ").toLowerCase());
		}
	}

	private String generateId() {
		String id = this.scryfallCard.getId();
		if (this.mtgJsonCard != null) id += this.mtgJsonCard.getId();
		id += (this.getMultiverseid() > 0) ? String.valueOf(this.getMultiverseid()) : "";
		return DigestUtils.sha1Hex(id);
	}

}
