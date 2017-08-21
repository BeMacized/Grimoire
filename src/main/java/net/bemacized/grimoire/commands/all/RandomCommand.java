package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.card.MtgSet;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.providers.CardProvider;
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
		return "<[supertype] [type] [subtype] [rarity] [set] [setcode] [layout]>...";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		List<String> supertypes = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<String> subtypes = new ArrayList<>();
		MtgCard.Rarity rarity = null;
		MtgSet set = null;
		MtgCard.Layout layout = null;

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Drawing random card...", true);

		// Retrieve all types
		List<String> allSupertypes = Grimoire.getInstance().getCardProvider().getAllSupertypes();
		List<String> allTypes = Grimoire.getInstance().getCardProvider().getAllTypes();
		List<String> allSubtypes = Grimoire.getInstance().getCardProvider().getAllSubtypes();

		Map<String, MtgCard.Rarity> rarityAliases = new HashMap<String, MtgCard.Rarity>() {{
			put("mythic", MtgCard.Rarity.MYTHIC_RARE);
			put("basic", MtgCard.Rarity.BASIC_LAND);
		}};

		// Extract filters
		for (String arg : args) {
			MtgSet tmpSet = Grimoire.getInstance().getCardProvider().forceSingleSetByNameOrCode(arg);
			if (allSupertypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
			} else if (allTypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
			} else if (allSubtypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
			} else if (Arrays.stream(MtgCard.Rarity.values()).parallel().anyMatch(r -> r.toString().equalsIgnoreCase(arg)) || rarityAliases.containsKey(arg.toLowerCase())) {
				if (rarity != null) {
					sendErrorEmbed(loadMsg, "Please do not specify more than one rarity.");
					return;
				}
				rarity = rarityAliases.containsKey(arg.toLowerCase()) ? rarityAliases.get(arg.toLowerCase()) : Arrays.stream(MtgCard.Rarity.values()).parallel().filter(r -> r.toString().equalsIgnoreCase(arg)).findFirst().orElse(null);
			} else if (Arrays.stream(MtgCard.Layout.values()).parallel().anyMatch(r -> r.toString().toLowerCase().startsWith(arg.toLowerCase()))) {
				if (layout != null) {
					sendErrorEmbed(loadMsg, "Please do not specify more than one layout.");
					return;
				}
				layout = Arrays.stream(MtgCard.Layout.values()).parallel().filter(r -> r.toString().toLowerCase().startsWith(arg.toLowerCase())).findFirst().orElse(null);
			} else if (tmpSet != null) {
				if (set != null) {
					sendErrorEmbed(loadMsg, "Please do not specify more than one set.");
					return;
				}
				set = tmpSet;
			} else {
				sendErrorEmbedFormat(loadMsg, "**'%s'** is neither a rarity, set, setcode, type, supertype or subtype. Please only specify valid properties.", arg);
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
		if (rarity != null) properties.add(rarity.toString());
		if (joinedType != null && !joinedType.isEmpty()) properties.add(joinedType);
		if (layout != null) properties.add(layout.toString());
		if (set != null) properties.add(String.format("from set '%s (%s)'", set.getName(), set.getCode()));
		loadMsg.setLineFormat(
				"Drawing random **%s**...",
				properties.isEmpty() ? "card" : String.join(" ", properties)
		);

		//Find cards
		CardProvider.SearchQuery query = new CardProvider.SearchQuery().inLanguage("English");
		if (layout != MtgCard.Layout.TOKEN) query = query.noTokens();
		if (layout != MtgCard.Layout.EMBLEM) query = query.noEmblems();
		for (String supertype : supertypes) query = query.hasSupertype(supertype);
		for (String type : types) query = query.hasType(type);
		for (String subtype : subtypes) query = query.hasSubtype(subtype);
		if (rarity != null) query = query.isOfRarity(rarity);
		if (set != null) query = query.inSet(set);
		if (layout != null) query = query.hasLayout(layout);

		//Stop if none found
		if (query.isEmpty()) {
			sendErrorEmbed(loadMsg, "No cards have been found with the properties you've supplied.");
			return;
		}

		//Draw a random card
		MtgCard card = query.get(new Random().nextInt(query.size()));

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		if (("Random " + String.join(" ", properties)).length() <= 128)
			eb.setAuthor("Random " + String.join(" ", properties), null, null);
		else eb.appendDescription("Random " + String.join(" ", properties) + "\n");
		eb.setTitle(card.getName(), guildPreferences.getCardUrl(card));
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.complete(eb.build());
	}
}
