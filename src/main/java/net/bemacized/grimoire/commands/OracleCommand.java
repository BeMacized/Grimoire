package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import net.bemacized.grimoire.utils.CardUtils;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;

public class OracleCommand extends BaseCommand {

	@Override
	public String name() {
		return "oracle";
	}

	@Override
	public String[] aliases() {
		return new String[]{"cardtext"};
	}

	@Override
	public String description() {
		return "Retrieves the oracle text of a card.";
	}

	@Override
	public String paramUsage() {
		return "<card name>";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Obtain card name
		String cardname = String.join(" ", args);

		// Quit and error out if none provided
		if (cardname.isEmpty()) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, please provide a card name to check the oracle text for!",
					e.getAuthor().getId()
			)).submit();
			return;
		}

		// Retrieve card
		Card card;
		try {
			card = CardUtils.getCard(cardname);
		}
		// Handle too many results
		catch (CardUtils.TooManyResultsException ex) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, There are too many results for a card named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardname
			)).submit();
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
			e.getChannel().sendMessage(sb.toString()).submit();
			return;
		}
		// Handle no results
		catch (CardUtils.NoResultsException e1) {
			e.getChannel().sendMessage(String.format(
					"<@%s>, There are no results for a card named **'%s'**",
					e.getAuthor().getId(),
					cardname
			)).submit();
			return;
		}

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
		String msg = String.format(
				"<@%s>, Here is the oracle text for **'%s'**:\n\n%s",
				e.getAuthor().getId(),
				card.getName(),
				card.getText()
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

		// Show the text
		e.getChannel().sendMessage(msg).submit();
	}
}
