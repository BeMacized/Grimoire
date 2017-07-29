package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class PricingCommand extends BaseCommand {
	@Override
	public String name() {
		return "pricing";
	}

	@Override
	public String[] aliases() {
		return new String[]{"price", "dollarydoos"};
	}

	@Override
	public String description() {
		return "Retrieves the current pricing for a card.";
	}

	@Override
	public String paramUsage() {
		return "<card name>[|setcode/setname]";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		try {
			// Quit and error out if none provided
			if (args.length == 0) {
				e.getChannel().sendMessageFormat(
						"<@%s>, please provide a card name to check pricing for!",
						e.getAuthor().getId()
				).submit();
				return;
			}

			// Send load message
			RequestFuture<Message> loadMsg = e.getChannel().sendMessage("```\n" + "Checking price data..." + "\n```").submit();

			// Obtain card name
			String[] split = String.join(" ", args).split("\\|");
			String cardname = split[0].trim();
			String setname = (split.length > 1 && !split[1].trim().isEmpty()) ? split[1].trim() : null;

			// If a set(code) was provided, check its validity.
			MtgSet set = null;
			try {
				if (setname != null) set = SetUtils.getSet(setname);
			}
			// Handle too many results
			catch (SetUtils.TooManyResultsException ex) {
				loadMsg.get().editMessageFormat(
						"<@%s>, There are too many results for a set named **'%s'**. Please be more specific.",
						e.getAuthor().getId(),
						setname
				).submit();
				return;
			}
			// Handle multiple results
			catch (SetUtils.MultipleResultsException ex) {
				StringBuilder sb = new StringBuilder(String.format(
						"<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n",
						e.getAuthor().getId(),
						setname
				));
				for (MtgSet s : ex.getResults())
					sb.append(String.format(
							"\n - %s _(%s)_",
							s.getName(),
							s.getCode())
					);
				loadMsg.get().editMessage(sb.toString()).submit();
				return;
			}
			// Handle no results
			catch (SetUtils.NoResultsException e1) {
				loadMsg.get().editMessageFormat(
						"<@%s>, I could not find a set with **'%s' as its code or name**.",
						e.getAuthor().getId(),
						setname
				).submit();
				return;
			}

			// Retrieve card
			Card card;
			try {
				card = CardUtils.getCard(cardname, (set == null) ? null : set.getCode());
			}
			// Handle too many results
			catch (CardUtils.TooManyResultsException ex) {
				loadMsg.get().editMessageFormat(
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
				loadMsg.get().editMessage(sb.toString()).submit();
				return;
			}
			// Handle no results
			catch (CardUtils.NoResultsException e1) {
				e.getChannel().sendMessageFormat(
						"<@%s>, There are no results for a card named **'%s'**" + ((set == null) ? "" : " in the set you requested."),
						e.getAuthor().getId(),
						cardname
				).submit();
				return;
			}

			// Update load text
			loadMsg.get().editMessageFormat(
					"```\n" + "Loading price data for card '%s' from set '%s, (%s)'..." + "\n```",
					card.getName(),
					card.getSetName(),
					card.getSet()
			).submit();

			//Send the message
			loadMsg.get().editMessage(Grimoire.getInstance().getPricingManager().getPricingEmbed(card)).submit();
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "An error occurred getting price data", ex);
			e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred getting the price data. Please notify my developer to fix me up!").submit();
		}
	}
}
