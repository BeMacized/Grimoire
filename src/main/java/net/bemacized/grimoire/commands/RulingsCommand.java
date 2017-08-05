package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public class RulingsCommand extends BaseCommand {

	private final static int MAX_CARD_ALTERNATIVES = 15;

	@Override
	public String name() {
		return "rulings";
	}

	@Override
	public String[] aliases() {
		return new String[]{"rules", "ruling", "ruling"};
	}

	@Override
	public String description() {
		return "Retrieves the current rulings of the specified card.";
	}

	@Override
	public String paramUsage() {
		return "<card name>";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Obtain card name
		String cardname = String.join(" ", args);

		// Quit and error out if none provided
		if (cardname.isEmpty()) {
			sendEmbed(e.getChannel(),"Please provide a card name to check rulings for.");
			return;
		}

		// Retrieve card
		Card card;
		Cards.SearchQuery query = new Cards.SearchQuery().hasName(cardname);

		// Find exact match
		if (!query.hasExactName(cardname).isEmpty())
			card = query.hasExactName(cardname).get(0);
			// Find single match
		else if (query.distinctNames().size() == 1)
			card = query.distinctNames().get(0);
			// No results then?
		else if (query.isEmpty()) {
			Cards.SearchQuery foreignQuery = new Cards.SearchQuery().foreignAllowed().hasName(cardname);
			// Check if there's an exact foreign match
			if (!foreignQuery.hasExactName(cardname).isEmpty())
				card = foreignQuery.hasExactName(cardname).get(0);
				// Check if there's a single foreign match
			else if (foreignQuery.distinctNames().size() == 1)
				card = foreignQuery.distinctNames().get(0);
			else{
				sendEmbedFormat(e.getChannel(), "There are no results for a card named **'%s'**.", cardname);
				return;
			}
		}
		// We got multiple results. Check if too many?
		else if (query.distinctNames().size() > MAX_CARD_ALTERNATIVES) {
			sendEmbedFormat(e.getChannel(), "There are too many results for a card named **'%s'**. Please be more specific.", cardname);
			return;
		}
		// Nope, show the alternatives!
		else {
			sendEmbedFormat(e.getChannel(), "There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n\n%s", cardname,
					String.join("\n", query.distinctNames().parallelStream().map(c -> String.format(":small_orange_diamond: %s", c.getName())).collect(Collectors.toList())));
			return;
		}

		// We have found it. Let's check if there are any rulings
		if (card.getRulings().length == 0) {
			sendEmbedFormat(e.getChannel(), "There are no rulings for **'%s'**.", cardname);
			return;
		}

		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), (card.getMultiverseid() <= 0) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid())
				.setDescription("**Rulings**");
		for (Card.Ruling ruling : card.getRulings())
			eb.addField(ruling.getDate(), ruling.getText(), false);
		e.getChannel().sendMessage(eb.build()).submit();
	}
}
