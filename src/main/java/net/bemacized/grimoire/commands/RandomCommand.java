package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.api.CardAPI;
import io.magicthegathering.javasdk.resource.Card;
import net.bemacized.grimoire.utils.CardUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RandomCommand extends BaseCommand {
	@Override
	public String name() {
		return "random";
	}

	@Override
	public String[] aliases() {
		return new String[]{"rand", "rng"};
	}

	@Override
	public String description() {
		return "Show a random card of a certain type. Example: `!random legendary creature angel`.";
	}

	@Override
	public String paramUsage() {
		return "<[supertype] [type] [subtype]>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		try {
			List<String> supertypes = new ArrayList<>();
			List<String> types = new ArrayList<>();
			List<String> subtypes = new ArrayList<>();

			// Make sure we have at least one argument
			if (args.length < 1) {
				e.getChannel().sendMessage(String.format(
						"<@%s>, Please specify one or more (sub/super)types.",
						e.getAuthor().getId()
				)).submit();
				return;
			}

			// Send load message
			RequestFuture<Message> loadMsg = e.getChannel().sendMessage("```\n" + "Drawing random card..." + "\n```").submit();

			// Retrieve all types
			List<String> allSupertypes = CardAPI.getAllCardSupertypes();
			List<String> allTypes = CardAPI.getAllCardTypes();
			List<String> allSubtypes = CardAPI.getAllCardSubtypes();

			// Extract types
			for (String arg : args) {
				if (allSupertypes.stream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
					if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
				} else if (allTypes.stream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
					if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
				} else if (allSubtypes.stream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
					if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
				} else {
					loadMsg.get().editMessage(String.format(
							"<@%s>, **'%s'** is neither a type, supertype or subtype. Please only specify valid types.",
							e.getAuthor().getId(),
							arg
					)).submit();
					return;
				}
			}

			// Edit load message to reflect chosen types
			String joinedType = String.join(" ", new ArrayList<String>() {{
				addAll(supertypes);
				addAll(types);
				addAll(subtypes);
			}}.stream().filter(Objects::nonNull).map(t -> t.substring(0, 1).toUpperCase() + t.substring(1, t.length())).collect(Collectors.toList()));
			loadMsg.get().editMessage(String.format(
					"```\n" + "Drawing random %s..." + "\n```",
					joinedType
			)).submit();

			//Find cards
			CardUtils.CardSearchQuery query = new CardUtils.CardSearchQuery();
			if (!supertypes.isEmpty()) query.setSuperType(String.join(",", supertypes));
			if (!types.isEmpty()) query.setType(String.join(",", types));
			if (!subtypes.isEmpty()) query.setSubType(String.join(",", subtypes));
			List<Card> cards = query.exec();

			//Stop if none found
			if (cards.isEmpty()) {
				loadMsg.get().editMessage(String.format(
						"<@%s>, No cards have been found with the type(s) you've supplied.",
						e.getAuthor().getId()
				)).submit();
				return;
			}

			//Draw a random card
			Card card = cards.get(new Random().nextInt(cards.size()));

			// Show card
			try {
				// Obtain stream
				InputStream artStream = new URL(card.getImageUrl()).openStream();
				// Upload art
				RequestFuture<Message> artMsg = e.getChannel().sendFile(artStream, "card.png", null).submit();
				// Attach text, card name & set name + code
				artMsg.get().editMessage(String.format("<@!%s> Here is your random **%s**:\n\n**%s**\n%s (%s)", e.getAuthor().getId(), joinedType, card.getName(), card.getSetName(), card.getSet())).submit();
				// Delete loading message
				loadMsg.get().delete().submit();
			} catch (IOException ex) {
				LOG.log(Level.SEVERE, "Could not upload random card art", ex);
				loadMsg.get().editMessage(String.format(
						"<@%s>, An error occurred while uploading the random card art! Please try again later.",
						e.getAuthor().getId()
				)).submit();
			}

		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "An error occurred getting a random card", ex);
		}
	}
}
