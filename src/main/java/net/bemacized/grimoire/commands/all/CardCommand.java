package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CardCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "card";
	}

	@Override
	public String[] aliases() {
		return new String[]{"c"};
	}

	@Override
	public String description() {
		return "Fetch information for a card";
	}

	@Override
	protected String getInitialLoadLine() {
		return "Loading card data...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSet().getName(), card.getSet().getCode());

		// Build embed & show
		loadMsg.complete(card.getEmbed(e.getGuild(), guildPreferences));

		// Queue price if needed
		if (guildPreferences.showPriceOnCard())
			e.getChannel().sendMessage(Grimoire.getInstance().getPricingProvider().getPricingEmbed(card, guildPreferences)).queue();
	}
}
