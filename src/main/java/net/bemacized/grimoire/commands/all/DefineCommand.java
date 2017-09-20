package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.rules.ComprehensiveRule;
import net.bemacized.grimoire.data.models.rules.Definition;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	public String[] usages() {
		return new String[]{"<keyword>"};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"enchant",
				"vigilance",
				"prowess"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Verify that a keyword was given
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please specify a keyword to look up.");
			return;
		}

		// Verify that paragraph number exists
		Definition definition = Grimoire.getInstance().getComprehensiveRuleProvider().getDefinitions()
				.parallelStream()
				.filter(k -> k.getKeyword().equalsIgnoreCase(String.join(" ", args)))
				.findFirst()
				.orElse(null);

		if (definition == null) {
			//TODO: SUGGEST ALTERNATIVES USING SOME BETTER ALGORITHM THAN LEVENSHTEIN
			List<String> suggestions = Grimoire.getInstance().getComprehensiveRuleProvider().getDefinitions().parallelStream()
					.sorted(Comparator.comparingInt(o -> StringUtils.getLevenshteinDistance(o.getKeyword(), String.join(" ", args))))
					.map(Definition::getKeyword).collect(Collectors.toList());

			sendErrorEmbed(e.getChannel(), "Unknown keyword.\n\nDid you perhaps mean any of the following?\n" + String.join("\n", suggestions.subList(0, 3).parallelStream().map(k -> ":small_orange_diamond: " + k).collect(Collectors.toList())));
			return;
		}

		String explanation = definition.getExplanation();

		Pattern rulePattern = Pattern.compile("rule [0-9]([0-9]{2}([.][0-9]{1,3}([a-z]|[.])?|[.])?)?");
		Matcher ruleMatcher = rulePattern.matcher(explanation);
		List<String> rules = new ArrayList<>();

		while (ruleMatcher.find())
			rules.add(ruleMatcher.group().substring(5));

		explanation = formatText(explanation, e.getGuild());

		EmbedBuilder eb = new EmbedBuilder().setTitle(definition.getKeyword(), "https://blogs.magicjudges.org/rules/cr-glossary/").setColor(Globals.EMBED_COLOR_PRIMARY).setDescription(explanation);

		if (rules.size() == 1) {
			String rule = rules.get(0).endsWith(".") ? rules.get(0).substring(0, rules.get(0).length() - 1) : rules.get(0);
			List<ComprehensiveRule> crules = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream().filter(r -> r.getParagraphId().startsWith(rule) && r.getParagraphId().length() - 1 <= rule.length()).collect(Collectors.toList());
			crules.stream().filter(r -> r.getText().split("\\s+").length > 4 && !r.getText().equalsIgnoreCase(definition.getKeyword())).limit(4).sorted().forEachOrdered(r -> {
				String text = formatText(r.getText(), e.getGuild());
				String[] split = MessageUtils.splitMessage(text, 1024);
				for (int i = 0; i < split.length; i++)
					eb.addField(i == 0 ? "CR " + r.getParagraphId() : "", split[i], true);
			});
			if (crules.size() > 4)
				eb.addField("", "More rules available: `" + guildPreferences.getPrefix() + "cr " + rule + "`", false);
		}

		// Show definition
		e.getChannel().sendMessage(eb.build()).submit();
	}

	private String formatText(String str, Guild guild) {
		return Grimoire.getInstance().getEmojiParser().parseEmoji(String.join("\n", Arrays.stream(str.split("[\r\n]")).parallel().map(line -> {
			String text = String.join(" ", Arrays.stream(line.split("\\s+")).parallel().map(word ->
					(Grimoire.getInstance().getComprehensiveRuleProvider().getDefinitions().parallelStream().map(Definition::getKeyword).anyMatch(w -> w.equalsIgnoreCase(word)))
							? "__" + word + "__"
							: word
			).collect(Collectors.toList()));
			Pattern pattern = Pattern.compile("(rules? )?[0-9]{3}(([.][0-9]{1,3}([a-z]|[.])?)|[.])?");
			Matcher matcher = pattern.matcher(text);
			while (matcher.find())
				text = text.replaceAll(matcher.group(), "**" + matcher.group() + "**");
			return text;
		}).collect(Collectors.toList())), guild);
	}
}
