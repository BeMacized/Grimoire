package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
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
	protected String getInitialLoadLine() {
		return "Loading oracle text...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e) {
		// Verify that text exists
		if (card.getText() == null) {
			sendErrorEmbedFormat(loadMsg, "The card **'%s'** has no oracle text available.", card.getName());
			return;
		}

		// Show the text
		loadMsg.complete(
				new EmbedBuilder()
						.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
						.setTitle(card.getName(), card.getGathererUrl())
						.addField("Oracle Text", Grimoire.getInstance().getEmojiParser().parseEmoji(card.getText(), e.getGuild()), false)
						.build()
		);
	}
}
