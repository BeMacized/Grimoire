package net.bemacized.grimoire.data.retrievers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.ScryfallCard;
import net.bemacized.grimoire.data.models.ScryfallSet;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.python.jline.internal.Log;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
		LOG.info("Starting retrieval of sets from Scryfall");
		Gson gson = new Gson();
		List<JsonElement> items = getAllContents(HOST + "/sets", null);
		LOG.info("Parsing sets...");
		List<ScryfallSet> sets = items.parallelStream().map(obj -> gson.fromJson(obj, ScryfallSet.class)).collect(Collectors.toList());
		sets.forEach(ScryfallSet::assertValidity);
		LOG.info("Retrieved " + sets.size() + " sets.");
		return sets;
	}

	public static List<ScryfallCard> retrieveCards() throws IOException, InterruptedException {
		LOG.info("Starting retrieval of cards from Scryfall");
		Gson gson = new Gson();
		List<JsonElement> items = getAllContents(HOST + "/cards", new ListProgressCallback() {

			private int total = -1;
			private int loaded = 0;

			@Override
			public void start(int recordsExpected) {
				total = recordsExpected;
			}

			@Override
			public void update(int newRecordsPulled) {
				loaded += newRecordsPulled;
				if (total >= 0) {
					int percentage = (int) Math.round(((double) loaded) / ((double) total) * 100d);
					Log.info(String.format("(%s%%) Downloaded %s/%s...", percentage, loaded, total));
				}
			}

			@Override
			public void done() {
				Log.info("Downloaded card data");
			}
		});
		LOG.info("Parsing card data...");
		List<ScryfallCard> cards = items.parallelStream().map(obj -> gson.fromJson(obj, ScryfallCard.class)).collect(Collectors.toList());

		// Check validity
		cards.forEach(ScryfallCard::assertValidity);

		LOG.info("Retrieved " + cards.size() + " cards.");
		return cards;
	}

	private static List<JsonElement> getAllContents(String uri, @Nullable ListProgressCallback cb) throws IOException, InterruptedException {
		List<JsonElement> objects = new ArrayList<>();
		// Get list object
		String json = IOUtils.toString(new URL(uri), "UTF-8");
		JsonObject list = new JsonParser().parse(json).getAsJsonObject();
		// Send total
		try {
			if (cb != null && list.has("total_cards") && new URIBuilder(uri).getQueryParams().parallelStream().filter(p -> p.getName().equals("page")).findFirst().map(p -> Integer.parseInt(p.getValue()) == 1).orElse(true)) {
				cb.start(list.get("total_cards").getAsInt());
			}
		} catch (URISyntaxException e) {
			LOG.log(Level.SEVERE, "Could not parse page parameter from uri. Should never happen.", e);
		}
		// Add contents to return list
		list.getAsJsonArray("data").forEach(objects::add);
		// If there's more, add the contents of the next page(s) too
		if (list.has("has_more") && list.get("has_more").getAsBoolean()) {
			// Callback update
			if (cb != null) cb.update(objects.size());
			// Sleep for 100ms to make sure scryfall won't hate us :(
			Thread.sleep(100);
			// Add the objects from other pages
			objects.addAll(getAllContents(list.get("next_page").getAsString(), cb));
		} else if (cb != null) {
			cb.update(objects.size());
			cb.done();
		}
		return objects;
	}

	private interface ListProgressCallback {
		void start(int recordsExpected);

		void update(int newRecordsPulled);

		void done();
	}

}
