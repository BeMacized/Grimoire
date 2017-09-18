package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class OracleCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "oracle";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cardtext"};
	}

	@Override
	public String description() {
		return "Retrieves the oracle text of a card.";
	}

	@Override
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		// Verify that text exists
		if (card.getText() == null) {
			return errorEmbedFormat("The card **'%s'** has no oracle text available.", card.getName()).get(0);
		}

		// Show the text
		return new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.addField("Oracle Text", Grimoire.getInstance().getEmojiParser().parseEmoji(card.getText(), e.getGuild()), false)
				.build();
	}

}
