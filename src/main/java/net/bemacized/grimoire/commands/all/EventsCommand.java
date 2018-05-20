package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.TimedValue;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventsCommand extends BaseCommand {
	@Override
	public String name() {
		return "events";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Tells you about upcoming MTG related events.";
	}

	@Override
	public String[] usages() {
		return new String[]{""};
	}

	@Override
	public String[] examples() {
		return new String[0];
	}

	private TimedValue<String> eventData = new TimedValue<String>(3600 * 6 * 1000) {
		@Override
		public String refresh() {
			// Fetch sidebar data
			String sidebarData = Grimoire.getInstance().getRedditAPI().subreddit("MagicTCG").about().getSidebar();
			// Unescape HTML
			sidebarData = StringEscapeUtils.unescapeHtml4(sidebarData);
			// Extract raw event data
			int startIndex = sidebarData.indexOf('\n', sidebarData.indexOf("---", sidebarData.indexOf("Upcoming Events")));
			int endIndex = sidebarData.indexOf("---", startIndex + 10);
			String eventData = sidebarData.substring(startIndex, endIndex);
			// Parse event data into events
			Map<String, List<String>> events = new LinkedHashMap<>();
			String category = null;
			List<String> categoryEvents = new ArrayList<>();
			for (String line : eventData.split("\\n")) {
				if (line.trim().isEmpty()) continue;
				else if (line.matches("^[*]{2}.+[*]{2}$")) {
					if (category != null)
						events.put(category, categoryEvents);
					categoryEvents = new ArrayList<>();
					category = line.substring(2, line.length() - 2);
				} else if (line.matches("^\\s{4}.+?[.]+.+?$")) {
					categoryEvents.add(line.trim());
				} else break;
			}
			// Construct parsed events into list
			StringBuilder msg = new StringBuilder(" \n");
			for (Map.Entry<String, List<String>> eventCategory : events.entrySet()) {
				// Append category
				msg.append("**" + eventCategory.getKey() + "**\n");
				msg.append("```\n");
				eventCategory.getValue().forEach(event -> msg.append(event + "\n"));
				msg.append("```\n");
			}
			// Return built message
			return msg.toString();
		}
	};

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Check if enabled
		if (Grimoire.getInstance().getRedditAPI() == null) {
			sendErrorEmbed(e.getChannel(), "This instance of Mac's Grimoire is not configured correctly to make use of Reddit's API. As this command depends on this functionality, it has been disabled.");
			return;
		}
		// Construct message
		String events = eventData.get();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setDescription(events);
		eb.setTitle("Upcoming Events");
		// Send event data
		e.getChannel().sendMessage(eb.build()).submit();
	}
}
