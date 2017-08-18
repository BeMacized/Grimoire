package net.bemacized.grimoire.data.retrievers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonSet;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class MtgJsonRetriever {

	private final static Logger LOG = Logger.getLogger(MtgJsonRetriever.class.getName());
	private final static String SOURCE = "https://mtgjson.com/json/AllSets-x.json.zip";

	public static Map<MtgJsonSet, List<MtgJsonCard>> retrieveData() throws IOException {
		LOG.info("Starting retrieval of sets & cards from MTGJSON");

		LOG.info("Downloading set & card data from " + SOURCE + "...");
		byte[] data = IOUtils.toByteArray(new URL(SOURCE));

		LOG.info("Unpacking set & card data...");
		List<JsonObject> jsonsets;
		try {
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
			zis.getNextEntry();
			String jsonStr = IOUtils.toString(zis, "UTF-8");
			jsonsets = new JsonParser().parse(jsonStr).getAsJsonObject().entrySet().parallelStream().map(e -> e.getValue().getAsJsonObject()).collect(Collectors.toList());
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not unpack mtgjson data", e);
			return new HashMap<>();
		}

		LOG.info("Loading set & card data...");
		Gson gson = new GsonBuilder().registerTypeAdapter(MtgJsonSet.Booster.class, new MtgJsonSet.BoosterDeserializer()).create();
		List<MtgJsonSet> sets = jsonsets.parallelStream().map(e -> gson.fromJson(e, MtgJsonSet.class)).collect(Collectors.toList());

		LOG.info("Retrieved " + sets.size() + " sets. Extracting cards...");

		Map<MtgJsonSet, List<MtgJsonCard>> results = new HashMap<>();

		for (MtgJsonSet set : sets) {
			List<MtgJsonCard> cards = new ArrayList<>();
			try {
				Field cardsField = MtgJsonSet.class.getDeclaredField("cards");
				cardsField.setAccessible(true);
				cards.addAll(Arrays.stream((MtgJsonCard[]) cardsField.get(set)).collect(Collectors.toList()));
				cardsField.set(set, null);
				cardsField.setAccessible(false);
				for (MtgJsonCard card : cards) {
					Field setCodeField = MtgJsonCard.class.getDeclaredField("setCode");
					setCodeField.setAccessible(true);
					setCodeField.set(card, set.getCode().toUpperCase());
					setCodeField.setAccessible(false);
				}
			} catch (NoSuchFieldException | IllegalAccessException e) {
				LOG.log(Level.SEVERE, "Could not extract cards", e);
				return new HashMap<>();
			}
			results.put(set, cards);
		}

		LOG.info("Retrieved " + results.values().parallelStream().mapToInt(List::size).sum() + " cards.");

		results.keySet().parallelStream().forEach(MtgJsonSet::assertValidity);
		results.values().parallelStream().map(Collection::parallelStream).flatMap(o -> o).forEach(MtgJsonCard::assertValidity);

		return results;
	}

}
