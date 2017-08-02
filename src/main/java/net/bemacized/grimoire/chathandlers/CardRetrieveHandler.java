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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
		List<CardRequest> requests = new ArrayList<CardRequest>() {{
			for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) {
				String[] data = m.group().substring(2, m.group().length() - 2).split("[|]");
				String cardName = data[0].trim();
				String set = (data.length > 1) ? data[1].trim() : null;
				add(new CardRequest(cardName, set));
			}
		}};

		// Retrieve card
		requests.parallelStream().forEach(cardReq -> new Thread(() -> handleCardRequest(cardReq, e)).start());

		next.handle(e);
	}

	private void handleCardRequest(CardRequest cardReq, MessageReceivedEvent e) {
		// Construct load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading card...", true);

		// If a set(code) was provided, check its validity.
		MtgSet set;
		try {
			set = cardReq.getSet() != null ? Grimoire.getInstance().getSets().getSingleByNameOrCode(cardReq.getSet()) : null;
			if (set == null && cardReq.getSet() != null) {
				loadMsg.finalizeFormat("<@%s>, I could not find a set with **'%s' as its code or name**.", e.getAuthor().getId(), cardReq.getSet());
				return;
			}
		} catch (Sets.MultipleResultsException ex) {
			if (ex.getSets().size() > MAX_SET_ALTERNATIVES)
				loadMsg.finalizeFormat(
						"<@%s>, There are too many results for a set named **'%s'**. Please be more specific.",
						e.getAuthor().getId(),
						cardReq.getCardName()
				);
			else
				loadMsg.finalizeFormat("<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n%s",
						e.getAuthor().getId(), cardReq.getSet(),
						String.join("", ex.getSets().parallelStream().map(s -> String.format("\n:small_orange_diamond: %s _(%s)_",
								s.getName(), s.getCode())).collect(Collectors.toList())
						));
			return;
		}

		// Retrieve card
		Card card;
		Cards.SearchQuery query = new Cards.SearchQuery().hasName(cardReq.getCardName());
		if (set != null) query = query.inSet(set);

		// Find exact match
		if (!query.hasExactName(cardReq.getCardName()).isEmpty())
			card = query.hasExactName(cardReq.getCardName()).get(0);
			// Find single match
		else if (query.distinctNames().size() == 1)
			card = query.distinctNames().get(0);
			// No results then?
		else if (query.isEmpty()) {
			if (set == null)
				loadMsg.finalizeFormat("<@%s>, There are no results for a card named **'%s'**", e.getAuthor().getId(), cardReq.getCardName());
			else
				loadMsg.finalizeFormat("<@%s>, There are no results for a card named **'%s'** in set **'%s (%s)'**", e.getAuthor().getId(), cardReq.getCardName(), set.getName(), set.getCode());
			return;
		}
		// We got multiple results. Check if too many?
		else if (query.distinctNames().size() > MAX_CARD_ALTERNATIVES) {
			loadMsg.finalizeFormat("<@%s>, There are too many results for a card named **'%s'**. Please be more specific.", e.getAuthor().getId(), cardReq.getCardName());
			return;
		}
		// Nope, show the alternatives!
		else {
			StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n", e.getAuthor().getId(), cardReq.getCardName()));
			for (Card c : query.distinctNames()) sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
			loadMsg.finalize(sb.toString());
			return;
		}

		//TODO: VERIFY ART EXISTENCE

		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		// Construct the data we need

		String formats = (card.getLegalities() == null) ? "" : String.join(", ", Arrays.stream(card.getLegalities())
				.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
				.map(Card.Legality::getFormat)
				.collect(Collectors.toList()));
		String rarities = String.join(", ", new Cards.SearchQuery().hasExactName(card.getName()).parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
		String printings = String.join(", ", new String[]{"**" + card.getSet().getName() + " (" + card.getSet().getCode() + ")**", String.join(", ", Arrays.stream(card.getPrintings()).parallel().filter(setCode -> !card.getSet().getCode().equalsIgnoreCase(setCode)).collect(Collectors.toList()))});
		String pat = MTGUtils.parsePowerAndToughness(card.getPower(), card.getToughness());

		//TODO: ENABLE AGAIN WHEN DISCORD FIXES EMOJI IN EMBED TITLES ---
		//		String title = card.getName()
		//				+ " "
		//				+ CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		String separateCost = "";
		//		if (title.length() > 256) {
		//			title = card.getName();
		//			separateCost = CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		}

		String title = card.getName();
		String separateCost = MTGUtils.parseEmoji(e.getGuild(), card.getManaCost()) + " **(" + new DecimalFormat("##.###").format(card.getCmc()) + ")**";
		//TODO: ---END

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setThumbnail(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		eb.setTitle(title, (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		if (!separateCost.isEmpty()) eb.appendDescription(separateCost + "\n");
		if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		eb.appendDescription(card.getType());
		eb.appendDescription("\n\n");
		eb.appendDescription(MTGUtils.parseEmoji(e.getGuild(), card.getText()));
		if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		if (!printings.isEmpty()) eb.addField("Printings", printings, true);

		// Show message
		loadMsg.finalize(eb.build());
	}

	private class CardRequest {

		private String cardName;
		private String set;

		CardRequest(String cardName, String set) {
			this.cardName = cardName;
			this.set = set;
		}

		String getCardName() {
			return cardName;
		}

		String getSet() {
			return set;
		}
	}


}
