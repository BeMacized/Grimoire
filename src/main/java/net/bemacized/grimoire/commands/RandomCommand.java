package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
		List<String> supertypes = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<String> subtypes = new ArrayList<>();

		// Make sure we have at least one argument
		if (args.length < 1) {
			e.getChannel().sendMessageFormat("<@%s>, Please specify one or more (sub/super)types.", e.getAuthor().getId()
			).submit();
			return;
		}

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Drawing random card...", true);

		// Retrieve all types
		List<String> allSupertypes = Grimoire.getInstance().getCards().getAllSupertypes();
		List<String> allTypes = Grimoire.getInstance().getCards().getAllTypes();
		List<String> allSubtypes = Grimoire.getInstance().getCards().getAllSubtypes();

		// Extract types
		for (String arg : args) {
			if (allSupertypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
			} else if (allTypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
			} else if (allSubtypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
			} else {
				loadMsg.finalizeFormat("<@%s>, **'%s'** is neither a type, supertype or subtype. Please only specify valid types.", e.getAuthor().getId(), arg);
				return;
			}
		}

		// Edit load message to reflect chosen types
		String joinedType = String.join(" ", new ArrayList<String>() {{
			addAll(supertypes);
			addAll(types);
			addAll(subtypes);
		}}.parallelStream().filter(Objects::nonNull).map(t -> t.substring(0, 1).toUpperCase() + t.substring(1, t.length())).collect(Collectors.toList()));
		loadMsg.setLineFormat("Drawing random %s...", joinedType);

		//Find cards
		Cards.SearchQuery query = new Cards.SearchQuery();
		supertypes.forEach(query::hasSupertype);
		types.forEach(query::hasType);
		subtypes.forEach(query::hasSubtype);

		//Stop if none found
		if (query.isEmpty()) {
			loadMsg.finalizeFormat("<@%s>, No cards have been found with the type(s) you've supplied.", e.getAuthor().getId());
			return;
		}

		//Draw a random card
		Card card = query.get(new Random().nextInt(query.size()));

		// Show card
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(("Random " + joinedType).length() > 128 ? "Random" : "Random " + joinedType);
		if (("Random " + joinedType).length() > 128) eb.appendDescription(joinedType + "\n");
		eb.appendDescription("**" + card.getName() + "**");
		eb.appendDescription(String.format("\n%s (%s)", card.getSet().getName(), card.getSet().getName()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.finalize(eb.build());
	}
}
