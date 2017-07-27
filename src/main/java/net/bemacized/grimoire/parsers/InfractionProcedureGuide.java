package net.bemacized.grimoire.parsers;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class InfractionProcedureGuide {

	private static final Logger LOG = Logger.getLogger(InfractionProcedureGuide.class.getName());

	private List<Section> sections;

	public InfractionProcedureGuide() {
		sections = new ArrayList<>();

		// Fetch text
		String ruleText;
		try {
			// Sourced from https://sites.google.com/site/mtgfamiliar/rules/InfractionProcedureGuide-light.html
			ruleText = IOUtils.toString(getClass().getResourceAsStream("/infraction_procedure_guide.html"));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load tournament rules!", e);
			return;
		}

		// Parse HTML
		Document document = Jsoup.parse(ruleText);

		final ArrayList<Pattern> patterns = new ArrayList<Pattern>() {{
			add(Pattern.compile("[0-9]+[A-Z ]+"));
			add(Pattern.compile("[0-9]+[.][0-9]+.*"));
			add(Pattern.compile(".*"));
		}};

		Stack<Section> parentStack = new Stack<>();
		for (Element e : document.getElementsByTag("h4")) {
			// Extract id & title
			String id = e.text().split("\\s+")[0];
			if (!id.matches("[0-9.]+")) id = "";
			String title = e.text().substring(id.length()).trim();
			if (id.isEmpty()) id = null;
			// Determine depth level
			int level = 0;
			for (Pattern pattern : patterns) {
				if (!pattern.matcher(e.text()).matches()) level++;
				else break;
			}
			// Parse content
			// Parse content
			String content;
			{
				StringBuilder contentSb = new StringBuilder();
				Element cursor = e.nextElementSibling();
				while (cursor != null && (cursor.tagName().equalsIgnoreCase("p") || cursor.tagName().equalsIgnoreCase("table"))) {
					switch (cursor.tagName().toLowerCase()) {
						case "p": {
							contentSb.append(cursor.text()).append("\n\n");
							break;
						}
						case "table": {
							contentSb.append("Penalty: ").append(cursor.getElementsByTag("td").get(0).text()).append("\n\n");
							break;
						}
						default:
							break;
					}
					cursor = cursor.nextElementSibling();
				}
				content = contentSb.toString().replaceAll("[\n\r]{3,}", "\n\n").trim();
			}

			// Create section
			Section s = new Section(id, title, content);

			while (level < parentStack.size()) parentStack.pop();

			// Add section properly
			if (parentStack.isEmpty()) {
				sections.add(s);
				parentStack.push(s);
			} else {
				parentStack.get(parentStack.size() - 1).setSubSections(new ArrayList<Section>(parentStack.get(parentStack.size() - 1).getSubSections()) {{
					add(s);
				}});
				parentStack.push(s);
			}
		}

		LOG.info("Loaded infraction procedure guide");
	}

	public List<Section> getSections() {
		return sections;
	}

	public static class Section {

		private String sectionId;
		private String title;
		private String content;
		private List<Section> subSections;

		public Section(String sectionId, String title, String content) {
			this.sectionId = sectionId;
			this.title = title;
			this.content = content;
			this.subSections = new ArrayList<>();
		}

		public String getSectionId() {
			return sectionId;
		}

		public String getTitle() {
			return title;
		}

		public String getContent() {
			return content;
		}

		public List<Section> getSubSections() {
			return new ArrayList<>(subSections);
		}

		void setSubSections(List<Section> subSections) {
			this.subSections = new ArrayList<>(subSections);
		}
	}
}
