package net.bemacized.grimoire.utils;

import io.magicthegathering.javasdk.api.CardAPI;
import io.magicthegathering.javasdk.resource.Card;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import java.awt.*;
import java.util.*;
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

	public static Color colorIdentitiesToColor(String[] colorCodes) {
		switch (String.join("", Arrays.stream(colorCodes).sorted().collect(Collectors.toList()))) {
			case "B":
				return Color.BLACK;
			case "G":
				return new Color(0, 153, 0);
			case "R":
				return new Color(255, 51, 0);
			case "U":
				return new Color(0, 153, 255);
			case "W":
				return Color.WHITE;
			case "BG":
			case "BR":
			case "BU":
			case "BW":
			case "BGR":
			case "BGU":
			case "BGW":
			case "BRU":
			case "BRW":
			case "BUW":
			case "GRU":
			case "GRW":
			case "GUW":
			case "RUW":
			case "BGRU":
			case "GRUW":
			case "BGRUW":
				return Color.ORANGE;
			default:
				return Color.GRAY;
		}
	}

	public static String parseEmoji(Guild guild, String msg) {
		// Return message if we don't have the necessary info
		if (guild == null || msg == null || msg.isEmpty()) return msg;
		// Define emoji mapping
		Map<String, String> emojiMap = new HashMap<String, String>() {{
			put("W", "manaW");
			put("U", "manaU");
			put("B", "manaB");
			put("R", "manaR");
			put("G", "manaG");
			put("C", "manaC");
			put("W/U", "manaWU");
			put("U/B", "manaUB");
			put("B/R", "manaBR");
			put("R/G", "manaRG");
			put("G/W", "manaGW");
			put("W/B", "manaWB");
			put("U/R", "manaUR");
			put("B/G", "manaBG");
			put("R/W", "manaRW");
			put("G/U", "manaGU");
			put("2/W", "mana2W");
			put("2/U", "mana2U");
			put("2/B", "mana2B");
			put("2/R", "mana2R");
			put("2/G", "mana2G");
			put("WP", "manaWP");
			put("UP", "manaUP");
			put("BP", "manaBP");
			put("RP", "manaRP");
			put("GP", "manaGP");
			put("0", "manaZero");
			put("1", "manaOne");
			put("2", "manaTwo");
			put("3", "manaThree");
			put("4", "manaFour");
			put("5", "manaFive");
			put("6", "manaSix");
			put("7", "manaSeven");
			put("8", "manaEight");
			put("9", "manaNine");
			put("10", "manaTen");
			put("11", "manaEleven");
			put("12", "manaTwelve");
			put("13", "manaThirteen");
			put("14", "manaFourteen");
			put("15", "manaFifteen");
			put("16", "manaSixteen");
			put("20", "manaTwenty");
			put("T", "manaT");
			put("Q", "manaQ");
			put("S", "manaS");
			put("X", "manaX");
			put("E", "manaE");
		}};
		for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
			if (msg.contains("{" + entry.getKey() + "}")) {
				Emote emote = guild.getEmotesByName(entry.getValue(), true).parallelStream().findAny().orElse(null);
				if (emote != null) msg = msg.replaceAll("\\{" + entry.getKey() + "\\}", emote.getAsMention());
			}
		}
		return msg;
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
