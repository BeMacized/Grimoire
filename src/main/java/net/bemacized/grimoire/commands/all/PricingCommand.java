package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PricingCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "pricing";
	}

	@Override
	public String[] aliases() {
		return new String[]{"price", "dollarydoos"};
	}

	@Override
	public String description() {
		return "Retrieves the current pricing for a card.";
	}

	@Override
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		EmbedBuilder eb = new EmbedBuilder(Grimoire.getInstance().getPricingProvider().getPricingEmbed(card, guildPreferences));

		//Send the message
		return eb.build();
	}
}
