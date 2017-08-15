package net.bemacized.grimoire.data.retrievers;

import net.bemacized.grimoire.data.models.TournamentRule;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TournamentRuleRetriever {

	private final static Logger LOG = Logger.getLogger(ScryfallRetriever.class.getName());
	private final static String SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/MagicTournamentRules-light.html";
	private final static String CHARSET = "UTF-8";

	public static List<TournamentRule> retrieve() {
		LOG.info("Retrieving tournament rules...");

		String ruleText;
		try {
			ruleText = IOUtils.toString(new URL(SOURCE), CHARSET);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not retrieve tournament rules!", e);
			return new ArrayList<>();
		}

		// Parse HTML
		Document document = Jsoup.parse(ruleText);
		// Get sections
		List<TournamentRule> rules = document.getElementsByAttributeValueMatching("name", "[0-9]+[.]$")
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

		LOG.info("Retrieved " + rules.size() + " tournament rules");

		return rules;
	}
}
