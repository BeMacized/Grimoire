package net.bemacized.grimoire.chathandlers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import net.bemacized.grimoire.commands.all.CardCommand;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InlineCardHandler extends ChatHandler {

	private final static int MAX_REQUESTS_PER_MESSAGE = 3;

	private RequestRateLimiter rateLimiter;
	private RequestRateLimiter rateLimitLimiter;

	public InlineCardHandler(ChatHandler next) {
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
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("(<<|\\[\\[)[^$<|> ][^$<|>]*?([|][^<|>]+?)?(>>|]])");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		Multimap<String, String> references = HashMultimap.create();

		for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) {
			String[] data = m.group().substring(2, m.group().length() - 2).split("[|]");
			String cardname = data[0].trim();
			String setname = (data.length > 1) ? data[1].trim() : null;
			references.put(cardname, setname);
		}

		// Check rate limits
		if (!references.isEmpty() && rateLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId())) {
			if (!rateLimitLimiter.overLimitWhenIncremented("user:" + e.getAuthor().getId()))
				sendErrorEmbed(e.getChannel(), "Woah woah woah, easy there! Please don't spam inline card references!");
			return;
		}

		references.entries().forEach(entry -> {
			String cardname = entry.getKey();
			String setname = entry.getValue();
			new Thread(() -> new CardCommand().exec((cardname + (setname == null ? "" : " | " + setname)).split("\\s+"), e)).start();
		});

		next.handle(e);
	}
}
