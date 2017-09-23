package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
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
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		// Let's check if there are any rulings
		if (card.getRulings().length == 0)
			return errorEmbedFormat("There are no rulings for **'%s'**.", card.getName()).get(0);

		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.setDescription("**Rulings**");
		for (MtgJsonCard.Ruling ruling : card.getRulings()) {
			String rulingText = ruling.getText().length() <= 1024 ? ruling.getText() : "Ruling is too large to be displayed. You can go read it on [Gatherer](" + card.getGathererUrl() + ").";
			eb.addField(ruling.getDate(), rulingText, false);
		}

		return eb.build();
	}
}
