package net.bemacized.grimoire.parsers;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TournamentRules {

	private static final Logger LOG = Logger.getLogger(TournamentRules.class.getName());

	private List<Section> rules;

	public TournamentRules() {
		// Fetch text
		String ruleText;
		try {
			// Sourced from https://sites.google.com/site/mtgfamiliar/rules/MagicTournamentRules-light.html
			ruleText = IOUtils.toString(getClass().getResourceAsStream("/tournament_rules.html"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load tournament rules!", e);
			return;
		}

		// Parse HTML
		Document document = Jsoup.parse(ruleText);
		// Get sections
		this.rules = document.getElementsByAttributeValueMatching("name", "[0-9]+[.]$")
				.parallelStream()
				.map(sectionElement -> new Section(
						sectionElement.attr("name"),
						sectionElement.parent().nextElementSibling().text().substring(sectionElement.attr("name").length()).trim(),
						new ArrayList<SubSection>() {{
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
								add(new SubSection(
										index,
										name,
										content
								));
							});
						}}
				))
				.collect(Collectors.toList());
	}

	public List<Section> getRules() {
		return rules;
	}

	public abstract static class TournamentRule {
		private String paragraphNr;
		private String title;

		public TournamentRule(String paragraphNr, String title) {
			this.paragraphNr = paragraphNr;
			this.title = title;
		}

		public String getParagraphNr() {
			return paragraphNr;
		}

		public String getTitle() {
			return title;
		}

	}

	public static class Section extends TournamentRule {

		private List<SubSection> subsections;

		public Section(String paragraphNr, String title, List<SubSection> subsections) {
			super(paragraphNr, title);
			this.subsections = new ArrayList<>(subsections);
		}

		public List<SubSection> getSubsections() {
			return subsections;
		}
	}

	public static class SubSection extends TournamentRule {

		private String content;

		public SubSection(String paragraphNr, String title, String content) {
			super(paragraphNr, title);
			this.content = content;
		}

		public String getContent() {
			return content;
		}
	}
}
