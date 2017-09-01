package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.EmbedBuilder;
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
	protected String getInitialLoadLine() {
		return "Fetching prices...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Update load text
		loadMsg.setLineFormat("Loading price data for card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		EmbedBuilder eb = new EmbedBuilder(Grimoire.getInstance().getPricingProvider().getPricingEmbed(card, guildPreferences));
		eb.setFooter(guildPreferences.showRequestersName() ? "Requested by " + e.getAuthor().getName() : null, null);

		//Send the message
		loadMsg.complete(eb.build());
	}
}
