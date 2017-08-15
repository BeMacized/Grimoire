package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.MtgCard;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.bemacized.grimoire.utils.StreamUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TokenCommand extends BaseCommand {

	private static final int MAX_TOKEN_RESULTS = 40;

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
			sendEmbed(e.getChannel(), "Please provide a valid token name.");
			return;
		}

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Searching for token...", true);

		// Extract card name and optional choice id
		int choice = -1;
		String cardname;
		if (args.length >= 2 && isNumber(args[args.length - 1])) {
			choice = Integer.parseInt(args[args.length - 1]);
			cardname = String.join(" ", Arrays.copyOf(args, args.length - 1));
		} else {
			cardname = String.join(" ", args);
		}

		final List<MtgCard> matches = new CardProvider.SearchQuery()
				.hasLayout(MtgCard.Layout.TOKEN)
				.parallelStream()
				.filter(StreamUtils.distinctByKey(c -> DigestUtils.sha1Hex(c.getSet().getCode() + c.getPower() + c.getToughness())))
				.filter(token -> token.getName().replaceAll("[^a-zA-Z0-9 ]+", "").toLowerCase().contains(cardname.toLowerCase()))
				.collect(Collectors.toList());
		final List<MtgCard> exactMatches = matches.parallelStream()
				.filter(StreamUtils.distinctByKey(c -> DigestUtils.sha1Hex(c.getSet().getCode() + c.getPower() + c.getToughness())))
				.filter(token -> token.getName().trim().equalsIgnoreCase(cardname.toLowerCase()))
				.collect(Collectors.toList());

		if (!exactMatches.isEmpty()) {
			matches.clear();
			matches.addAll(exactMatches);
		}

		// Sort matches
		matches.sort((o1, o2) -> {
			if (o1.getName().equalsIgnoreCase(o2.getName()) && o1.getPower() != null && o1.getToughness() != null && o2.getPower() != null && o2.getToughness() != null)
				return (o1.getPower() + "/" + o1.getToughness()).compareTo(o2.getPower() + "/" + o2.getToughness());
			else return o1.getName().compareTo(o2.getName());
		});

		// Test for no matches
		if (matches.isEmpty()) {
			sendEmbedFormat(loadMsg, ":anger: I couldn't find any tokens called **'%s'**", cardname);
			return;
		}

		// Get 1st match
		MtgCard match = matches.get(0);

		// Test for multiple matches
		if (matches.size() > 1) {
			// Check if choice # was set
			if (choice != -1) {
				// Check if choice # is in range
				if (choice < 1 || choice > matches.size()) {
					sendEmbed(loadMsg, ":anger: The choice number you provided is not within range. Please only pick a valid option.");
					return;
				}
				// Replace match with choice
				match = matches.get(choice - 1);
			} else if (matches.size() > MAX_TOKEN_RESULTS) {
				// List options
				sendEmbed(loadMsg, ":anger: There are too many tokens matching your search. Please be more specific.");
				return;
			} else {
				// List options
				sendEmbedFormat(loadMsg, "There are multiple tokens matching your search. Please pick any of the following using `!token %s [#]`:\n%s", match.getName(), String.join("\n",
						IntStream.range(0, matches.size()).parallel().mapToObj(i -> {
							MtgCard m = matches.get(i);
							return String.format(
									":small_orange_diamond: **%s.**%s%s%s%s%s",
									i + 1,
									(m.getTokenColor() == null) ? "" : " " + m.getTokenColor(),
									(m.getPower() == null || m.getToughness() == null) ? "" : " _" + MTGUtils.parsePowerAndToughness(m.getPower(), m.getToughness()) + "_",
									" " + m.getName(),
									" _(" + m.getSet().getCode() + ")_",
									(m.getImageUrl() == null) ? " __[NO ART AVAILABLE]__" : "");
						}).collect(Collectors.toList())
				));
				return;
			}
		}

		// Check if match has image
		if (match.getImageUrl() == null) {
			sendEmbed(loadMsg, ":anger: I do not know of any art for this token. Please try a different one!");
			return;
		}

		// Update load message
		loadMsg.setLineFormat("Loading '%s' token...", match.getName());

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(match.getName());
		eb.setImage(match.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(new String[]{match.getTokenColor()}));
		loadMsg.complete(eb.build());
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
