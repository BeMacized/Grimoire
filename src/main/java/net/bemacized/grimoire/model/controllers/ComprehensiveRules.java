package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.ComprehensiveRule;
import net.bemacized.grimoire.model.models.Dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		if (!d.retrieve()) {
			LOG.severe("Could not load comprehensive rules!");
			return;
		}
		String ruleText = d.getString();
		d.release();

		// Parse text
		try {
			// Extract rule texts
			ruleText = ruleText.substring(ruleText.indexOf("1. Game Concepts", ruleText.indexOf("Credits"))); //Cut off TOC, Intro
			ruleText = ruleText.substring(0, ruleText.indexOf("Glossary")); //Cut off glossary

			// Parse rule text
			Pattern rulePattern = Pattern.compile("([0-9]+[.])+([0-9]*[a-z])?([^\\n\\r]+[\n]?)+");
			Matcher matcher = rulePattern.matcher(ruleText);
			while (matcher.find()) {
				String p = matcher.group().split("\\s+")[0];
				rules.add(new ComprehensiveRule(p, matcher.group().substring(p.length()).trim()));
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not parse comprehensive rules!", e);
		}

		// Sort rules
		Collections.sort(rules);

		LOG.info("Loaded " + rules.size() + " comprehensive rules");
	}

}
