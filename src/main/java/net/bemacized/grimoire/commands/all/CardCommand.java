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
		// Get card embed
		EmbedBuilder eb = new EmbedBuilder(card.getEmbed(e.getGuild(), guildPreferences));

		// Inject prices if preferred
		if (guildPreferences.showPriceOnCard()) {
			final MessageEmbed pricing = Grimoire.getInstance().getPricingProvider().getPricingEmbed(card, guildPreferences);
			String descLine = pricing.getDescription() != null ? pricing.getDescription().split("\n")[0] : "";
			final String set = (descLine.matches(".*?[(].+?[)]"))
					? descLine.substring(descLine.indexOf("(") + 1, descLine.indexOf(")", descLine.indexOf("(")))
					: "";
			boolean showSet = !set.equalsIgnoreCase(card.getSet().getCode()) && !set.isEmpty();
			pricing.getFields().forEach(f -> {
				List<String> desc = Stream.of(f.getValue().split("\n")).collect(Collectors.toList());
				desc.set(0, desc.get(0) + (showSet ? " _(" + set + ")_" : ""));
				eb.addField(f.getName(), String.join("\n", desc), f.isInline());
			});
		}

		// Build embed & show
		return eb.build();
	}


}
