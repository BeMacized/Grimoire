package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Map;

public class LegalityCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "legality";
	}

	@Override
	public String[] aliases() {
		return new String[]{"legal", "illegal", "format", "formats", "legalities"};
	}

	@Override
	public String description() {
		return "Checks the legality of a card, for every known format";
	}

	@Override
	protected String getInitialLoadLine() {
		return "Loading legalities...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.setFooter("Requested by " + e.getAuthor().getName(),null)
				.setDescription("**Legality**\n");
		for (Map.Entry<String, ScryfallCard.Legality> entry : card.getLegalities().entrySet()) {
			eb.addField(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1).toLowerCase(), entry.getValue().getDisplayName(), true);
		}
		loadMsg.complete(eb.build());
	}
}
