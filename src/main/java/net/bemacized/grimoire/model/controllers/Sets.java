package net.bemacized.grimoire.model.controllers;


import net.bemacized.grimoire.model.models.MtgSet;

import java.util.List;
import java.util.stream.Collectors;

public class Sets {

	private MTGJSON mtgjson;

	public Sets(MTGJSON mtgjson) {
		this.mtgjson = mtgjson;
	}

	public List<MtgSet> getSets() {
		return mtgjson.getSetList();
	}

	public MtgSet forceSingleByNameOrCode(String nameOrCode) {
		List<MtgSet> sets = getByNameOrCode(nameOrCode);
		if (sets.isEmpty()) return null;
		return sets.get(0);
	}

	public MtgSet getSingleByNameOrCode(String nameOrCode) throws MultipleResultsException {
		List<MtgSet> sets = getByNameOrCode(nameOrCode);
		switch (sets.size()) {
			case 0:
				return null;
			case 1:
				return sets.get(0);
			default: {
				MtgSet exactCodeMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getCode())).findAny().orElse(
						sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getGathererCode())).findAny().orElse(
								sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getOldCode())).findAny().orElse(null)
						));
				if (exactCodeMatch != null) return exactCodeMatch;
				throw new MultipleResultsException(sets);
			}
		}
	}

	public List<MtgSet> getByNameOrCode(String nameOrCode) {
		return this.getSets().parallelStream()
				.filter(set -> nameOrCode.equalsIgnoreCase(set.getCode())
						|| nameOrCode.equalsIgnoreCase(set.getGathererCode())
						|| nameOrCode.equalsIgnoreCase(set.getOldCode())
						|| set.getName().toLowerCase().contains(nameOrCode.toLowerCase()))
				.collect(Collectors.toList());
	}

	public class MultipleResultsException extends Exception {
		private List<MtgSet> sets;

		MultipleResultsException(List<MtgSet> sets) {
			this.sets = sets;
		}

		public List<MtgSet> getSets() {
			return sets;
		}
	}
}
