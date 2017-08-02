package net.bemacized.grimoire.model.models;

public class Card {

	private String id;
	private String layout;
	private String name;
	private String[] names;
	private String manaCost;
	private int cmc;
	private String[] colors;
	private String[] colorIdentity;
	private String type;
	private String[] supertypes;
	private String[] types;
	private String[] subtypes;
	private String rarity;
	private String text;
	private String flavor;
	private String artist;
	private String number;
	private String power;
	private String toughness;
	private int loyalty;
	private int multiverseid;
	private int[] variations;
	private String border;
	private String reserved;
	private String releaseDate;
	private boolean starter;
	private String mciNumber;
	private Ruling[] rulings;
	private ForeignName[] foreignNames;
	private String[] printings;
	private String originalText;
	private String originalType;
	private Legality[] legalities;
//	private String source;
//	private String imageName;
//	private String watermark;
//	private boolean timeshifted;
//	private int hand;
//	private int life;

	private transient MtgSet set;

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
		return supertypes == null ? new String[0] : supertypes;
	}

	public String[] getTypes() {
		return types == null ? new String[0] : types;
	}

	public String[] getSubtypes() {
		return subtypes == null ? new String[0] : subtypes;
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
		return "https://api.scryfall.com/cards/multiverse/" + multiverseid + "?format=image";
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

		public String getLanguage() {
			return language;
		}

		public String getName() {
			return name;
		}

		public int getMultiverseid() {
			return multiverseid;
		}
	}

}
