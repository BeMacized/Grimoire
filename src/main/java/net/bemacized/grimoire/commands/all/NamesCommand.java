package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class NamesCommand extends CardBaseCommand {

	@Override
	public String name() {
		return "names";
	}

	@Override
	public String[] aliases() {
		return new String[]{"abroad", "foreign", "named"};
	}

	@Override
	public String description() {
		return "Retrieves all known foreign names for a card";
	}


	@Override
	protected String getInitialLoadLine() {
		return "Loading names...";
	}

	@Override
	protected void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Show the rulings
		EmbedBuilder eb = new EmbedBuilder()
				.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()))
				.setTitle(card.getName(), guildPreferences.getCardUrl(card))
				.setFooter(guildPreferences.showRequestersName() ? "Requested by " + e.getAuthor().getName() : null, null)
				.setDescription("**Foreign Names**\n");
		for (MtgJsonCard.ForeignName foreignName : card.getForeignNames())
			eb.addField(foreignName.getLanguage(), foreignName.getMultiverseid() > 0 ? String.format("[%s](%s)", foreignName.getName(), foreignName.getGathererUrl()) : foreignName.getName(), true);
		loadMsg.complete(eb.build());
	}
}
