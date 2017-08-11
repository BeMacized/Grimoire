package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.ComprehensiveRule;
import net.bemacized.grimoire.model.models.Dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComprehensiveRules {

	private static final Logger LOG = Logger.getLogger(ComprehensiveRules.class.getName());

	private List<ComprehensiveRule> rules;

	public ComprehensiveRules() {
		rules = new ArrayList<>();
	}

	public List<ComprehensiveRule> getRules() {
		return rules;
	}

	public void load() {
		rules.clear();
		LOG.info("Loading comprehensive rules...");

		// Fetch text
		Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("CR_DOC");
		String ruleText = d.getString();
		if (ruleText == null) {
			LOG.severe("Could not load comprehensive rules!");
			return;
		}
		d.release(); // Release dependency from memory after loading

		// Parse text
		try {
			// Extract rule texts
			ruleText = ruleText.substring(ruleText.indexOf("1. Game Concepts", ruleText.indexOf("Credits"))); //Cut off TOC, Intro
			ruleText = ruleText.substring(0, ruleText.indexOf("Glossary")); //Cut off glossary

			// Split texts
			String[] splitRules = Arrays.stream(ruleText.split("[\\r\\n]{3,}")).map(String::trim).collect(Collectors.toList()).toArray(new String[0]);

			for (String rule : splitRules) {
				Pattern idPattern = Pattern.compile("([0-9]+[.])+([0-9]+([a-z]|\\.))?");
				Matcher matcher = idPattern.matcher(rule);
				if (!matcher.find()) continue;
				String id = matcher.group();
				rules.add(new ComprehensiveRule(id, rule.substring(id.length()).trim()));
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not parse comprehensive rules!", e);
		}

		// Sort rules
		Collections.sort(rules);

		LOG.info("Loaded " + rules.size() + " comprehensive rules");
	}

}
