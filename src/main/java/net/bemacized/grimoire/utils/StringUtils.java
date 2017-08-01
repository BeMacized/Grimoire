package net.bemacized.grimoire.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {

	public static String[] splitMessage(String text, int maxlength, boolean codeblock) {
		if (text.length() <= maxlength) return new String[]{((codeblock) ? "```\n" + text + "\n```" : text)};
		final String[] split = text.split("[\r\n]");
		if (split.length == 1) throw new IllegalArgumentException("Message cannot be split properly.");
		List<String> messages = new ArrayList<>();
		StringBuilder message = new StringBuilder();
		for (String s : split) {
			if (message.length() + s.length() > maxlength) {
				messages.add(message.toString());
				message = new StringBuilder();
			}
			message.append(s).append("\n");
		}
		messages.add(message.toString());
		return messages
				.parallelStream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(s -> (codeblock) ? "```\n" + s + "\n```" : s)
				.collect(Collectors.toList())
				.toArray(new String[0]);
	}

	public static String[] splitMessage(String text, int maxlength) {
		return splitMessage(text, maxlength, false);
	}

	public static String[] splitMessage(String text, boolean codeblock) {
		return splitMessage(text, 1950, codeblock);
	}

	public static String[] splitMessage(String text) {
		return splitMessage(text, 1950, false);
	}
}
