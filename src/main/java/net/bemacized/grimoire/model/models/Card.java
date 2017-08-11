package net.bemacized.grimoire.model.models;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Card implements Cloneable {

	private static final Logger LOG = Logger.getLogger(Card.class.getName());

	private String id;
	private String layout;
	private String name;
	private String[] names = new String[0];
	private String manaCost;
	private int cmc;
	private String[] colors = new String[0];
	private String[] colorIdentity = new String[0];
	private String type;
	private String[] supertypes = new String[0];
	private String[] types = new String[0];
	private String[] subtypes = new String[0];
	private String rarity;
	private String text;
	private String flavor;
	private String artist;
	private String number;
	private String power;
	private String toughness;
	private int loyalty;
	private int multiverseid;
	private int[] variations = new int[0];
	private String border;
	private String reserved;
	private String releaseDate;
	private boolean starter;
	private String mciNumber;
	private Ruling[] rulings;
	private ForeignName[] foreignNames = new ForeignName[0];
	private String[] printings = new String[0];
	private String originalText;
	private String originalType;
	private Legality[] legalities = new Legality[0];
//	private String source;
//	private String imageName;
//	private String watermark;
//	private boolean timeshifted;
//	private int hand;
//	private int life;

	private transient MtgSet set;
	private transient String imageUrl;
	private transient String language = "English";

	public String getId() {
		return id;
	}

	public String getLayout() {
		return layout;
	}

	public String getName() {
		return name;
	}

	public String[] getNames() {
		return names;
	}

	public String getManaCost() {
		return manaCost;
	}

	public int getCmc() {
		return cmc;
	}

	public String[] getColors() {
		return colors;
	}

	public String[] getColorIdentity() {
		return colorIdentity;
	}

	public String getType() {
		return type;
	}

	public String[] getSupertypes() {
		return supertypes;
	}

	public String[] getTypes() {
		return types;
	}

	public String[] getSubtypes() {
		return subtypes;
	}

	public String getRarity() {
		return rarity;
	}

	public String getText() {
		return text;
	}

	public String getFlavor() {
		return flavor;
	}

	public String getArtist() {
		return artist;
	}

	public String getNumber() {
		return number;
	}

	public String getPower() {
		return power;
	}

	public String getToughness() {
		return toughness;
	}

	public int getLoyalty() {
		return loyalty;
	}

	public int getMultiverseid() {
		return multiverseid;
	}

	public int[] getVariations() {
		return variations;
	}

	public String getBorder() {
		return border;
	}

	public String getReserved() {
		return reserved;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public boolean isStarter() {
		return starter;
	}

	public String getMciNumber() {
		return mciNumber;
	}

	public Ruling[] getRulings() {
		return rulings;
	}

	public ForeignName[] getForeignNames() {
		return foreignNames;
	}

	public String[] getPrintings() {
		return printings;
	}

	public String getOriginalText() {
		return originalText;
	}

	public String getOriginalType() {
		return originalType;
	}

	public Legality[] getLegalities() {
		return legalities;
	}

	public MtgSet getSet() {
		return set;
	}

	public String getImageUrl() {
		if (imageUrl == null) imageUrl = Grimoire.getInstance().getImageProviders().findUrl(this);
		return imageUrl;
	}

	public String getLanguage() {
		return language;
	}

	public Card[] getForeignVersions() {
		return Arrays.stream(getForeignNames()).parallel().map(this::foreignClone).collect(Collectors.toList()).toArray(new Card[0]);
	}

	public String getGathererUrl() {
		return (multiverseid > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseid : null;
	}

	public MessageEmbed getEmbed(Guild guild) {
		String formats = (getLegalities() == null) ? "" : String.join(", ", Arrays.stream(getLegalities())
				.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
				.map(Legality::getFormat)
				.collect(Collectors.toList()));
		String rarities = String.join(", ", new Cards.SearchQuery().hasExactName(getName()).parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
		String printings = String.join(", ", new String[]{"**" + getSet().getName() + " (" + getSet().getCode() + ")**", String.join(", ", Arrays.stream(getPrintings()).parallel().filter(setCode -> !getSet().getCode().equalsIgnoreCase(setCode)).collect(Collectors.toList()))}).trim();
		if (printings.endsWith(",")) printings = printings.substring(0, printings.length() - 1);
		String pat = MTGUtils.parsePowerAndToughness(getPower(), getToughness());

		//TODO: ENABLE AGAIN WHEN DISCORD FIXES EMOJI IN EMBED TITLES ON MOBILE ---
		//		String title = getName()
		//				+ " "
		//				+ CardUtils.parseEmoji(e.getGuild(), getManaCost());
		//		String separateCost = "";
		//		if (title.length() > 256) {
		//			title = getName();
		//			separateCost = CardUtils.parseEmoji(e.getGuild(), getManaCost());
		//		}

		String title = getName();
		String separateCost = (getManaCost() == null || getManaCost().isEmpty()) ? "" : Grimoire.getInstance().getEmojiParser().parseEmoji(getManaCost(), guild) + " **(" + new DecimalFormat("##.###").format(getCmc()) + ")**";

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setThumbnail(getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(getColorIdentity()));
		eb.setTitle(title, getGathererUrl());
		if (!separateCost.isEmpty()) eb.appendDescription(separateCost + "\n");
		if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		eb.appendDescription(getType());
		eb.appendDescription("\n\n");
		eb.appendDescription(Grimoire.getInstance().getEmojiParser().parseEmoji(getText(), guild));
		if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		if (!printings.isEmpty()) eb.addField("Printings", printings, true);
		if (loyalty > 0) eb.addField("Loyalty", String.valueOf(loyalty), true);

		// Return result
		return eb.build();
	}

	public static class Ruling {

		private String date;
		private String text;

		public String getDate() {
			return date;
		}

		public String getText() {
			return text;
		}
	}

	public static class Legality {

		private String format;
		private String legality;

		public Legality(String format, String legality) {
			this.format = format;
			this.legality = legality;
		}

		public Legality() {
		}

		public String getFormat() {
			return format;
		}

		public String getLegality() {
			return legality;
		}
	}

	public static class ForeignName {

		private String language;
		private String name;
		private int multiverseid;

		public ForeignName() {
		}

		private ForeignName(String language, String name, int multiverseid) {
			this.language = language;
			this.name = name;
			this.multiverseid = multiverseid;
		}

		public String getLanguage() {
			return language;
		}

		public String getName() {
			return name;
		}

		public int getMultiverseid() {
			return multiverseid;
		}

		public String getGathererUrl() {
			return (multiverseid > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseid : null;
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private Card foreignClone(ForeignName fn) {
		try {
			Card clone = (Card) this.clone();
			clone.multiverseid = fn.getMultiverseid();
			clone.name = fn.getName();
			clone.language = fn.getLanguage();
			clone.foreignNames = Stream.concat(
					Stream.of(new ForeignName(language, name, multiverseid)),
					Arrays.stream(clone.foreignNames).filter(fn_ -> !fn_.language.equals(fn.getLanguage()))
			).collect(Collectors.toList()).toArray(new ForeignName[0]);
			return clone;
		} catch (CloneNotSupportedException e) {
			LOG.log(Level.SEVERE, "Could not create foreign card clone", e);
		}
		return null;
	}


}
