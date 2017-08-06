package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.controllers.Sets;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.MtgSet;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CardRetrieveHandler extends ChatHandler {

	private final static Logger LOG = Logger.getLogger(CardRetrieveHandler.class.getName());

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;
	private final static int MAX_SET_ALTERNATIVES = 15;
	private final static int MAX_CARD_ALTERNATIVES = 15;

	public CardRetrieveHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("(<<|\\[\\[)[^$<|>]+([|][^<|>]+)?(>>|]])");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) {
			String[] data = m.group().substring(2, m.group().length() - 2).split("[|]");
			String cardname = data[0].trim();
			String setname = (data.length > 1) ? data[1].trim() : null;

			// Retrieve card
			new Thread(() -> {
				// Construct load message
				LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading card...", true);

				// If a set(code) was provided, check its validity.
				MtgSet set;
				try {
					set = setname != null ? Grimoire.getInstance().getSets().getSingleByNameOrCode(setname) : null;
					if (set == null && setname != null)
						sendEmbedFormat(loadMsg, ":anger: No set found with **'%s'** as its code or name.", setname);
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
						sendEmbedFormat(loadMsg, ":anger: There are no results for a card named **'%s'**", cardname);
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
				loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

				// Construct the data we need

				String formats = (card.getLegalities() == null) ? "" : String.join(", ", Arrays.stream(card.getLegalities())
						.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
						.map(Card.Legality::getFormat)
						.collect(Collectors.toList()));
				String rarities = String.join(", ", new Cards.SearchQuery().hasExactName(card.getName()).parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
				String printings = String.join(", ", new String[]{"**" + card.getSet().getName() + " (" + card.getSet().getCode() + ")**", String.join(", ", Arrays.stream(card.getPrintings()).parallel().filter(setCode -> !card.getSet().getCode().equalsIgnoreCase(setCode)).collect(Collectors.toList()))}).trim();
				if (printings.endsWith(",")) printings = printings.substring(0, printings.length() - 1);
				String pat = MTGUtils.parsePowerAndToughness(card.getPower(), card.getToughness());

				//TODO: ENABLE AGAIN WHEN DISCORD FIXES EMOJI IN EMBED TITLES ON MOBILE ---
				//		String title = card.getName()
				//				+ " "
				//				+ CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
				//		String separateCost = "";
				//		if (title.length() > 256) {
				//			title = card.getName();
				//			separateCost = CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
				//		}

				String title = card.getName();
				String separateCost = (card.getManaCost() == null || card.getManaCost().isEmpty()) ? "" : Grimoire.getInstance().getEmojiParser().parseEmoji(card.getManaCost(), e.getGuild()) + " **(" + new DecimalFormat("##.###").format(card.getCmc()) + ")**";

				// Build the embed
				EmbedBuilder eb = new EmbedBuilder();
				eb.setThumbnail(card.getImageUrl());
				eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
				eb.setTitle(title, card.getGathererUrl());
				if (!separateCost.isEmpty()) eb.appendDescription(separateCost + "\n");
				if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
				eb.appendDescription(card.getType());
				eb.appendDescription("\n\n");
				eb.appendDescription(Grimoire.getInstance().getEmojiParser().parseEmoji(card.getText(), e.getGuild()));
				if (!formats.isEmpty()) eb.addField("Formats", formats, true);
				if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
				if (!printings.isEmpty()) eb.addField("Printings", printings, true);

				// Show message
				loadMsg.complete(eb.build());
			}).start();
		}

		next.handle(e);
	}
}
