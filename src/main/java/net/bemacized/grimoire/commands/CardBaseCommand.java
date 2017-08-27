package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
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

		// Check if we match with a non-english card name;
		MtgCard card = Grimoire.getInstance().getCardProvider().matchAnyCardName(query, set, guildPreferences);

		// If none found, relay our query to scryfall
		if (card == null) {
			try {
				List<MtgCard> results = Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(query + ((set != null) ? " s:" + set.getCode() : ""), 1);
				// Find exact match
				MtgCard directMatch = results.parallelStream().filter(c ->
						StringUtils.stripAccents(c.getName()).toLowerCase().replaceAll("[-',/?10\"!.:()&_4®]", "").equals(StringUtils.stripAccents(query).toLowerCase().replaceAll("[-',/?10\"!.:()&_4®]", ""))
				).findFirst().orElse(null);
				if (directMatch != null) card = directMatch;
					// If no exact match, just go with the next best thing
				else card = results.get(0);
			} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException ex) {
				LOG.log(Level.SEVERE, "An unknown error occurred with Scryfall", ex);
			} catch (ScryfallRetriever.ScryfallRequest.ScryfallErrorException ex) {
				LOG.log(Level.SEVERE, "An error occurred with Scryfall", ex);
			} catch (ScryfallRetriever.ScryfallRequest.NoResultException e1) {
				sendErrorEmbedFormat(loadMsg, "There are no results for your query: **'%s'**.", query);
				return;
			}
		}

		execForCard(card, loadMsg, e, guildPreferences);
	}

	protected abstract String getInitialLoadLine();

	protected abstract void execForCard(MtgCard card, LoadMessage loadMsg, MessageReceivedEvent e, GuildPreferences guildPreferences);
}
