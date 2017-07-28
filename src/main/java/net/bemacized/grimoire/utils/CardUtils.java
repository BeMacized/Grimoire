package net.bemacized.grimoire.utils;

import io.magicthegathering.javasdk.api.CardAPI;
import io.magicthegathering.javasdk.resource.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CardUtils {

	private final static int MAX_ALTERNATIVES = 20;

	public static Card getCard(String name) throws TooManyResultsException, MultipleResultsException, NoResultsException {
		return getCard(name, null);
	}

	public static Card getCard(String name, String setCode) throws TooManyResultsException, MultipleResultsException, NoResultsException {
		// Create search for the card
		CardSearchQuery query = new CardSearchQuery().setName(name);
		// Specify set if provided
		if (setCode != null) query = query.setSetCode(setCode);
		// Execute search
		List<Card> cards = query.exec();
		// Quit if there are no results
		if (cards.isEmpty()) throw new NoResultsException();
		// Find single match
		if (cards.parallelStream().filter(ExtraStreamUtils.distinctByKey(Card::getName)).count() == 1)
			return cards.get(0);
		// Find exact match (of the most recent set
		Card card = cards.parallelStream().filter(c -> c.getName().equalsIgnoreCase(name)).reduce((a, b) -> b).orElse(null);
		if (card != null) return card;
		// If none found return alternatives
		// Get the newest distinct results
		Collections.reverse(cards);
		cards = cards.parallelStream().filter(ExtraStreamUtils.distinctByKey(Card::getName)).collect(Collectors.toList());
		// Quit if too many results
		if (cards.size() > MAX_ALTERNATIVES) throw new TooManyResultsException(cards);
		else throw new MultipleResultsException(cards);
	}

	public static List<Card> getCards(String name, String setCode) throws TooManyResultsException, MultipleResultsException, NoResultsException {
		// Create search for the card
		CardSearchQuery query = new CardSearchQuery().setName(name);
		// Specify set if provided
		if (setCode != null) query = query.setSetCode(setCode);
		// Execute search
		List<Card> cards = query.exec();
		// Quit if there are no results
		if (cards.isEmpty()) throw new NoResultsException();
		// Find single match
		if (cards.parallelStream().filter(ExtraStreamUtils.distinctByKey(Card::getName)).count() == 1)
			return cards;
		// Find exact match (of the most recent set)
		List<Card> _cards = cards.parallelStream().filter(c -> c.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
		if (!_cards.isEmpty()) return _cards;
		// If none found return alternatives
		// Get the newest distinct results
		Collections.reverse(cards);
		cards = cards.parallelStream().filter(ExtraStreamUtils.distinctByKey(Card::getName)).collect(Collectors.toList());
		// Quit if too many results
		if (cards.size() > MAX_ALTERNATIVES) throw new TooManyResultsException(cards);
		else throw new MultipleResultsException(cards);
	}

	public static List<Card> getCards(String cardname) throws TooManyResultsException, MultipleResultsException, NoResultsException {
		return getCards(cardname, null);
	}


	public static class NoResultsException extends Exception {
	}

	public static class MultipleResultsException extends Exception {
		private List<Card> results;

		MultipleResultsException(List<Card> alternatives) {
			this.results = alternatives;
		}

		public List<Card> getResults() {
			return results;
		}
	}

	public static class TooManyResultsException extends Exception {

		private List<Card> results;

		TooManyResultsException(List<Card> alternatives) {
			this.results = alternatives;
		}

		public List<Card> getResults() {
			return results;
		}
	}


	public static class CardSearchQuery {

		private List<String> filters;

		public CardSearchQuery() {
			filters = new ArrayList<>();
		}

		public CardSearchQuery setName(String name) {
			filters.add("name=" + name);
			return this;
		}

		public CardSearchQuery setSetCode(String setCode) {
			filters.add("set=" + setCode);
			return this;
		}

		public CardSearchQuery setExactName(String name) {
			filters.add("name=\"" + name + "\"");
			return this;
		}

		public CardSearchQuery setSuperType(String supertype) {
			filters.add("supertypes=" + supertype);
			return this;
		}

		public CardSearchQuery setType(String type) {
			filters.add("types=" + type);
			return this;
		}

		public CardSearchQuery setSubType(String subtype) {
			filters.add("subtypes=" + subtype);
			return this;
		}

		public List<Card> exec() {
			return CardAPI.getAllCards(filters);
		}
	}

}
