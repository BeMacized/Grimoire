package net.bemacized.grimoire.data.retrievers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GathererRetriever {

	private static Map<String, String> symbolTable = new HashMap<String, String>() {{
		put("{0}", "{0}");
		put("{1}", "{1}");
		put("{2}", "{2}");
		put("{3}", "{3}");
		put("{4}", "{4}");
		put("{5}", "{5}");
		put("{6}", "{6}");
		put("{7}", "{7}");
		put("{8}", "{8}");
		put("{9}", "{9}");
		put("{10}", "{10}");
		put("{11}", "{11}");
		put("{12}", "{12}");
		put("{13}", "{13}");
		put("{14}", "{14}");
		put("{15}", "{15}");
		put("{16}", "{16}");
		put("{20}", "{20}");
		put("{B}", "{Black}");
		put("{C}", "{Colorless}");
		put("{G}", "{Green}");
		put("{R}", "{Red}");
		put("{U}", "{Blue}");
		put("{W}", "{White}");
		put("{X}", "{Variable Colorless}");
		put("{2/B}", "{Two or Black}");
		put("{2/G}", "{{Two or Green}");
		put("{2/R}", "{Two or Red}");
		put("{2/U}", "{Two or Blue}");
		put("{2/W}", "{Two or White}");
		put("{B/G}", "{Blue or Green}");
		put("{B/P}", "{Phyrexian Black}");
		put("{B/R}", "{Blue or Red}");
		put("{G/P}", "{Phyrexian Green}");
		put("{G/U}", "{Green or Blue}");
		put("{G/W}", "{Green or White}");
		put("{R/G}", "{Red or Green}");
		put("{R/P}", "{Phyrexian Red}");
		put("{R/W}", "{Red or White}");
		put("{U/B}", "{Blue or Black}");
		put("{U/P}", "{Phyrexian Blue}");
		put("{U/R}", "{Blue or Red}");
		put("{W/B}", "{White or Black}");
		put("{W/P}", "{Phyrexian White}");
		put("{W/U}", "{White or Blue}");
		put("{hw}", "{Half a White}");
		put("{1000000}", "{1000000}");
		put("{CHAOS}", "");
		put("{P}", "");
		put("{½}", "");
		put("{hr}", "");
		put("{∞}", "");
		put("{100}", "");
		put("{E}", "");
		put("{T}", "");
		put("{Q}", "");
		put("{S}", "");
		put("{Y}", "");
		put("{Z}", "");
	}};

	public static GathererData getGathererData(int multiverseId, String cardName) {
		String url = "http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=true&multiverseid=" + multiverseId;

		Document doc;
		try {
			doc = Jsoup.parse(new URL(url), 10000);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// Fetch all available columns
		Elements columns = new Elements(doc.getElementsByClass("cardComponentContainer")
				.parallelStream()
				.filter(e -> !e.html().isEmpty())
				.collect(Collectors.toList()));

		return columns.parallelStream()
				.filter(c -> c.select("div[id$=nameRow]").get(0).getElementsByClass("value").get(0).html().trim().equalsIgnoreCase(cardName))
				.map(e -> {

					String name;
					String typeLine = null;
					String text = null;

					name = e.select("div[id$=nameRow]").get(0).getElementsByClass("value").get(0).html().trim();

					Elements typeRow = e.select("div[id$=typeRow]");
					if (!typeRow.isEmpty())
						typeLine = typeRow.get(0).getElementsByClass("value").get(0).html().trim();

					Elements textRow = e.select("div[id$=textRow]");
					if (!textRow.isEmpty()) {
						Element textElement = textRow.get(0).getElementsByClass("value").get(0);
						textElement.getElementsByTag("img").forEach(img -> img.replaceWith(new TextNode("{" + img.attr("alt").trim() + "}", null)));
						text = String.join("\n", textElement.children().parallelStream().map(Element::text).collect(Collectors.toList())).trim();
						for (Map.Entry<String, String> entry : symbolTable.entrySet()) {
							if (entry.getValue().isEmpty()) continue;
							text = text.replaceAll(Pattern.quote(entry.getValue()), entry.getKey());
						}
					}
					return new GathererData(name, typeLine, text);
				})
				.findFirst()
				.orElse(null);
	}

	public static class GathererData {
		private String name;
		private String typeLine;
		private String text;

		public GathererData(String name, String typeLine, String text) {
			this.name = name;
			this.typeLine = typeLine;
			this.text = text;
		}

		public String getTypeLine() {
			return typeLine;
		}

		public String getText() {
			return text;
		}

		public String getName() {
			return name;
		}
	}
}
