package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.MtgSet;
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
		return "<[supertype] [type] [subtype] [rarity] [set] [setcode]>...";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		List<String> supertypes = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<String> subtypes = new ArrayList<>();
		String rarity = null;
		MtgSet set = null;

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
			MtgSet tmpSet = Grimoire.getInstance().getSets().forceSingleByNameOrCode(arg);
			if (allSupertypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
			} else if (allTypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
			} else if (allSubtypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
			} else if (allRarities.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg)) || rarityAliases.containsKey(arg.toLowerCase())) {
				if (rarity != null) {
					sendEmbed(loadMsg, ":anger: Please do not specify more than one rarity.");
					return;
				}
				rarity = rarityAliases.containsKey(arg.toLowerCase()) ? rarityAliases.get(arg.toLowerCase()) : arg.substring(0, 1).toUpperCase() + arg.substring(1).toLowerCase();
			} else if (tmpSet != null) {
				if (set != null) {
					sendEmbed(loadMsg, ":anger: Please do not specify more than one set.");
					return;
				}
				set = tmpSet;
			} else {
				sendEmbedFormat(loadMsg, ":anger: **'%s'** is neither a rarity, set, setcode, type, supertype or subtype. Please only specify valid properties.", arg);
				return;
			}
		}

		// Edit load message to reflect chosen types
		String joinedType = String.join(" ", new ArrayList<String>() {{
			addAll(supertypes);
			addAll(types);
			addAll(subtypes);
		}}.parallelStream().filter(Objects::nonNull).map(t -> t.substring(0, 1).toUpperCase() + t.substring(1, t.length())).collect(Collectors.toList()));
		List<String> properties = new ArrayList<>();
		if (rarity != null) properties.add(rarity);
		if (joinedType != null && !joinedType.isEmpty()) properties.add(joinedType);
		if (set != null) properties.add(String.format("from set '%s (%s)'", set.getName(), set.getCode()));
		loadMsg.setLineFormat(
				"Drawing random **%s**...",
				String.join(" ", properties)
		);

		//Find cards
		Cards.SearchQuery query = new Cards.SearchQuery();
		for (String supertype : supertypes) query = query.hasSupertype(supertype);
		for (String type : types) query = query.hasType(type);
		for (String subtype : subtypes) query = query.hasSubtype(subtype);
		if (rarity != null) query = query.isOfRarity(rarity);
		if (set != null) query = query.inSet(set);

		//Stop if none found
		if (query.isEmpty()) {
			sendEmbed(loadMsg, ":anger: No cards have been found with the properties you've supplied.");
			return;
		}

		//Draw a random card
		Card card = query.get(new Random().nextInt(query.size()));

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		if (("Random " + String.join(" ", properties)).length() <= 128)
			eb.setAuthor("Random " + String.join(" ", properties), null, null);
		else eb.appendDescription("Random " + String.join(" ", properties) + "\n");
		eb.setTitle(card.getName(), card.getGathererUrl());
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.complete(eb.build());
	}
}
