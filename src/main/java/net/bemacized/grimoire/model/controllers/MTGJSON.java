package net.bemacized.grimoire.model.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.Dependency;
import net.bemacized.grimoire.model.models.MtgSet;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipInputStream;

public class MTGJSON {

	private static final Logger LOG = Logger.getLogger(MTGJSON.class.getName());

	private List<MtgSet> setList;
	private List<Card> cardList;

	public MTGJSON() {
		setList = new ArrayList<>();
		cardList = new ArrayList<>();
	}

	public void load() {
		setList.clear();
		cardList.clear();
		LOG.info("Loading Sets & Cards...");

		Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("MTGJSON");
		if (!d.retrieve()) {
			LOG.severe("Could not retrieve card data!");
			return;
		}
		byte[] zipData = d.getBinary();
		d.release();

		// Load json string
		String json;
		try {
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData));
			zis.getNextEntry();
			json = IOUtils.toString(zis);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not load mtgjson.zip", e);
			return;
		}

		// Parse json
		JsonElement jsonElement = new JsonParser().parse(json);
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		// Parse into sets
		Gson gson = new Gson();
		setList.addAll(jsonObject.entrySet().parallelStream().map(setObj -> gson.fromJson(setObj.getValue(), MtgSet.class)).collect(Collectors.toList()));

		// Parse into cards
		cardList.addAll(jsonObject.entrySet().parallelStream().map(setObj ->
				StreamSupport.stream(setObj.getValue().getAsJsonObject().getAsJsonArray("cards").spliterator(), false).map(cardObj -> {
					Card card = gson.fromJson(cardObj, Card.class);
					// Inject set object
					try {
						Field field = Card.class.getDeclaredField("set");
						field.setAccessible(true);
						field.set(card, setList.parallelStream().filter(set -> set.getCode().equals(setObj.getKey())).findFirst().orElse(null));
						field.setAccessible(false);
					} catch (NoSuchFieldException | IllegalAccessException e) {
						LOG.log(Level.SEVERE, "Could not inject set into card model", e);
					}
					return card;
				})
		).flatMap(o -> o).collect(Collectors.toList()));

		// Sort lists by recency
		setList.sort(Comparator.comparing(MtgSet::getReleaseDate).reversed());
		cardList.sort((o1, o2) -> o2.getSet().getReleaseDate().compareTo(o1.getSet().getReleaseDate()));

		LOG.info("Loaded " + setList.size() + " sets");
		LOG.info("Loaded " + cardList.size() + " cards");
	}

	List<MtgSet> getSetList() {
		return new ArrayList<>(setList);
	}

	List<Card> getCardList() {
		return new ArrayList<>(cardList);
	}
}
