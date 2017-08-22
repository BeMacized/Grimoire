package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.all.HelpCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class HelpHandler extends ChatHandler{

	public HelpHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, GuildPreferences guildPreferences, ChatHandler next) {
		// Only enable in PM
		if (e.getGuild() != null) {
			next.handle(e);
			return;
		}

		final String msg = e.getMessage().getContent();

		// Resend regular help
		if (msg.equalsIgnoreCase("help") || msg.equalsIgnoreCase("g!help") || msg.equalsIgnoreCase("!help") || msg.equalsIgnoreCase("_help") || msg.equalsIgnoreCase("@" + Grimoire.BOT_NAME + " help")) {
			new HelpCommand().exec(null, e, guildPreferences);
			return;
		}

		// List commands
		if (msg.equalsIgnoreCase("commands")) {
			sendEmbed(e.getChannel(), "This section is under construction, please try again later!");
			return;
		}

		// Explain inline references
		if (msg.equalsIgnoreCase("inline references")) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setAuthor("Mac's Grimoire: Inline References", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);

			eb.appendDescription("Inline references allow you to trigger commands straight from a regular message.\n");
			eb.appendDescription("Two commands currently have support for these references:\n`card` and `pricing`.\n");

			eb.appendDescription("\n\nTo trigger the `card` command, you can use either\n`<<card>>` or `[[card]]`");
			eb.appendDescription("\nYou can too specify the set by adding it after a spacer (`|`) like so:\n`[[card|set]]`");
			eb.appendDescription("\nBoth set names and set codes are usable!");

			eb.appendDescription("\n\nTo trigger the `pricing` command, simply use the same, but prefix the card name with a dollar sign: `<<$card>>`");

			eb.appendDescription("\n\nAn example of these in use would be as follows:");

			eb.appendDescription("\n\n_\"I like <<Mighty Leap>> a lot, but the old <<Mighty Leap|OGW>> art is much prettier. What does <<$Mighty Leap|ORI>> cost?\"_");

			e.getChannel().sendMessage(eb.build()).submit();
			return;
		}

		// Show info
		if (msg.equalsIgnoreCase("info")) {
			sendEmbed(e.getChannel(), "This section is under construction, please try again later!");
			return;
		}

		// Show command help
		if (msg.matches("help [a-zA-Z]+")) {
			sendEmbed(e.getChannel(), "This section is under construction, please try again later!");
			return;
		}

		// Send help on first attempt
		try {
			if (e.getChannel().getHistory().retrievePast(1).submit().get().isEmpty()) {
				new HelpCommand().exec(null, e, guildPreferences);
				return;
			}
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "could not retrieve history", ex);
			return;
		}

		// Send error
		sendErrorEmbed(e.getChannel(), "I did not understand what you mean. Please only choose one of the options given:\n`help`, `commands`, `inline referneces`, `info`, or `help <command>`.");

	}
}
