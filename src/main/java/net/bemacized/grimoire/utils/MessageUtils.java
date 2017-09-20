package net.bemacized.grimoire.utils;

import net.bemacized.grimoire.Globals;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageUtils {

	public static List<MessageEmbed> simpleEmbed(String text) {
		return Arrays.stream(splitMessage(text)).map(t -> new EmbedBuilder().setColor(Globals.EMBED_COLOR_PRIMARY).setDescription(t).build()).collect(Collectors.toList());
	}

	public static List<MessageEmbed> errorEmbed(String text) {
		return Arrays.stream(splitMessage(":anger: " + text)).map(t -> new EmbedBuilder().setColor(Color.RED).setDescription(t).build()).collect(Collectors.toList());
	}

	public static List<MessageEmbed> simpleEmbedFormat(String template, Object... inserts) {
		return simpleEmbed(String.format(template, inserts));
	}

	public static List<MessageEmbed> errorEmbedFormat(String template, Object... inserts) {
		return errorEmbed(String.format(template, inserts));
	}


	public static List<RequestFuture<Message>> sendEmbed(MessageChannel c, String text) {
		return simpleEmbed(text).stream().map(t -> c.sendMessage(t).submit()).collect(Collectors.toList());
	}

	public static List<RequestFuture<Message>> sendEmbedFormat(MessageChannel c, String template, Object... inserts) {
		return sendEmbed(c, String.format(template, inserts));
	}

	public static void sendEmbed(LoadMessage lm, String text) {
		List<MessageEmbed> embeds = simpleEmbed(text);
		if (embeds.size() == 1) lm.complete(embeds.get(0));
		else {
			lm.complete();
			embeds.forEach(t -> lm.getChannel().sendMessage(t).submit());
		}
	}

	public static void sendErrorEmbedFormat(LoadMessage lm, String template, Object... inserts) {
		sendErrorEmbed(lm, String.format(template, inserts));
	}

	public static List<RequestFuture<Message>> sendErrorEmbed(MessageChannel c, String text) {
		return errorEmbed(text).stream().map(t -> c.sendMessage(t).submit()).collect(Collectors.toList());
	}

	public static List<RequestFuture<Message>> sendErrorEmbedFormat(MessageChannel c, String template, Object... inserts) {
		return sendErrorEmbed(c, String.format(template, inserts));
	}

	public static void sendErrorEmbed(LoadMessage lm, String text) {
		List<MessageEmbed> embeds = errorEmbed(text);
		if (embeds.size() == 1) lm.complete(embeds.get(0));
		else {
			lm.complete();
			embeds.forEach(t -> lm.getChannel().sendMessage(t).submit());
		}
	}

	public static void sendEmbedFormat(LoadMessage lm, String template, Object... inserts) {
		sendEmbed(lm, String.format(template, inserts));
	}


	public static String[] splitMessage(String text, int maxlength, boolean codeblock) {
		int margin = codeblock ? 9 : 1;
		String[] lines = text.split("\\r?\\n");
		List<String> source = new ArrayList<>();
		// Split by line
		if (Arrays.stream(lines).parallel().noneMatch(l -> l.length() > (maxlength - margin))) {
			source.addAll(Arrays.asList(lines).parallelStream().map(l -> l + "\n").collect(Collectors.toList()));
		}
		// Split by word
		else {
			for (String line : lines) {
				source.addAll(Arrays.asList(line.split("\\s")));
				source.add("\n");
			}
		}
		List<String> splits = new ArrayList<>();
		StringBuilder splitBuilder = new StringBuilder();
		while (!source.isEmpty()) {
			if (splitBuilder.length() + (source.get(0)).length() + 1 > maxlength - margin) {
				splits.add(splitBuilder.toString());
				splitBuilder = new StringBuilder();
			}
			splitBuilder.append(source.get(0));
			if (!source.get(0).endsWith("\n")) splitBuilder.append(" ");
			source.remove(0);
		}
		if (splitBuilder.length() > 0) splits.add(splitBuilder.toString());
		return splits.parallelStream().map(s -> (codeblock ? "```\n" + s + "\n```" : s)).collect(Collectors.toList()).toArray(new String[0]);
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
