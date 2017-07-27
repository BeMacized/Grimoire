package net.bemacized.grimoire.parsers;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComprehensiveRules {

	private static final Logger LOG = Logger.getLogger(ComprehensiveRules.class.getName());

	private Map<String, String> rules;
	private Map<String, String> definitions;

	public ComprehensiveRules() {
		// Initialize fields
		rules = new HashMap<>();
		definitions = new HashMap<>();

		// Fetch text
		String ruleText;
		try {
			ruleText = IOUtils.toString(getClass().getResourceAsStream("/comprehensive_rules.txt"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load comprehensive rules!", e);
			return;
		}

		// Parse text
		try {
			// Split into glossary and rule texts
			ruleText = ruleText.substring(ruleText.indexOf("1. Game Concepts", ruleText.indexOf("Credits"))); //Cut off TOC & Intro
			String glossaryText = ruleText.substring(ruleText.indexOf("Glossary"), ruleText.indexOf("Credits")).substring(ruleText.indexOf("\n"));
			ruleText = ruleText.substring(0, ruleText.indexOf("Glossary"));

			// Parse rule text
			{
				Pattern rulePattern = Pattern.compile("[0-9]{3}[.][0-9]([.]|[a-z])([^\\n\\r]+[\n]?)+");
				Matcher matcher = rulePattern.matcher(ruleText);
				while (matcher.find()) {
					String p = matcher.group().split("\\s+")[0];
					rules.put(p, matcher.group().substring(p.length()).trim());
				}
			}

			// Parse glossary text
			{
				String keyword = null;
				StringBuilder sb = new StringBuilder();
				for (String line : glossaryText.split("\\n")) {
					if (keyword == null) {
						if (!line.trim().isEmpty()) keyword = line.trim();
						continue;
					}
					if (!line.isEmpty()) {
						if (!sb.toString().isEmpty()) sb.append("\n");
						sb.append(line.trim());
					} else {
						definitions.put(keyword, sb.toString());
						keyword = null;
						sb = new StringBuilder();
					}
				}
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not parse comprehensive rules!", e);
		}
	}

	public Map<String, String> getRules() {
		return new HashMap<>(rules);
	}

	public Map<String, String> getDefinitions() {
		return new HashMap<>(definitions);
	}
}
