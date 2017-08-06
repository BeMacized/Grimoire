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
			sendEmbed(e.getChannel(), ":anger: Please provide a card name to fetch art for.");
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

		// Check if an image is available
		if (card.getImageUrl() == null) {
			sendEmbedFormat(loadMsg, ":anger: There is no known art for **'%s'**.", cardname);
			return;
		}

		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(card.getName(), card.getGathererUrl());
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.complete(eb.build());
	}
}
