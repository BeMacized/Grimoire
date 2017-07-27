package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.utils.StringUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CompRulesCommand extends BaseCommand {

	@Override
	public String name() {
		return "comprules";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cr", "crules", "comprehensiverules"};
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
					"<@%s>, Please specify a paragraph number to display.\nYou can find the full comprehensive rules over here: <https://blogs.magicjudges.org/rules/cr/>",
					e.getAuthor().getId()
			)).submit();
			return;
		}

		Map<String, String> rulesToShow = new HashMap<>();

		// Check if we referenced a section
		if (args[0].matches("[0-9]{3}[.][0-9][.]?")) {
			// Get possible results
			final String paragraphNr = (args[0].endsWith(".")) ? args[0].substring(0, args[0].length() - 1) : args[0];
			Grimoire.getInstance().getComprehensiveRules().getRules()
					.keySet()
					.parallelStream()
					.filter(p -> p.substring(0, paragraphNr.length()).equals(paragraphNr))
					.forEach(p -> rulesToShow.put(p, Grimoire.getInstance().getComprehensiveRules().getRules().get(p)));
		}
		// If not try find exact match instead
		else {
			String rule = Grimoire.getInstance().getComprehensiveRules().getRules().get(args[0]);
			if (rule != null) rulesToShow.put(args[0], rule);
		}

		// If we couldn't find anything, tell the user
		if (rulesToShow.isEmpty()) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, Unknown paragraph.\n**Comprehensive Rules:** <https://blogs.magicjudges.org/rules/cr/>",
					e.getAuthor().getId()
			)).submit();
			return;
		}

		StringBuilder sb = new StringBuilder(String.format(
				"<@%s>",
				e.getAuthor().getId()
		));
		rulesToShow.entrySet().parallelStream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.map(entry -> {

					// Underline keywords
					String ruleText = String.join("\n", Arrays.stream(entry.getValue().split("[\r\n]")).parallel().map(line ->
							String.join(" ", Arrays.stream(line.split("\\s+")).parallel().map(word ->
									(Grimoire.getInstance().getComprehensiveRules().getDefinitions().keySet().parallelStream().anyMatch(w -> w.equalsIgnoreCase(word)))
											? "__" + word + "__"
											: word
							).collect(Collectors.toList()))
					).collect(Collectors.toList()));

					return String.format(
							"\n:small_orange_diamond: __**%s**__ %s\n",
							entry.getKey(),
							ruleText
					);
				})
				.forEachOrdered(sb::append);
		for (String s : StringUtils.splitMessage(sb.toString())) e.getChannel().sendMessage(s).submit();
	}
}
