package net.bemacized.grimoire.data.models;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.codec.digest.DigestUtils;
import org.jongo.marshall.jackson.oid.MongoId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MtgSet {

	public static final String COLLECTION = "MtgSets";

	@MongoId
	private String _id;

	@Nullable
	private MtgJsonSet mtgJsonSet;
	@Nonnull
	private ScryfallSet scryfallSet;

	public MtgSet() {
	}

	public MtgSet(@Nonnull ScryfallSet scryfallSet, @Nullable MtgJsonSet mtgJsonSet) {
		this.mtgJsonSet = mtgJsonSet;
		this.scryfallSet = scryfallSet;
		this._id = generateId();
	}

	@Nullable
	public MtgJsonSet getMtgJsonSet() {
		return mtgJsonSet;
	}

	@Nonnull
	public ScryfallSet getScryfallSet() {
		return scryfallSet;
	}

	@Nonnull
	public String getCode() {
		return scryfallSet.getCode().toUpperCase();
	}

	@Nonnull
	public String getName() {
		return scryfallSet.getName();
	}

	@Nullable
	public String getReleaseDate() {
		return scryfallSet.getReleasedAt();
	}

	@Nonnull
	public ScryfallSet.Type getType() {
		return scryfallSet.getSetType();
	}

	@Nullable
	public String getBlock() {
		return scryfallSet.getBlock();
	}

	public boolean isOnlineOnly() {
		return scryfallSet.isDigital();
	}

	public int getCardCount() {
		return scryfallSet.getCardCount();
	}

	public void assertValidity() {
		assert scryfallSet != null;
	}

	public void save() {
		Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).save(this);
	}

	private String generateId() {
		String id = scryfallSet.getCode();
		if (mtgJsonSet != null) id += mtgJsonSet.getCode();
		return DigestUtils.sha1Hex(id);
	}

	public MessageEmbed getEmbed() {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setTitle(String.format("%s (%s)", getName(), getCode()), "https://scryfall.com/sets/" + getCode().toLowerCase());
		if (getReleaseDate() != null) eb.addField("Release Date", getReleaseDate(), true);
		eb.addField("Type", getType().getDisplayName().substring(0, 1).toUpperCase() + getType().getDisplayName().substring(1), true);
		if (getBlock() != null && !getBlock().isEmpty()) eb.addField("Block", getBlock(), true);
		eb.addField("MTG Online Only", isOnlineOnly() ? "Yes" : "No", true);
		eb.addField("Cards", String.valueOf(getCardCount()), true);
		return eb.build();
	}

	@Nullable
	public MtgSet getParentSet() {
		if (scryfallSet.getParentSetCode() == null) return null;
		return Grimoire.getInstance().getCardProvider().getSetByCode(scryfallSet.getParentSetCode());
	}
}
