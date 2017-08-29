package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.logging.Level;
import java.util.stream.Collectors;

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
	protected String getInitialLoadLine() {
		return "Loading card prints...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Show the sets
		String sets;
		try {
			sets = String.join("\n", card.getAllPrintings().parallelStream().map(c -> String.format(":small_orange_diamond: %s **(%s)** *[%s]*", c.getSet().getName(), c.getSet().getCode(), c.getSet().getReleasedAt() == null ? "Unknown" : c.getSet().getReleasedAt())).collect(Collectors.toList()));
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e1) {
			LOG.log(Level.WARNING, "Scryfall returned an error when retrieving prints: " + e1.getError().getDetails(), e1);
			sendErrorEmbed(loadMsg, "I could not retrieve prints: " + e1.getError().getDetails());
			return;
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e1) {
			LOG.log(Level.WARNING, "Scryfall returned an unknown error when retrieving prints.", e1);
			sendErrorEmbed(loadMsg, "I could not retrieve prints because of an unknown error.");
			return;
		}
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setFooter("Requested by " + e.getAuthor().getName(),null)
				.setTitle(card.getName(), guildPreferences.getCardUrl(card));
		String[] splits = MessageUtils.splitMessage(sets, 1000);
		for (int i = 0; i < splits.length; i++)
			eb.addField((i == 0) ? "Sets" : "", splits[i], false);
		loadMsg.complete(eb.build());
	}
}
