package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.parsers.TournamentRules;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public class TournamentRulesCommand extends BaseCommand {
	@Override
	public String name() {
		return "tournamentrules";
	}

	@Override
	public String[] aliases() {
		return new String[]{"magictournamentrules", "mtr", "tr"};
	}

	@Override
	public String description() {
		return "Retrieve a paragraph from the tournament rules";
	}

	@Override
	public String paramUsage() {
		return "<paragraph nr>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Verify that paragraph number was given
		if (args.length == 0) {
			e.getChannel().sendMessageFormat(
					"<@%s>, Please specify a chapter (ex. `1` or `1.1`). The following sections are available:\n%s",
					e.getAuthor().getId(),
					String.join("", Grimoire.getInstance().getTournamentRules().getRules().parallelStream().map(section -> "\n:small_orange_diamond: **" + section.getParagraphNr() + "** " + section.getTitle()).collect(Collectors.toList()))
			).submit();
			return;
		}

		// Handle subsections
		if (args[0].matches("[0-9]+[.][0-9]+")) {
			// Find section
			TournamentRules.Section section = Grimoire.getInstance().getTournamentRules().getRules().parallelStream().filter(s -> args[0].startsWith(s.getParagraphNr())).findFirst().orElse(null);
			if (section == null) {
				e.getChannel().sendMessageFormat(
						"<@%s>, The section you specified is unknown. Please choose one of the following:\n%s",
						e.getAuthor().getId(),
						String.join("", Grimoire.getInstance().getTournamentRules().getRules().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getParagraphNr() + "** " + s.getTitle()).collect(Collectors.toList()))
				).submit();
				return;
			}
			// Find subsection
			TournamentRules.SubSection subsection = section.getSubsections().parallelStream().filter(s -> s.getParagraphNr().equalsIgnoreCase(args[0])).findFirst().orElse(null);
			if (subsection == null) {
				e.getChannel().sendMessageFormat(
						"<@%s>, The subsection you specified is unknown. \nThe following subsections are available in **%s %s**:\n%s",
						e.getAuthor().getId(),
						section.getParagraphNr(),
						section.getTitle(),
						String.join("", section.getSubsections().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getParagraphNr() + "** " + s.getTitle()).collect(Collectors.toList()))).submit();
				return;
			}
			// Show text
			e.getChannel().sendMessageFormat(
					"<@%s>\n**Magic Tournament Rules** - _%s %s_ - **%s %s**\n\n%s",
					e.getAuthor().getId(),
					section.getParagraphNr(),
					section.getTitle(),
					subsection.getParagraphNr(),
					subsection.getTitle(),
					subsection.getContent()
			).submit();
			// Handle sections
		} else {
			// Find section
			TournamentRules.Section section = Grimoire.getInstance().getTournamentRules().getRules().parallelStream().filter(s -> s.getParagraphNr().startsWith(args[0])).findFirst().orElse(null);
			// Check if section was found
			if (section == null) {
				e.getChannel().sendMessageFormat(
						"<@%s>, The section you specified is unknown. Please choose one of the following:\n",
						e.getAuthor().getId(),
						String.join("", Grimoire.getInstance().getTournamentRules().getRules().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getParagraphNr() + "** " + s.getTitle()).collect(Collectors.toList()))
				).submit();
				return;
			}
			e.getChannel().sendMessageFormat(
					"<@%s>\n**Magic Tournament Rules - %s %s**\nThe following subsections are available:\n%s",
					e.getAuthor().getId(),
					section.getParagraphNr(),
					section.getTitle(),
					String.join("", section.getSubsections().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getParagraphNr() + "** " + s.getTitle()).collect(Collectors.toList()))
			).submit();
		}
	}
}
