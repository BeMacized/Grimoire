package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
		return "Show a random card of a certain type.";
	}

	@Override
	public String[] usages() {
		return new String[]{"[supertype] [type] [subtype] [rarity] [set] [setcode]"};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"",
				"legendary creature",
				"C17 mythic",
				"rare artifact"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		List<String> supertypes = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<String> subtypes = new ArrayList<>();
		ScryfallCard.Rarity rarity = null;
		ScryfallSet set = null;

		// Send load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Drawing random card...", true);

		// Retrieve all types
		List<String> allSupertypes = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getAllSupertypes();
		List<String> allTypes = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getAllTypes();
		List<String> allSubtypes = Grimoire.getInstance().getCardProvider().getMtgJsonProvider().getAllSubtypes();

		// Extract filters
		for (String arg : args) {
			if (allSupertypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!supertypes.contains(arg.toLowerCase())) supertypes.add(arg.toLowerCase());
			} else if (allTypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!types.contains(arg.toLowerCase())) types.add(arg.toLowerCase());
			} else if (allSubtypes.parallelStream().anyMatch(t -> t.equalsIgnoreCase(arg))) {
				if (!subtypes.contains(arg.toLowerCase())) subtypes.add(arg.toLowerCase());
			} else if (Arrays.stream(MtgJsonCard.Rarity.values()).parallel().anyMatch(r -> r.toString().equalsIgnoreCase(arg))) {
				if (rarity != null) {
					sendErrorEmbed(loadMsg, "Please do not specify more than one rarity.");
					return;
				}
				rarity = Arrays.stream(ScryfallCard.Rarity.values()).parallel().filter(r -> r.toString().equalsIgnoreCase(arg)).findFirst().orElse(null);
			} else {
				ScryfallSet tmpSet = Grimoire.getInstance().getCardProvider().getSetByNameOrCode(arg);
				if (tmpSet != null) {
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
		if (set != null) properties.add(String.format("from set '%s (%s)'", set.getName(), set.getCode()));
		loadMsg.setLineFormat(
				"Drawing random **%s**...",
				properties.isEmpty() ? "card" : String.join(" ", properties)
		);

		//Find cards
		List<String> query = new ArrayList<>();
		supertypes.forEach(t -> query.add("t:" + t));
		types.forEach(t -> query.add("t:" + t));
		subtypes.forEach(t -> query.add("t:" + t));
		if (rarity != null) query.add("r:" + rarity.name().toLowerCase());
		if (set != null) query.add("s:" + set.getCode());

		MtgCard card;
		try {
			card = Grimoire.getInstance().getCardProvider().getRandomCardByScryfallQuery("++" + String.join(" ", query));
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException ex) {
			LOG.log(Level.WARNING, "Scryfall gave an error when trying to get a random card.", ex);
			sendErrorEmbed(loadMsg, "Could not get random card: " + ex.getError().getDetails());
			return;
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException ex) {
			LOG.log(Level.WARNING, "Scryfall gave an unknown response when trying to get a random card.", ex);
			sendErrorEmbed(loadMsg, "An unknown error occurred when trying to get a random card.");
			return;
		} catch (ScryfallRetriever.ScryfallRequest.NoResultException e1) {
			sendErrorEmbed(loadMsg, "No cards have been found with the properties you've supplied.");
			return;
		}

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		if (("Random " + String.join(" ", properties)).length() <= 128)
			eb.setAuthor("Random " + String.join(" ", properties), null, null);
		else eb.appendDescription("Random " + String.join(" ", properties) + "\n");
		eb.setTitle(card.getName(), guildPreferences.getCardUrl(card));
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		if (guildPreferences.showRequestersName()) eb.setFooter("Requested by " + e.getAuthor().getName(), null);
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.complete(eb.build());
	}
}
