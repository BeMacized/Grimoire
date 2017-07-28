package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.Legality;
import net.bemacized.grimoire.utils.CardUtils;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CardCommand extends BaseCommand {
	@Override
	public String name() {
		return "card";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Fetch general card information about a card";
	}

	@Override
	public String paramUsage() {
		return "<card name>";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		try {
			// Obtain card name
			String cardname = String.join(" ", args);

			// Quit and error out if none provided
			if (cardname.isEmpty()) {
				e.getChannel().sendMessageFormat(
						"<@%s>, please provide a card name to fetch info for!",
						e.getAuthor().getId()
				).submit();
				return;
			}

			// Send load message
			RequestFuture<Message> loadMsg = e.getChannel().sendMessage("```\n" + "Fetching card info..." + "\n```").submit();

			// Retrieve card
			Card card;
			try {
				// Retrieve all card variations
				List<Card> cards = CardUtils.getCards(cardname);
				// Find variation with art
				card = cards.stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty()).findFirst().orElse(null);
				// Fallback to non-art card if needed
				if (card == null) card = CardUtils.getCard(cardname);
			}
			// Handle too many results
			catch (CardUtils.TooManyResultsException ex) {
				loadMsg.get().editMessageFormat(
						"<@%s>, There are too many results for a card named **'%s'**. Please be more specific.",
						e.getAuthor().getId(),
						cardname
				).submit();
				return;
			}
			// Handle multiple results
			catch (CardUtils.MultipleResultsException ex) {
				StringBuilder sb = new StringBuilder(String.format(
						"<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n",
						e.getAuthor().getId(),
						cardname
				));
				for (Card c : ex.getResults()) sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
				loadMsg.get().editMessage(sb.toString()).submit();
				return;
			}
			// Handle no results
			catch (CardUtils.NoResultsException e1) {
				loadMsg.get().editMessageFormat(
						"<@%s>, There are no results for a card named **'%s'**",
						e.getAuthor().getId(),
						cardname
				).submit();
				return;
			}

			// Update load text
			loadMsg.get().editMessageFormat(
					"```\n" + "Loading card '%s'..." + "\n```",
					card.getName()
			).submit();

			// Define emoji mapping
			Map<String, String> emojiMap = new HashMap<String, String>() {{
				put("W", "manaW");
				put("U", "manaU");
				put("B", "manaB");
				put("R", "manaR");
				put("G", "manaG");
				put("C", "manaC");
				put("W/U", "manaWU");
				put("U/B", "manaUB");
				put("B/R", "manaBR");
				put("R/G", "manaRG");
				put("G/W", "manaGW");
				put("W/B", "manaWB");
				put("U/R", "manaUR");
				put("B/G", "manaBG");
				put("R/W", "manaRW");
				put("G/U", "manaGU");
				put("2/W", "mana2W");
				put("2/U", "mana2U");
				put("2/B", "mana2B");
				put("2/R", "mana2R");
				put("2/G", "mana2G");
				put("WP", "manaWP");
				put("UP", "manaUP");
				put("BP", "manaBP");
				put("RP", "manaRP");
				put("GP", "manaGP");
				put("0", "manaZero");
				put("1", "manaOne");
				put("2", "manaTwo");
				put("3", "manaThree");
				put("4", "manaFour");
				put("5", "manaFive");
				put("6", "manaSix");
				put("7", "manaSeven");
				put("8", "manaEight");
				put("9", "manaNine");
				put("10", "manaTen");
				put("11", "manaEleven");
				put("12", "manaTwelve");
				put("13", "manaThirteen");
				put("14", "manaFourteen");
				put("15", "manaFifteen");
				put("16", "manaSixteen");
				put("20", "manaTwenty");
				put("T", "manaT");
				put("Q", "manaQ");
				put("S", "manaS");
				put("X", "manaX");
				put("E", "manaE");
			}};

			// We have found it. Let's construct the oracle text.
			String formats = (card.getLegalities() == null) ? "" : String.join(", ", Arrays.stream(card.getLegalities())
					.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
					.map(Legality::getFormat)
					.collect(Collectors.toList()));
			String rarities = String.join(", ", new CardUtils.CardSearchQuery().setExactName(card.getName()).exec().parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
			String printings = String.join(", ", card.getPrintings());
			String pat = parsePowerAndToughness(card.getPower(), card.getToughness());
			String msg = String.format(
					"<@%s>\n:black_square_button: **%s** %s\n:small_orange_diamond: %s%s\n\n%s\n%s%s%s\n%s",
					e.getAuthor().getId(),
					card.getName(),
					card.getManaCost(),
					(pat.isEmpty()) ? pat : "**" + pat + "** ",
					card.getType(),
					card.getText(),
					(!formats.isEmpty()) ? "\n**Formats:** " + formats : "",
					(!rarities.isEmpty()) ? "\n**Rarities:** " + rarities : "",
					(!printings.isEmpty()) ? "\n**Printings:** " + printings : "",
					(card.getMultiverseid() == -1) ? "" : "**Gatherer:** http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid()
			);

			// Convert emojis if guild supports it
			if (e.getGuild() != null) {
				for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
					if (msg.contains("{" + entry.getKey() + "}")) {
						Emote emote = e.getGuild().getEmotesByName(entry.getValue(), true).parallelStream().findAny().orElse(null);
						if (emote != null) msg = msg.replaceAll("\\{" + entry.getKey() + "\\}", emote.getAsMention());
					}
				}
			}

			// Show card
			try {
				if (card.getImageUrl() != null && !card.getImageUrl().isEmpty()) {
					// Obtain stream
					InputStream artStream = new URL(card.getImageUrl()).openStream();
					// Upload art
					RequestFuture<Message> artMsg = e.getChannel().sendFile(artStream, "card.png", null).submit();
					// Attach card name & set name + code
					artMsg.get().editMessage(msg).submit();
					// Delete loading message
					loadMsg.get().delete().submit();
				} else {
					loadMsg.get().editMessage(msg).submit();
				}

			} catch (IOException ex) {
				LOG.log(Level.SEVERE, "Could not upload card art + info", ex);
				loadMsg.get().editMessageFormat(
						"<@%s>, An error occurred while uploading the card info! Please try again later.",
						e.getAuthor().getId()
				).submit();
			}
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "An error occurred fetching card info", ex);
			e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred fetching card info. Please notify my developer to fix me up!").submit();
		}
	}

	private String parsePowerAndToughness(String power, String toughness) {
		if (power == null || toughness == null || power.isEmpty() || toughness.isEmpty()) return "";
		return parsePowerOrToughness(power) + "/" + parsePowerOrToughness(toughness);
	}

	private String parsePowerOrToughness(String value) {
		if (value == null) return null;
		switch(value) {
			case "*": return "\\*";
			default: return value;
		}
	}
}
