package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgSet;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.stream.Collectors;

public abstract class SetBaseCommand extends BaseCommand {

	private final static int MAX_SET_ALTERNATIVES = 15;

	@Override
	public String paramUsage() {
		return "<set>";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Quit and error out if none provided
		if (args.length == 0) {
			sendEmbed(e.getChannel(), ":anger: Please provide a set name.");
			return;
		}

		// Obtain card name
		String setname = String.join(" ", args);

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), getInitialLoadLine(), true);

		// If a set(code) was provided, check its validity.
		MtgSet set;
		try {
			set = setname != null ? Grimoire.getInstance().getCardProvider().getSingleSetByNameOrCode(setname) : null;
			if (set == null && setname != null) {
				sendEmbedFormat(loadMsg, ":anger: No set found with **'%s'** as its code or name.", setname);
				return;
			}
		} catch (CardProvider.MultipleSetResultsException ex) {
			if (ex.getResults().size() > MAX_SET_ALTERNATIVES)
				sendEmbedFormat(loadMsg, ":anger: There are too many results for a set named **'%s'**. Please be more specific.", setname);
			else
				sendEmbedFormat(
						loadMsg,
						"There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n\n%s",
						setname,
						String.join("\n", ex.getResults().parallelStream().map(s -> String.format(":small_orange_diamond: %s _(%s)_", s.getName(), s.getCode())).collect(Collectors.toList()))
				);
			return;
		}

		execForSet(set, loadMsg, e);
	}

	protected abstract String getInitialLoadLine();

	protected abstract void execForSet(MtgSet set, LoadMessage loadMsg, MessageReceivedEvent e);
}
