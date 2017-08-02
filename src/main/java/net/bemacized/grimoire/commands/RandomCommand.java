package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Cards;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.DecimalFormat;
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
		for (String supertype : supertypes) query = query.hasSupertype(supertype);
		for (String type : types) query = query.hasType(type);
		for (String subtype : subtypes) query = query.hasSubtype(subtype);

		//Stop if none found
		if (query.isEmpty()) {
			loadMsg.finalizeFormat("<@%s>, No cards have been found with the type(s) you've supplied.", e.getAuthor().getId());
			return;
		}

		//Draw a random card
		Card card = query.get(new Random().nextInt(query.size()));

		// Construct the data we need

		String formats = (card.getLegalities() == null) ? "" : String.join(", ", Arrays.stream(card.getLegalities())
				.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
				.map(Card.Legality::getFormat)
				.collect(Collectors.toList()));
		String rarities = String.join(", ", new Cards.SearchQuery().hasExactName(card.getName()).parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
		String printings = String.join(", ", new String[]{"**" + card.getSet().getName() + " (" + card.getSet().getCode() + ")**", String.join(", ", Arrays.stream(card.getPrintings()).parallel().filter(setCode -> !card.getSet().getCode().equalsIgnoreCase(setCode)).collect(Collectors.toList()))}).trim();
		if (printings.endsWith(",")) printings = printings.substring(0, printings.length() - 1);
		String pat = MTGUtils.parsePowerAndToughness(card.getPower(), card.getToughness());

		//TODO: ENABLE AGAIN WHEN DISCORD FIXES EMOJI IN EMBED TITLES ---
		//		String title = card.getName()
		//				+ " "
		//				+ CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		String separateCost = "";
		//		if (title.length() > 256) {
		//			title = card.getName();
		//			separateCost = CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		}

		String title = card.getName();
		String separateCost = MTGUtils.parseEmoji(e.getGuild(), card.getManaCost()) + " **(" + new DecimalFormat("##.###").format(card.getCmc()) + ")**";
		//TODO: ---END

		// Build the embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setAuthor(("Random " + joinedType).length() > 128 ? "Random" : "Random " + joinedType, (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid(), null);
		if (("Random " + joinedType).length() > 128) eb.appendDescription(joinedType + "\n");
		eb.setThumbnail(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		eb.setTitle(title, (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		if (!separateCost.isEmpty()) eb.appendDescription(separateCost + "\n");
		if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		eb.appendDescription(card.getType());
		eb.appendDescription("\n\n");
		eb.appendDescription(MTGUtils.parseEmoji(e.getGuild(), card.getText()));
		if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		if (!printings.isEmpty()) eb.addField("Printings", printings, true);

		// Show card
		loadMsg.finalize(eb.build());
	}
}
