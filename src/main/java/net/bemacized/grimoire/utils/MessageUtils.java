package net.bemacized.grimoire.utils;

import net.bemacized.grimoire.Globals;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageUtils {

	public static List<MessageEmbed> simpleEmbed(String text) {
		return Arrays.stream(splitMessage(text)).map(t -> new EmbedBuilder().setColor(Globals.EMBED_COLOR_PRIMARY).setDescription(t).build()).collect(Collectors.toList());
	}

	public static List<MessageEmbed> simpleEmbedFormat(String template, Object... inserts) {
		return simpleEmbed(String.format(template, inserts));
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

	public static void sendEmbedFormat(LoadMessage lm, String template, Object... inserts) {
		sendEmbed(lm, String.format(template, inserts));
	}

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
