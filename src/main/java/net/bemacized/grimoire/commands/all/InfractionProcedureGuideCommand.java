package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.InfractionProcedureGuideSection;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
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
			e.getChannel().sendMessage(new EmbedBuilder()
					.setColor(Globals.EMBED_COLOR_PRIMARY)
					.setAuthor("Infraction Procedure Guide", null, null)
					.setDescription("The following sections are available:\n" + String.join("\n", Grimoire.getInstance().getInfractionProcedureGuideProvider().getSections().parallelStream()
							.map(s -> ":small_orange_diamond: **" + s.getSectionId() + ".** " + s.getTitle())
							.collect(Collectors.toList()))).build()).submit();
			return;
		}

		// Sections
		if (args[0].matches("[0-9]+([.][0-9]*)?")) {
			final boolean isSubsection = !args[0].matches("[0-9]+[.]?");
			if (!isSubsection && args[0].endsWith(".")) args[0] = args[0].replaceAll("[.]", "");

			// Find section
			InfractionProcedureGuideSection section = Grimoire.getInstance().getInfractionProcedureGuideProvider().getSections().parallelStream().filter(s -> args[0].startsWith(s.getSectionId())).findFirst().orElse(null);

			// Verify that section exists
			if (section == null) {
				e.getChannel().sendMessage(new EmbedBuilder()
						.setColor(Globals.EMBED_COLOR_PRIMARY)
						.setAuthor("Infraction Procedure Guide", null, null)
						.setDescription(":anger: The specified section could not be found.\n\nThe following sections are available:\n" + String.join("\n", Grimoire.getInstance().getInfractionProcedureGuideProvider().getSections().parallelStream()
								.map(s -> ":small_orange_diamond: **" + s.getSectionId() + ".** " + s.getTitle())
								.collect(Collectors.toList()))).build()).submit();
				return;
			}

			// Find subsection if needed
			InfractionProcedureGuideSection subsection = null;
			if (isSubsection) {
				subsection = section.getSubSections().parallelStream().filter(s -> s.getSectionId().equalsIgnoreCase(args[0])).findFirst().orElse(null);
				if (subsection == null) {
					e.getChannel().sendMessage(new EmbedBuilder()
							.setColor(Globals.EMBED_COLOR_PRIMARY)
							.setAuthor("Infraction Procedure Guide", null, null)
							.setDescription(String.format(":anger: The specified subsection could not be found.\n\nWithin **'%s %s'**, the following sections are available:\n%s",
									section.getSectionId(), section.getTitle(),
									String.join("\n", section.getSubSections().parallelStream()
											.map(s -> ":small_orange_diamond: **" + s.getSectionId() + "** " + s.getTitle())
											.collect(Collectors.toList())))
							).build()).submit();
					return;
				}
			}

			// Find header section if needed
			InfractionProcedureGuideSection topic = null;
			if (subsection != null && args.length > 1 && !subsection.getSubSections().isEmpty()) {
				String topicname = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				topic = subsection.getSubSections().parallelStream().filter(s -> s.getTitle().equalsIgnoreCase(topicname)).findFirst().orElse(null);
				if (topic == null) {
					e.getChannel().sendMessage(new EmbedBuilder()
							.setColor(Globals.EMBED_COLOR_PRIMARY)
							.setAuthor("Infraction Procedure Guide", null, null)
							.setDescription(String.format(":anger: The specified topic could not be found.\n\nWithin **'%s %s'**, the following topics are available:\n%s", subsection.getSectionId(), subsection.getTitle(), String.join("\n",
									subsection.getSubSections().parallelStream()
											.map(s -> ":small_orange_diamond: " + s.getTitle())
											.collect(Collectors.toList())))).build()).submit();
					return;
				}
			}

			if (topic != null) {
				String[] splits = MessageUtils.splitMessage(String.format("**%s**\n%s", topic.getTitle(), topic.getContent()));
				for (int i = 0; i < splits.length; i++) {
					EmbedBuilder eb = new EmbedBuilder().setDescription(splits[i]).setColor(Globals.EMBED_COLOR_PRIMARY);
					if (i == 0)
						eb.setAuthor("Infraction Procedure Guide", null, null).setTitle(String.format("%s %s", subsection.getSectionId(), subsection.getTitle()));
					e.getChannel().sendMessage(eb.build()).submit();
				}
			} else if (subsection != null) {
				String content = (subsection.getContent() != null && !subsection.getContent().isEmpty()) ? "\n\n" + subsection.getContent() + "\n" : "";
				String subsections = (subsection.getSubSections().isEmpty()) ? "" : String.join("\n\n", subsection.getSubSections().parallelStream().map(t -> String.format("**%s**\n%s", t.getTitle(), t.getContent())).collect(Collectors.toList()));
				String[] splits = MessageUtils.splitMessage(content + subsections);
				for (int i = 0; i < splits.length; i++) {
					EmbedBuilder eb = new EmbedBuilder().setDescription(splits[i]).setColor(Globals.EMBED_COLOR_PRIMARY);
					if (i == 0)
						eb.setAuthor("Infraction Procedure Guide", null, null).setTitle(String.format("%s %s", subsection.getSectionId(), subsection.getTitle()));
					e.getChannel().sendMessage(eb.build()).submit();
				}
			} else {
				String content = (section.getContent() != null && !section.getContent().isEmpty()) ? "\n\n" + section.getContent() + "\n" : "";
				String subsections = (section.getSubSections().isEmpty()) ? "" : "\n\n**The following subsections are available:**\n" + String.join("\n", section.getSubSections().parallelStream().map(t -> ":small_orange_diamond: **" + t.getSectionId() + "** " + t.getTitle()).collect(Collectors.toList()));
				String[] splits = MessageUtils.splitMessage(content + subsections);
				for (int i = 0; i < splits.length; i++) {
					EmbedBuilder eb = new EmbedBuilder().setDescription(splits[i]).setColor(Globals.EMBED_COLOR_PRIMARY);
					if (i == 0)
						eb.setAuthor("Infraction Procedure Guide", null, null).setTitle(String.format("%s %s", section.getSectionId(), section.getTitle()));
					e.getChannel().sendMessage(eb.build()).submit();
				}
			}
		} else {
			sendEmbedFormat(e.getChannel(), ":anger: The specified section could not be found.\n\nThe following sections are available:\n%s", String.join("\n",
					Grimoire.getInstance().getInfractionProcedureGuideProvider().getSections().parallelStream()
							.map(section -> ":small_orange_diamond: **" + section.getSectionId() + "** " + section.getTitle())
							.collect(Collectors.toList())
			));
		}
	}
}
