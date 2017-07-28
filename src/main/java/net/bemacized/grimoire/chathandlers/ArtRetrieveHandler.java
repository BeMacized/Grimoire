package net.bemacized.grimoire.chathandlers;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtRetrieveHandler extends ChatHandler {

	private final static Logger LOG = Logger.getLogger(ArtRetrieveHandler.class.getName());

	private final static int MAX_REQUESTS_PER_MESSAGE = 5;

	public ArtRetrieveHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// Find matches for <<CARD[|SET(CODE)]>> pattern.
		Pattern p = Pattern.compile("<<[^<|>]+([|][^<|>]+)?>>");
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

		// Stop here if no matches found
		if (requests.isEmpty()) {
			next.handle(e);
			return;
		}

		// Retrieve card
		requests.parallelStream().forEach(cardReq -> {
			try {
				handleCardRequest(cardReq, e);
			} catch (ExecutionException | InterruptedException ex) {
				LOG.log(Level.SEVERE, "Could not handle card art request", ex);
				e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred fetching your card data. Please notify my developer to fix me up!").submit();
			}
		});
	}

	private void handleCardRequest(RawCardRequest cardReq, MessageReceivedEvent e) throws ExecutionException, InterruptedException {
		LOG.info("Retrieving card art for card '" + cardReq.getCardName() + "'" + ((cardReq.getSet() == null) ? "" : " from set '" + cardReq.getSet() + "'"));

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
						"\n - %s _(%s)_",
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
			card = cards.stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty()).findFirst().orElse(null);
			if (card == null) {
				if (set == null) {
					loadMsg.get().editMessageFormat(
							"<@%s>, I could not find any art for **'%s'**. ",
							e.getAuthor().getId(),
							cardReq.getCardName()
					).submit();
					return;
				} else {
					card = (CardUtils.getCards(cardReq.getCardName(), null).stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty())).findFirst().orElse(null);
					e.getChannel().sendMessageFormat(
							"<@%s>, I could not find any art for **'%s'**%s",
							e.getAuthor().getId(),
							cardReq.getCardName(),
							(card != null)
									? " in this specific set. There is however art available from another set. This will be shown instead."
									: "."
					).submit();
				}
			}
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

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("**" + card.getName() + "**", (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		eb.setDescription(String.format("%s (%s)", card.getSetName(), card.getSet()));
		eb.setImage(card.getImageUrl());
		eb.setColor(CardUtils.colorIdentitiesToColor(card.getColorIdentity()));
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
}
