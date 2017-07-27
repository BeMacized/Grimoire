package net.bemacized.grimoire.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CompRulesCommand extends CompRulesSuperCommand {

	@Override
	public String name() {
		return "comprules";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cr", "crules"};
	}

	@Override
	public String description() {
		return "Retrieve a paragraph from the comprehensive rules";
	}

	@Override
	public String paramUsage() {
		return "<paragraph nr>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Verify that paragraph number was given
		if (args.length == 0) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, Please specify a paragraph number to display. You can find the full comprehensive rules over here: <https://blogs.magicjudges.org/rules/cr/>",
					e.getAuthor().getId()
			)).submit();
			return;
		}

		// Verify that paragraph number exists
		if (!this.rules.containsKey(args[0])) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, Unknown paragraph. **Comprehensive Rules:** <https://blogs.magicjudges.org/rules/cr/>",
					e.getAuthor().getId()
			)).submit();
			return;
		}

		e.getChannel().sendMessage(String.format(
				"<@%s>, __**%s**__ %s",
				e.getAuthor().getId(),
				args[0],
				this.rules.get(args[0])
		)).submit();
	}
}
