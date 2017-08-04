package net.bemacized.grimoire.utils;

import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MTGUtils {

	public static String colourIdToName(String id) {
		switch (id.toUpperCase()) {
			case "R":
				return "Red";
			case "B":
				return "Black";
			case "U":
				return "Blue";
			case "W":
				return "White";
			case "G":
				return "Green";
			default:
				return "Colourless";
		}
	}

	public static String colourNameToId(String name) {
		if (name == null || name.isEmpty()) return "";
		switch (name.toLowerCase()) {
			case "red":
				return "R";
			case "black":
				return "B";
			case "blue":
				return "U";
			case "white":
				return "W";
			case "green":
				return "G";
			case "colourless":
			case "colorless":
				return "";
			default:
				return name;
		}
	}

	public static String parsePowerAndToughness(String power, String toughness) {
		if (power == null || toughness == null || power.isEmpty() || toughness.isEmpty()) return "";
		return parsePowerOrToughness(power) + "/" + parsePowerOrToughness(toughness);
	}

	public static String parsePowerAndToughness(String powerAndToughness) {
		String[] split = powerAndToughness.split("/");
		return parsePowerAndToughness(split[0], split[1]);
	}

	private static String parsePowerOrToughness(String value) {
		if (value == null) return null;
		switch (value) {
			case "*":
				return "\\*";
			default:
				return value;
		}
	}

	public static Color colorIdentitiesToColor(String[] colorCodes) {
		// Return null if none provided
		if (colorCodes == null) return Color.GRAY;
		// First try parsing away full colour names
		colorCodes = Arrays.stream(colorCodes).map(MTGUtils::colourNameToId).collect(Collectors.toList()).toArray(new String[0]);
		// Return the colour identity
		switch (String.join("", Arrays.stream(colorCodes).sorted().collect(Collectors.toList()))) {
			case "B":
				return new Color(1, 1, 1);
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
			case "GR":
			case "GU":
			case "GW":
			case "RU":
			case "RW":
			case "UW":
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
		// Return message if the guild is null
		if (guild == null) return msg;
		// Return an empty string if we don't have the necessary info
		if (msg == null || msg.isEmpty()) return "";
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
}
