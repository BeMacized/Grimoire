package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class DefineCommand extends BaseCommand {
	@Override
	public String name() {
		return "define";
	}

	@Override
	public String[] aliases() {
		return new String[]{"definition", "keyword"};
	}

	@Override
	public String description() {
		return "Looks up the definition for the specified keyword";
	}

	@Override
	public String paramUsage() {
		return "<keyword>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Verify that a keyword was given
		if (args.length == 0) {
			e.getChannel().sendMessageFormat(
					"<@%s>, Please specify a keyword to look up",
					e.getAuthor().getId()
			).submit();
			return;
		}

		// Verify that paragraph number exists
		String keyword = Grimoire.getInstance().getComprehensiveRules().getDefinitions()
				.keySet()
				.parallelStream()
				.filter(k -> k.equalsIgnoreCase(String.join(" ", args)))
				.findFirst()
				.orElse(null);
		if (keyword == null) {
			e.getChannel().sendMessageFormat(
					"<@%s>, Unknown keyword :(",
					e.getAuthor().getId()
			).submit();
			return;
		}

		// Show definition
		e.getChannel().sendMessageFormat(
				"<@%s>, __**%s:**__\n%s",
				e.getAuthor().getId(),
				keyword,
				Grimoire.getInstance().getComprehensiveRules().getDefinitions().get(keyword)
		).submit();
	}
}
