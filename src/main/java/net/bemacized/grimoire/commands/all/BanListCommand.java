package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BanListCommand extends BaseCommand {
	@Override
	public String name() {
		return "banlist";
	}

	@Override
	public String[] aliases() {
		return new String[]{"bl"};
	}

	@Override
	public String description() {
		return "Retrieve the banlist for a specific format";
	}

	@Override
	public String[] usages() {
		return new String[]{
				"<standard | modern | legacy | vintage | commander | future | pauper | frontier | penny | 1v1 | duel>"
		};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"standard",
				"vintage",
				"duel"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Check if format was supplied
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please specify one of the following formats: `standard` `modern` `legacy` `vintage` `commander` `future` `pauper` `frontier` `penny` `1v1` or `duel`.");
			return;
		}
		// Validate format
		String format = args[0].toLowerCase();
		if (!Stream.of("standard", "modern", "legacy", "vintage", "commander", "future", "pauper", "frontier", "penny", "1v1", "duel").collect(Collectors.toList()).contains(format)) {
			sendErrorEmbed(e.getChannel(), "**\"" + format + "\"** is not a known format.\nPlease specify one of the following formats: `standard` `modern` `legacy` `vintage` `commander` `future` `pauper` `frontier` `penny` `1v1` or `duel`.");
			return;
		}
		format = WordUtils.capitalize(format);

		// Send initial load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading " + format + " Banlist...", true, guildPreferences.disableLoadingMessages());

		// Construct embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":no_entry: " + format + " Banlist.", "http://magic.wizards.com/en/game-info/gameplay/rules-and-formats/banned-restricted");
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);

		try {
			// Retrieve banlist
			try {
				List<MtgCard> bannedCards = Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery("banned:" + format.toLowerCase());
				// Filter out conspiracies if needed
				boolean bannedConspiracies = bannedCards.parallelStream().anyMatch(c -> c.getTypeLine() != null && c.getTypeLine().contains("Conspiracy"));
				if (bannedConspiracies)
					bannedCards = bannedCards.parallelStream().filter(c -> !(c.getTypeLine() != null && c.getTypeLine().contains("Conspiracy"))).collect(Collectors.toList());
				// Sort list
				bannedCards.sort(Comparator.comparing(MtgCard::getName));
				eb.appendDescription("The following cards are banned in **\"" + format + "\"**:");
				// List card categories
				if (bannedConspiracies)
					eb.appendDescription("\n\nAll [Conspiracy](https://scryfall.com/search?q=t%3Aconspiracy) cards");
				// Split list in 3 or more if needed and append fields properly.
				((bannedCards.size() < 3)
						? Stream.of(bannedCards, new ArrayList<MtgCard>(), new ArrayList<MtgCard>()).collect(Collectors.toList())
						: ListUtils.partition(bannedCards, (int) Math.ceil(((double) bannedCards.size()) / 3d)))
						.forEach(list -> {
							final StringBuilder sb = new StringBuilder();
							list.forEach(c -> sb.append("\n" + c.getName()));
							eb.addField("", sb.toString().trim(), true);
						});
			} catch (ScryfallRetriever.ScryfallRequest.NoResultException e1) {
				eb.addField("", "No cards are banned in **\"" + format + "\"**!", false);
			}

			// Retrieve restricted cards
			try {
				List<MtgCard> restrictedCards = Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery("restricted:" + format.toLowerCase());
				if (!restrictedCards.isEmpty()) {
					restrictedCards.sort(Comparator.comparing(MtgCard::getName));
					eb.addField("", "The following cards are **Restricted** in **\"" + format + "\"**:", false);
					((restrictedCards.size() < 3)
							? Stream.of(restrictedCards, new ArrayList<MtgCard>(), new ArrayList<MtgCard>()).collect(Collectors.toList())
							: ListUtils.partition(restrictedCards, (int) Math.ceil(((double) restrictedCards.size()) / 3d)))
							.forEach(list -> {
								final StringBuilder sb = new StringBuilder();
								list.forEach(c -> sb.append("\n" + c.getName()));
								eb.addField("", sb.toString().trim(), true);
							});
				}
			} catch (ScryfallRetriever.ScryfallRequest.NoResultException ignored) {
			}
			loadMsg.complete(eb.build());
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e1) {
			LOG.log(Level.SEVERE, "Unknown scryfall response", e1);
			loadMsg.complete(errorEmbed("An unknown error ocurred when retrieving the list.").get(0));
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e1) {
			LOG.log(Level.SEVERE, "Scryfall error response: " + e1.getError().getDetails(), e1);
			loadMsg.complete(errorEmbed("Scryfall returned an error while retrieving the lists: " + e1.getError().getDetails()).get(0));
		}
	}
}
