package net.bemacized.grimoire.data.retrievers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
}
