package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.commands.all.CardCommand;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineCardHandler extends ChatHandler {

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;

	public InlineCardHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("(<<|\\[\\[)[^$<|>]+?([|][^<|>]+?)?(>>|]])");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) {
			String[] data = m.group().substring(2, m.group().length() - 2).split("[|]");
			String cardname = data[0].trim();
			String setname = (data.length > 1) ? data[1].trim() : null;
			new Thread(() -> new CardCommand().exec((cardname + (setname == null ? "" : " | " + setname)).split("\\s+"), e)).start();
		}

		next.handle(e);
	}
}
