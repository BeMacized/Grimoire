package net.bemacized.grimoire.controllers.quiz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class AvatarQuizResults {

	private Map<Color, Integer> devotion = new HashMap<Color, Integer>() {{
		for (Color color : Color.values()) put(color, 0);
	}};

	private List<AbilityLine> abilities = new ArrayList<>();
	private int power = 2;
	private int toughness = 2;
	private int cmc = 4;

	private String userName = "";
	private String avatarUrl = "";

	public AvatarQuizResults(String userName, String avatarUrl) {
		this.userName = userName;
		this.avatarUrl = avatarUrl;
	}

	public AvatarQuizResults addAbility(Ability ability) {
		abilities.add(ability.getRandomLine());
		return this;
	}

	public AvatarQuizResults modDevotion(Color color, int mod) {
		devotion.put(color, devotion.get(color) + mod);
		return this;
	}

	public AvatarQuizResults modPower(int mod) {
		power += mod;
		return this;
	}

	public AvatarQuizResults modToughness(int mod) {
		toughness += mod;
		return this;
	}

	public AvatarQuizResults modCmc(int mod) {
		cmc += mod;
		return this;
	}

	Card getCard() {
		Random rand = new Random();

		// Construct oracle text
		String oracle = "";
		oracle += String.join(", ", abilities.parallelStream()
				.filter(a -> a instanceof Keyword)
				.sorted(Comparator.comparing(AbilityLine::getLine))
				.map(AbilityLine::getLine)
				.collect(Collectors.toList())).trim();
		if (!oracle.isEmpty()) oracle += "\n";
		oracle += String.join(", ", abilities.parallelStream()
				.filter(a -> a instanceof StaticAbility)
				.sorted(Comparator.comparing(AbilityLine::getLine))
				.map(AbilityLine::getLine)
				.collect(Collectors.toList())).trim();
		if (!oracle.isEmpty()) oracle += "\n";
		oracle += String.join(", ", abilities.parallelStream()
				.filter(a -> a instanceof ActivatedAbility)
				.sorted(Comparator.comparing(AbilityLine::getLine))
				.map(AbilityLine::getLine)
				.collect(Collectors.toList())).trim();

		// Determine color identity
		List<Map.Entry<Color, Integer>> rankedIdentities = devotion.entrySet().parallelStream()
				.sorted((x, y) -> y.getValue().compareTo(x.getValue()))
				.collect(Collectors.toList());
		List<Color> identity = new ArrayList<>();
		for (int i = 0; i < rankedIdentities.size(); i++)
			if (i == 0 || rankedIdentities.get(i - 1).getValue() - rankedIdentities.get(i).getValue() <= 1)
				identity.add(rankedIdentities.get(i).getKey());
			else break;

		// Determine manacost
		String cost = "";
		int remaining = cmc;
		for (Color color : identity) {
			if (remaining == 0) break;
			for (int i = 0; i < rand.nextInt(3) + 1; i++) {
				remaining--;
				cost = "{" + color.getSymbol() + "}" + cost;
				if (remaining == 0 || remaining <= identity.size()) break;
			}
		}
		if (remaining > 0 || cost.isEmpty()) cost = "{" + remaining + "}" + cost;

		// Calculate base pt mod
		int basePTmod = rand.nextInt(3) - 1;

		// Return card
		return new Card(
				Math.max(power + basePTmod, 0),
				Math.max(toughness - basePTmod, 0),
				userName,
				avatarUrl,
				oracle,
				identity.toArray(new Color[identity.size()]),
				cmc,
				cost
		);
	}

	class Card {

		private int power;
		private int toughness;
		private String name;
		private String artUrl;
		private String oracle;
		private Color[] colorIdentity;
		private int cmc;
		private String manacost;

		public Card(int power, int toughness, String name, String artUrl, String oracle, Color[] colorIdentity, int cmc, String manacost) {
			this.power = power;
			this.toughness = toughness;
			this.name = name;
			this.artUrl = artUrl;
			this.oracle = oracle;
			this.colorIdentity = colorIdentity;
			this.cmc = cmc;
			this.manacost = manacost;
		}

		public int getPower() {
			return power;
		}

		public int getToughness() {
			return toughness;
		}

		public String getName() {
			return name;
		}

		public String getArtUrl() {
			return artUrl;
		}

		public String getOracle() {
			return oracle;
		}

		public Color[] getColorIdentity() {
			return colorIdentity;
		}

		public int getCmc() {
			return cmc;
		}

		public String getManacost() {
			return manacost;
		}

		@Override
		public String toString() {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			return gson.toJson(this);
		}
	}

	enum Color {
		WHITE("W"),
		RED("R"),
		BLUE("U"),
		BLACK("B"),
		GREEN("G");

		private String symbol;

		Color(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	enum Ability {
		PINGER(new AbilityLine[]{
				new Keyword("Ping"),
				new ActivatedAbility("{T}: Deal 1 damage to target opponent"),
				new ActivatedAbility("{1}{R}, {T}: Deal 3 damage to target creature"),
				new ActivatedAbility("Discard a land card, {T}: Deal 2 damage to each opponent"),
				new StaticAbility("At the end of your turn, each opponent takes 1 damage")
		}),
		EVADE(new AbilityLine[]{
				new Keyword("Evade")
		}),
		COMBAT(new AbilityLine[]{
				new Keyword("Combat")
		}), INDESCRUCTIBLE(new AbilityLine[]{
				new Keyword("Indestructible")
		});

		private AbilityLine[] lines;

		Ability(AbilityLine[] lines) {
			this.lines = lines;
		}

		public AbilityLine[] getLines() {
			return lines;
		}

		public AbilityLine getRandomLine() {
			return lines[new Random().nextInt(lines.length)];
		}
	}

	private static class Keyword extends AbilityLine {

		public Keyword(String line) {
			super(line);
		}
	}

	private static class StaticAbility extends AbilityLine {

		public StaticAbility(String line) {
			super(line);
		}
	}

	private static class ActivatedAbility extends AbilityLine {

		public ActivatedAbility(String line) {
			super(line);
		}
	}

	private static class AbilityLine {
		private String line;

		public AbilityLine(String line) {
			this.line = line;
		}

		public String getLine() {
			return line;
		}
	}
}
