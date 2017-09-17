package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonSet;
import net.bemacized.grimoire.data.retrievers.MtgJsonRetriever;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MtgJsonProvider {

	private static final Logger LOG = Logger.getLogger(MtgJsonRetriever.class.getName());

	private List<MtgJsonCard> cards;
	private List<MtgJsonSet> sets;

	public MtgJsonProvider() {
		cards = new ArrayList<>();
		sets = new ArrayList<>();
	}

	public List<String> getAllSupertypes() {
		return cards.parallelStream()
				.map(card -> Arrays.stream(card.getSupertypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<String> getAllTypes() {
		return cards.parallelStream()
				.map(card -> Arrays.stream(card.getTypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<String> getAllSubtypes() {
		return cards.parallelStream()
				.map(card -> Arrays.stream(card.getSubtypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public void load() {
		try {
			Map<MtgJsonSet, List<MtgJsonCard>> mtgJsonSetListMap = MtgJsonRetriever.retrieveData();
			LOG.info("Transforming MTGJSON data...");
			sets = mtgJsonSetListMap.keySet().parallelStream().collect(Collectors.toList());
			cards = new ArrayList<>(mtgJsonSetListMap.values().parallelStream().map(Collection::parallelStream).flatMap(o -> o).map(c -> c.getAllLanguages().stream()).flatMap(o -> o).collect(Collectors.toList()));
			cards.sort(Comparator.comparing(o -> getSetByCodeOrName(o.getSetCode()).getReleaseDate()));
			Collections.reverse(cards);
			LOG.info("Transformed " + cards.size() + " cards!");
		} catch (IOException e) {
			e.printStackTrace();
			LOG.log(Level.SEVERE, "Could not fetch data from MTGJSON", e);
			return;
		}
	}

	public List<MtgJsonCard> getCardsByMultiverseId(int multiverseId) {
		return cards.parallelStream().filter(c -> c.getMultiverseid() == multiverseId).sorted((x,y)->{
			int xi = Arrays.asList(x.getNames()).indexOf(x.getName());
			int yi = Arrays.asList(x.getNames()).indexOf(y.getName());
			if (xi == -1 || yi == -1) return 0;
			else return Integer.compare(xi, yi);
		}).collect(Collectors.toList());
	}

	public List<MtgJsonCard> getCardsByName(String name) {
		return cards.parallelStream().filter(c -> c.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
	}

	@Nullable
	public MtgJsonCard getCardByName(String name) {
		return getCardsByName(name).parallelStream().findFirst().orElse(null);
	}

	public List<MtgJsonCard> getCards() {
		return cards;
	}

	public List<MtgJsonSet> getSets() {
		return sets;
	}

	@Nullable
	public MtgJsonSet getSetByCodeOrName(String arg) {
		return sets.parallelStream().filter(set -> arg.equalsIgnoreCase(set.getCode()) || arg.equalsIgnoreCase(set.getGathererCode()) || arg.equalsIgnoreCase(set.getOldCode())).findFirst().orElse(
				sets.parallelStream().filter(set -> set.getName().toLowerCase().contains(arg.toLowerCase())).findFirst().orElse(null)
		);
	}


}
