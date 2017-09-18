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
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {

		// Check if an image is available
		if (card.getImageUrl() == null)
			return errorEmbedFormat("There is no known art for **'%s'**.", card.getName()).get(0);

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(card.getName(), guildPreferences.getCardUrl(card));
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		return eb.build();
	}
}
