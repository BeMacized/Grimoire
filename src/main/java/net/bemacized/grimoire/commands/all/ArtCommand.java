package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ArtCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "art";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cardart"};
	}

	@Override
	public String description() {
		return "Fetch the full art of a card";
	}

	@Override
	protected EmbedButton[] getEnabledButtons() {
		return new EmbedButton[]{
			EmbedButton.NEXT,
			EmbedButton.PREVIOUS,
			EmbedButton.COLLAPSE,
			EmbedButton.EXPAND,
			EmbedButton.FLIP,
			EmbedButton.REMOVE
		};
	}

	@Override
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		// Return card embed
		return card.getArtEmbed(guildPreferences);
	}
}
