package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.rules.Definition;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;
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
	public String paramUsage() {
		return "<keyword>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Verify that a keyword was given
		if (args.length == 0) {
			sendEmbed(e.getChannel(), "Please specify a keyword to look up.");
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

		// Show definition
		e.getChannel().sendMessage(new EmbedBuilder().setColor(Globals.EMBED_COLOR_PRIMARY).addField(definition.getKeyword(), definition.getExplanation(), false).build()).submit();
	}
}
