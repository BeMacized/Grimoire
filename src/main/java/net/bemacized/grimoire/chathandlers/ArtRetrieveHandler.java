package net.bemacized.grimoire.chathandlers;

import com.sun.istack.internal.NotNull;
import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
				String cardName = data[0];
				String set = (data.length > 1) ? data[1] : null;
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
			loadMsg.get().editMessage(String.format(
					"<@%s>, There are too many results for a set named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardReq.getSet()
			)).submit();
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
			loadMsg.get().editMessage(String.format(
					"<@%s>, I could not find a set with **'%s' as its code or name**.",
					e.getAuthor().getId(),
					cardReq.getSet()
			)).submit();
			return;
		}

		// Retrieve cards
		Card card;
		try {
			card = CardUtils.getCard(cardReq.getCardName(), (set == null) ? null : set.getCode());
		}
		// Handle too many results
		catch (CardUtils.TooManyResultsException ex) {
			loadMsg.get().editMessage(String.format(
					"<@%s>, There are too many results for a card named **'%s'**. Please be more specific.",
					e.getAuthor().getId(),
					cardReq.getCardName()
			)).submit();
			return;
		}
		// Handle multiple results
		catch (CardUtils.MultipleResultsException ex) {
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, There are multiple cards which match **'%s'**. Did you perhaps mean any of the following?\n",
					e.getAuthor().getId(),
					cardReq.getCardName()
			));
			for (Card c : ex.getResults()) sb.append(String.format("\n - %s", c.getName()));
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
		loadMsg.get().editMessage(String.format(
				"```\n" + "Loading card '%s' from set '%s, (%s)'..." + "\n```",
				card.getName(),
				card.getSetName(),
				card.getSet()
		)).submit();

		// Show card
		try {
			// Obtain stream
			InputStream artStream = new URL(card.getImageUrl()).openStream();
			// Upload art
			RequestFuture<Message> artMsg = e.getChannel().sendFile(artStream, "card.png", null).submit();
			// Attach card name & set name + code
			artMsg.get().editMessage(String.format("**%s**\n%s (%s)", card.getName(), card.getSetName(), card.getSet())).submit();
			// Delete loading message
			loadMsg.get().delete().submit();
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not upload card art", ex);
			loadMsg.get().editMessage(String.format(
					"<@%s>, An error occurred while uploading the card art! Please try again later.",
					e.getAuthor().getId()
			)).submit();
		}
	}


	private class RawCardRequest {

		private String cardName;
		private String set;

		RawCardRequest(@NotNull String cardName, String set) {
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
