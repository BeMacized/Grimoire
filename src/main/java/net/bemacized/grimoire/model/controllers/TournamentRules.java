package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Dependency;
import net.bemacized.grimoire.model.models.TournamentRule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TournamentRules {

	private static Logger LOG = Logger.getLogger(TournamentRules.class.getName());

	private List<TournamentRule> rules;

	public TournamentRules() {
		rules = new ArrayList<>();
	}

	public List<TournamentRule> getRules() {
		return new ArrayList<>(rules);
	}

	public void load() {
		rules.clear();
		LOG.info("Loading tournament rules...");

		// Fetch text
		Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("TR_DOC");
		if (!d.retrieve()) {
			LOG.severe("Could not load tournament rules!");
			return;
		}
		String ruleText = d.getString();
		d.release();

		// Parse HTML
		Document document = Jsoup.parse(ruleText);
		// Get sections
		rules = document.getElementsByAttributeValueMatching("name", "[0-9]+[.]$")
				.parallelStream()
				.map(sectionElement -> new TournamentRule(
						sectionElement.attr("name"),
						sectionElement.parent().nextElementSibling().text().substring(sectionElement.attr("name").length()).trim(),
						new ArrayList<TournamentRule.SubSection>() {{
							document.getElementsByAttributeValueMatching("name", sectionElement.attr("name") + "[0-9]+").forEach(subSectionElement -> {
								// Parse subsection id & name
								boolean rootLevel = subSectionElement.parent().tagName().equalsIgnoreCase("body");
								String index = subSectionElement.attr("name");
								String name = (rootLevel
										? subSectionElement.nextElementSibling().text()
										: subSectionElement.parent().nextElementSibling().text())
										.substring(index.length()).trim();

								// Parse subsection content
								StringBuilder contentSB = new StringBuilder();
								Element cursor = (rootLevel ? subSectionElement : subSectionElement.parent()).nextElementSibling().nextElementSibling();
								while (cursor.tagName().equalsIgnoreCase("p") && cursor.children().isEmpty()) {
									contentSB.append(cursor.text()).append("\n\n");
									cursor = cursor.nextElementSibling();
								}
								String content = contentSB.toString().replaceAll("[\n]{3,}", "\n\n").trim();

								//Create subsection
								add(new TournamentRule.SubSection(
										index,
										name,
										content
								));
							});
						}}
				))
				.collect(Collectors.toList());

		LOG.info("Loaded " + rules.size() + " tournament rules");
	}
}
