package net.bemacized.grimoire.utils;

import com.sun.istack.internal.NotNull;
import io.magicthegathering.javasdk.api.SetAPI;
import io.magicthegathering.javasdk.resource.MtgSet;

import java.util.ArrayList;
import java.util.List;

public class SetUtils {

	private final static int MAX_ALTERNATIVES = 20;

	public static MtgSet getSet(@NotNull String nameOrCode) throws TooManyResultsException, MultipleResultsException, NoResultsException {
		// Check set code
		MtgSet set = SetAPI.getSet(nameOrCode.toUpperCase());
		if (set != null) return set;
		// If not found, check set name instead
		List<MtgSet> sets = new SetSearchQuery().setName(nameOrCode.toLowerCase()).exec();
		// Check for single name match
		if (sets.size() == 1) return sets.get(0);
		// If none, check for exact name match
		set = sets.stream().filter(s -> s.getName().equalsIgnoreCase(nameOrCode)).findAny().orElse(null);
		if (set != null) return set;
		// If none found, list options to user and quit
		if (sets.size() > MAX_ALTERNATIVES) throw new TooManyResultsException(sets);
		else if (sets.isEmpty()) throw new NoResultsException();
		else throw new MultipleResultsException(sets);
	}

	public static class NoResultsException extends Exception {
	}

	public static class MultipleResultsException extends Exception {
		private List<MtgSet> results;

		MultipleResultsException(List<MtgSet> alternatives) {
			this.results = alternatives;
		}

		public List<MtgSet> getResults() {
			return results;
		}
	}

	public static class TooManyResultsException extends Exception {

		private List<MtgSet> results;

		TooManyResultsException(List<MtgSet> alternatives) {
			this.results = alternatives;
		}

		public List<MtgSet> getResults() {
			return results;
		}
	}

	public static class SetSearchQuery {

		private List<String> filters;

		public SetSearchQuery() {
			filters = new ArrayList<>();
		}

		public SetSearchQuery setName(String name) {
			filters.add("name=" + name);
			return this;
		}


		public List<MtgSet> exec() {
			return SetAPI.getAllSets(filters);
		}
	}

}
