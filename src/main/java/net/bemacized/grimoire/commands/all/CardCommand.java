package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.CardBaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	protected MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e) {
		// Return card embed
		return card.getEmbed(e.getGuild(), guildPreferences);
	}


}
