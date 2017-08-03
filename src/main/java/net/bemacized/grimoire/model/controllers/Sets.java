package net.bemacized.grimoire.model.controllers;


import net.bemacized.grimoire.model.models.MtgSet;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		MtgSet exactCodeMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getCode())).findAny().orElse(
				sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getGathererCode())).findAny().orElse(
						sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getOldCode())).findAny().orElse(null)
				));
		if (exactCodeMatch != null) return exactCodeMatch;
		MtgSet exactNameMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getName())).findAny().orElse(null);
		if (exactNameMatch != null) return exactNameMatch;
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
				MtgSet exactNameMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getName())).findAny().orElse(null);
				if (exactNameMatch != null) return exactNameMatch;
				throw new MultipleResultsException(sets);
			}
		}
	}

	public List<MtgSet> getByNameOrCode(String nameOrCode) {
		return Stream.concat(
				this.getSets().parallelStream()
						.filter(set -> nameOrCode.equalsIgnoreCase(set.getCode())
								|| nameOrCode.equalsIgnoreCase(set.getGathererCode())
								|| nameOrCode.equalsIgnoreCase(set.getOldCode())),
				this.getSets().parallelStream()
						.filter(set -> set.getName().toLowerCase().contains(nameOrCode.toLowerCase()))
		).collect(Collectors.toList());
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
