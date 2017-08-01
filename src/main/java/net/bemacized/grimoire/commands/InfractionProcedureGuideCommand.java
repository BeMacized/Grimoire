package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.InfractionProcedureGuideSection;
import net.bemacized.grimoire.utils.StringUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

public class InfractionProcedureGuideCommand extends BaseCommand {
	@Override
	public String name() {
		return "infractionprocedure";
	}

	@Override
	public String[] aliases() {
		return new String[]{"ipg", "ipguide"};
	}

	@Override
	public String description() {
		return "Retrieve a paragraph from the tournament rules.";
	}

	@Override
	public String paramUsage() {
		return "<paragraph> [topic]";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Verify that paragraph number was given
		if (args.length == 0) {
			for (String s : StringUtils.splitMessage(String.format(
					"<@%s>, Please specify a section (ex. `1` or `1.1`). The following sections are available:\n%s",
					e.getAuthor().getId(),
					String.join("", Grimoire.getInstance().getInfractionProcedureGuide().getSections().parallelStream().map(section -> "\n:small_orange_diamond: **" + section.getSectionId() + ".** " + section.getTitle()).collect(Collectors.toList()))
			)))
				e.getChannel().sendMessage(s).submit();
			return;
		}

		// Sections
		if (args[0].matches("[0-9]+([.][0-9]*)?")) {
			final boolean isSubsection = !args[0].matches("[0-9]+[.]?");
			if (!isSubsection && args[0].endsWith(".")) args[0] = args[0].replaceAll("[.]", "");

			// Find section
			InfractionProcedureGuideSection section = Grimoire.getInstance().getInfractionProcedureGuide().getSections().parallelStream().filter(s -> args[0].startsWith(s.getSectionId())).findFirst().orElse(null);

			// Verify that section exists
			if (section == null) {
				for (String s : StringUtils.splitMessage(String.format(
						"<@%s>, The specified section could not be found. The following sections are available:\n%s",
						e.getAuthor().getId(),
						String.join("", Grimoire.getInstance().getInfractionProcedureGuide().getSections().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getSectionId() + ".** " + s.getTitle()).collect(Collectors.toList()))
				)))
					e.getChannel().sendMessage(s).submit();
				return;
			}

			// Find subsection if needed
			InfractionProcedureGuideSection subsection = null;
			if (isSubsection) {
				subsection = section.getSubSections().parallelStream().filter(s -> s.getSectionId().equalsIgnoreCase(args[0])).findFirst().orElse(null);
				if (subsection == null) {
					for (String s : StringUtils.splitMessage(String.format(
							"<@%s>, The specified subsection could not be found. Within **'%s %s'**, the following sections are available:\n%s",
							e.getAuthor().getId(),
							section.getSectionId(),
							section.getTitle(),
							String.join("", section.getSubSections().parallelStream().map(s -> "\n:small_orange_diamond: **" + s.getSectionId() + "** " + s.getTitle()).collect(Collectors.toList()))
					)))
						e.getChannel().sendMessage(s).submit();
					return;
				}
			}

			// Find header section if needed
			InfractionProcedureGuideSection topic = null;
			if (subsection != null && args.length > 1 && !subsection.getSubSections().isEmpty()) {
				String topicname = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				topic = subsection.getSubSections().parallelStream().filter(s -> s.getTitle().equalsIgnoreCase(topicname)).findFirst().orElse(null);
				if (topic == null) {
					for (String s : StringUtils.splitMessage(String.format(
							"<@%s>, The specified topic could not be found. Within **'%s %s'**, the following topics are available:\n%s",
							e.getAuthor().getId(),
							subsection.getSectionId(),
							subsection.getTitle(),
							String.join("", subsection.getSubSections().parallelStream().map(s -> "\n:small_orange_diamond: " + s.getTitle()).collect(Collectors.toList()))
					)))
						e.getChannel().sendMessage(s).submit();
					return;
				}
			}

			if (topic != null) {
				for (String s : StringUtils.splitMessage(String.format(
						"<@%s>\n**Infraction Procedure Guide** - _%s. %s_ - **%s %s**\n\n**%s**\n\n%s",
						e.getAuthor().getId(),
						section.getSectionId(),
						section.getTitle(),
						subsection.getSectionId(),
						subsection.getTitle(),
						topic.getTitle(),
						topic.getContent()
				)))
					e.getChannel().sendMessage(s).submit();
			} else if (subsection != null) {
				for (String s : StringUtils.splitMessage(String.format(
						"<@%s>\n**Infraction Procedure Guide** - _%s. %s_ - **%s %s**%s%s",
						e.getAuthor().getId(),
						section.getSectionId(),
						section.getTitle(),
						subsection.getSectionId(),
						subsection.getTitle(),
						(subsection.getContent() != null && !subsection.getContent().isEmpty()) ? "\n\n" + subsection.getContent() : "",
						(subsection.getSubSections().isEmpty()) ? "" : String.join("", subsection.getSubSections().parallelStream().map(t -> String.format(
								"\n\n**%s**\n%s",
								t.getTitle(),
								t.getContent()
						)).collect(Collectors.toList()))
				)))
					e.getChannel().sendMessage(s).submit();
			} else {
				for (String s : StringUtils.splitMessage(String.format(
						"<@%s>\n**Infraction Procedure Guide** - **%s. %s**%s%s",
						e.getAuthor().getId(),
						section.getSectionId(),
						section.getTitle(),
						(section.getContent() != null && !section.getContent().isEmpty()) ? "\n\n" + section.getContent() : "",
						(section.getSubSections().isEmpty()) ? "" : "\n\n**The following subsections are available:**" + String.join("", section.getSubSections().parallelStream().map(t -> "\n:small_orange_diamond: **" + t.getSectionId() + "** " + t.getTitle()).collect(Collectors.toList()))
				)))
					e.getChannel().sendMessage(s).submit();
			}
		} else {
			e.getChannel().sendMessageFormat(
					"<@%s>, The specified section could not be found. The following sections are available:\n%s",
					e.getAuthor().getId(),
					String.join("", Grimoire.getInstance().getInfractionProcedureGuide().getSections().parallelStream().map(section -> "\n:small_orange_diamond: **" + section.getSectionId() + "** " + section.getTitle()).collect(Collectors.toList()))
			).submit();
			return;
		}
	}
}
