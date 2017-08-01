package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.model.models.ComprehensiveRule;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
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
		String ruleText;
		try {
			// Sourced from http://media.wizards.com/2016/docs/MagicCompRules_20160930.txt
			ruleText = IOUtils.toString(ComprehensiveRule.class.getResourceAsStream("/comprehensive_rules.txt"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load comprehensive rules!", e);
			return;
		}

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

		LOG.info("Loaded " + rules.size() + " comprehensive rules");
	}

}
