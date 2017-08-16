package net.bemacized.grimoire.data.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "MismatchedReadAndWriteOfArray", "unused"})
public class MtgJsonSet {

	@Expose
	@Nonnull
	private String name = "UNKNOWN";
	@Expose
	@Nonnull
	private String code = "UNKNOWN";
	//	@Expose
//	@Nonnull
//	private String releaseDate = "UNKNOWN";
//	@Expose
//	@Nonnull
//	private Border border = Border.UNKNOWN;
//	@Expose
//	@Nonnull
//	private Type type = Type.UNKNOWN;
	@Expose(serialize = false)
	@Nonnull
	private MtgJsonCard[] cards = new MtgJsonCard[0];

//	@Expose
//	@Nullable
//	private String gathererCode;
//	@Expose
//	@Nullable
//	private String oldCode;
//	@Expose
//	@Nullable
//	private String magicCardsInfoCode;
//	@Expose
//	@Nullable
//	private Booster booster;
//	@Expose
//	@Nullable
//	private String block;

//	@Expose
//	private boolean onlineOnly;

	private long timestamp = System.currentTimeMillis();

	
	@Nonnull
	public String getName() {
		return name;
	}

	
	@Nonnull
	public String getCode() {
		return code;
	}

	@JsonIgnore
	@Nonnull
	public String getReleaseDate() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return releaseDate;
	}

	@JsonIgnore
	@Nonnull
	public Border getBorder() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return border;
	}

	@JsonIgnore
	@Nonnull
	public Type getType() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return type;
	}

	@JsonIgnore
	@Nullable
	public String getBlock() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return block;
	}

	@JsonIgnore
	@Nullable
	public String getGathererCode() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return gathererCode;
	}

	@JsonIgnore
	@Nullable
	public String getOldCode() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return oldCode;
	}

	@JsonIgnore
	@Nullable
	public String getMagicCardsInfoCode() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return magicCardsInfoCode;
	}

	@JsonIgnore
	@Nullable
	public Booster getBooster() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return booster;
	}

	@JsonIgnore
	public boolean isOnlineOnly() {
		throw new NotImplementedException("Property disabled. Checklist: Uncomment field+return, Remove @JsonIgnore, Fix assertValidity & toString methods.");
//		return onlineOnly;
	}

	
	public long getTimestamp() {
		return timestamp;
	}

	public static class Booster {

		private BoosterCardType[][] cardTypes;

		public Booster() {
		}

		Booster(BoosterCardType[][] cardTypes) {
			this.cardTypes = cardTypes;
		}

		public BoosterCardType[][] getCardTypes() {
			return cardTypes;
		}

	}

	public static class BoosterDeserializer implements JsonDeserializer<Booster> {

		@Override
		public Booster deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray arr = json.getAsJsonArray();
			List<BoosterCardType[]> booster = new ArrayList<>();
			for (JsonElement slot : arr) {
				List<BoosterCardType> types = new ArrayList<>();
				if (slot.isJsonArray())
					for (JsonElement e : slot.getAsJsonArray())
						types.add(context.deserialize(e, BoosterCardType.class));
				else types.add(context.deserialize(slot, BoosterCardType.class));
				booster.add(types.toArray(new BoosterCardType[0]));
			}
			return new Booster(booster.toArray(new BoosterCardType[0][0]));
		}
	}

	private enum BoosterCardType {
		UNKNOWN,
		@SerializedName("common")
		COMMON,
		@SerializedName("uncommon")
		UNCOMMON,
		@SerializedName("rare")
		RARE,
		@SerializedName("mythic rare")
		MYTHIC_RARE,
		@SerializedName("land")
		LAND,
		@SerializedName("marketing")
		MARKETING,
		@SerializedName("checklist")
		CHECKLIST,
		@SerializedName("double faced")
		DOUBLE_FACED
	}

	private enum Border {
		UNKNOWN,
		@SerializedName("black")
		BLACK,
		@SerializedName("white")
		WHITE,
		@SerializedName("silver")
		SILVER
	}

	private enum Type {
		UNKNOWN,
		@SerializedName("core")
		CORE,
		@SerializedName("expansion")
		EXPANSION,
		@SerializedName("reprint")
		REPRINT,
		@SerializedName("box")
		BOX,
		@SerializedName("un")
		UN,
		@SerializedName("from the vault")
		FROM_THE_VAULT,
		@SerializedName("premium deck")
		PREMIUM_DECK,
		@SerializedName("duel deck")
		DUEL_DECK,
		@SerializedName("starter")
		STARTER,
		@SerializedName("commander")
		COMMANDER,
		@SerializedName("planechase")
		PLANECHASE,
		@SerializedName("archenemy")
		ARCHENEMY,
		@SerializedName("promo")
		PROMO,
		@SerializedName("vanguard")
		VANGUARD,
		@SerializedName("masters")
		MASTERS,
		@SerializedName("conspiracy")
		CONSPIRACY,
		@SerializedName("masterpiece")
		MASTERPIECE
	}

	public void assertValidity() {
//		assert !type.equals(Type.UNKNOWN);
//		assert !border.equals(Border.UNKNOWN);
//		assert !releaseDate.equals("UNKNOWN");
		assert !code.equals("UNKNOWN");
		assert !name.equals("UNKNOWN");
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("name", name)
				.append("code", code)
//				.append("releaseDate", releaseDate)
//				.append("border", border)
//				.append("type", type)
//				.append("gathererCode", gathererCode)
//				.append("oldCode", oldCode)
//				.append("magicCardsInfoCode", magicCardsInfoCode)
//				.append("booster", booster)
//				.append("block", block)
//				.append("onlineOnly", onlineOnly)
				.append("timestamp", timestamp)
				.toString();
	}
}
