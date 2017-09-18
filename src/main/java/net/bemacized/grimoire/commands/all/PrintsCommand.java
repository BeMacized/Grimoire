package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.ListUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrintsCommand extends CardBaseCommand {

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
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		if (card.getTypeLine().contains("Basic Land")) {
			return new EmbedBuilder()
					.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
					.setTitle(card.getName(), guildPreferences.getCardUrl(card))
					.setDescription("**Basic Lands** are available in every set!")
					.build();
		}
		// Show the sets
		List<String> sets;
		try {
			List<MtgCard> printings = card.getAllPrintings(-1);
			if (printings.size() <= 10) {
				sets = printings.parallelStream().map(c -> String.format(":small_orange_diamond: %s **(%s)** *[%s]*", c.getSet().getName(), c.getSet().getCode(), c.getSet().getReleasedAt() == null ? "Unknown" : c.getSet().getReleasedAt())).collect(Collectors.toList());
			} else {
				sets = printings.parallelStream().map(c -> String.format("**(%s)** %s\n*[%s]*", c.getSet().getCode(), c.getSet().getName(), c.getSet().getReleasedAt() == null ? "Unknown" : c.getSet().getReleasedAt())).collect(Collectors.toList());
			}
			sets = sets.parallelStream().filter(s -> s != null && !s.trim().isEmpty()).map(s -> s.trim()).collect(Collectors.toList());
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e1) {
			LOG.log(Level.WARNING, "Scryfall returned an error when retrieving prints: " + e1.getError().getDetails(), e1);
			return errorEmbed("I could not retrieve prints: " + e1.getError().getDetails()).get(0);
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e1) {
			LOG.log(Level.WARNING, "Scryfall returned an unknown error when retrieving prints.", e1);
			return errorEmbed("I could not retrieve prints because of an unknown error.").get(0);
		}
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card));

		((sets.size() <= 10)
				? Stream.of(sets).collect(Collectors.toList())
				: ListUtils.partition(sets, (int) Math.ceil(((double) sets.size()) / 3d)))
				.forEach(list -> {
					final StringBuilder sb = new StringBuilder();
					list.forEach(s -> sb.append("\n" + s));
					eb.addField("", sb.toString().trim(), true);
				});

		return eb.build();
	}

}
