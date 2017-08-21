package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.logging.Logger;

public abstract class ChatHandler extends MessageUtils {

	final Logger LOG;
	private final ChatHandler next;

	protected ChatHandler(ChatHandler next) {
		this.next = next;
		this.LOG = Logger.getLogger(this.getClass().getName());
	}

	public void handle(MessageReceivedEvent e) {
		GuildPreferences preferences = Grimoire.getInstance().getGuildPreferenceProvider().getPreferences(e.getGuild());
		this.handle(e, preferences, next);
	}

	protected abstract void handle(MessageReceivedEvent e, GuildPreferences guildPreferences, ChatHandler next);
}
