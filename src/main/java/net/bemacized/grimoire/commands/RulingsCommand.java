package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.Ruling;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.StringUtils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RulingsCommand extends BaseCommand {

	@Override
	public String name() {
		return "rulings";
	}

	@Override
	public String[] aliases() {
		return new String[]{"rules", "ruling"};
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
			e.getChannel().sendMessageFormat(
					"<@%s>, please provide a card name to check rulings for!",
					e.getAuthor().getId()
			).submit();
			return;
		}

		// Retrieve card
		Card card;
		try {
			card = CardUtils.getCard(cardname);
		}
		// Handle too many results
		catch (CardUtils.TooManyResultsException ex) {
			e.getChannel().sendMessageFormat(
					"<@%s>, There are too many results for a card named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardname
			).submit();
			return;
		}
		// Handle multiple results
		catch (CardUtils.MultipleResultsException ex) {
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n",
					e.getAuthor().getId(),
					cardname
			));
			for (Card c : ex.getResults()) sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
			e.getChannel().sendMessage(sb.toString()).submit();
			return;
		}
		// Handle no results
		catch (CardUtils.NoResultsException e1) {
			e.getChannel().sendMessageFormat(
					"<@%s>, There are no results for a card named **'%s'**",
					e.getAuthor().getId(),
					cardname
			).submit();
			return;
		}

		// We have found it. Let's check if there are any rulings
		if (card.getRulings() == null || card.getRulings().length == 0) {
			e.getChannel().sendMessageFormat(
					"<@%s>, Card **'%s'** does not have any rulings.",
					e.getAuthor().getId(),
					card.getName()
			).submit();
			return;
		}

		// Show the rulings
		StringBuilder sb = new StringBuilder(String.format(
				"<@%s>, The following ruling(s) were released for **'%s'**:",
				e.getAuthor().getId(),
				card.getName())
		);
		for (Ruling ruling : card.getRulings()) {
			sb.append(String.format("\n\n**%s**:", ruling.getDate()));
			sb.append(String.format("\n%s", ruling.getText()));
		}
		for (String s : StringUtils.splitMessage(sb.toString()))
			e.getChannel().sendMessage(s).submit();

	}
}
