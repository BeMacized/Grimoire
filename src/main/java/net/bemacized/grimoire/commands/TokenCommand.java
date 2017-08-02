package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.MtgSet;
import net.bemacized.grimoire.model.models.Token;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TokenCommand extends BaseCommand {

	private static final int MAX_TOKEN_RESULTS = 15;

	@Override
	public String name() {
		return "token";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Retrieve the art of a token.";
	}

	@Override
	public String paramUsage() {
		return "<token_name> [choice]";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Verify token name presence
		if (args.length == 0) {
			e.getChannel().sendMessageFormat(
					"<@%s>, Please provide a valid token name",
					e.getAuthor().getId()
			).submit();
			return;
		}

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Searching for token...", true);

		// Extract card name and optional choice id
		int choice = -1;
		String cardName;
		if (args.length >= 2 && isNumber(args[args.length - 1])) {
			choice = Integer.parseInt(args[args.length - 1]);
			cardName = String.join(" ", Arrays.copyOf(args, args.length - 1));
		} else {
			cardName = String.join(" ", args);
		}

		// Find token(s)
		List<Token> matches = Grimoire.getInstance().getTokens().getTokens().parallelStream().filter(token -> token.getName().replaceAll("[^a-zA-Z0-9 ]+", "").toLowerCase().contains(cardName.toLowerCase())).collect(Collectors.toList());
		// filter down to exact matches only if available
		List<Token> exactMatches = matches.parallelStream().filter(token -> token.getName().trim().equalsIgnoreCase(cardName.toLowerCase())).collect(Collectors.toList());
		if (!exactMatches.isEmpty()) matches = exactMatches;

		// Sort matches
		matches.sort((o1, o2) -> {
			if (o1.getName().equalsIgnoreCase(o2.getName()) && o1.getPt() != null && o2.getPt() != null)
				return o1.getPt().compareTo(o2.getPt());
			else return o1.getName().compareTo(o2.getName());
		});

		// Test for no matches
		if (matches.isEmpty()) {
			loadMsg.finalizeFormat("<@%s>, I couldn't find any tokens called **'%s'**", e.getAuthor().getId(), cardName);
			return;
		}

		// Get 1st match
		Token match = matches.get(0);

		// Test for multiple matches
		if (matches.size() > 1) {
			// Check if choice # was set
			if (choice != -1) {
				// Check if choice # is in range
				if (choice < 1 || choice > matches.size()) {
					loadMsg.finalizeFormat("<@%s>, The choice number you provided is not within range. Please only pick a valid option.", e.getAuthor().getId());
					return;
				}
				// Replace match with choice
				match = matches.get(choice - 1);
			} else if (matches.size() > MAX_TOKEN_RESULTS) {
				// List options
				loadMsg.finalizeFormat("<@%s>, There are too many tokens matching your search. Please be more specific.", e.getAuthor().getId(), cardName);
				return;
			} else {
				// List options
				StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple tokens matching your search. Please pick any of the following using `!token %s [#]`:\n", e.getAuthor().getId(), cardName));
				for (int i = 0; i < matches.size(); i++) {
					Token m = matches.get(i);
					boolean hasArts = m.getSetArt().parallelStream().anyMatch(art -> art.getUrl() != null && !art.getUrl().isEmpty());
					sb.append(String.format(
							"\n:small_orange_diamond: **%s.**%s%s%s%s",
							i + 1,
							(m.getColor() == null) ? "" : " " + m.getColor(),
							(m.getPt() == null) ? "" : " _" + MTGUtils.parsePowerAndToughness(m.getPt()) + "_",
							" " + m.getName(),
							(hasArts) ? "" : " __[NO ART AVAILABLE]__"
					));
				}
				loadMsg.finalize(sb.toString());
				return;
			}
		}

		// Check if match has image
		List<Token.SetArt> arts = match.getSetArt().parallelStream().filter(art -> art.getUrl() != null && !art.getUrl().isEmpty()).collect(Collectors.toList());
		if (arts.isEmpty()) {
			loadMsg.finalizeFormat("<@%s>, I sadly do not know of any art for this token. Please try a different one!", e.getAuthor().getId(), cardName);
			return;
		}

		// Update load message
		loadMsg.setLineFormat("Loading '%s' token...", match.getName());

		// Pick random art
		Token.SetArt art = arts.get(new Random().nextInt(arts.size()));

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(match.getName());
		eb.setImage(art.getUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(new String[]{match.getColor()}));
		loadMsg.finalize(eb.build());
	}

	private boolean isNumber(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
