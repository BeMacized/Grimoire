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
	public String[] usages() {
		return new String[0];
	}

	@Override
	public String[] examples() {
		return new String[0];
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setAuthor("Mac's Grimoire: An MTG Discord Bot", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());

		eb.appendDescription("**Mac's Grimoire** is a Discord bot that brings many **Magic The Gathering** related tools straight into your discord server. ");
		eb.appendDescription("I can perform tasks like card-, price- or rule lookups, and more!");

		if (e.getGuild() != null)
			eb.appendDescription("\n\n:large_blue_diamond: The current command prefix for guild **'" + e.getGuild().getName() + "'** is set to `" + guildPreferences.getPrefix() + "`.");

		eb.appendDescription("\n\n**Type any of the following options to get more info:**");

		eb.addField("commands", "List all available commands", false);

		eb.addField("help <command>", "Get help with a specific command", false);

		eb.addField("inline references", "Explain inline references", false);

		StringBuilder sb = new StringBuilder();

		sb.append("\n:white_check_mark: [Invite me!](" + Grimoire.WEBSITE + "/invite)");
		sb.append("\n:earth_africa: [Website](" + Grimoire.WEBSITE + ")");
		sb.append("\n:wrench:  [Preference Dashboard](" + Grimoire.WEBSITE + "/dashboard)");

		eb.addField("", sb.toString(), true);
		sb.delete(0, sb.length());

		sb.append("\n:link: [Command Reference](" + Grimoire.WEBSITE + "/reference)");
		sb.append("\n:link: [About & FAQ Page](" + Grimoire.WEBSITE + "/about)");
		sb.append("\n:speech_balloon: [Support Server](" + Grimoire.WEBSITE + "/support)");

		eb.addField("", sb.toString(), true);
		sb.delete(0, sb.length());

		sb.append("\n:heart: [Donate](https://paypal.me/BeMacized)");
		sb.append("\n:gear: [Source Code](https://github.com/BeMacized/Grimoire)");

		eb.addField("", sb.toString(), true);
		sb.delete(0, sb.length());

		eb.addField(":globe_with_meridians: Server Count", "**" + e.getJDA().getGuilds().size() + "** Server" + (e.getJDA().getGuilds().size() > 1 ? "s" : ""), true);
		long users = e.getJDA().getGuilds().parallelStream().map(g -> g.getMembers().parallelStream()).flatMap(o -> o).map(m -> m.getUser().getId()).distinct().count();
		eb.addField(":busts_in_silhouette: Total Users", "**" + users + "** User" + (users > 1 ? "s" : ""), true);
		eb.addField(":gear: Discord Library", "JDA", true);

		if (e.getGuild() != null)
			sendEmbed(e.getChannel(), "I've sent you help in a private message!");

		try {
			e.getAuthor().openPrivateChannel().submit().get().sendMessage(eb.build()).submit();
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "Could not send help", ex);
		}
	}
}