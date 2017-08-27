package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.rules.TournamentRule;
import net.bemacized.grimoire.data.retrievers.TournamentRuleRetriever;

import java.util.ArrayList;
import java.util.List;

public class TournamentRuleProvider extends CachedProvider {

	private static final String COLLECTION = "TournamentRules";

	private List<TournamentRule> rules;

	public TournamentRuleProvider() {
		super();
		rules = new ArrayList<>();
	}

	public List<TournamentRule> getRules() {
		return new ArrayList<>(rules);
	}

	@Override
	boolean loadFromDB() {
		LOG.info("Attempting to load tournament from database...");
		rules.clear();
		Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).find().as(TournamentRule.class).forEach(rule -> rules.add(rule));
		boolean success = !rules.isEmpty();
		if (success) {
			LOG.info("Loaded tournament rules from database.");
		} else {
			LOG.info("Could not load tournament rules from database. Retrieving from web instead.");
		}
		return success;
	}

	@Override
	void saveToDB() {
		rules.forEach(rule -> Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).save(rule));
	}

	@Override
	public void loadFromSource() {
		this.rules = TournamentRuleRetriever.retrieve();
		saveToDB();
	}
}
