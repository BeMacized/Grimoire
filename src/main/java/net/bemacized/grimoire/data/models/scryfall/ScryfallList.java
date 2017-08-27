package net.bemacized.grimoire.data.models.scryfall;

import com.google.gson.JsonArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("FieldCanBeLocal")
public class ScryfallList {

	private int total_cards = -1;
	private boolean has_more;
	@Nonnull
	private String[] warnings = new String[0];
	@Nullable
	private String next_page;
	@Nonnull
	private JsonArray data = new JsonArray();

	public ScryfallList() {
	}

	public int getTotalCards() {
		return total_cards;
	}

	public boolean hasMore() {
		return has_more;
	}

	@Nonnull
	public String[] getWarnings() {
		return warnings;
	}

	@Nullable
	public String getNextPage() {
		return next_page;
	}

	@Nonnull
	public JsonArray getData() {
		return data;
	}
}
