package net.bemacized.grimoire.commands.all.avatar;

import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AvatarCommand extends BaseCommand{
	@Override
	public String name() {
		return "avatar";
	}

	@Override
	public String[] aliases() {
		return new String[]{"a"};
	}

	@Override
	public String description() {
		return "View your, or someone else's current avatar card.";
	}

	@Override
	public String[] usages() {
		return new String[]{
			"[User Mention]"
		};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"",
				"@BeMacized#8951"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {

	}
}
