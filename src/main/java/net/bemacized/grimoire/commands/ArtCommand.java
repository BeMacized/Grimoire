package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.controllers.Sets;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.MtgSet;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public class ArtCommand extends BaseCommand {

	private final static int MAX_SET_ALTERNATIVES = 15;
	private final static int MAX_CARD_ALTERNATIVES = 15;

	@Override
	public String name() {
		return "art";
	}

	@Override
	public String[] aliases() {
		return new String[]{"card", "cardart"};
	}

	@Override
	public String description() {
		return "Fetch the full art of a card";
	}

	@Override
	public String paramUsage() {
		return "<card name>[|setcode/setname]";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Quit and error out if none provided
		if (args.length == 0) {
			e.getChannel().sendMessageFormat("<@%s>, please provide a card name to fetch art for!", e.getAuthor().getId()).submit();
			return;
		}

		// Obtain card name
		String[] split = String.join(" ", args).split("\\|");
		String cardname = split[0].trim();
		String setname = (split.length > 1 && !split[1].trim().isEmpty()) ? split[1].trim() : null;

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading card art...", true);

		// If a set(code) was provided, check its validity.
		MtgSet set;
		try {
			set = setname != null ? Grimoire.getInstance().getSets().getSingleByNameOrCode(setname) : null;
			if (set == null && setname != null) {
				loadMsg.finalizeFormat("<@%s>, I could not find a set with **'%s' as its code or name**.", e.getAuthor().getId(), setname);
				return;
			}
		} catch (Sets.MultipleResultsException ex) {
			if (ex.getSets().size() > MAX_SET_ALTERNATIVES)
				loadMsg.finalizeFormat(
						"<@%s>, There are too many results for a set named **'%s'**. Please be more specific.",
						e.getAuthor().getId(),
						cardname
				);
			else
				loadMsg.finalizeFormat("<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n%s",
						e.getAuthor().getId(), setname,
						String.join("", ex.getSets().parallelStream().map(s -> String.format("\n:small_orange_diamond: %s _(%s)_",
								s.getName(), s.getCode())).collect(Collectors.toList())
						));
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
				loadMsg.finalizeFormat("<@%s>, There are no results for a card named **'%s'**", e.getAuthor().getId(), cardname);
				return;
			} else {
				loadMsg.finalizeFormat("<@%s>, There are no results for a card named **'%s'** in set **'%s (%s)'**", e.getAuthor().getId(), cardname, set.getName(), set.getCode());
				return;
			}
		}
		// We got multiple results. Check if too many?
		else if (query.distinctNames().size() > MAX_CARD_ALTERNATIVES) {
			loadMsg.finalizeFormat("<@%s>, There are too many results for a card named **'%s'**. Please be more specific.", e.getAuthor().getId(), cardname);
			return;
		}
		// Nope, show the alternatives!
		else {
			StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n", e.getAuthor().getId(), cardname));
			for (Card c : query.distinctNames())
				sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
			loadMsg.finalize(sb.toString());
			return;
		}

		// Check if an image is available
		if (card.getImageUrl() == null) {
			loadMsg.finalizeFormat("<@%s>, There is no known art for **'%s'**.", e.getAuthor().getId(), cardname);
			return;
		}

		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.finalize(eb.build());
	}
}
