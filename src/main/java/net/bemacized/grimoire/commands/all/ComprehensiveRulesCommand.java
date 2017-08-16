package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.ComprehensiveRule;
import net.bemacized.grimoire.data.models.Definition;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComprehensiveRulesCommand extends BaseCommand {

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
			sendEmbedFormat(e.getChannel(), "Please specify a section or paragraph from the comprehensive rules (ex. `1` or `1.1`). The following sections are available:\n%s", String.join("\n",
					Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
							.filter(r -> r.getParagraphId().matches("[0-9][.]"))
							.map(section -> ":small_orange_diamond: **" + section.getParagraphId() + "** " + section.getText())
							.collect(Collectors.toList())
			));
			return;
		}

		// Determine mode
		int mode;
		if (args[0].matches("[0-9][.]?")) mode = 0;
		else if (args[0].matches("[0-9]{3}[.]?")) mode = 1;
		else if (args[0].matches("[0-9]{3}[.][0-9]+[.]?")) mode = 2;
		else if (args[0].matches("[0-9]{3}[.][0-9]+[a-z][.]?")) mode = 3;
		else {
			sendEmbedFormat(e.getChannel(), ":anger: Invalid section or paragraph provided.\n\nPlease specify a section or paragraph from the comprehensive rules (ex. `1` or `1.1`).\nThe following sections are available:\n%s", String.join("\n",
					Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
							.filter(r -> r.getParagraphId().matches("[0-9][.]"))
							.map(section -> ":small_orange_diamond: **" + section.getParagraphId() + "** " + section.getText())
							.collect(Collectors.toList())
			));
			return;
		}

		// Fix up paragraph id for parsing
		final String reqId = (args[0].endsWith(".")) ? args[0].substring(0, args[0].length() - 1) : args[0];

		// Verify section
		final ComprehensiveRule section = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
				.filter(c -> c.getParagraphId().matches(reqId.substring(0, 1) + "[.]"))
				.findFirst().orElse(null);
		if (section == null) {
			sendEmbedFormat(e.getChannel(), ":anger: The section specified is not a valid option.\n\nThe following sections are available:\n%s", String.join("\n",
					Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
							.filter(r -> r.getParagraphId().matches("[0-9][.]"))
							.map(s -> ":small_orange_diamond: **" + s.getParagraphId() + "** " + s.getText())
							.collect(Collectors.toList())
			));
			return;
		}

		String errorline = null;

		// Verify subsection
		final ComprehensiveRule subsection;
		if (mode > 0) {
			subsection = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(c -> c.getParagraphId().matches(reqId.substring(0, 3) + "[.]"))
					.findFirst().orElse(null);
			if (subsection == null) {
				errorline = "The subsection specified is not a valid option.";
				mode = 0;
			}
		} else subsection = null;

		// Verify paragraph
		final ComprehensiveRule paragraph;
		if (mode > 1) {
			int finalMode = mode;
			paragraph = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(c -> c.getParagraphId().matches(Pattern.quote(finalMode == 3 ? reqId.substring(0, reqId.length() - 1) : reqId) + "[.]"))
					.findFirst().orElse(null);
			if (paragraph == null) {
				errorline = "The paragraph specified is not a valid option.";
				mode = 1;
			}
		} else paragraph = null;

		final ComprehensiveRule subparagraph;
		if (mode > 2) {
			subparagraph = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(c -> c.getParagraphId().equalsIgnoreCase(reqId))
					.findFirst().orElse(null);
			if (subparagraph == null) {
				errorline = "The subparagraph specified is not a valid option.";
				//mode = 2;
			}
		} else subparagraph = null;

		// Build embed data
		String title;
		String subtitle = null;
		final StringBuilder description = new StringBuilder();

		// Subparagraph output
		if (subparagraph != null) {
			//Construct title
			title = String.format("%s %s", section.getParagraphId(), section.getText());
			subtitle = "**" + subsection.getParagraphId() + " " + subsection.getText() + "**";
			description.append(":small_orange_diamond: __**" + subparagraph.getParagraphId() + "**__ " + underlineKeywordsAndParseEmoji(subparagraph.getText(), e.getGuild()));
			long peers = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(r -> r.getParagraphId().matches(Pattern.quote(paragraph.getParagraphId().substring(0, paragraph.getParagraphId().length() - 1)) + "[a-z]"))
					.count() - 1;
			if (peers > 0) description.append("\n\n**More info:** `!cr " + paragraph.getParagraphId() + "`");
		}
		// Paragraph output
		else if (paragraph != null) {
			title = String.format("%s %s", section.getParagraphId(), section.getText());
			subtitle = "**" + subsection.getParagraphId() + " " + subsection.getText() + "**";
			List<ComprehensiveRule> rules = Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(r -> r.getParagraphId().matches(Pattern.quote(paragraph.getParagraphId().substring(0, paragraph.getParagraphId().length() - 1)) + "[a-z.]"))
					.sorted().collect(Collectors.toList());
			rules.forEach(r -> description.append("\n\n:small_orange_diamond: __**" + r.getParagraphId() + "**__ " + underlineKeywordsAndParseEmoji(r.getText(), e.getGuild())));
		}
		// Subsection output
		else if (subsection != null) {
			title = String.format("%s %s", section.getParagraphId(), section.getText());
			subtitle = "**" + subsection.getParagraphId() + " " + subsection.getText() + "**";
			description.append(String.format("Within **'%s %s'**, the following paragraphs are available:\n", subsection.getParagraphId(), subsection.getText()));

			Supplier<Stream<ComprehensiveRule>> paragraphStream = () -> Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(c -> c.getParagraphId().matches(Pattern.quote(subsection.getParagraphId()) + "[0-9]+[.]")).sorted();
			if (paragraphStream.get().mapToInt(p -> p.getText().split("\\s+").length >= 7 ? 2 : 1).sum() <= 40) {
				paragraphStream.get().forEachOrdered(p -> {
					int textlength = p.getText().split("\\s+").length;
					if (textlength < 7) {
						description.append("\n:small_orange_diamond: **" + p.getParagraphId() + " " + underlineKeywordsAndParseEmoji(p.getText(), e.getGuild()) + "**");
					} else if (textlength < 20) {
						description.append("\n:small_orange_diamond: **" + p.getParagraphId() + "** " + underlineKeywordsAndParseEmoji(p.getText(), e.getGuild()));
						if (Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream().filter(c -> c.getParagraphId().matches(Pattern.quote(p.getParagraphId().substring(0, p.getParagraphId().length() - 1)) + "[a-z]")).count() > 0)
							description.append(" **Read More:** `!cr " + p.getParagraphId().substring(0, p.getParagraphId().length() - 1) + "`");
					} else {
						description.append("\n:small_orange_diamond: **" + p.getParagraphId() + "** ");
						description.append(underlineKeywordsAndParseEmoji(String.join(" ", Arrays.copyOfRange(p.getText().split("\\s+"), 0, 19)), e.getGuild()));
						description.append("..... **Read More:** `!cr " + p.getParagraphId().substring(0, p.getParagraphId().length() - 1) + "`");
					}
				});
			} else {
				paragraphStream.get().forEachOrdered(p -> {
					if (p.getText().split("\\s+").length >= 7) {
						description.append("\n:small_orange_diamond: **" + p.getParagraphId() + "** " + underlineKeywordsAndParseEmoji(p.getText(), e.getGuild()));
						if (Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream().filter(c -> c.getParagraphId().matches(Pattern.quote(p.getParagraphId().substring(0, p.getParagraphId().length() - 1)) + "[a-z]")).count() > 0)
							description.append(" **Read More:** `!cr " + p.getParagraphId().substring(0, p.getParagraphId().length() - 1) + "`");
					}
				});
				if (subsection.getParagraphId().equals("702."))
					description.append("\n\n**In order to get more information about a keyword, please look up the specific rule reference using `!define <keyword>`.**");
			}
		}
		// Section output
		else {
			title = String.format("%s %s", section.getParagraphId(), section.getText());
			description.append(String.format("Within **'%s %s'**, the following subsections are available:\n", section.getParagraphId(), section.getText()));
			Grimoire.getInstance().getComprehensiveRuleProvider().getRules().parallelStream()
					.filter(r -> r.getParagraphId().matches(section.getParagraphId().substring(0, 1) + "[0-9]{2}[.]"))
					.sorted().forEachOrdered(r -> description.append("\n:small_orange_diamond: **" + r.getParagraphId() + "** " + r.getText()));
		}

		// Add error line
		if (errorline != null) description.insert(0, ":anger: " + errorline + "\n\n");
			// Add subtitle
		else if (subtitle != null) description.insert(0, subtitle + "\n\n");

		// Fix triple linebreaks
		description.replace(0, description.length(), description.toString().replaceAll("[\n]{3,}", "\n\n"));

		// Send rules
		String[] splits = MessageUtils.splitMessage(description.toString());
		for (int i = 0; i < splits.length; i++) {
			EmbedBuilder eb = new EmbedBuilder().setDescription(splits[i]);
			if (i == 0)
				eb.setAuthor("Comprehensive Rules", null, null).setTitle(errorline == null ? title : null);
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);
			e.getChannel().sendMessage(eb.build()).submit();
		}
	}

	private String underlineKeywordsAndParseEmoji(String str, Guild guild) {
		return Grimoire.getInstance().getEmojiParser().parseEmoji(String.join("\n", Arrays.stream(str.split("[\r\n]")).parallel().map(line ->
				String.join(" ", Arrays.stream(line.split("\\s+")).parallel().map(word ->
						(Grimoire.getInstance().getComprehensiveRuleProvider().getDefinitions().parallelStream().map(Definition::getKeyword).anyMatch(w -> w.equalsIgnoreCase(word)))
								? "__" + word + "__"
								: (word.matches("[0-9]{3}([.]([0-9]+[.a-z]?)?)?") ? "`" + word + "`" : word)
				).collect(Collectors.toList()))
		).collect(Collectors.toList())), guild);
	}
}
