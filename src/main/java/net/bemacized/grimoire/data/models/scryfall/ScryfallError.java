package net.bemacized.grimoire.data.models.scryfall;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScryfallError {

	private int status;
	@Nonnull
	private String code;
	@Nonnull
	private String details;

	@Nullable
	private String type;
	@Nullable
	private String[] warnings = new String[0];

	public ScryfallError() {
	}

	public int getStatus() {
		return status;
	}

	@Nonnull
	public String getCode() {
		return code;
	}

	@Nonnull
	public String getDetails() {
		return details;
	}

	@Nullable
	public String getType() {
		return type;
	}

	@Nullable
	public String[] getWarnings() {
		return warnings;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("status", status)
				.append("code", code)
				.append("details", details)
				.append("type", type)
				.append("warnings", String.join("\n", warnings))
				.toString();
	}
}
