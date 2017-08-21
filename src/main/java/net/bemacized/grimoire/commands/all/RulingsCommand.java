package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RulingsCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "rulings";
	}

	@Override
	public String[] aliases() {
		return new String[]{"rules", "ruling"};
	}

	@Override
	public String description() {
		return "Retrieves the current rulings of the specified card.";
	}

	@Override
	protected String getInitialLoadLine() {
		return "Loading rulings...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {

		// We have found it. Let's check if there are any rulings
		if (card.getRulings().length == 0) {
			sendErrorEmbedFormat(e.getChannel(), "There are no rulings for **'%s'**.", card.getName());
			return;
		}

		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.setDescription("**Rulings**");
		for (MtgJsonCard.Ruling ruling : card.getRulings())
			eb.addField(ruling.getDate(), ruling.getText(), false);

		loadMsg.complete(eb.build());
	}
}
