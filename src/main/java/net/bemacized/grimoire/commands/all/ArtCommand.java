package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
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
	protected String getInitialLoadLine() {
		return "Loading card art...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Check if an image is available
		if (card.getImageUrl() == null) {
			sendErrorEmbedFormat(loadMsg, "There is no known art for **'%s'**.", card.getName());
			return;
		}

		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(card.getName(), guildPreferences.getCardUrl(card));
		eb.setDescription(String.format("%s (%s)", card.getSet().getName(), card.getSet().getCode()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		if (guildPreferences.showRequestersName()) eb.setFooter("Requested by " + e.getAuthor().getName(), null);
		loadMsg.complete(eb.build());
	}
}
