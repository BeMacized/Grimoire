package net.bemacized.grimoire.data.models.standard;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;

public class StandardSet {

	private String name;
	private String block;
	private String code;
	private String enter_date;
	private String exit_date;
	private String rough_exit_date;

	public String getName() {
		return name;
	}

	public String getBlock() {
		return block != null ? block : name;
	}

	public String getCode() {
		return code;
	}

	@Nullable
	public DateTime getEnterDate() {
		return enter_date != null ? ISODateTimeFormat.dateTime().parseDateTime(enter_date) : null;
	}

	@Nullable
	public DateTime getExitDate() {
		return exit_date != null ? ISODateTimeFormat.dateTime().parseDateTime(exit_date) : null;
	}

	public String getRoughExitDate() {
		return rough_exit_date;
	}
}
