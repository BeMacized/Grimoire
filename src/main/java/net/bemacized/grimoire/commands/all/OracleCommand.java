package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

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
	protected String getInitialLoadLine() {
		return "Loading oracle text...";
	}

	@Override
	protected void execForCards(List<MtgCard> cards, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		MtgCard card = cards.get(0);

		// Verify that text exists
		if (card.getPrintedText() == null) {
			sendErrorEmbedFormat(loadMsg, "The card **'%s'** has no oracle text available.", card.getName());
			return;
		}

		// Show the text
		loadMsg.complete(
				new EmbedBuilder()
						.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
						.setTitle(card.getName(), guildPreferences.getCardUrl(card))
						.setFooter(guildPreferences.showRequestersName() ? "Requested by " + e.getAuthor().getName() : null, null)
						.addField("Oracle Text", Grimoire.getInstance().getEmojiParser().parseEmoji(card.getPrintedText(), e.getGuild()), false)
						.build()
		);
	}
}
