package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.logging.Logger;

public abstract class BaseCommand extends MessageUtils {

	final Logger LOG;

	protected BaseCommand() {
		LOG = Logger.getLogger(this.getClass().getName());
	}

	public abstract String name();

	public abstract String[] aliases();

	public abstract String description();

	public abstract String paramUsage();

	public abstract void exec(String[] args, MessageReceivedEvent e, GuildPreferences guildPreferences);

}
