package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.pricing.PricingManager;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

			// Fetch pricing
			List<PricingManager.StoreCardPrice> pricing = Grimoire.getInstance().getPricingManager().getPricing(card);

			// Construct response message;
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, I found the following pricing data for **'%s'** from set **'%s'**:",
					e.getAuthor().getId(),
					card.getName(),
					card.getSetName()
			));
			pricing.forEach(storeprice -> {
				sb.append("\n\n");
				switch (storeprice.getStatus()) {
					case UNKNOWN_ERROR:
						sb.append(String.format("**%s**: An unknown error occurred for this store. Please notify my developer.", storeprice.getStoreName()));
						break;
					case CARD_UNKNOWN:
						sb.append(String.format("**%s**: I could not find this card in this store.", storeprice.getStoreName()));
						break;
					case AUTH_ERROR:
						sb.append(String.format("**%s**: This store rejected my authentication attempt. Please notify my developer.", storeprice.getStoreName()));
						break;
					case SET_UNKNOWN:
						sb.append(String.format("**%s**: Set **'%s'** is currently not supported for this store.", storeprice.getStoreName(), card.getSetName()));
						break;
					case SERVER_ERROR:
						sb.append(String.format("**%s**: This store is currently having server issues!", storeprice.getStoreName()));
						break;
					case SUCCESS:
						sb.append(String.format("**%s**: ", storeprice.getStoreName()));
						DecimalFormat formatter = new DecimalFormat("#.00");
						sb.append(String.join(" **|** ", storeprice.getRecord().getPrices().entrySet().parallelStream().map(price -> String.format(
								"%s: %s%s",
								price.getKey(),
								(price.getValue() > 0) ? storeprice.getRecord().getCurrency() : "",
								(price.getValue() > 0) ? formatter.format(price.getValue()) : "N/A"
						)).collect(Collectors.toList())));
						sb.append("\nFor more information visit ").append(storeprice.getRecord().getUrl());
						final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						sb.append(String.format("\n_(Last updated at %s)_", sdf.format(new Date(storeprice.getRecord().getTimestamp()))));
						break;

				}
			});

			//Send the message
			loadMsg.get().editMessage(sb.toString()).submit();

		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "An error occurred getting price data", ex);
			e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred getting the price data. Please notify my developer to fix me up!").submit();
		}
	}
}
