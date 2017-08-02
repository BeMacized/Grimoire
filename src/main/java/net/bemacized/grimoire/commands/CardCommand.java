package net.bemacized.grimoire.commands;

import io.magicthegathering.javasdk.resource.Card;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.bemacized.grimoire.utils.CardUtils;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.MTGUtils;
import net.bemacized.grimoire.utils.SetUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CardCommand extends BaseCommand {
	@Override
	public String name() {
		return "card";
	}

	@Override
	public String[] aliases() {
		return new String[]{"art", "cardart"};
	}

	@Override
	public String description() {
		return "Fetch the full art of a card";
	}

	@Override
	public String paramUsage() {
		return "<card name>[|setcode/setname]";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Quit and error out if none provided
		if (args.length == 0) {
			e.getChannel().sendMessageFormat("<@%s>, please provide a card name to fetch art for!", e.getAuthor().getId()).submit();
			return;
		}

		// Obtain card name
		String[] split = String.join(" ", args).split("\\|");
		String cardname = split[0].trim();
		String setname = (split.length > 1 && !split[1].trim().isEmpty()) ? split[1].trim() : null;

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading card art...", true);

		// If a set(code) was provided, check its validity.
		MtgSet set = null;
		try {
			if (setname != null) set = SetUtils.getSet(setname);
		}
		// Handle too many results
		catch (SetUtils.TooManyResultsException ex) {
			loadMsg.finalizeFormat("<@%s>, There are too many results for a set named **'%s'**. Please be more specific.", e.getAuthor().getId(), setname);
			return;
		}
		// Handle multiple results
		catch (SetUtils.MultipleResultsException ex) {
			StringBuilder sb = new StringBuilder(String.format(
					"<@%s>, There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n",
					e.getAuthor().getId(),
					setname
			));
			for (MtgSet s : ex.getResults())
				sb.append(String.format(
						"\n:small_orange_diamond: %s _(%s)_",
						s.getName(),
						s.getCode())
				);
			loadMsg.finalize(sb.toString());
			return;
		}
		// Handle no results
		catch (SetUtils.NoResultsException e1) {
			loadMsg.finalizeFormat("<@%s>, I could not find a set with **'%s' as its code or name**.", e.getAuthor().getId(), setname);
			return;
		}

		// Retrieve cards
		Card card;
		try {
			// Retrieve all card variations
			List<Card> cards = CardUtils.getCards(cardname, (set != null) ? set.getCode() : null);
			// Find variation with art
			card = cards.stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty()).collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
				Collections.shuffle(collected);
				return collected.stream();
			})).findFirst().orElse(null);
			if (card == null) {
				if (set == null) {
					loadMsg.finalizeFormat("<@%s>, I could not find any art for **'%s'**. ", e.getAuthor().getId(), cardname);
					return;
				} else {
					card = (CardUtils.getCards(cardname, null).stream().filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty())).findFirst().orElse(null);
					e.getChannel().sendMessageFormat(
							"<@%s>, I could not find any art for **'%s'**%s",
							e.getAuthor().getId(),
							cardname,
							(card != null)
									? " in this specific set. There is however art available from another set. This will be shown instead."
									: "."
					).submit();
				}
			}
		}
		// Handle too many results
		catch (CardUtils.TooManyResultsException ex) {
			loadMsg.finalizeFormat("<@%s>, There are too many results for a card named **'%s'**. Please be more specific.", e.getAuthor().getId(), cardname);
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
			loadMsg.finalize(sb.toString());
			return;
		}
		// Handle no results
		catch (CardUtils.NoResultsException e1) {
			StringBuilder newMsg = new StringBuilder(String.format(
					"<@%s>, There are no results for a card named **'%s'**",
					e.getAuthor().getId(),
					cardname
			));
			if (set != null) newMsg.append(String.format(
					" in set **'%s (%s)'**",
					set.getName(),
					set.getCode()
			));
			loadMsg.finalize(newMsg.toString());
			return;
		}

		// Update load text
		loadMsg.setLineFormat("Loading card '%s' from set '%s, (%s)'...", card.getName(), card.getSetName(), card.getSet());

		// Build embed & show
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(card.getName(), (card.getMultiverseid() == -1) ? null : "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + card.getMultiverseid());
		eb.setDescription(String.format("%s (%s)", card.getSetName(), card.getSet()));
		eb.setImage(card.getImageUrl());
		eb.setColor(MTGUtils.colorIdentitiesToColor(card.getColorIdentity()));
		loadMsg.finalize(eb.build());
	}
}
