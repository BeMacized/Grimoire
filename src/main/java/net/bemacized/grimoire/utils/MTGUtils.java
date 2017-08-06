package net.bemacized.grimoire.utils;

import java.awt.*;
import java.util.Arrays;
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
		if (colorCodes == null) return new Color(128, 128, 128);
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
				return new Color(255, 255, 255);
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
				return new Color(255, 200, 0);
			default:
				return new Color(128, 128, 128);
		}
	}
}
