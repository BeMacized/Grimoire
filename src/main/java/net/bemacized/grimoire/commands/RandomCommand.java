package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.*;
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
		return "<[supertype] [type] [subtype] [rarity]>...";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		List<String> supertypes = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<String> subtypes = new ArrayList<>();
		String rarity = null;

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Drawing random card...", true);

		// Retrieve all types
		List<String> allSupertypes = Grimoire.getInstance().getCards().getAllSupertypes();
		List<String> allTypes = Grimoire.getInstance().getCards().getAllTypes();
		List<String> allSubtypes = Grimoire.getInstance().getCards().getAllSubtypes();
		List<String> allRarities = Grimoire.getInstance().getCards().getAllRarities();
		Map<String, String> rarityAliases = new HashMap<String, String>() {{
			put("mythic", "Mythic Rare");
			put("basic", "Basic Land");
		}};

		// Extract filters
		for (String arg : args) {
			if (allSupertypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
			} else if (allTypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
			} else if (allSubtypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
			} else if (allRarities.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg)) || rarityAliases.containsKey(arg.toLowerCase())) {
				if (rarity != null) {
					loadMsg.finalizeFormat("<@%s>, please do not specify more than one rarity", e.getAuthor().getId(), arg);
					return;
				}
				rarity = rarityAliases.containsKey(arg.toLowerCase()) ? rarityAliases.get(arg.toLowerCase()) : arg.substring(0, 1).toUpperCase() + arg.substring(1).toLowerCase();
			} else {
				loadMsg.finalizeFormat("<@%s>, **'%s'** is neither a rarity, type, supertype or subtype. Please only specify valid properties.", e.getAuthor().getId(), arg);
				return;
			}
		}

		// Edit load message to reflect chosen types
		String joinedType = String.join(" ", new ArrayList<String>() {{
			addAll(supertypes);
			addAll(types);
			addAll(subtypes);
		}}.parallelStream().filter(Objects::nonNull).map(t -> t.substring(0, 1).toUpperCase() + t.substring(1, t.length())).collect(Collectors.toList()));
		loadMsg.setLineFormat("Drawing random %s%s...", rarity == null ? "" : rarity + " ", joinedType);

		//Find cards
		Cards.SearchQuery query = new Cards.SearchQuery();
		for (String supertype : supertypes) query = query.hasSupertype(supertype);
		for (String type : types) query = query.hasType(type);
		for (String subtype : subtypes) query = query.hasSubtype(subtype);
		if (rarity != null) query = query.isOfRarity(rarity);

		//Stop if none found
		if (query.isEmpty()) {
			loadMsg.finalizeFormat("<@%s>, No cards have been found with the properties you've supplied.", e.getAuthor().getId());
			return;
		}

		//Draw a random card
		Card card = query.get(new Random().nextInt(query.size()));

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setAuthor("Random " + joinedType, null, null);
		eb.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.finalize(eb.build());
	}
}
