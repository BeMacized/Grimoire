package net.bemacized.grimoire.chathandlers;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceRetrieveHandler extends ChatHandler {

	private final static Logger LOG = Logger.getLogger(PriceRetrieveHandler.class.getName());

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;

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
			MtgSet set = null;
			try {
				if (setname != null) set = SetUtils.getSet(setname);
			}
			// Handle too many results
			catch (SetUtils.TooManyResultsException ex) {
				loadMsg.setLineFinalFormat("<@%s>, There are too many results for a set named **'%s'**. Please be more specific.", e.getAuthor().getId(), setname);
				return;
			}
			// Handle multiple results
			catch (SetUtils.MultipleResultsException ex) {
				StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n", e.getAuthor().getId(), setname));
				for (MtgSet s : ex.getResults())
					sb.append(String.format("\n:small_orange_diamond: %s _(%s)_", s.getName(), s.getCode()));
				loadMsg.setLineFinal(sb.toString());
				return;
			}
			// Handle no results
			catch (SetUtils.NoResultsException e1) {
				loadMsg.setLineFinalFormat("<@%s>, I could not find a set with **'%s' as its code or name**.", e.getAuthor().getId(), setname);
				return;
			}

			// Retrieve card
			Card card;
			try {
				card = CardUtils.getCard(cardname, (set == null) ? null : set.getCode());
			}
			// Handle too many results
			catch (CardUtils.TooManyResultsException ex) {
				loadMsg.setLineFinalFormat("<@%s>, There are too many results for a card named **'%s'**. Please be more specific.", e.getAuthor().getId(), cardname);
				return;
			}
			// Handle multiple results
			catch (CardUtils.MultipleResultsException ex) {
				StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n", e.getAuthor().getId(), cardname));
				for (Card c : ex.getResults()) sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
				loadMsg.setLineFinal(sb.toString());
				return;
			}
			// Handle no results
			catch (CardUtils.NoResultsException e1) {
				loadMsg.setLineFinalFormat("<@%s>, There are no results for a card named **'%s'**" + ((set == null) ? "" : " in the set you requested."), e.getAuthor().getId(), cardname);
				return;
			}

			// Update load text
			loadMsg.setLineFormat("Loading price data for card '%s' from set '%s, (%s)'...", card.getName(), card.getSetName(), card.getSet());

			//Send the message
			loadMsg.setLineFinal(Grimoire.getInstance().getPricingManager().getPricingEmbed(card));
		}).start());

		next.handle(e);
	}


}
