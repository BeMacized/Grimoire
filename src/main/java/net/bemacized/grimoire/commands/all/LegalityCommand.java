package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
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
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.setDescription("**Legality**\n");
		for (Map.Entry<String, ScryfallCard.Legality> entry : card.getLegalities().entrySet()) {
			eb.addField(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1).toLowerCase(), entry.getValue().getDisplayName(), true);
		}
		return eb.build();
	}
}
