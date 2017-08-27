package net.bemacized.grimoire.chathandlers;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.commands.all.PricingCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.eventlogger.EventLogger;
import net.bemacized.grimoire.eventlogger.events.LogEntry;
import net.bemacized.grimoire.eventlogger.events.UserCommandInvocation;
import net.bemacized.grimoire.eventlogger.events.UserRateLimited;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InlinePriceHandler extends ChatHandler {

	private final static int MAX_REQUESTS_PER_MESSAGE = 3;

	private RequestRateLimiter rateLimiter;
	private RequestRateLimiter rateLimitLimiter;

	public InlinePriceHandler(ChatHandler next) {
		super(next);
		rateLimiter = new InMemorySlidingWindowRequestRateLimiter(Stream.of(
				RequestLimitRule.of(20, TimeUnit.SECONDS, 10),
				RequestLimitRule.of(5, TimeUnit.MINUTES, 30)
		).collect(Collectors.toSet()));
		rateLimitLimiter = new InMemorySlidingWindowRequestRateLimiter(Stream.of(
				RequestLimitRule.of(2, TimeUnit.MINUTES, 1)
		).collect(Collectors.toSet()));
	}

	@Override
	protected void handle(MessageReceivedEvent e, GuildPreferences guildPreferences, ChatHandler next) {
		if (!guildPreferences.inlinePriceReferencesEnabled()) {
			next.handle(e);
			return;
		}

		// Find matches for <<$CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("(<<|\\[\\[)\\$[^<> ][^<>]*?(>>|]])");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		Set<String> references = new HashSet<>();

		for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++)
			references.add(m.group().substring(3, m.group().length() - 2));

		BaseCommand command = new PricingCommand();

		// Execute inline references as commands
		for (String reference : references) {

			// Check rate limits
			if (!references.isEmpty() && rateLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId())) {
				// Send & log Warning
				if (!rateLimitLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId())) {
					sendErrorEmbed(e.getChannel(), "Woah woah woah, easy there! Please don't spam inline price references!");
					EventLogger.saveLog(new UserRateLimited(
							new LogEntry.User(e.getAuthor().getName(), e.getAuthor().getIdLong()),
							(e.getGuild() == null) ? null : new LogEntry.Guild(e.getGuild().getIdLong(), e.getGuild().getName()),
							new LogEntry.Channel(e.getChannel().getIdLong(), e.getChannel().getName()),
							command.name(),
							new String[0],
							reference,
							command.name(),
							new Date(System.currentTimeMillis()),
							false
					));
				}
				return;
			}

			// Save command log
			EventLogger.saveLog(new UserCommandInvocation(
					new LogEntry.User(e.getAuthor().getName(), e.getAuthor().getIdLong()),
					(e.getGuild() == null) ? null : new LogEntry.Guild(e.getGuild().getIdLong(), e.getGuild().getName()),
					new LogEntry.Channel(e.getChannel().getIdLong(), e.getChannel().getName()),
					command.name(),
					new String[0],
					reference,
					command.name(),
					new Date(System.currentTimeMillis()),
					true
			));

			new Thread(() -> command.exec(new String[0], reference, e, guildPreferences)).start();
		}

		next.handle(e);
	}
}
