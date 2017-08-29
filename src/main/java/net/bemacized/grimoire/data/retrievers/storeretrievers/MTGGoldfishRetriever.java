package net.bemacized.grimoire.data.retrievers.storeretrievers;

import net.bemacized.grimoire.data.models.card.MtgCard;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MTGGoldfishRetriever extends StoreRetriever {


	@Override
	public String getStoreName() {
		return "MTGGoldfish";
	}

	@Override
	public String getStoreId() {
		return "MTGG";
	}

	@Override
	public String[] supportedLanguages() {
		return new String[]{"English"};
	}

	@Override
	public long timeout() {
		return 1000 * 60 * 60 * 6; // 6 hours
	}

	@Nullable
	@Override
	protected StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException {
		Map<String, Price> prices = new HashMap<>();
		String url;
		try {
			Document doc = Jsoup.connect("https://www.mtggoldfish.com/index/" + card.getSet().getCode().toUpperCase()).userAgent("Mozilla/5.0").timeout(5 * 1000).get();
			Elements paperTableRows = doc.getElementsByClass("index-price-table-paper").first().getElementsByTag("table").first().getElementsByTag("tbody").first().getElementsByTag("tr");
			Elements onlineTableRows = doc.getElementsByClass("index-price-table-online").first().getElementsByTag("table").first().getElementsByTag("tbody").first().getElementsByTag("tr");
			Element paperTableRow = paperTableRows.parallelStream().filter(r -> r.child(0).text().equalsIgnoreCase(card.getName())).findFirst().orElse(null);
			Element onlineTableRow = onlineTableRows.parallelStream().filter(r -> r.child(0).text().equalsIgnoreCase(card.getName())).findFirst().orElse(null);
			double paperPrice = paperTableRow == null ? 0 : Double.parseDouble(paperTableRow.child(3).text().isEmpty() ? "0" : paperTableRow.child(3).text().replaceAll(",",""));
			double onlinePrice = onlineTableRow == null ? 0 : Double.parseDouble(onlineTableRow.child(3).text().isEmpty() ? "0" : onlineTableRow.child(3).text().replaceAll(",",""));
			url = paperTableRow != null ? paperTableRow.child(0).child(0).attr("href") : onlineTableRow != null ? onlineTableRow.child(0).child(0).attr("href") : null;
			if (url != null) url = "https://www.mtggoldfish.com" + url;
			if (paperPrice > 0) prices.put("Paper", new Price(paperPrice, Currency.USD));
			if (onlinePrice > 0) prices.put("MTGO", new Price(onlinePrice, Currency.TIX));
		} catch (HttpStatusException e) {
			return null;
		} catch (IOException e) {
			LOG.log(java.util.logging.Level.SEVERE, "Could not fetch pricing from MTGGoldfish", e);
			throw new UnknownStoreException();
		} catch (Exception e) {
			LOG.log(java.util.logging.Level.SEVERE, "An unknown error occurred while parsing pricing from MTGGoldfish", e);
			throw new UnknownStoreException();
		}

		if (prices.isEmpty()) return null;

		return new StoreCardPriceRecord(
				card.getName(),
				card.getSet().getCode(),
				url,
				System.currentTimeMillis(),
				getStoreId(),
				prices
		);
	}
}
