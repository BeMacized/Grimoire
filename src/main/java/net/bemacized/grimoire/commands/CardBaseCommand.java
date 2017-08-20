package net.bemacized.grimoire.commands;

import com.google.gson.Gson;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.card.MtgSet;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public abstract class CardBaseCommand extends BaseCommand {

	private final static int MAX_SET_ALTERNATIVES = 15;
	private final static int MAX_CARD_ALTERNATIVES = 15;

	@Override
	public String paramUsage() {
		return "<card>\n<card|set>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Quit and error out if none provided
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please provide a card name.");
			return;
		}

		// Obtain card name
		String[] split = String.join(" ", args).split("\\|");
		String cardname = split[0].trim();
		String setname = (split.length > 1 && !split[1].trim().isEmpty()) ? split[1].trim() : null;

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), getInitialLoadLine(), true);

		// If a set(code) was provided, check its validity.
		MtgSet set;
		try {
			set = setname != null ? Grimoire.getInstance().getCardProvider().getSingleSetByNameOrCode(setname) : null;
			if (set == null && setname != null) {
				sendErrorEmbedFormat(loadMsg, "No set found with **'%s'** as its code or name.", setname);
				return;
			}
		} catch (CardProvider.MultipleSetResultsException ex) {
			if (ex.getResults().size() > MAX_SET_ALTERNATIVES)
				sendErrorEmbedFormat(loadMsg, "There are too many results for a set named **'%s'**. Please be more specific.", setname);
			else
				sendEmbedFormat(
						loadMsg,
						"There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n\n%s",
						setname,
						String.join("\n", ex.getResults().parallelStream().map(s -> String.format(":small_orange_diamond: %s _(%s)_", s.getName(), s.getCode())).collect(Collectors.toList()))
				);
			return;
		}

		// Retrieve card
		MtgCard card;
		CardProvider.SearchQuery query = new CardProvider.SearchQuery().noTokens().noEmblems().inLanguage("English").containsName(cardname).inSet(set);

		// Find exact match
		if (!query.hasName(cardname).isEmpty())
			card = query.hasName(cardname).get(0);
			// Find single match
		else if (query.distinctCards().size() == 1)
			card = query.distinctCards().get(0);
			// No results then?
		else if (query.isEmpty()) {
			CardProvider.SearchQuery foreignQuery = new CardProvider.SearchQuery().noTokens().noEmblems().containsName(cardname).inSet(set);
			// Check if there's an exact foreign match
			if (!foreignQuery.hasName(cardname).isEmpty())
				card = foreignQuery.hasName(cardname).get(0);
				// Check if there's a single foreign match
			else if (foreignQuery.distinctCards().size() == 1)
				card = foreignQuery.distinctCards().get(0);
			else if (set == null) {
				sendErrorEmbedFormat(loadMsg, "There are no results for a card named **'%s'**", cardname);
				return;
			} else {
				sendErrorEmbedFormat(loadMsg, "There are no results for a card named **'%s'** in set **'%s (%s)'**", cardname, set.getName(), set.getCode());
				return;
			}
		}
		// We got multiple results. Check if too many?
		else if (query.distinctCards().size() > MAX_CARD_ALTERNATIVES) {
			sendErrorEmbedFormat(loadMsg, "There are too many results for a card named **'%s'**. Please be more specific.", cardname);
			return;
		}
		// Nope, show the alternatives!
		else {
			sendEmbedFormat(loadMsg, "There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n\n%s", cardname,
					String.join("\n", query.distinctCards().parallelStream().map(c -> String.format(":small_orange_diamond: %s", c.getName())).collect(Collectors.toList())));
			return;
		}

		execForCard(card, loadMsg, e);
	}

	protected abstract String getInitialLoadLine();

	protected abstract void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e);
}
