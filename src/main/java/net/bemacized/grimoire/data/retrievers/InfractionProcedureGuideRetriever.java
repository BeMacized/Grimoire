package net.bemacized.grimoire.data.retrievers;

import net.bemacized.grimoire.data.models.InfractionProcedureGuideSection;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class InfractionProcedureGuideRetriever {

	private final static Logger LOG = Logger.getLogger(ScryfallRetriever.class.getName());
	private final static String SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/InfractionProcedureGuide-light.html";
	private final static String CHARSET = "UTF-8";

	public static List<InfractionProcedureGuideSection> retrieve() {
		LOG.info("Retrieving infraction procedure guide...");

		String ruleText;
		try {
			ruleText = IOUtils.toString(new URL(SOURCE), CHARSET);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not retrieve infraction procedure guide!", e);
			return new ArrayList<>();
		}

		List<InfractionProcedureGuideSection> sections = new ArrayList<>();

		// Parse HTML
		Document document = Jsoup.parse(ruleText);

		final ArrayList<Pattern> patterns = new ArrayList<Pattern>() {{
			add(Pattern.compile("[0-9]+[A-Z ]+"));
			add(Pattern.compile("[0-9]+[.][0-9]+.*"));
			add(Pattern.compile(".*"));
		}};

		Stack<InfractionProcedureGuideSection> parentStack = new Stack<>();
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
							if (cursor.children().size() > 0 && cursor.child(0).tagName().equalsIgnoreCase("b") && cursor.child(0).text().startsWith("APPENDIX"))
								break;
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
			InfractionProcedureGuideSection s = new InfractionProcedureGuideSection(id, title, content);

			while (level < parentStack.size()) parentStack.pop();

			// Add section properly
			if (parentStack.isEmpty()) {
				sections.add(s);
				parentStack.push(s);
			} else {
				parentStack.get(parentStack.size() - 1).setSubSections(new ArrayList<InfractionProcedureGuideSection>(parentStack.get(parentStack.size() - 1).getSubSections()) {{
					add(s);
				}});
				parentStack.push(s);
			}
		}

		LOG.info("Retrieved infraction procedure guide with " + sections.size() + " sections.");

		return sections;
	}
}
