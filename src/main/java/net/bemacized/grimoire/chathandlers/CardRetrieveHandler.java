package net.bemacized.grimoire.chathandlers;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.Legality;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CardRetrieveHandler extends ChatHandler {

	private final static Logger LOG = Logger.getLogger(CardRetrieveHandler.class.getName());

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;

	public CardRetrieveHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("<<[^$<|>]+([|][^<|>]+)?>>");
		Matcher m = p.matcher(e.getMessage().getContent());

		// Parse matches
		List<RawCardRequest> requests = new ArrayList<RawCardRequest>() {{
			for (int i = 0; i < MAX_REQUESTS_PER_MESSAGE && m.find(); i++) {
				String[] data = m.group().substring(2, m.group().length() - 2).split("[|]");
				String cardName = data[0].trim();
				String set = (data.length > 1) ? data[1].trim() : null;
				add(new RawCardRequest(cardName, set));
			}
		}};

		// Retrieve card
		requests.parallelStream().forEach(cardReq -> new Thread(() -> {
			try {
				handleCardRequest(cardReq, e);
			} catch (ExecutionException | InterruptedException ex) {
				LOG.log(Level.SEVERE, "Could not handle card art request", ex);
				e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred fetching your card data. Please notify my developer to fix me up!").submit();
			}
		}).start());

		next.handle(e);
	}

	private void handleCardRequest(RawCardRequest cardReq, MessageReceivedEvent e) throws ExecutionException, InterruptedException {
		// Send initial status message
		RequestFuture<Message> loadMsg = e.getChannel().sendMessage("```\n" + "Loading card..." + "\n```").submit();

		// If a set(code) was provided, check its validity.
		MtgSet set = null;
		try {
			if (cardReq.getSet() != null) set = SetUtils.getSet(cardReq.getSet());
		}
		// Handle too many results
		catch (SetUtils.TooManyResultsException ex) {
			loadMsg.get().editMessageFormat(
					"<@%s>, There are too many results for a set named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardReq.getSet()
			).submit();
			return;
		}
		// Handle multiple results
		catch (SetUtils.MultipleResultsException ex) {
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n",
					e.getAuthor().getId(),
					cardReq.getSet()
			));
			for (MtgSet s : ex.getResults())
				sb.append(String.format(
						"\n:small_orange_diamond: %s _(%s)_",
						s.getName(),
						s.getCode())
				);
			loadMsg.get().editMessage(sb.toString()).submit();
			return;
		}
		// Handle no results
		catch (SetUtils.NoResultsException e1) {
			loadMsg.get().editMessageFormat(
					"<@%s>, I could not find a set with **'%s' as its code or name**.",
					e.getAuthor().getId(),
					cardReq.getSet()
			).submit();
			return;
		}

		// Retrieve cards
		Card card;
		try {
			// Retrieve all card variations
			List<Card> cards = CardUtils.getCards(cardReq.getCardName(), (set != null) ? set.getCode() : null);
			// Find variation with art
			card = cards.stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty()).collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
				Collections.shuffle(collected);
				return collected.stream();
			})).findFirst().orElse(null);
			if (card == null) card = cards.get(new Random().nextInt(cards.size()));
		}
		// Handle too many results
		catch (CardUtils.TooManyResultsException ex) {
			loadMsg.get().editMessageFormat(
					"<@%s>, There are too many results for a card named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardReq.getCardName()
			).submit();
			return;
		}
		// Handle multiple results
		catch (CardUtils.MultipleResultsException ex) {
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n",
					e.getAuthor().getId(),
					cardReq.getCardName()
			));
			for (Card c : ex.getResults()) sb.append(String.format("\n:small_orange_diamond: %s", c.getName()));
			loadMsg.get().editMessage(sb.toString()).submit();
			return;
		}
		// Handle no results
		catch (CardUtils.NoResultsException e1) {
			StringBuilder newMsg = new StringBuilder(String.format(
					"<@%s>, There are no results for a card named **'%s'**",
					e.getAuthor().getId(),
					cardReq.getCardName()
			));
			if (set != null) newMsg.append(String.format(
					" in set **'%s (%s)'**",
					set.getName(),
					set.getCode()
			));
			loadMsg.get().editMessage(newMsg.toString()).submit();
			return;
		}

		// Update load text
		loadMsg.get().editMessageFormat(
				"```\n" + "Loading card '%s' from set '%s, (%s)'..." + "\n```",
				card.getName(),
				card.getSetName(),
				card.getSet()
		).submit();

		// We have found it. Let's construct the oracle text.
		String formats = (card.getLegalities() == null) ? "" : String.join(", ", Arrays.stream(card.getLegalities())
				.filter(l -> l.getLegality().equalsIgnoreCase("Legal"))
				.map(Legality::getFormat)
				.collect(Collectors.toList()));
		String rarities = String.join(", ", new CardUtils.CardSearchQuery().setExactName(card.getName()).exec().parallelStream().map(Card::getRarity).distinct().collect(Collectors.toList()));
		Card finalCard = card;
		String printings = String.join(", ", new ArrayList<String>() {{
			add("**" + finalCard.getSetName() + " (" + finalCard.getSet() + ")**");
			addAll(Arrays.stream(finalCard.getPrintings()).parallel().filter(s -> !s.equalsIgnoreCase(finalCard.getSet())).collect(Collectors.toList()));
		}});
		String pat = parsePowerAndToughness(card.getPower(), card.getToughness());

		//TODO: ENABLE AGAIN WHEN DISCORD FIXES EMOJI IN EMBED TITLES ---
		//		String title = card.getName()
		//				+ " "
		//				+ CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		String separateCost = "";
		//		if (title.length() > 256) {
		//			title = card.getName();
		//			separateCost = CardUtils.parseEmoji(e.getGuild(), card.getManaCost());
		//		}

		String title = card.getName();
		String separateCost = CardUtils.parseEmoji(e.getGuild(), card.getManaCost()) + " **(" + new DecimalFormat("##.###").format(card.getCmc()) + ")**";
		//TODO: ---END

		EmbedBuilder eb = new EmbedBuilder();
		eb.setThumbnail(card.getImageUrl());
		eb.setColor(CardUtils.colorIdentitiesToColor(card.getColorIdentity()));
		eb.setTitle(title, (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		if (!separateCost.isEmpty()) eb.appendDescription(separateCost + "\n");
		if (!pat.isEmpty()) eb.appendDescription("**" + pat + "** ");
		eb.appendDescription(card.getType());
		eb.appendDescription("\n\n");
		eb.appendDescription(CardUtils.parseEmoji(e.getGuild(), card.getText()));
		if (!formats.isEmpty()) eb.addField("Formats", formats, true);
		if (!rarities.isEmpty()) eb.addField("Rarities", rarities, true);
		if (!printings.isEmpty()) eb.addField("Printings", printings, true);

		// Show message
		loadMsg.get().editMessage(eb.build()).submit();
	}

	private class RawCardRequest {

		private String cardName;
		private String set;

		RawCardRequest(String cardName, String set) {
			this.cardName = cardName;
			this.set = set;
		}

		String getCardName() {
			return cardName;
		}

		String getSet() {
			return set;
		}
	}

	private String parsePowerAndToughness(String power, String toughness) {
		if (power == null || toughness == null || power.isEmpty() || toughness.isEmpty()) return "";
		return parsePowerOrToughness(power) + "/" + parsePowerOrToughness(toughness);
	}

	private String parsePowerOrToughness(String value) {
		if (value == null) return null;
		switch (value) {
			case "*":
				return "\\*";
			default:
				return value;
		}
	}
}
