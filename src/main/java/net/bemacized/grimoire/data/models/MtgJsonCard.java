package net.bemacized.grimoire.data.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class MtgJsonCard {

	@Nonnull
	private String id = "UNKNOWN";
	@Nonnull
	private Layout layout = Layout.UNKNOWN;
	@Nonnull
	private String name = "UNKNOWN";
	@Nonnull
	private String[] names = new String[0];
	@Nonnull
	private String manaCost = "";
	@Nonnull
	private String[] colors = new String[0];
	@Nonnull
	private String[] colorIdentity = new String[0];
	@Nonnull
	private String type = "UNKNOWN";
	@Nonnull
	private String[] supertypes = new String[0];
	@Nonnull
	private String[] types = new String[0];
	@Nonnull
	private String[] subtypes = new String[0];
	@Nonnull
	private Rarity rarity = Rarity.UNKNOWN;
	@Nonnull
	private String artist = "UNKNOWN";
	@Nonnull
	private int[] variations = new int[0];
	@Nonnull
	private Ruling[] rulings = new Ruling[0];
	@Nonnull
	private ForeignName[] foreignNames = new ForeignName[0];
	@Nonnull
	private String[] printings = new String[0];
	@Nonnull
	private Legality[] legalities = new Legality[0];
	@Nonnull
	private String setCode = "UNKNOWN";

	@Nullable
	private String border; //TODO: IF NULL INHERIT FROM SET
	@Nullable
	private String releaseDate; //TODO: INHERIT FROM SET IF NULL
	@Nullable
	private String originalText;
	@Nullable
	private String originalType;
	@Nullable
	private String source;
	@Nullable
	private String mciNumber;
	@Nullable
	private String power;
	@Nullable
	private String toughness;
	@Nullable
	private String flavor;
	@Nullable
	private String text;
	@Nullable
	private String number;

	private int cmc = 0;
	private int loyalty = 0;
	private int multiverseid = -1;
	private boolean reserved;
	private boolean starter;

	private long timestamp = System.currentTimeMillis();

	@Nonnull
	public String getId() {
		return id;
	}

	@Nonnull
	public Layout getLayout() {
		return layout;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String[] getNames() {
		return names;
	}

	@Nonnull
	public String getManaCost() {
		return manaCost;
	}

	@Nonnull
	public String[] getColors() {
		return colors;
	}

	@Nonnull
	public String[] getColorIdentity() {
		return colorIdentity;
	}

	@Nonnull
	public String getType() {
		return type;
	}

	@Nonnull
	public String[] getSupertypes() {
		return supertypes;
	}

	@Nonnull
	public String[] getTypes() {
		return types;
	}

	@Nonnull
	public String[] getSubtypes() {
		return subtypes;
	}

	@Nonnull
	public Rarity getRarity() {
		return rarity;
	}

	@Nullable
	public String getText() {
		return text;
	}

	@Nullable
	public String getFlavor() {
		return flavor;
	}

	@Nonnull
	public String getArtist() {
		return artist;
	}

	@Nullable
	public String getNumber() {
		return number;
	}

	@Nullable
	public String getPower() {
		return power;
	}

	@Nullable
	public String getToughness() {
		return toughness;
	}

	@Nonnull
	public int[] getVariations() {
		return variations;
	}

	@Nullable
	public String getReleaseDate() {
		return releaseDate;
	}

	@Nonnull
	public Ruling[] getRulings() {
		return rulings;
	}

	@Nonnull
	public ForeignName[] getForeignNames() {
		return foreignNames;
	}

	@Nonnull
	public String[] getPrintings() {
		return printings;
	}

	@Nonnull
	public Legality[] getLegalities() {
		return legalities;
	}

	@Nullable
	public String getBorder() {
		return border;
	}

	@Nullable
	public String getOriginalText() {
		return originalText;
	}

	@Nullable
	public String getOriginalType() {
		return originalType;
	}

	@Nullable
	public String getSource() {
		return source;
	}

	@Nullable
	public String getMciNumber() {
		return mciNumber;
	}

	@Nonnull
	public String getSetCode() {
		return setCode;
	}

	public int getCmc() {
		return cmc;
	}

	public int getLoyalty() {
		return loyalty;
	}

	public int getMultiverseid() {
		return multiverseid;
	}

	public boolean isReserved() {
		return reserved;
	}

	public boolean isStarter() {
		return starter;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public static class Legality {
		@Nonnull
		private String format = "UNKNOWN";
		@Nonnull
		private String legality = "UNKNOWN";
		@Nullable
		private String condition;

		public Legality() {
		}

		@Nonnull
		public String getFormat() {
			return format;
		}

		@Nonnull
		public String getLegality() {
			return legality;
		}

		@Nullable
		public String getCondition() {
			return condition;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("format", format)
					.append("legality", legality)
					.append("condition", condition)
					.toString();
		}
	}

	public static class ForeignName {
		@Nonnull
		private String language = "UNKNOWN";
		@Nonnull
		private String name = "UNKNOWN";

		private int multiverseid = -1;

		public ForeignName() {
		}

		ForeignName(@Nonnull String language, @Nonnull String name, int multiverseid) {
			this.language = language;
			this.name = name;
			this.multiverseid = multiverseid;
		}

		@Nonnull
		public String getLanguage() {
			return language;
		}

		@Nonnull
		public String getName() {
			return name;
		}

		public int getMultiverseid() {
			return multiverseid;
		}

		@Override
		public String toString() {
			return "ForeignName{" +
					"language='" + language + '\'' +
					", name='" + name + '\'' +
					", multiverseid=" + multiverseid +
					'}';
		}

		@Nullable
		public String getGathererUrl() {
			return (getMultiverseid() > 0) ? "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + getMultiverseid() : null;
		}
	}

	public static class Ruling {
		@Nonnull
		private String date = "UNKNOWN";
		@Nonnull
		private String text = "UNKNOWN";

		public Ruling() {
		}

		@Nonnull
		public String getDate() {
			return date;
		}

		@Nonnull
		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("date", date)
					.append("text", text)
					.toString();
		}
	}

	enum Rarity {
		UNKNOWN,
		@SerializedName("Common")
		COMMON,
		@SerializedName("Uncommon")
		UNCOMMON,
		@SerializedName("Rare")
		RARE,
		@SerializedName("Mythic Rare")
		MYTHIC_RARE,
		@SerializedName("Special")
		SPECIAL,
		@SerializedName("Basic Land")
		BASIC_LAND
	}

	enum Layout {
		UNKNOWN,
		@SerializedName("normal")
		NORMAL,
		@SerializedName("split")
		SPLIT,
		@SerializedName("flip")
		FLIP,
		@SerializedName("double-faced")
		DOUBLE_FACED,
		@SerializedName("token")
		TOKEN,
		@SerializedName("plane")
		PLANE,
		@SerializedName("scheme")
		SCHEME,
		@SerializedName("phenomenon")
		PHENOMENON,
		@SerializedName("leveler")
		LEVELER,
		@SerializedName("vanguard")
		VANGUARD,
		@SerializedName("meld")
		MELD,
		@SerializedName("aftermath")
		AFTERMATH
	}

	public void assertValidity() {
		assert !artist.equals("UNKNOWN");
		assert !rarity.equals(Rarity.UNKNOWN);
		assert !type.equals("UNKNOWN");
		assert !name.equals("UNKNOWN");
		assert !id.equals("UNKNOWN");
		assert !layout.equals(Layout.UNKNOWN);
		assert !setCode.equals("UNKNOWN");
		if (rulings.length > 0) {
			for (Ruling ruling : rulings) {
				assert !ruling.date.equals("UNKNOWN");
				assert !ruling.text.equals("UNKNOWN");
			}
		}
		if (foreignNames.length > 0) {
			for (ForeignName foreignName : foreignNames) {
				assert !foreignName.language.equals("UNKNOWN");
				assert !foreignName.name.equals("UNKNOWN");
			}
		}
		if (legalities.length > 0) {
			for (Legality legality : legalities) {
				assert !legality.legality.equals("UNKNOWN");
				assert !legality.format.equals("UNKNOWN");
			}
		}
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		Gson gson = new Gson();
		return gson.fromJson(gson.toJson(this), MtgJsonCard.class);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("id", id)
				.append("layout", layout)
				.append("name", name)
				.append("names", names)
				.append("manaCost", manaCost)
				.append("colors", colors)
				.append("colorIdentity", colorIdentity)
				.append("type", type)
				.append("supertypes", supertypes)
				.append("types", types)
				.append("subtypes", subtypes)
				.append("rarity", rarity)
				.append("artist", artist)
				.append("variations", variations)
				.append("rulings", rulings)
				.append("foreignNames", foreignNames)
				.append("printings", printings)
				.append("legalities", legalities)
				.append("border", border)
				.append("releaseDate", releaseDate)
				.append("originalText", originalText)
				.append("originalType", originalType)
				.append("source", source)
				.append("mciNumber", mciNumber)
				.append("power", power)
				.append("toughness", toughness)
				.append("flavor", flavor)
				.append("text", text)
				.append("number", number)
				.append("cmc", cmc)
				.append("loyalty", loyalty)
				.append("multiverseid", multiverseid)
				.append("reserved", reserved)
				.append("starter", starter)
				.append("timestamp", timestamp)
				.append("setCode", setCode)
				.toString();
	}
}
