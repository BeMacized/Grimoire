package net.bemacized.grimoire.data.models.card;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MtgCardBuilder {
	private int multiverseId;
	private String name;
	private String manacost;
	private String cmc;
	private String language;
	private String typeLine;
	private ScryfallCard.Rarity rarity = ScryfallCard.Rarity.UNKNOWN;
	private ScryfallSet set;
	private String text;
	private String power;
	private String toughness;
	private String flavorText;
	private String loyalty;
	private String vgHandModifier;
	private String vgLifeModifier;
	private String scryfallUrl;
	private String scryfallId;
	private String number;
	private String[] colorIdentity = new String[0];
	private MtgJsonCard.Ruling[] rulings = new MtgJsonCard.Ruling[0];
	private MtgJsonCard.ForeignName[] foreignNames = new MtgJsonCard.ForeignName[0];
	private HashMap<String, ScryfallCard.Legality> legalities = new HashMap<>();
	private Supplier<MtgCard> otherSide = () -> null;
	private ScryfallCard.Layout layout = ScryfallCard.Layout.UNKNOWN;

	public MtgCardBuilder(final ScryfallCard sCard) {
		this(sCard, sCard.getCardFaces().length > 0 ? sCard.getCardFaces()[0] : null);
	}

	public MtgCardBuilder(final ScryfallCard sCard, @Nullable final ScryfallCard.Face face) {
		this(sCard, face, null);
	}


	private MtgCardBuilder(final ScryfallCard sCard, @Nullable final ScryfallCard.Face face, final MtgJsonCard mCard) {
		if (face != null) {
			setName(face.getName());
			setManacost(face.getManaCost());
			setText(face.getOracleText());
			setTypeLine(face.getTypeLine());
			setPower(face.getPower());
			setToughness(face.getToughness());
		} else {
			setName(sCard.getName());
			setManacost(sCard.getManaCost());
			setTypeLine(sCard.getTypeLine());
			setText(sCard.getOracleText());
			setPower(sCard.getPower());
			setToughness(sCard.getToughness());
		}
		setLayout(sCard.getLayout());
		setMultiverseId(sCard.getMultiverseId());
		setCmc(sCard.getCMC());
		setLanguage("English");
		setRarity(sCard.getRarity());
		setSet(Grimoire.getInstance().getCardProvider().getSetByNameOrCode(sCard.getSet()));
		setLegalities(sCard.getLegalities());
		setFlavorText(sCard.getFlavorText());
		setLoyalty(sCard.getLoyalty());
		setVgHandModifier(sCard.getHandModifier());
		setVgLifeModifier(sCard.getLifeModifier());
		setScryfallUrl(sCard.getScryfallUri());
		setScryfallId(sCard.getId());
		setColorIdentity(sCard.getColorIdentity());
		setNumber(sCard.getCollectorNumber());

		// If there's another face, make sure to set the reference
		if (sCard.getCardFaces().length > 1) {
			// Find the index of the current face
			int faceIndex = (face == null)
					? (mCard == null ? 0 : Arrays.stream(sCard.getCardFaces()).parallel()
					.map(ScryfallCard.Face::getName)
					.collect(Collectors.toList())
					.indexOf(mCard.getLanguage().equals("English") ? mCard.getName() : mCard.getAllLanguages().parallelStream().filter(c -> c.getLanguage().equals("English")).map(MtgJsonCard::getName).findFirst().orElse(null)))
					: Arrays.stream(sCard.getCardFaces()).parallel()
					.map(ScryfallCard.Face::getName)
					.collect(Collectors.toList())
					.indexOf(face.getName());
			if (faceIndex < 0) faceIndex = 0;
			// Find the face reference of the other side
			final ScryfallCard.Face otherSide = sCard.getCardFaces()[faceIndex == 0 ? 1 : 0];
			// Find the corresponding mtgjson card of the other side
			MtgJsonCard otherSideMCard = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getCardByName(otherSide.getName());
			// Get the correct language version of it
			if (otherSideMCard != null && mCard != null && !mCard.getLanguage().equals("English")) {
				otherSideMCard = otherSideMCard.getAllLanguages()
						.parallelStream()
						.filter(c -> c.getLanguage().equalsIgnoreCase(mCard.getLanguage()))
						.findFirst()
						.orElse(otherSideMCard);
			}
			// Set the property
			if (otherSideMCard == null) setOtherSide(() -> new MtgCardBuilder(sCard, otherSide).createMtgCard());
			else {
				MtgJsonCard finalOtherSideMCard = otherSideMCard;
				setOtherSide(() -> new MtgCardBuilder(sCard, finalOtherSideMCard).createMtgCard());
			}
		}
	}

	public MtgCardBuilder(final ScryfallCard sCard, final MtgJsonCard mCard) {
		this(sCard, Arrays.stream(sCard.getCardFaces()).parallel().filter(f -> f.getName().equalsIgnoreCase(mCard.getName())).findFirst().orElse(null), mCard);

		if (mCard.getText() != null) setText(mCard.getText());
		setName(mCard.getName());
		setLanguage(mCard.getLanguage());
		setMultiverseId(mCard.getMultiverseid());
		setRulings(mCard.getRulings());
		setForeignNames(mCard.getForeignNames());
	}

	public MtgCardBuilder setMultiverseId(int multiverseId) {
		this.multiverseId = multiverseId;
		return this;
	}

	public MtgCardBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public MtgCardBuilder setManacost(String manacost) {
		this.manacost = manacost;
		return this;
	}

	public MtgCardBuilder setCmc(String cmc) {
		this.cmc = cmc;
		return this;
	}

	public MtgCardBuilder setLanguage(String language) {
		this.language = language;
		return this;
	}

	public MtgCardBuilder setTypeLine(String typeLine) {
		this.typeLine = typeLine;
		return this;
	}

	public MtgCardBuilder setRarity(ScryfallCard.Rarity rarity) {
		this.rarity = rarity;
		return this;
	}

	public MtgCardBuilder setSet(ScryfallSet set) {
		this.set = set;
		return this;
	}

	public MtgCardBuilder setLegalities(HashMap<String, ScryfallCard.Legality> legalities) {
		this.legalities = legalities;
		return this;
	}

	public MtgCardBuilder setText(String text) {
		this.text = text;
		return this;
	}

	public MtgCardBuilder setPower(String power) {
		this.power = power;
		return this;
	}

	public MtgCardBuilder setToughness(String toughness) {
		this.toughness = toughness;
		return this;
	}

	public MtgCardBuilder setFlavorText(String flavorText) {
		this.flavorText = flavorText;
		return this;
	}

	public MtgCardBuilder setLoyalty(String loyalty) {
		this.loyalty = loyalty;
		return this;
	}

	public MtgCardBuilder setVgHandModifier(String vgHandModifier) {
		this.vgHandModifier = vgHandModifier;
		return this;
	}

	public MtgCardBuilder setVgLifeModifier(String vgLifeModifier) {
		this.vgLifeModifier = vgLifeModifier;
		return this;
	}

	public MtgCardBuilder setScryfallUrl(String scryfallUrl) {
		this.scryfallUrl = scryfallUrl;
		return this;
	}

	public MtgCardBuilder setScryfallId(String scryfallId) {
		this.scryfallId = scryfallId;
		return this;
	}

	public MtgCard createMtgCard() {
		return new MtgCard(multiverseId, name, manacost, cmc, language, typeLine, rarity, set, scryfallId, colorIdentity, legalities, text, power, toughness, flavorText, loyalty, vgHandModifier, vgLifeModifier, scryfallUrl, number, rulings, foreignNames, otherSide, layout);
	}

	public MtgCardBuilder setColorIdentity(String[] colorIdentity) {
		this.colorIdentity = colorIdentity;
		return this;
	}

	public MtgCardBuilder setNumber(String number) {
		this.number = number;
		return this;
	}

	public MtgCardBuilder setRulings(MtgJsonCard.Ruling[] rulings) {
		this.rulings = rulings;
		return this;
	}

	public MtgCardBuilder setForeignNames(MtgJsonCard.ForeignName[] foreignNames) {
		this.foreignNames = foreignNames;
		return this;
	}

	public MtgCardBuilder setOtherSide(Supplier<MtgCard> otherSide) {
		this.otherSide = otherSide;
		return this;
	}

	public MtgCardBuilder setLayout(ScryfallCard.Layout layout) {
		this.layout = layout;
		return this;
	}
}