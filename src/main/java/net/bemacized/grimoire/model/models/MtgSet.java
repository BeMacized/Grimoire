package net.bemacized.grimoire.model.models;

public class MtgSet {

	private String name;
	private String code;
	private String gathererCode;
	private String oldCode;
	private String magicCardsInfoCode;
	private String releaseDate;
	private String border;
	private String type;
	private String block;
	private boolean onlineOnly;
	private Object[] booster;

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public String getGathererCode() {
		return gathererCode;
	}

	public String getOldCode() {
		return oldCode;
	}

	public String getMagicCardsInfoCode() {
		return magicCardsInfoCode;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public String getBorder() {
		return border;
	}

	public String getType() {
		return type;
	}

	public String getBlock() {
		return block;
	}

	public boolean isOnlineOnly() {
		return onlineOnly;
	}

	public Object[] getBooster() {
		return booster;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MtgSet mtgSet = (MtgSet) o;

		if (onlineOnly != mtgSet.onlineOnly) return false;
		if (!name.equals(mtgSet.name)) return false;
		if (!code.equals(mtgSet.code)) return false;
		if (gathererCode != null ? !gathererCode.equals(mtgSet.gathererCode) : mtgSet.gathererCode != null)
			return false;
		if (oldCode != null ? !oldCode.equals(mtgSet.oldCode) : mtgSet.oldCode != null) return false;
		if (magicCardsInfoCode != null ? !magicCardsInfoCode.equals(mtgSet.magicCardsInfoCode) : mtgSet.magicCardsInfoCode != null)
			return false;
		if (releaseDate != null ? !releaseDate.equals(mtgSet.releaseDate) : mtgSet.releaseDate != null) return false;
		if (border != null ? !border.equals(mtgSet.border) : mtgSet.border != null) return false;
		if (type != null ? !type.equals(mtgSet.type) : mtgSet.type != null) return false;
		return block != null ? block.equals(mtgSet.block) : mtgSet.block == null;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + code.hashCode();
		result = 31 * result + (gathererCode != null ? gathererCode.hashCode() : 0);
		result = 31 * result + (oldCode != null ? oldCode.hashCode() : 0);
		result = 31 * result + (magicCardsInfoCode != null ? magicCardsInfoCode.hashCode() : 0);
		result = 31 * result + (releaseDate != null ? releaseDate.hashCode() : 0);
		result = 31 * result + (border != null ? border.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (block != null ? block.hashCode() : 0);
		result = 31 * result + (onlineOnly ? 1 : 0);
		return result;
	}

}
