package net.bemacized.grimoire.commands.all.avatar;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CreateAvatarCommand extends BaseCommand {
	@Override
	public String name() {
		return "createavatar";
	}

	@Override
	public String[] aliases() {
		return new String[]{
				"ca"
		};
	}

	@Override
	public String description() {
		return "Start the creation process for a new avatar card!";
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
		Grimoire.getInstance().getAvatarQuizManager().startAvatarQuiz(e);
	}
}
