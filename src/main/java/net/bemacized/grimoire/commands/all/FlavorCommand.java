package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class FlavorCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "flavor";
	}

	@Override
	public String[] aliases() {
		return new String[]{"flavortext"};
	}

	@Override
	public String description() {
		return "Retrieves the flavor text of a card.";
	}

	@Override
	protected String getInitialLoadLine() {
		return "Loading flavor text...";
	}

	@Override
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		// Verify that text exists
		if (card.getFlavorText() == null)
			return errorEmbedFormat("The card **'%s'** has no flavor text.", card.getName()).get(0);

		// Show the text
		return new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.addField("Flavor Text", "_" + Grimoire.getInstance().getEmojiParser().parseEmoji(card.getFlavorText(), e.getGuild()) + "_", false)
				.build();
	}

}
