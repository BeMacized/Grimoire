package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
		sendEmbed(e.getChannel(), ":worried: The help command is currently being rewritten. Please try again later!");
	}
}