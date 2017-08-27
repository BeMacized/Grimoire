package net.bemacized.grimoire.data.providers;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StandardRotationProvider {

	private static final String SOURCE = "http://whatsinstandard.com/api/4/sets.json";
	private static final String CHARSET = "UTF-8";
	private static final long TIMEOUT = 60 * 5 * 1000;

	private final Logger LOG;
	private List<StandardSet> sets;
	private long lastRetrieval;

	public StandardRotationProvider() {
		LOG = Logger.getLogger(this.getClass().getName());
		sets = new ArrayList<>();
	}

	public void load() {

		sets.clear();
		LOG.info("Retrieving standard sets...");

		String rawJson;
		try {
			rawJson = IOUtils.toString(new URL(SOURCE), CHARSET);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not retrieve standard sets!", e);
			return;
		}

		// Parse json
		Gson gson = new Gson();
		sets = StreamSupport.stream(new JsonParser().parse(rawJson).getAsJsonArray().spliterator(), false).map(s -> gson.fromJson(s.getAsJsonObject(), StandardSet.class)).collect(Collectors.toList());

		// Sort sets
		sets.sort(Comparator.comparing(StandardSet::getEnterDate));

		this.lastRetrieval = System.currentTimeMillis();

		LOG.info("Retrieved " + sets.size() + " standard sets");
	}

	public List<StandardSet> getSets() {
		if (System.currentTimeMillis() - lastRetrieval >= TIMEOUT) load();
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
}
