package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.providers.StandardRotationProvider;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.format.DateTimeFormat;

import java.util.Comparator;
import java.util.stream.Collectors;

public class StandardCommand extends BaseCommand {

	@Override
	public String name() {
		return "standard";
	}

	@Override
	public String[] aliases() {
		return new String[]{"whatsinstandard", "wis"};
	}

	@Override
	public String description() {
		return "See what sets are currently in standard rotation";
	}

	@Override
	public String paramUsage() {
		return "";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setTitle("What's in Standard?", "http://whatsinstandard.com");
		// Group by blocks & add them to the embed
		Grimoire.getInstance().getStandardRotationProvider().getSets()
				.parallelStream()
				.collect(Collectors.groupingBy(StandardRotationProvider.StandardSet::getBlock))
				.values()
				.parallelStream()
				.sorted(Comparator.comparing(b -> b.get(0).getEnterDate()))
				.forEachOrdered(b -> {
					final StringBuilder sb = new StringBuilder();
					StandardRotationProvider.StandardSet refSet = b.parallelStream().filter(s -> s.getEnterDate() != null && s.getEnterDate().isAfterNow()).findFirst().orElse(b.get(0));
					// Add release label
					if (refSet.getEnterDate() != null && refSet.getEnterDate().isAfterNow()) {
						sb.append(String.format(":tada: __%s releases %s__", refSet.getName(), DateTimeFormat.forPattern("MMMM dd, yyyy").print(refSet.getEnterDate())));
					}
					if (refSet.getExitDate() != null) {
						sb.append(String.format("\n:alarm_clock: *Until %s*", DateTimeFormat.forPattern("MMMM dd, yyyy").print(refSet.getExitDate())));
					} else {
						sb.append(String.format("\n:clock7: *Until %s*", refSet.getRoughExitDate()));
					}
					sb.append("\n");
					// Add sets
					b.forEach(s -> sb.append(String.format(":small_orange_diamond: %s (%s)\n", s.getName(), s.getCode())));
					// Add field
					eb.addField(refSet.getBlock(), sb.toString(), false);
				});
		// Send message
		e.getChannel().sendMessage(eb.build()).submit();
	}
}
