package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class NamesCommand extends BaseCommand {

	private final static int MAX_CARD_ALTERNATIVES = 15;

	@Override
	public String name() {
		return "names";
	}

	@Override
	public String[] aliases() {
		return new String[]{"abroad", "foreign", "named"};
	}

	@Override
	public String description() {
		return "Retrieves all known foreign names for a card";
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
			e.getChannel().sendMessageFormat(
					"<@%s>, please provide a card name to check names for!",
					e.getAuthor().getId()
			).submit();
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
			else {
				e.getChannel().sendMessageFormat("<@%s>, There are no results for a card named **'%s'**", e.getAuthor().getId(), cardname).submit();
				return;
			}
		}
		// We got multiple results. Check if too many?
		else if (query.distinctNames().size() > MAX_CARD_ALTERNATIVES) {
			e.getChannel().sendMessageFormat("<@%s>, There are too many results for a card named **'%s'**. Please be more specific.", e.getAuthor().getId(), cardname).submit();
			return;
		}
		// Nope, show the alternatives!
		else {
			StringBuilder sb = new StringBuilder(String.format("<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n", e.getAuthor().getId(), cardname));
			for (Card c : query.distinctNames())
				sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
			e.getChannel().sendMessageFormat(sb.toString()).submit();
			return;
		}

		// We have found it. Let's check if there are any foreign names known
		if (card.getForeignNames().length == 0) {
			e.getChannel().sendMessage(
					new EmbedBuilder()
							.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
							.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid())
							.setDescription("**Foreign Names**")
							.addField("", "There are no known foreign names for this card.", false)
							.build()).submit();
			return;
		}

		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), (card.getMultiverseid() <= 0) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid())
				.setDescription("**Foreign Names**\n");
		for (Card.ForeignName foreignName : card.getForeignNames())
			eb.addField(foreignName.getLanguage(), foreignName.getMultiverseid() > 0 ? String.format("[%s](%s)", foreignName.getName(), "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + foreignName.getMultiverseid()) : foreignName.getName(), true);
		e.getChannel().sendMessage(eb.build()).submit();
	}
}
