package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class HelpCommand extends BaseCommand {

	@Override
	public String name() {
		return "help";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Shows the help text, containing all of the command references.";
	}

	@Override
	public String paramUsage() {
		return "";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setAuthor("Mac's Grimoire: An MTG Discord Bot", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());

		eb.appendDescription("**Mac's Grimoire** is a Discord bot that brings many **Magic The Gathering** related tools straight into your discord server. ");
		eb.appendDescription("I can perform tasks like card-, price- or rule lookups, and more!");

		eb.appendDescription("\n\n:earth_africa: [**Website**](" + Grimoire.WEBSITE + ")");
		eb.appendDescription("\n:gear: [**Preference Dashboard**](" + Grimoire.WEBSITE + "/dashboard)");
		eb.appendDescription("\n:link: [**Command Reference**](" + Grimoire.WEBSITE + "/reference)");

		eb.appendDescription("\n\n:white_check_mark: [**Invite me!**](" + Grimoire.WEBSITE + "/invite)");
		eb.appendDescription("\n:speech_balloon: [**Support Server**](" + Grimoire.WEBSITE + "/support)");

		if (e.getGuild() != null) eb.appendDescription("\n\n:large_blue_diamond: The current command prefix for guild **'" + e.getGuild().getName() + "'** is set to `" + guildPreferences.getPrefix() + "`.");

		eb.appendDescription("\n\n**Type any of the following options to get more info:**");

		eb.addField("commands", "List all available commands", false);

		eb.addField("help <command>", "Get help with a specific command", false);

		eb.addField("inline references", "Explain inline references", false);

		eb.addField("info", "Get more information about the bot", false);

		try {
			e.getAuthor().openPrivateChannel().submit().get().sendMessage(eb.build()).submit();
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "Could not send help", ex);
		}
	}
}