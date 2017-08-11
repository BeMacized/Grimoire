package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.controllers.Sets;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.MtgSet;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PriceRetrieveHandler extends ChatHandler {

	private final static Logger LOG = Logger.getLogger(PriceRetrieveHandler.class.getName());

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;
	private final static int MAX_SET_ALTERNATIVES = 15;
	private final static int MAX_CARD_ALTERNATIVES = 15;

	public PriceRetrieveHandler(ChatHandler next) {
		super(next);
	}

	@SuppressWarnings("Duplicates")
	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<$CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("(<<|\\[\\[)[$][^<|>]+([|][^<|>]+)?(>>|]])");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		List<String> matches = new ArrayList<>();
		for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) matches.add(m.group());

		matches.parallelStream().forEach(match -> new Thread(() -> {
			String[] data = match.substring(3, match.length() - 2).split("[|]");
			String cardname = data[0].trim();
			String setname = (data.length > 1) ? data[1].trim() : null;

			// Send load message
			LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Checking price data...", true);

			// If a set(code) was provided, check its validity.
			MtgSet set;
			try {
				set = setname != null ? Grimoire.getInstance().getSets().getSingleByNameOrCode(setname) : null;
				if (set == null && setname != null) {
					sendEmbedFormat(loadMsg, ":anger: No set found with **'%s'** as its code or name.", setname);
					return;
				}
			} catch (Sets.MultipleResultsException ex) {
				if (ex.getSets().size() > MAX_SET_ALTERNATIVES)
					sendEmbedFormat(loadMsg, ":anger: There are too many results for a set named **'%s'**. Please be more specific.", setname);
				else
					sendEmbedFormat(loadMsg, "There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n\n%s",
							setname, String.join("\n", ex.getSets().parallelStream().map(s -> String.format(":small_orange_diamond: %s _(%s)_", s.getName(), s.getCode())).collect(Collectors.toList())));
				return;
			}

			// Retrieve card
			Card card;
			Cards.SearchQuery query = new Cards.SearchQuery().hasName(cardname).inSet(set);

			// Find exact match
			if (!query.hasExactName(cardname).isEmpty())
				card = query.hasExactName(cardname).get(0);
				// Find single match
			else if (query.distinctNames().size() == 1)
				card = query.distinctNames().get(0);
				// No results then?
			else if (query.isEmpty()) {
				Cards.SearchQuery foreignQuery = new Cards.SearchQuery().foreignAllowed().hasName(cardname).inSet(set);
				// Check if there's an exact foreign match
				if (!foreignQuery.hasExactName(cardname).isEmpty())
					card = foreignQuery.hasExactName(cardname).get(0);
					// Check if there's a single foreign match
				else if (foreignQuery.distinctNames().size() == 1)
					card = foreignQuery.distinctNames().get(0);
				else if (set == null) {
					sendEmbedFormat(loadMsg, ":anger: here are no results for a card named **'%s'**", cardname);
					return;
				} else {
					sendEmbedFormat(loadMsg, ":anger: There are no results for a card named **'%s'** in set **'%s (%s)'**", cardname, set.getName(), set.getCode());
					return;
				}
			}
			// We got multiple results. Check if too many?
			else if (query.distinctNames().size() > MAX_CARD_ALTERNATIVES) {
				sendEmbedFormat(loadMsg, ":anger: There are too many results for a card named **'%s'**. Please be more specific.", cardname);
				return;
			}
			// Nope, show the alternatives!
			else {
				sendEmbedFormat(loadMsg, "There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n\n%s", cardname,
						String.join("\n", query.distinctNames().parallelStream().map(c -> String.format(":small_orange_diamond: %s", c.getName())).collect(Collectors.toList())));
				return;
			}

			// Update load text
			loadMsg.setLineFormat("Loading price data for card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

			//Send the message
			loadMsg.complete(Grimoire.getInstance().getPricingManager().getPricingEmbed(card));

		}).start());

		next.handle(e);
	}


}
