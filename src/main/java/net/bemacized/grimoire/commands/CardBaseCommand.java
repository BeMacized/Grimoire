package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.NavigableEmbed;
import net.bemacized.grimoire.utils.ReactionListener;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public abstract class CardBaseCommand extends BaseCommand {

	private static final String PREVIOUS_ICON = "⬅";
	private static final String NEXT_ICON = "➡";
	private static final String FLIP_ICON = "\uD83D\uDD04";
	private static final String REMOVE_ICON = "❎";

	@Override
	public String[] usages() {
		return new String[]{
				"<query>",
				"<query|set>"
		};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"Mighty Leap",
				"Mighty Leap | ORI",
				"Mighty Leap | Magic Origins"
		};
	}

	@Override
	public boolean scryfallSyntax() {
		return true;
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {

		// Quit and error out if none provided
		if (rawArgs.trim().length() == 0) {
			sendErrorEmbed(e.getChannel(), "Please provide a card name.");
			return;
		}

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), getInitialLoadLine(), true);

		// Obtain parameters
		String manualSet = null;
		final String query = (!rawArgs.matches("[^|]+?[|][^|]+")) ? rawArgs : rawArgs.split("[|]")[0].trim();
		if (!query.equals(rawArgs)) manualSet = rawArgs.split("[|]")[1].trim();

		// If a set was specified, check it
		ScryfallSet set = null;
		if (manualSet != null) {
			set = Grimoire.getInstance().getCardProvider().getSetByNameOrCode(manualSet);
			if (set == null) {
				sendErrorEmbedFormat(loadMsg, "I couldn't find any sets with **'%s'** as its name or code.", manualSet);
				return;
			}
		}

		// Start collecting results
		List<MtgCard> results = new ArrayList<>();

		// Check if we match with a mtgjson card name.
		MtgCard card = Grimoire.getInstance().getCardProvider().matchCardAnyLanguage(query, set, guildPreferences);
		if (card != null) results.add(card);

		// Relay our query to scryfall
		try {
			results.addAll(Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(query + ((set != null) ? " s:" + set.getCode() : ""), 1));
			if (results.isEmpty()) throw new ScryfallRetriever.ScryfallRequest.NoResultException();
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException ex) {
			LOG.log(Level.SEVERE, "An unknown error occurred with Scryfall", ex);
		} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException ex) {
			LOG.log(Level.SEVERE, "An error occurred with Scryfall", ex);
		} catch (ScryfallRetriever.ScryfallRequest.NoResultException e1) {
			if (results.isEmpty()) {
				sendErrorEmbedFormat(loadMsg, "There are no results for your query: **'%s'**.", query);
				return;
			}
		}

		// Remove duplicates in first 2 results
		if (card != null && results.size() >= 2 && results.get(0).sameCardAs(results.get(1))) {
			results.remove(1);
		}

		// Construct navigable embed
		NavigableEmbed.Builder builder = new NavigableEmbed.Builder(e.getChannel());
		for (int x = 0; x < results.size(); x++) {
			MtgCard result = results.get(x);
			builder.addEmbed(() -> getEmbedForCard(result, guildPreferences, e));
			if (result.getOtherSide() != null)
				builder.addEmbed(() -> getEmbedForCard(result.getOtherSide(), guildPreferences, e), x);
		}

		// Build the embed and send it
		NavigableEmbed navEb = builder.build();

		// Complete loading
		loadMsg.complete();

		// Add controls
		boolean removalDisabled = e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE);
		applyControls(navEb, removalDisabled);

		// Setup controls
		ReactionListener rl = new ReactionListener(Grimoire.getInstance().getDiscord(), navEb.getMessage(), false, 30 * 1000);
		rl.addController(e.getAuthor());
		rl.addResponse(PREVIOUS_ICON, (emoji, event) -> {
			navEb.setY(0);
			if (navEb.getX() > 0) navEb.left();
			applyControls(navEb, removalDisabled);
		});
		rl.addResponse(NEXT_ICON, (emoji, event) -> {
			navEb.setY(0);
			if (navEb.getX() < navEb.getWidth() - 1) navEb.right();
			applyControls(navEb, removalDisabled);
		});
		rl.addResponse(FLIP_ICON, (emoji, event) -> {
			if (navEb.getY() > 0) navEb.up();
			else navEb.down();
			applyControls(navEb, removalDisabled);
		});
		if (removalDisabled) {
			rl.addResponse(REMOVE_ICON, (emoji, event) -> {
				rl.disable();
				navEb.getMessage().delete().queue();
				e.getMessage().delete().queue();
			});
		}
	}

	protected abstract String getInitialLoadLine();

	protected abstract MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e);

	private void applyControls(NavigableEmbed navEb, boolean removalEnabled) {
		Message m = navEb.getMessage();
		applyControl(PREVIOUS_ICON, m, navEb.getWidth() > 1);
		applyControl(NEXT_ICON, m, navEb.getWidth() > 1);
		applyControl(FLIP_ICON, m, navEb.getHeightAt(navEb.getX()) > 1);
		if (removalEnabled) {
			applyControl(REMOVE_ICON, m, true);
		}
	}

	private void applyControl(String emote, Message message, boolean enabled) {
		boolean present = message.getReactions().parallelStream().anyMatch(r -> r.getEmote().getName().equals(emote));
		if (!present && enabled) {
			message.addReaction(emote).queue();
		} else if (present && !enabled) {
			message.getReactions().parallelStream().filter(r -> r.getEmote().getName().equals(emote))
					.forEach(r -> {
						try {
							r.getUsers().submit().get().parallelStream().forEach(u -> {
								r.removeReaction(u).queue();
							});
						} catch (InterruptedException | ExecutionException e) {
							LOG.log(Level.SEVERE, "Could not remove specific reaction", e);
						}
					});
		}
	}

}
