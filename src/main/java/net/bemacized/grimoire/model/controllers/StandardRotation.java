package net.bemacized.grimoire.model.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Dependency;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StandardRotation {

	private static Logger LOG = Logger.getLogger(Tokens.class.getName());

	private List<StandardSet> sets;

	public StandardRotation() {
		sets = new ArrayList<>();
	}

	public void load() {
		LOG.info("Loading standard sets...");

		// Get raw json
		Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("STANDARDSETS");
		String rawJson = d.getString();
		if (rawJson == null) {
			LOG.severe("Could not load standard sets!");
			return;
		}
		d.release(); // Release dependency from memory after loading

		// Parse json
		Gson gson = new Gson();
		sets = StreamSupport.stream(new JsonParser().parse(rawJson).getAsJsonArray().spliterator(), false).map(s -> gson.fromJson(s.getAsJsonObject(), StandardSet.class)).collect(Collectors.toList());

		// Sort sets
		sets.sort(Comparator.comparing(StandardSet::getEnterDate));

		LOG.info("Loaded " + sets.size() + " standard sets");
	}

	public List<StandardSet> getSets() {
		return new ArrayList<>(sets);
	}

	public static class StandardSet {

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
			return block;
		}

		public String getCode() {
			return code;
		}

		public DateTime getEnterDate() {
			return enter_date != null ? ISODateTimeFormat.dateTime().parseDateTime(enter_date) : null;
		}

		public DateTime getExitDate() {
			return exit_date != null ? ISODateTimeFormat.dateTime().parseDateTime(exit_date) : null;
		}

		public String getRoughExitDate() {
			return rough_exit_date;
		}
	}
}
