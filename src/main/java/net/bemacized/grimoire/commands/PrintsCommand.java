package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.StringUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public class PrintsCommand extends BaseCommand {

	@Override
	public String name() {
		return "prints";
	}

	@Override
	public String[] aliases() {
		return new String[]{"sets", "versions", "printings"};
	}

	@Override
	public String description() {
		return "Retrieves all sets that a card was printed in. ";
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
					"<@%s>, please provide a card name to check printings for!",
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

		// Show the sets
		String sets = String.join("\n", new CardUtils.CardSearchQuery().setExactName(card.getName()).exec().parallelStream().map(c -> String.format(":small_orange_diamond: %s (%s)", c.getSetName(), c.getSet())).collect(Collectors.toList()));
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(CardUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		String[] splits = StringUtils.splitMessage(sets, 1000);
		for (int i = 0; i < splits.length; i++)
			eb.addField((i == 0) ? "Sets" : "", splits[i], false);
		e.getChannel().sendMessage(eb.build()).submit();
	}
}
