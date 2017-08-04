package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.MtgSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cards {

	private MTGJSON mtgjson;

	public Cards(MTGJSON mtgjson) {
		this.mtgjson = mtgjson;
	}

	public List<Card> getCards() {
		return mtgjson.getCardList();
	}

	public List<String> getAllSupertypes() {
		return getCards().parallelStream().map(c -> Arrays.stream(c.getSupertypes())).flatMap(o -> o).distinct().collect(Collectors.toList());
	}

	public List<String> getAllSubtypes() {
		return getCards().parallelStream().map(c -> Arrays.stream(c.getSubtypes())).flatMap(o -> o).distinct().collect(Collectors.toList());
	}

	public List<String> getAllTypes() {
		return getCards().parallelStream().map(c -> Arrays.stream(c.getTypes())).flatMap(o -> o).distinct().collect(Collectors.toList());
	}

	public List<String> getAllRarities() {
		return getCards().parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList());
	}

	public static class SearchQuery extends ArrayList<Card> {

		public SearchQuery() {
			this(Grimoire.getInstance().getCards().getCards());
		}

		SearchQuery(Collection<? extends Card> c) {
			super(c);
		}

		public SearchQuery hasName(String name) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList()));
		}

		public SearchQuery hasExactName(String name) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getName().equalsIgnoreCase(name)).collect(Collectors.toList()));
		}

		public SearchQuery hasSupertype(String supertype) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getSupertypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(supertype))).collect(Collectors.toList()));
		}

		public SearchQuery hasType(String type) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getTypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(type))).collect(Collectors.toList()));
		}

		public SearchQuery hasSubtype(String subtype) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getSubtypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(subtype))).collect(Collectors.toList()));
		}

		public SearchQuery inSet(MtgSet set) {
			if (set == null) return this;
			return new SearchQuery(this.parallelStream().filter(card -> card.getSet().equals(set)).collect(Collectors.toList()));
		}

		public SearchQuery isOfRarity(String rarity) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getRarity().equalsIgnoreCase(rarity)).collect(Collectors.toList()));
		}

		public SearchQuery distinctSets() {
			return new SearchQuery(this.parallelStream().filter(distinctByKey(card -> card.getSet().getCode())).collect(Collectors.toList()));
		}

		public SearchQuery distinctNames() {
			return new SearchQuery(this.parallelStream().filter(distinctByKey(Card::getName)).collect(Collectors.toList()));
		}

		public SearchQuery foreignAllowed() {
			return foreignAllowed(true);
		}

		public SearchQuery foreignAllowed(boolean value) {
			return value
					? new SearchQuery(this.parallelStream().map(card -> Stream.concat(Stream.of(card), Arrays.stream(card.getForeignVersions()))).flatMap(o -> o).collect(Collectors.toList()))
					: new SearchQuery(this.parallelStream().filter(card -> card.getLanguage().equalsIgnoreCase("English")).collect(Collectors.toList()));
		}

		public SearchQuery hasMultiverseId(int multiverseId) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getMultiverseid() == multiverseId).collect(Collectors.toList()));
		}

		private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
			Map<Object, Boolean> seen = new ConcurrentHashMap<>();
			return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
		}


	}
}
