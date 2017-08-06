package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.model.models.ComprehensiveRule;
import net.bemacized.grimoire.model.models.Definition;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Definitions {

	private static final Logger LOG = Logger.getLogger(Definitions.class.getName());

	private List<Definition> definitions;

	public Definitions() {
		definitions = new ArrayList<>();
	}

	public List<Definition> getDefinitions() {
		return definitions;
	}

	public void load() {
		definitions.clear();
		LOG.info("Loading definitions...");

		// Fetch text
		String glossaryText;
		try {
			// Sourced from http://media.wizards.com/2017/downloads/MagicCompRules_20170707.txt
			glossaryText = IOUtils.toString(ComprehensiveRule.class.getResourceAsStream("/comprehensive_rules.txt"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load definitions!", e);
			return;
		}

		// Parse text
		try {
			// Extract glossarytext
			glossaryText = glossaryText.substring(glossaryText.indexOf("1. Game Concepts", glossaryText.indexOf("Credits")));
			glossaryText = glossaryText.substring(glossaryText.indexOf("Glossary"), glossaryText.indexOf("Credits")).substring(glossaryText.indexOf("\n"));

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
					definitions.add(new Definition(keyword, sb.toString()));
					keyword = null;
					sb = new StringBuilder();
				}
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not parse definitions!", e);
		}

		LOG.info("Loaded " + definitions.size() + " definitions");
	}

}
