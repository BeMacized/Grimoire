package net.bemacized.grimoire.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {

	public static String[] splitMessage(String text) {
		final int MAX_LENGTH = 1950;
		if (text.length() <= MAX_LENGTH) return new String[]{text};
		final String[] split = text.split("[\r\n]");
		if (split.length == 1) throw new IllegalArgumentException("Message cannot be split properly.");
		List<String> messages = new ArrayList<>();
		StringBuilder message = new StringBuilder();
		for (String s : split) {
			if (message.length() + s.length() > MAX_LENGTH) {
				messages.add(message.toString());
				message = new StringBuilder();
			}
			message.append(s).append("\n");
		}
		messages.add(message.toString());

		return messages.parallelStream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()).toArray(new String[0]);
	}
}
