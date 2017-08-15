package net.bemacized.grimoire.data.retrievers;

import net.bemacized.grimoire.data.models.ComprehensiveRule;
import net.bemacized.grimoire.data.models.Definition;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComprehensiveRuleRetriever {

	private final static Logger LOG = Logger.getLogger(ComprehensiveRuleRetriever.class.getName());
	private final static String SOURCE = "http://media.wizards.com/2017/downloads/MagicCompRules_20170707.txt";
	private final static String CHARSET = "windows-1252";

	public static Result retrieveData() {
		LOG.info("Loading comprehensive rules & definitions...");

		List<ComprehensiveRule> rules = new ArrayList<>();
		List<Definition> definitions = new ArrayList<>();

		String ruleText;
		try {
			ruleText = IOUtils.toString(new URL(SOURCE), CHARSET);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not retrieve comprehensive rules!", e);
			return new Result(rules, definitions);
		}

		// Parse text
		try {
			// Extract rule texts
			ruleText = ruleText.substring(ruleText.indexOf("1. Game Concepts", ruleText.indexOf("Credits"))); //Cut off TOC, Intro
			String glossaryText = ruleText.substring(ruleText.indexOf("Glossary"), ruleText.indexOf("Credits")).substring(ruleText.indexOf("\n"));
			ruleText = ruleText.substring(0, ruleText.indexOf("Glossary")); //Cut off glossary

			// Get rules
			{
				String[] splitRules = Arrays.stream(ruleText.split("[\\r\\n]{3,}")).map(String::trim).collect(Collectors.toList()).toArray(new String[0]);
				for (String rule : splitRules) {
					Pattern idPattern = Pattern.compile("([0-9]+[.])+([0-9]+([a-z]|\\.))?");
					Matcher matcher = idPattern.matcher(rule);
					if (!matcher.find()) continue;
					String id = matcher.group();
					rules.add(new ComprehensiveRule(id, rule.substring(id.length()).trim()));
				}
			}

			// Get definitions
			{
				String keyword = null;
				StringBuilder sb = new StringBuilder();
				for (String line : glossaryText.split("\\n")) {
					if (keyword == null) {
						if (!line.trim().isEmpty()) keyword = line.trim();
						continue;
					}
					if (!line.trim().isEmpty()) {
						if (!sb.toString().isEmpty()) sb.append("\n");
						sb.append(line.trim());
					} else {
						definitions.add(new Definition(keyword, sb.toString()));
						keyword = null;
						sb = new StringBuilder();
					}
				}
				if (keyword != null) {
					definitions.add(new Definition(keyword, sb.toString()));
				}
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not parse comprehensive rules!", e);
			return new Result(rules, definitions);
		}

		// Sort rules
		Collections.sort(rules);

		LOG.info("Retrieved " + rules.size() + " comprehensive rules & " + definitions.size() + " definitions");
		return new Result(rules, definitions);
	}

	public static class Result {
		private List<ComprehensiveRule> comprehensiveRules;
		private List<Definition> definitions;

		public Result(List<ComprehensiveRule> comprehensiveRules, List<Definition> definitions) {
			this.comprehensiveRules = comprehensiveRules;
			this.definitions = definitions;
		}

		public List<ComprehensiveRule> getComprehensiveRules() {
			return comprehensiveRules;
		}

		public List<Definition> getDefinitions() {
			return definitions;
		}
	}
}
