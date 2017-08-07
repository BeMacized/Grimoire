package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.model.models.Dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DependencyManager {

	private List<Dependency> dependencies;

	public DependencyManager() {
		dependencies = new ArrayList<Dependency>() {{
			add(new Dependency("MTGJSON", Collections.singletonList("https://mtgjson.com/json/AllSets-x.json.zip"), Dependency.Type.BINARY, null));
			add(new Dependency("SET_DICTIONARY", Collections.singletonList("https://raw.githubusercontent.com/BeMacized/MTG-Marketplace-Set-Dictionary/master/SetDictionary.json"), Dependency.Type.TEXT, "UTF-8"));
			add(new Dependency("CR_DOC", Collections.singletonList("http://media.wizards.com/2017/downloads/MagicCompRules_20170707.txt"), Dependency.Type.TEXT, "windows-1252"));
			add(new Dependency("IPG_DOC", Collections.singletonList("https://sites.google.com/site/mtgfamiliar/rules/InfractionProcedureGuide-light.html"), Dependency.Type.TEXT, "UTF-8"));
			add(new Dependency("TR_DOC", Collections.singletonList("https://sites.google.com/site/mtgfamiliar/rules/MagicTournamentRules-light.html"), Dependency.Type.TEXT, "UTF-8"));
			add(new Dependency("TOKENS", Collections.singletonList("https://raw.githubusercontent.com/Cockatrice/Magic-Token/master/tokens.xml"), Dependency.Type.TEXT, "UTF-8"));
		}};
	}

	public void retrieveAll() {
		dependencies.parallelStream().forEach(Dependency::retrieve);
	}

	public List<Dependency> getDependencies() {
		return new ArrayList<>(dependencies);
	}

	public Dependency getDependency(String id) {
		return dependencies.parallelStream().filter(d -> d.getId().equalsIgnoreCase(id)).findAny().orElse(null);
	}
}
