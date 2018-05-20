package net.bemacized.grimoire.data.retrievers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XLanderRetriever {

	public static List<String> retrieveHighlanderBanlist() throws IOException {
		List<String> banList = new ArrayList<>();

		Document doc = Jsoup.parse(new URL("http://www.highlandermagic.info/index.php?id=bannedlist"), 10000);
		banList.addAll(doc.getElementsByTag("table").get(0)
				.getElementsByTag("li")
				.parallelStream()
				.map(e -> e.html(e.html().replaceAll(Pattern.quote("<strong>new!</strong>"), "")))
				.map(Element::text).collect(Collectors.toList()));

		return banList;
	}

	public static Map<String, Integer> retrieveCanlanderPointlist() throws IOException {
		Document doc = Jsoup.parse(new URL("https://canadianhighlander.wordpress.com/rules-the-points-list-and-deck-construction/points-list-plain-text/"), 10000);
		return doc
				.getElementsByClass("entry-content")
				.get(0)
				.getElementsByTag("table")
				.get(0)
				.getElementsByTag("tr")
				.stream()
				.map(e -> e.getElementsByTag("td"))
				.collect(Collectors.toMap((Elements e) -> e.get(0).text().toLowerCase(), (Elements e) -> {
					try {
						return Integer.parseInt(e.get(1).text());
					} catch (NumberFormatException ex) {
						return 0;
					}
				}));
	}
}
