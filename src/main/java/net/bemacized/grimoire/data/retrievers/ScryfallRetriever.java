package net.bemacized.grimoire.data.retrievers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.ScryfallCard;
import net.bemacized.grimoire.data.models.ScryfallSet;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScryfallRetriever {

	public static void main(String[] args) {
		try {
			System.out.println(retrieveCards().size());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private final static String HOST = "https://api.scryfall.com";
	private final static Logger LOG = Logger.getLogger(ScryfallRetriever.class.getName());

	public static List<ScryfallSet> retrieveSets() throws IOException, InterruptedException {
		LOG.info("Starting retrieval of sets from ScryfallRetriever");
		final Gson gson = new Gson();
		List<ScryfallSet> sets = new ArrayList<>();
		CountDownLatch completionLatch = new CountDownLatch(1);
		getAllContents(HOST + "/sets", new ListProgressCallback() {
			@Override
			public void start(int recordsExpected) {

			}

			@Override
			public void update(List<JsonElement> objects) {
				sets.addAll(objects.parallelStream().map(obj -> gson.fromJson(obj, ScryfallSet.class)).collect(Collectors.toList()));
			}

			@Override
			public void done() {
				sets.forEach(ScryfallSet::assertValidity);
				completionLatch.countDown();
			}
		});
		completionLatch.await();
		LOG.info("Retrieved " + sets.size() + " sets.");
		return sets;
	}

	public static List<ScryfallCard> retrieveCards() throws IOException, InterruptedException {
		LOG.info("Starting retrieval of cards from ScryfallRetriever");
		Gson gson = new Gson();
		List<ScryfallCard> cards = new ArrayList<>();
		CountDownLatch completionLatch = new CountDownLatch(1);
		getAllContents(HOST + "/cards", new ListProgressCallback() {

			private int total = -1;
			private int loaded = 0;

			@Override
			public void start(int recordsExpected) {
				total = recordsExpected;
			}

			@Override
			public void update(List<JsonElement> objects) {
				loaded += objects.size();
				cards.addAll(objects.parallelStream().map(obj -> gson.fromJson(obj, ScryfallCard.class)).collect(Collectors.toList()));
				if (total >= 0) {
					int percentage = (int) Math.round(((double) loaded) / ((double) total) * 100d);
					LOG.info(String.format("(%s%%) Downloaded %s/%s...", percentage, loaded, total));
				}
			}

			@Override
			public void done() {
				cards.forEach(ScryfallCard::assertValidity);
				LOG.info("Downloaded card data");
				completionLatch.countDown();
			}
		});
		completionLatch.await();
		LOG.info("Retrieved " + cards.size() + " cards.");
		return cards;
	}

	private static void getAllContents(@Nonnull String uri, @Nonnull ListProgressCallback cb) throws IOException, InterruptedException {
		// Get list object
		String json = IOUtils.toString(new URL(uri), "UTF-8");
		JsonObject list = new JsonParser().parse(json).getAsJsonObject();
		// Send total
		try {
			if (list.has("total_cards") && new URIBuilder(uri).getQueryParams().parallelStream().filter(p -> p.getName().equals("page")).findFirst().map(p -> Integer.parseInt(p.getValue()) == 1).orElse(true)) {
				cb.start(list.get("total_cards").getAsInt());
			}
		} catch (URISyntaxException e) {
			LOG.log(Level.SEVERE, "Could not parse page parameter from uri. Should never happen.", e);
		}
		// Add contents to return list
		cb.update(IteratorUtils.toList(list.getAsJsonArray("data").iterator()));
		// If there's more, add the contents of the next page(s) too
		if (list.has("has_more") && list.get("has_more").getAsBoolean()) {
			// Sleep for 100ms to make sure scryfall won't hate us :(
			Thread.sleep(100);
			// Load the other pages too
			getAllContents(list.get("next_page").getAsString(), cb);
		} else {
			cb.done();
		}
	}

	@Nullable
	public static ScryfallCard retrieveCard(String scryfallId) {
		String json;
		try {
			json = IOUtils.toString(new URL(HOST + "/cards/" + scryfallId), "UTF-8");
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could retrieve specific card from ScryfallRetriever.", e);
			return null;
		}
		try {
			JsonObject card = new JsonParser().parse(json).getAsJsonObject();
			Gson gson = new Gson();
			return gson.fromJson(card, ScryfallCard.class);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could parse specific card from ScryfallRetriever.", e);
			return null;
		}
	}

	private interface ListProgressCallback {
		void start(int recordsExpected);

		void update(List<JsonElement> objects);

		void done();
	}

}
