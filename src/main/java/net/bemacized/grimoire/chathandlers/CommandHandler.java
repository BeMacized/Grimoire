package net.bemacized.grimoire.chathandlers;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.commands.all.*;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.eventlogger.EventLogger;
import net.bemacized.grimoire.eventlogger.events.LogEntry;
import net.bemacized.grimoire.eventlogger.events.UserCommandInvocation;
import net.bemacized.grimoire.eventlogger.events.UserRateLimited;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler extends ChatHandler {

	private List<BaseCommand> commands;
	private RequestRateLimiter rateLimiter;
	private RequestRateLimiter rateLimitLimiter;

	public CommandHandler(ChatHandler next) {
		super(next);
		commands = Stream.of(
				new HelpCommand(),
				new ArtCommand(),
				new CardCommand(),
				new OracleCommand(),
				new PrintsCommand(),
				new LegalityCommand(),
				new NamesCommand(),
				new RulingsCommand(),
				new ComprehensiveRulesCommand(),
				new DefineCommand(),
				new InfractionProcedureGuideCommand(),
				new TournamentRulesCommand(),
				new SetCommand(),
				new TokenCommand(),
				new StandardCommand(),
				new RandomCommand(),
				new PricingCommand(),
				new FlavorCommand(),
				new ReloadPreferencesCommand(),
				new StatsCommand(),
				new BanListCommand()
		).collect(Collectors.toList());
		rateLimiter = new InMemorySlidingWindowRequestRateLimiter(Stream.of(
				RequestLimitRule.of(20, TimeUnit.SECONDS, 6),
				RequestLimitRule.of(5, TimeUnit.MINUTES, 20)
		).collect(Collectors.toSet()));
		rateLimitLimiter = new InMemorySlidingWindowRequestRateLimiter(Stream.of(
				RequestLimitRule.of(2, TimeUnit.MINUTES, 1)
		).collect(Collectors.toSet()));
	}

	@Override
	protected void handle(MessageReceivedEvent e, GuildPreferences guildPreferences, ChatHandler next) {
		// Check command prefix;
		String prefix = guildPreferences.getPrefix();
		if (!e.getMessage().getContent().startsWith(prefix)) {
			next.handle(e);
			return;
		}

		// Extract command and arguments
		String[] data = e.getMessage().getContent().substring(prefix.length()).split("\\s+");
		String cmd = data[0];
		String rawArgs = e.getMessage().getContent().substring(prefix.length()).substring(cmd.length()).trim();
		Matcher argsMatcher = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|[^\\s\"]+").matcher(rawArgs);
		List<String> args = new ArrayList<>();
		while (argsMatcher.find()) {
			String arg = argsMatcher.group();
			if (arg.startsWith("\"") && arg.endsWith("\"") && arg.length() > 1)
				arg = arg.substring(1, arg.length() - 1);
			arg = arg.replaceAll("\\\\\"", "\"");
			args.add(arg);
		}

		// Search for command
		BaseCommand command = commands
				.stream()
				.filter(c ->
						c.name().equalsIgnoreCase(cmd) || Arrays.stream(c.aliases()).anyMatch(a -> a.equalsIgnoreCase(cmd))
				)
				.findAny()
				.orElse(null);

		// Quit here if command was not found
		if (command == null) {
			next.handle(e);
			return;
		}

		// Check rate limits
		if (rateLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId())) {
			// Save log
			EventLogger.saveLog(new UserRateLimited(
					new LogEntry.User(e.getAuthor().getName(), e.getAuthor().getIdLong()),
					(e.getGuild() == null) ? null : new LogEntry.Guild(e.getGuild().getIdLong(), e.getGuild().getName()),
					new LogEntry.Channel(e.getChannel().getIdLong(), e.getChannel().getName()),
					command.name(),
					args.toArray(new String[0]),
					rawArgs,
					cmd.toLowerCase(),
					new Date(System.currentTimeMillis()),
					false
			));
			// Send warning
			if (!rateLimitLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId()))
				sendErrorEmbed(e.getChannel(), "Woah woah woah, easy there! Please don't spam my commands!");
			return;
		}

		// Save log
		EventLogger.saveLog(new UserCommandInvocation(
				new LogEntry.User(e.getAuthor().getName(), e.getAuthor().getIdLong()),
				(e.getGuild() == null) ? null : new LogEntry.Guild(e.getGuild().getIdLong(), e.getGuild().getName()),
				new LogEntry.Channel(e.getChannel().getIdLong(), e.getChannel().getName()),
				command.name(),
				args.toArray(new String[0]),
				rawArgs,
				cmd.toLowerCase(),
				new Date(System.currentTimeMillis()),
				false
		));

		// Execute command otherwise
		new Thread(() -> {
			if (guildPreferences.removeCommandCalls() && e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) e.getMessage().delete().queue();
			command.exec(args.toArray(new String[0]), rawArgs, e, guildPreferences);
		}).start();
	}
}
