package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.ComprehensiveRule;
import net.bemacized.grimoire.data.models.Definition;
import net.bemacized.grimoire.data.retrievers.ComprehensiveRuleRetriever;

import java.util.ArrayList;
import java.util.List;

public class ComprehensiveRuleProvider extends Provider {

	private static final String RULE_COLLECTION = "ComprehensiveRules";
	private static final String DEFINITION_COLLECTION = "Definitions";

	private List<ComprehensiveRule> rules;
	private List<Definition> definitions;

	public ComprehensiveRuleProvider() {
		rules = new ArrayList<>();
		definitions = new ArrayList<>();
	}

	public List<ComprehensiveRule> getRules() {
		return rules;
	}

	public List<Definition> getDefinitions() {
		return definitions;
	}

	@Override
	boolean loadFromDB() {
		LOG.info("Attempting to load comprehensive rules and definitions from database...");
		rules.clear();
		definitions.clear();
		Grimoire.getInstance().getDBManager().getJongo().getCollection(RULE_COLLECTION).find().as(ComprehensiveRule.class).forEach(rule -> rules.add(rule));
		Grimoire.getInstance().getDBManager().getJongo().getCollection(DEFINITION_COLLECTION).find().as(Definition.class).forEach(definition -> definitions.add(definition));
		boolean success = !rules.isEmpty() && !definitions.isEmpty();
		if (success) {
			LOG.info("Loaded comprehensive rules and definitions from database.");
		} else {
			LOG.info("Could not load comprehensive rules and definitions from database. Retrieving from web instead.");
		}
		return success;
	}

	@Override
	void saveToDB() {
		rules.forEach(rule -> Grimoire.getInstance().getDBManager().getJongo().getCollection(RULE_COLLECTION).save(rule));
		definitions.forEach(definition -> Grimoire.getInstance().getDBManager().getJongo().getCollection(DEFINITION_COLLECTION).save(definition));
	}

	@Override
	public void loadFromSource() {
		ComprehensiveRuleRetriever.Result result = ComprehensiveRuleRetriever.retrieveData();
		this.definitions = result.getDefinitions();
		this.rules = result.getComprehensiveRules();
		saveToDB();
	}
}
