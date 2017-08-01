package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.ComprehensiveRule;
import net.bemacized.grimoire.model.models.Definition;
import net.bemacized.grimoire.utils.StringUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
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
			e.getChannel().sendMessageFormat("<@%s>, Please specify a paragraph number to display.\nYou can find the full comprehensive rules over here: <https://blogs.magicjudges.org/rules/cr/>", e.getAuthor().getId()).submit();
			return;
		}

		// Find rules
		List<ComprehensiveRule> rulesToShow = Grimoire.getInstance().getComprehensiveRules().getRules().parallelStream()
				.filter(rule -> (args[0].endsWith(".") && rule.getParagraphId().equalsIgnoreCase(args[0])) ||
						rule.getParagraphId().equalsIgnoreCase(args[0]) ||
						rule.getParagraphId().equalsIgnoreCase(args[0] + ".") ||
						rule.getParagraphId().matches(Pattern.quote(args[0]) + "[a-z]")
				).collect(Collectors.toList());

		// If we couldn't find anything, tell the user
		if (rulesToShow.isEmpty()) {
			e.getChannel().sendMessageFormat("<@%s>, Unknown paragraph.\n**Comprehensive Rules:** <https://blogs.magicjudges.org/rules/cr/>", e.getAuthor().getId()).submit();
			return;
		}

		StringBuilder sb = new StringBuilder(String.format(
				"<@%s>",
				e.getAuthor().getId()
		));
		rulesToShow.parallelStream()
				.sorted(Comparator.comparing(ComprehensiveRule::getParagraphId))
				.map(rule -> {

					// Underline keywords
					String ruleText = String.join("\n", Arrays.stream(rule.getText().split("[\r\n]")).parallel().map(line ->
							String.join(" ", Arrays.stream(line.split("\\s+")).parallel().map(word ->
									(Grimoire.getInstance().getDefinitions().getDefinitions().parallelStream().map(Definition::getKeyword).anyMatch(w -> w.equalsIgnoreCase(word)))
											? "__" + word + "__"
											: word
							).collect(Collectors.toList()))
					).collect(Collectors.toList()));

					return String.format("\n:small_orange_diamond: __**%s**__ %s\n", rule.getParagraphId(), ruleText);
				})
				.forEachOrdered(sb::append);
		for (String s : StringUtils.splitMessage(sb.toString())) e.getChannel().sendMessage(s).submit();
	}
}
