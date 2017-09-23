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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
		String formatList = StringUtils.reverse(StringUtils.reverse(String.join(", ", formats.parallelStream().map(BanlistResolver::getName).map(n -> "`" + n + "`").collect(Collectors.toList()))).replaceFirst(" ,", StringUtils.reverse(" or ")));

		// Check if format was supplied
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please specify one of the following formats: " + formatList + ".");
			return;
		}

		// Validate format
		String format = WordUtils.capitalize(rawArgs.toLowerCase());
		BanlistResolver resolver = formats.parallelStream().filter(f -> f.name.equalsIgnoreCase(format.toLowerCase()) || Arrays.stream(f.getAliases()).map(String::toLowerCase).collect(Collectors.toList()).contains(format.toLowerCase())).findFirst().orElse(null);
		if (resolver == null) {
			sendErrorEmbed(e.getChannel(), "**\"" + format + "\"** is not a known format.\nPlease specify one of the following formats: " + formatList + ".");
			return;
		}

		// Send initial load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading " + format + " Banlist...", true, guildPreferences.disableLoadingMessages());

		// Construct embed
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":no_entry: " + format + " Banlist.", "http://magic.wizards.com/en/game-info/gameplay/rules-and-formats/banned-restricted");
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);

		// Retrieve banlist
		List<MtgCard> bannedCards = resolver.getBannedCards();
		if (bannedCards == null) {
			eb.addField("", "A problem occurred while trying to retrieve the banlist. Please try again later.", false);
		} else if (bannedCards.isEmpty()) {
			eb.addField("", "No cards are banned in **\"" + format + "\"**!", false);
		} else {
			// Sort list
			bannedCards.sort(Comparator.comparing(MtgCard::getName));
			// Set description
			eb.appendDescription("The following cards are banned in **\"" + format + "\"**:");
			// Filter out conspiracies
			if (bannedCards.parallelStream().filter(c -> c.getTypeLine() != null && c.getTypeLine().contains("Conspiracy")).count() >= 24) {
				bannedCards = bannedCards.parallelStream().filter(c -> !(c.getTypeLine() != null && c.getTypeLine().contains("Conspiracy"))).collect(Collectors.toList());
				eb.appendDescription("\n\nAll [Conspiracy](https://scryfall.com/search?q=t%3Aconspiracy) cards");
			}
			// Split list in 3 or more if needed and append fields properly.
			((bannedCards.size() < 3)
					? Stream.of(bannedCards, new ArrayList<MtgCard>(), new ArrayList<MtgCard>()).collect(Collectors.toList())
					: ListUtils.partition(bannedCards, (int) Math.ceil(((double) bannedCards.size()) / 3d)))
					.forEach(list -> {
						final StringBuilder sb = new StringBuilder();
						list.forEach(c -> sb.append("\n").append(c.getName()));
						eb.addField("", sb.toString().trim(), true);
					});
		}

		// Retrieve restricted cards
		List<MtgCard> restrictedCards = resolver.getRestrictedCards();
		if (restrictedCards == null) {
			eb.addField("", "A problem occurred while trying to retrieve the restricted list. Please try again later.", false);
		} else if (!restrictedCards.isEmpty()) {
			// Sort list
			restrictedCards.sort(Comparator.comparing(MtgCard::getName));
			// Add description
			eb.addField("", "The following cards are **Restricted** in **\"" + format + "\"**:", false);
			// Split list in 3 or more if needed and append fields properly.
			((restrictedCards.size() < 3)
					? Stream.of(restrictedCards, new ArrayList<MtgCard>(), new ArrayList<MtgCard>()).collect(Collectors.toList())
					: ListUtils.partition(restrictedCards, (int) Math.ceil(((double) restrictedCards.size()) / 3d)))
					.forEach(list -> {
						final StringBuilder sb = new StringBuilder();
						list.forEach(c -> sb.append("\n" + c.getName()));
						eb.addField("", sb.toString().trim(), true);
					});
		}

		// Send embed
		loadMsg.complete(eb.build());
	}

	private List<BanlistResolver> formats = new ArrayList<BanlistResolver>() {{
		add(new ScryfallResolver("standard", new String[0], "standard", true, true));
		add(new ScryfallResolver("modern", new String[0], "modern", true, true));
		add(new ScryfallResolver("legacy", new String[0], "legacy", true, true));
		add(new ScryfallResolver("vintage", new String[0], "vintage", true, true));
		add(new ScryfallResolver("commander", new String[]{"edh"}, "commander", true, true));
		add(new ScryfallResolver("future", new String[0], "future", true, true));
		add(new ScryfallResolver("pauper", new String[0], "pauper", true, true));
		add(new ScryfallResolver("frontier", new String[0], "frontier", true, true));
		add(new ScryfallResolver("penny", new String[]{"dreadful", "penny dreadful"}, "penny", true, true));
		add(new ScryfallResolver("1v1 commander", new String[]{"1v1"}, "1v1", true, true));
		add(new ScryfallResolver("duel commander", new String[]{"duel"}, "duel", true, true));

		add(new ScryfallResolver("canlander", new String[0], "vintage", true, false));
		add(new ScryfallResolver("auslander", new String[0], "vintage", true, false));

		add(new BanlistResolver("highlander", new String[]{"germanlander", "gerlander"}) {
			@Override
			List<MtgCard> getBannedCards() {
				return Grimoire.getInstance().getXLanderProvider().getHighlanderBanlist();
			}

			@Override
			List<MtgCard> getRestrictedCards() {
				return new ArrayList<>();
			}
		});
	}};

	private abstract class BanlistResolver {
		private String name;
		private String[] aliases;

		abstract List<MtgCard> getBannedCards();

		abstract List<MtgCard> getRestrictedCards();

		public BanlistResolver(String name, String[] aliases) {
			this.name = name;
			this.aliases = aliases;
		}

		public String getName() {
			return name;
		}

		public String[] getAliases() {
			return aliases;
		}
	}

	private class ScryfallResolver extends BanlistResolver {

		private String scryfallName;
		private boolean hasBanList;
		private boolean hasRestrictedList;

		public ScryfallResolver(String name, String[] aliases, String scryfallName, boolean hasBanList, boolean hasRestrictedList) {
			super(name, aliases);
			this.scryfallName = scryfallName;
			this.hasBanList = hasBanList;
			this.hasRestrictedList = hasRestrictedList;
		}

		@Override
		public List<MtgCard> getBannedCards() {
			if (!hasBanList) return new ArrayList<>();
			try {
				return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery("banned:" + scryfallName);
			} catch (ScryfallRetriever.ScryfallRequest.NoResultException e) {
				return new ArrayList<>();
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e1) {
				LOG.log(Level.SEVERE, "Unknown scryfall response", e1);
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e1) {
				LOG.log(Level.SEVERE, "Scryfall error response: " + e1.getError().getDetails(), e1);
			}
			return null;
		}

		@Override
		public List<MtgCard> getRestrictedCards() {
			if (!hasRestrictedList) return new ArrayList<>();
			try {
				return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery("restricted:" + scryfallName);
			} catch (ScryfallRetriever.ScryfallRequest.NoResultException e) {
				return new ArrayList<>();
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException e1) {
				LOG.log(Level.SEVERE, "Unknown scryfall response", e1);
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException e1) {
				LOG.log(Level.SEVERE, "Scryfall error response: " + e1.getError().getDetails(), e1);
			}
			return null;
		}
	}


}
