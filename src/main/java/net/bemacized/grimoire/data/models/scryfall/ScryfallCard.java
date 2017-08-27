package net.bemacized.grimoire.data.models.scryfall;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class ScryfallCard {

	@Nonnull
	private String id = "UNKNOWN";
	@Nonnull
	private String name = "UNKNOWN";
	@Nonnull
	private String cmc = "0";
	@Nonnull
	private String[] colors = new String[0];
	@Nonnull
	private String[] color_identity = new String[0];
	@Nonnull
	private Layout layout = Layout.UNKNOWN;
	@Nonnull
	private Face[] card_faces = new Face[0];
	@Nonnull
	private HashMap<String, Legality> legalities = new HashMap<>();
	@Nonnull
	private String set = "UNKNOWN"; //code
	@Nonnull
	private String set_name = "UNKNOWN";
	@Nonnull
	private Rarity rarity = Rarity.UNKNOWN;
	@Nonnull
	private Frame frame = Frame.UNKNOWN;
	@Nonnull
	private BorderColor border_color = BorderColor.UNKNOWN;
	@Nonnull
	private String scryfall_uri = "UNKNOWN";
	@Nonnull
	private ImageUris image_uris = new ImageUris();
	@Nonnull
	private HashMap<String, String> related_uris = new HashMap<>();
	@Nonnull
	private HashMap<String, String> purchase_uris = new HashMap<>();
	@Nonnull
	private Part[] all_parts = new Part[0];

	@Nullable
	private String mana_cost;
	@Nullable
	private String type_line;
	@Nullable
	private String oracle_text;
	@Nullable
	private String power;
	@Nullable
	private String toughness;
	@Nullable
	private String loyalty;
	@Nullable
	private String hand_modifier;
	@Nullable
	private String life_modifier;
	@Nullable
	private String mtgo_id;
	@Nullable
	private String collector_number;
	@Nullable
	private String watermark;
	@Nullable
	private String flavor_text;
	@Nullable
	private String usd;
	@Nullable
	private String eur;
	@Nullable
	private String tix;
	@Nullable
	private String image_uri;
	@Nullable
	private String artist;

	private boolean reserved;
	private boolean reprint;
	private boolean timeshifted;
	private boolean colorshifted;
	private boolean futureshifted;
	private int multiverse_id = -1;

	private long timestamp = System.currentTimeMillis();

	@Nonnull
	public String getId() {
		return id;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getCMC() {
		return cmc;
	}

	@Nonnull
	public String[] getColors() {
		return colors;
	}

	@Nonnull
	public String[] getColorIdentity() {
		return color_identity;
	}

	@Nonnull
	public Layout getLayout() {
		// EMBLEM FIX
		if (getTypeLine() != null && getTypeLine().contains("Emblem")) return Layout.EMBLEM;
		return layout;
	}

	@Nonnull
	public Face[] getCardFaces() {
		return card_faces;
	}

	@Nonnull
	public HashMap<String, Legality> getLegalities() {
		return legalities;
	}

	@Nonnull
	public String getSet() {
		return set;
	}

	@Nonnull
	public String getSetName() {
		return set_name;
	}

	@Nonnull
	public Rarity getRarity() {
		return rarity;
	}

	@Nullable
	public String getArtist() {
		return artist;
	}

	@Nonnull
	public Frame getFrame() {
		return frame;
	}

	@Nonnull
	public BorderColor getBorderColor() {
		return border_color;
	}

	@Nonnull
	public String getScryfallUri() {
		return scryfall_uri;
	}

	@Nonnull
	public ImageUris getImageUris() {
		return image_uris;
	}

	@Nonnull
	public HashMap<String, String> getRelatedUris() {
		return related_uris;
	}

	@Nonnull
	public HashMap<String, String> getPurchaseUris() {
		return purchase_uris;
	}

	@Nullable
	public String getManaCost() {
		return mana_cost;
	}

	@Nullable
	public String getTypeLine() {
		return type_line;
	}

	@Nullable
	public String getOracleText() {
		return oracle_text;
	}

	@Nullable
	public String getPower() {
		return power;
	}

	@Nullable
	public String getToughness() {
		return toughness;
	}

	@Nullable
	public String getLoyalty() {
		return loyalty;
	}

	@Nullable
	public String getHandModifier() {
		return hand_modifier;
	}

	@Nullable
	public String getLifeModifier() {
		return life_modifier;
	}

	@Nullable
	public String getMtgoId() {
		return mtgo_id;
	}

	@Nullable
	public String getCollectorNumber() {
		return collector_number;
	}

	@Nonnull
	public Part[] getAllParts() {
		return all_parts;
	}

	@Nullable
	public String getWatermark() {
		return watermark;
	}

	@Nullable
	public String getFlavorText() {
		return flavor_text;
	}

	@Nullable
	public String getUsd() {
		return usd;
	}

	@Nullable
	public String getEur() {
		return eur;
	}

	@Nullable
	public String getTix() {
		return tix;
	}

	@Nullable
	public String getImageUri() {
		return image_uri;
	}

	public boolean isReserved() {
		return reserved;
	}

	public boolean isReprint() {
		return reprint;
	}

	public boolean isTimeshifted() {
		return timeshifted;
	}

	public boolean isColorshifted() {
		return colorshifted;
	}

	public boolean isFutureshifted() {
		return futureshifted;
	}

	public int getMultiverseId() {
		return multiverse_id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	private enum BorderColor {
		UNKNOWN,
		@SerializedName("black")
		BLACK,
		@SerializedName("white")
		WHITE,
		@SerializedName("silver")
		SILVER,
		@SerializedName("gold")
		GOLD
	}

	private enum Frame {
		UNKNOWN,
		@SerializedName("1993")
		Y1993,
		@SerializedName("2003")
		Y2003,
		@SerializedName("2015")
		Y2015,
		@SerializedName("future")
		FUTURE
	}

	public enum Rarity {
		UNKNOWN,
		@SerializedName("common")
		COMMON,
		@SerializedName("uncommon")
		UNCOMMON,
		@SerializedName("rare")
		RARE,
		@SerializedName("mythic")
		MYTHIC
	}

	public static class Part {
		@Nonnull
		private String id = "UNKNOWN";
		@Nonnull
		private String name = "UNKNOWN";
		@Nonnull
		private String uri = "UNKNOWN";

		public Part() {
		}
	}

	public enum Layout {
		UNKNOWN,
		@SerializedName("normal")
		NORMAL,
		@SerializedName("split")
		SPLIT,
		@SerializedName("flip")
		FLIP,
		@SerializedName("transform")
		TRANSFORM,
		@SerializedName("meld")
		MELD,
		@SerializedName("leveler")
		LEVELER,
		@SerializedName("plane")
		PLANE,
		@SerializedName("phenomenon")
		PHENOMENON,
		@SerializedName("scheme")
		SCHEME,
		@SerializedName("vanguard")
		VANGUARD,
		@SerializedName("token")
		TOKEN,
		@SerializedName("emblem")
		EMBLEM
	}

	public enum Legality {
		UNKNOWN,
		@SerializedName("legal")
		LEGAL,
		@SerializedName("not_legal")
		NOT_LEGAL,
		@SerializedName("restricted")
		RESTRICTED,
		@SerializedName("banned")
		BANNED;

		public String getDisplayName() {
			return WordUtils.capitalize(name().replaceAll("_", " ").toLowerCase());
		}
	}

	public static class ImageUris {
		@Nullable
		private String small;
		@Nullable
		private String normal;
		@Nullable
		private String large;
		@Nullable
		private String png;

		public ImageUris() {
		}

		@Nullable
		public String getSmall() {
			return small;
		}

		@Nullable
		public String getNormal() {
			return normal;
		}

		@Nullable
		public String getLarge() {
			return large;
		}

		@Nullable
		public String getPng() {
			return png;
		}
	}

	public static class Face {
		@Nonnull
		private String name = "UNKNOWN";
		@Nullable
		private String mana_cost;
		@Nonnull
		private String type_line = "UNKNOWN";
		@Nullable
		private String oracle_text;
		@Nullable
		private String power;
		@Nullable
		private String toughness;

		public Face() {
		}

		@Nonnull
		public String getName() {
			return name;
		}

		@Nullable
		public String getManaCost() {
			return mana_cost;
		}

		@Nullable
		public String getTypeLine() {
			return type_line;
		}

		@Nullable
		public String getOracleText() {
			return oracle_text;
		}

		@Nullable
		public String getPower() {
			return power;
		}

		@Nullable
		public String getToughness() {
			return toughness;
		}
	}

	public void assertValidity() {
		assert !id.equals("UNKNOWN");
		assert !name.equals("UNKNOWN");
		assert !layout.equals(Layout.UNKNOWN);
		if (card_faces.length > 0) {
			for (Face card_face : card_faces) {
				assert !card_face.name.equals("UNKNOWN");
				assert !card_face.type_line.equals("UNKNOWN");
			}
		}
		for (Map.Entry<String, Legality> entry : legalities.entrySet()) {
			assert !entry.getValue().equals(Legality.UNKNOWN);
		}
		assert !set.equals("UNKNOWN");
		assert !set_name.equals("UNKNOWN");
		assert !rarity.equals(Rarity.UNKNOWN);
		assert !frame.equals(Frame.UNKNOWN);
		assert !border_color.equals(BorderColor.UNKNOWN);
		assert !scryfall_uri.equals("UNKNOWN");
		if (all_parts.length > 0) {
			for (Part all_part : all_parts) {
				assert !all_part.id.equals("UNKNOWN");
				assert !all_part.name.equals("UNKNOWN");
				assert !all_part.uri.equals("UNKNOWN");
			}
		}
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("id", id)
				.append("name", name)
				.append("cmc", cmc)
				.append("colors", colors)
				.append("color_identity", color_identity)
				.append("layout", layout)
				.append("card_faces", card_faces)
				.append("legalities", legalities)
				.append("set", set)
				.append("set_name", set_name)
				.append("rarity", rarity)
				.append("frame", frame)
				.append("border_color", border_color)
				.append("scryfall_uri", scryfall_uri)
				.append("image_uris", image_uris)
				.append("related_uris", related_uris)
				.append("purchase_uris", purchase_uris)
				.append("all_parts", all_parts)
				.append("mana_cost", mana_cost)
				.append("type_line", type_line)
				.append("oracle_text", oracle_text)
				.append("power", power)
				.append("toughness", toughness)
				.append("loyalty", loyalty)
				.append("hand_modifier", hand_modifier)
				.append("life_modifier", life_modifier)
				.append("multiverse_id", multiverse_id)
				.append("mtgo_id", mtgo_id)
				.append("collector_number", collector_number)
				.append("watermark", watermark)
				.append("flavor_text", flavor_text)
				.append("usd", usd)
				.append("eur", eur)
				.append("tix", tix)
				.append("image_uri", image_uri)
				.append("artist", artist)
				.append("reserved", reserved)
				.append("reprint", reprint)
				.append("timeshifted", timeshifted)
				.append("colorshifted", colorshifted)
				.append("futureshifted", futureshifted)
				.append("timestamp", timestamp)
				.toString();
	}
}
