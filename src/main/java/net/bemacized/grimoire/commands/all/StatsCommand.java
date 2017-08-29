package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.eventlogger.EventLogger;
import net.bemacized.grimoire.eventlogger.events.UserCommandInvocation;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.JSONObject;

import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;

public class StatsCommand extends BaseCommand {
	@Override
	public String name() {
		return "statistics";
	}

	@Override
	public String[] aliases() {
		return new String[]{"stats"};
	}

	@Override
	public String description() {
		return "View statistics for Grimoire";
	}

	@Override
	public String[] usages() {
		return new String[0];
	}

	@Override
	public String[] examples() {
		return new String[]{""};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		eb.setAuthor("Bot Statistics", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());
		if (guildPreferences.showRequestersName()) eb.setFooter("Requested by " + e.getAuthor().getName(), null);

		eb.addField(":globe_with_meridians: Server Count", "**" + e.getJDA().getGuilds().size() + "** Server" + (e.getJDA().getGuilds().size() > 1 ? "s" : ""), true);

		long users = e.getJDA().getGuilds().parallelStream().map(g -> g.getMembers().parallelStream()).flatMap(o -> o).map(m -> m.getUser().getId()).distinct().count();
		eb.addField(":busts_in_silhouette: Total Users", "**" + users + "** User" + (users > 1 ? "s" : ""), true);

		eb.addField(":gear: Discord Library", "JDA", true);

		Runtime runtime = Runtime.getRuntime();
		DecimalFormat memoryFormat = new DecimalFormat("#0.00");
		eb.addField(":fire: Memory Usage", String.format(
				"%s/%s GiB",
				memoryFormat.format((runtime.totalMemory() - runtime.freeMemory()) / 1024d / 1024d / 1024d),
				memoryFormat.format(runtime.totalMemory() / 1024d / 1024d / 1024d)
		), true);

		RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
		eb.addField(":clock: Uptime", DurationFormatUtils.formatDuration(runtimeBean.getUptime(), "HH:mm:ss"), true);

		OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		eb.addField(":dvd: System", osBean.getName() + " " + osBean.getVersion() + " (" + osBean.getArch() + ")", true);

		ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
		int cores = Runtime.getRuntime().availableProcessors();
		int threadCount = threadBean.getThreadCount();
		eb.addField(":wrench: CPU", String.format("**%s** Cores\n**%s** Running Threads", cores, threadCount), true);

		long totalCalls = Grimoire.getInstance().getDBManager().getJongo().getCollection(EventLogger.COLLECTION).count(String.format("{ _class: %s }", JSONObject.quote(UserCommandInvocation.class.getName())));
		long totalInlineCalls = Grimoire.getInstance().getDBManager().getJongo().getCollection(EventLogger.COLLECTION).count(String.format("{ _class: %s, inline: %s }", JSONObject.quote(UserCommandInvocation.class.getName()), "true"));
		double totalInlineCallPercentage = Math.ceil(Math.round(((double) totalInlineCalls) / ((double) totalCalls) * 10000d) / 100d);
		long guildCalls = Grimoire.getInstance().getDBManager().getJongo().getCollection(EventLogger.COLLECTION).count(String.format("{ _class: %s, guild.guildId: %s }", JSONObject.quote(UserCommandInvocation.class.getName()), e.getGuild().getId()));
		long guildInlineCalls = Grimoire.getInstance().getDBManager().getJongo().getCollection(EventLogger.COLLECTION).count(String.format("{ _class: %s, inline: %s, guild.guildId: %s }", JSONObject.quote(UserCommandInvocation.class.getName()), "true", e.getGuild().getId()));
		double guildInlineCallPercentage = Math.ceil(Math.round(((double) guildInlineCalls) / ((double) guildCalls) * 10000d) / 100d);
		eb.addField(":exclamation: Command Calls", String.format("Total: **%s** | Inline: **%s%%**\nGuild: **%s** | Inline: **%s%%**", totalCalls, totalInlineCallPercentage, guildCalls, guildInlineCallPercentage), true);

		eb.addField(":gear: Language", "Java", true);

		e.getChannel().sendMessage(eb.build()).submit();
	}
}
