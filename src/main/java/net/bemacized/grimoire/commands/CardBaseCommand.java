package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.bemacized.grimoire.utils.NavigableEmbed;
import net.bemacized.grimoire.utils.ReactionListener;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.logging.Level;

public abstract class CardBaseCommand extends BaseCommand {

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
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading card...", true, guildPreferences.disableLoadingMessages());

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
		MtgCard card = null;
		if (!guildPreferences.disableNonEnglishCardQueries()) {
			card = Grimoire.getInstance().getCardProvider().matchCardAnyLanguage(query, set, guildPreferences);
			if (card != null) results.add(card);
		}

		// Relay our query to scryfall
		try {
			results.addAll(Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(query + ((set != null) ? " s:" + set.getCode() : ""), 20));
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

		// Get a new view context
		final ViewContext context = getDefaultViewContext();

		// Define guild preferences for expanded view
		GuildPreferences expandedGuildPreferences = new GuildPreferences(guildPreferences);
		expandedGuildPreferences.setShowAuslanderPoints(true);
		expandedGuildPreferences.setShowCanlanderPoints(true);
		expandedGuildPreferences.setShowCardType(true);
		expandedGuildPreferences.setShowConvertedManaCost(true);
		expandedGuildPreferences.setShowFlavorText(true);
		expandedGuildPreferences.setShowLegalFormats(true);
		expandedGuildPreferences.setShowManaCost(true);
		expandedGuildPreferences.setShowMiscProperties(true);
		expandedGuildPreferences.setShowOracleText(true);
		expandedGuildPreferences.setShowPowerToughness(true);
		expandedGuildPreferences.setShowPrintedRarities(true);
		expandedGuildPreferences.setShowPrintings(true);
		expandedGuildPreferences.setShowThumbnail(true);

		// Construct navigable embed
		NavigableEmbed.Builder builder = new NavigableEmbed.Builder(e.getChannel());
		for (int x = 0; x < results.size(); x++) {
			MtgCard result = results.get(x);
			BiFunction<MtgCard, Integer, MessageEmbed> getEmbed = (c, resultIndex) -> {
				MessageEmbed embed;
				if (context.state == ViewContext.State.FULL_ART) {
					embed = c.getArtEmbed(guildPreferences);
				} else if (context.state == ViewContext.State.EXPANDED) {
					embed = c.getEmbed(e.getGuild(), expandedGuildPreferences);
				} else if (context.state == ViewContext.State.COLLAPSED) {
					embed = getEmbedForCard(c, guildPreferences, e);
				} else throw new NotImplementedException();
				EmbedBuilder eb = new EmbedBuilder(embed);
				if (results.size() > 1)
					eb.setFooter("Result " + (resultIndex + 1) + "/" + results.size(), null);
				return eb.build();
			};
			int finalIndex = x;
			builder.addEmbed(() -> getEmbed.apply(result, finalIndex));
			if (result.getOtherSide() != null)
				builder.addEmbed(() -> getEmbed.apply(result.getOtherSide(), finalIndex), x);
		}

		// Build the embed and send it
		NavigableEmbed navEb = builder.build();

		// Complete loading
		loadMsg.complete();

		// Add controls
		boolean removalPermitted = e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE);
		applyControls(navEb, context, removalPermitted);

		// Setup controls
		ReactionListener rl = new ReactionListener(Grimoire.getInstance().getDiscord(), navEb.getMessage(), false, 30 * 1000);
		rl.addController(e.getAuthor());
		rl.addResponse(EmbedButton.PREVIOUS.getIcon(), event -> {
			navEb.setY(0);
			if (navEb.getX() > 0) navEb.left();
			applyControls(navEb, context, removalPermitted);
		});
		rl.addResponse(EmbedButton.NEXT.getIcon(), event -> {
			navEb.setY(0);
			if (navEb.getX() < navEb.getWidth() - 1) navEb.right();
			applyControls(navEb, context, removalPermitted);
		});
		rl.addResponse(EmbedButton.FLIP.getIcon(), event -> {
			if (navEb.getY() > 0) navEb.up();
			else navEb.down();
			applyControls(navEb, context, removalPermitted);
		});
		rl.addResponse(EmbedButton.ART.getIcon(), event -> {
			context.setState(ViewContext.State.FULL_ART);
			navEb.render();
			applyControls(navEb, context, removalPermitted);
		});
		rl.addResponse(EmbedButton.EXPAND.getIcon(), event -> {
			context.setState(ViewContext.State.EXPANDED);
			navEb.render();
			applyControls(navEb, context, removalPermitted);
		});
		rl.addResponse(EmbedButton.COLLAPSE.getIcon(), event -> {
			context.setState(ViewContext.State.COLLAPSED);
			navEb.render();
			applyControls(navEb, context, removalPermitted);
		});
		if (removalPermitted) {
			rl.addResponse(EmbedButton.REMOVE.getIcon(), event -> {
				rl.disable();
				navEb.getMessage().delete().queue();
				e.getMessage().delete().queue();
			});
		}
	}

	private void applyControls(NavigableEmbed navEb, ViewContext context, boolean removalPermitted) {
		Message m = navEb.getMessage();
		if (EmbedButton.PREVIOUS.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.PREVIOUS.getIcon(), m, navEb.getWidth() > 1, removalPermitted);
		if (EmbedButton.NEXT.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.NEXT.getIcon(), m, navEb.getWidth() > 1, removalPermitted);
		if (EmbedButton.FLIP.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.FLIP.getIcon(), m, navEb.getHeightAt(navEb.getX()) > 1, removalPermitted);
		if (EmbedButton.ART.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.ART.getIcon(), m, context.getState() != ViewContext.State.FULL_ART, removalPermitted);
		if (EmbedButton.EXPAND.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.EXPAND.getIcon(), m, context.getState() == ViewContext.State.COLLAPSED, removalPermitted);
		if (EmbedButton.COLLAPSE.isEnabled(getEnabledButtons()))
			applyControl(EmbedButton.COLLAPSE.getIcon(), m, context.getState() != ViewContext.State.COLLAPSED, removalPermitted);
		if (removalPermitted && EmbedButton.COLLAPSE.isEnabled(getEnabledButtons())) {
			applyControl(EmbedButton.REMOVE.getIcon(), m, true, true);
		}
	}

	private void applyControl(String emote, Message message, boolean enabled, boolean removalEnabled) {
		boolean present = message.getReactions().parallelStream().anyMatch(r -> r.getEmote().getName().equals(emote));
		if (!present && enabled) {
			message.addReaction(emote).queue();
		} else if (present && !enabled && removalEnabled) {
			message.getReactions().parallelStream().filter(r -> r.getEmote().getName().equals(emote))
					.forEach(r -> {
						try {
							r.getUsers().submit().get().parallelStream().forEach(u -> r.removeReaction(u).queue());
						} catch (InterruptedException | ExecutionException e) {
							LOG.log(Level.SEVERE, "Could not remove specific reaction", e);
						}
					});
		}
	}

	protected abstract MessageEmbed getEmbedForCard(MtgCard card, GuildPreferences guildPreferences, MessageReceivedEvent e);

	protected ViewContext getDefaultViewContext() {
		return new ViewContext().setState(ViewContext.State.COLLAPSED);
	}

	protected EmbedButton[] getEnabledButtons() {
		return EmbedButton.values();
	}

	protected enum EmbedButton {
		PREVIOUS("⬅"),
		NEXT("➡"),
		FLIP("🔄"),
		REMOVE("❎"),
		ART("🖼"),
		EXPAND("➕"),
		COLLAPSE("➖");

		private String icon;

		EmbedButton(String icon) {
			this.icon = icon;
		}

		public String getIcon() {
			return icon;
		}

		public boolean isEnabled(EmbedButton[] enabledButtons) {
			return Arrays.asList(enabledButtons).contains(this);
		}

	}

	public static class ViewContext {

		private State state;

		public State getState() {
			return state;
		}

		public ViewContext setState(State state) {
			this.state = state;
			return this;
		}

		public enum State {
			COLLAPSED,
			FULL_ART,
			EXPANDED
		}
	}

}
