package net.bemacized.grimoire.data.models.scryfall;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScryfallSet {

	@Nonnull
	private String code = "UNKNOWN";
	@Nonnull
	private String name = "UNKNOWN";
	@Nonnull
	private String icon_svg_uri = "UNKNOWN";
	//	@Nonnull
//	private String search_uri = "UNKNOWN";
	@Nonnull
	private Type set_type = Type.UNKNOWN;

	//	@Nullable
//	private String block_code;
	@Nullable
	private String block;
	@Nullable
	private String parent_set_code;
	@Nullable
	private String released_at;

	private int card_count = 0;
	private boolean digital;
//	private boolean foil;

	private long timestamp = System.currentTimeMillis();

	
	@Nonnull
	public String getCode() {
		return code;
	}

	
	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getIconSvgUri() {
		return icon_svg_uri;
	}

	@JsonIgnore
	@Nonnull
	public String getSearchUri() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return search_uri;
	}

	
	@Nonnull
	public Type getSetType() {
		return set_type;
	}

	@JsonIgnore
	@Nullable
	public String getBlockCode() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return block_code;
	}

	
	@Nullable
	public String getBlock() {
		return block;
	}

	@Nullable
	public String getParentSetCode() {
		return parent_set_code;
	}

	
	@Nullable
	public String getReleasedAt() {
		return released_at;
	}

	
	public int getCardCount() {
		return card_count;
	}

	
	public boolean isDigital() {
		return digital;
	}

	@JsonIgnore
	public boolean isFoil() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return foil;
	}

	
	public long getTimestamp() {
		return timestamp;
	}

	@SuppressWarnings("SpellCheckingInspection")
	public enum Type {
		UNKNOWN,
		@SerializedName("core")
		CORE,
		@SerializedName("expansion")
		EXPANSION,
		@SerializedName("masters")
		MASTERS,
		@SerializedName("masterpiece")
		MASTERPIECE,
		@SerializedName("from_the_vault")
		FROM_THE_VAULT,
		@SerializedName("premium_deck")
		PREMIUM_DECK,
		@SerializedName("duel_deck")
		DUEL_DECK,
		@SerializedName("commander")
		COMMANDER,
		@SerializedName("conspiracy")
		CONSPIRACY,
		@SerializedName("archenemy")
		ARCHENEMY,
		@SerializedName("vanguard")
		VANGUARD,
		@SerializedName("funny")
		FUNNY,
		@SerializedName("starter")
		STARTER,
		@SerializedName("box")
		BOX,
		@SerializedName("promo")
		PROMO,
		@SerializedName("token")
		TOKEN,
		@SerializedName("treasure_chest")
		TREASURE_CHEST,
		@SerializedName("planechase")
		PLANECHASE,
		@SerializedName("memorabilia")
		MEMORABILIA;

		public String getDisplayName() {
			String name = name().replaceAll("_", " ");
			return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
		}
	}

	public void assertValidity() {
		assert !code.equals("UNKNOWN");
		assert !name.equals("UNKNOWN");
		assert !icon_svg_uri.equals("UNKNOWN");
//		assert !search_uri.equals("UNKNOWN");
		assert !set_type.equals(Type.UNKNOWN);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("code", code)
				.append("name", name)
				.append("icon_svg_uri", icon_svg_uri)
//				.append("search_uri", search_uri)
				.append("set_type", set_type)
//				.append("block_code", block_code)
				.append("block", block)
				.append("parent_set_code", parent_set_code)
				.append("released_at", released_at)
				.append("card_count", card_count)
				.append("digital", digital)
//				.append("foil", foil)
				.append("timestamp", timestamp)
				.toString();
	}
}
