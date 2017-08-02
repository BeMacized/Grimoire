package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class OracleCommand extends BaseCommand {

	private final static int MAX_CARD_ALTERNATIVES = 15;

	@Override
	public String name() {
		return "oracle";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cardtext"};
	}

	@Override
	public String description() {
		return "Retrieves the oracle text of a card.";
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
					"<@%s>, please provide a card name to check the oracle text for!",
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
			e.getChannel().sendMessageFormat("<@%s>, There are no results for a card named **'%s'**", e.getAuthor().getId(), cardname).submit();
			return;
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

		// Show the text
		e.getChannel().sendMessage(
				new EmbedBuilder()
						.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
						.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid())
						.addField("Oracle Text", MTGUtils.parseEmoji(e.getGuild(), card.getText()), false)
						.build()
		).submit();
	}
}
