package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ReloadPreferencesCommand extends BaseCommand {
	@Override
	public String name() {
		return "reloadpreferences";
	}

	@Override
	public String[] aliases() {
		return new String[]{"reloadprefs"};
	}

	@Override
	public String description() {
		return "Reload preferences immediately for your guild";
	}

	@Override
	public String[] usages() {
		return new String[]{""};
	}

	@Override
	public String[] examples() {
		return new String[0];
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Disable outside of guilds
		if (e.getGuild() == null) return;
		if (!e.getMember().hasPermission(Permission.MANAGE_SERVER)) {
			sendErrorEmbed(e.getChannel(), "You need to have the Manage Server permission in order to reload preferences.");
			return;
		}
		Grimoire.getInstance().getGuildPreferenceProvider().getPreferences(e.getGuild(), true);
		sendEmbed(e.getChannel(), "Your guild settings have been reloaded!");
	}
}
