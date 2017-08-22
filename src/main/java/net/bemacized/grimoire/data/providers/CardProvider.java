package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.card.MtgSet;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonCard;
import net.bemacized.grimoire.data.models.mtgjson.MtgJsonSet;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.bemacized.grimoire.data.retrievers.CardImageRetriever;
import net.bemacized.grimoire.data.retrievers.MtgJsonRetriever;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.utils.StreamUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CardProvider extends Provider {

	private CardImageRetriever imageRetriever;

	private List<MtgCard> cards;
	private List<MtgSet> sets;

	public CardProvider() {
		cards = new ArrayList<>();
		sets = new ArrayList<>();
		imageRetriever = new CardImageRetriever();
	}

	public List<MtgCard> getCards() {
		return new ArrayList<>(cards);
	}

	public List<MtgSet> getSets() {
		return new ArrayList<>(sets);
	}

	public CardImageRetriever getImageRetriever() {
		return imageRetriever;
	}

	public List<MtgSet> getSetsByNameOrCode(String query) {
		return Stream.concat(
				this.getSets().parallelStream().filter(set -> query.equalsIgnoreCase(set.getCode())),
				this.getSets().parallelStream().filter(set -> set.getName().toLowerCase().contains(query.toLowerCase()))
		).collect(Collectors.toList());
	}

	public MtgSet forceSingleSetByNameOrCode(String nameOrCode) {
		List<MtgSet> sets = getSetsByNameOrCode(nameOrCode);
		if (sets.isEmpty()) return null;
		MtgSet exactCodeMatch = getSetByCode(nameOrCode);
		if (exactCodeMatch != null) return exactCodeMatch;
		MtgSet exactNameMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getName())).findAny().orElse(null);
		if (exactNameMatch != null) return exactNameMatch;
		return sets.get(0);
	}

	public MtgSet getSingleSetByNameOrCode(String nameOrCode) throws MultipleSetResultsException {
		List<MtgSet> sets = getSetsByNameOrCode(nameOrCode);
		switch (sets.size()) {
			case 0:
				return null;
			case 1:
				return sets.get(0);
			default: {
				MtgSet exactCodeMatch = getSetByCode(nameOrCode);
				if (exactCodeMatch != null) return exactCodeMatch;
				MtgSet exactNameMatch = sets.parallelStream().filter(set -> nameOrCode.equalsIgnoreCase(set.getName())).findAny().orElse(null);
				if (exactNameMatch != null) return exactNameMatch;
				throw new MultipleSetResultsException(sets);
			}
		}
	}

	@Nullable
	public MtgSet getSetByCode(String code) {
		return this.getSets().parallelStream().filter(s -> s.getCode().equalsIgnoreCase(code)).findFirst().orElse(null);
	}

	@Override
	boolean loadFromDB() {
		LOG.info("Attempting to load card data from database...");
		Grimoire.getInstance().getDBManager().getJongo().getCollection(MtgSet.COLLECTION).find().as(MtgSet.class).forEach(set -> this.sets.add(set));
		if (sets.isEmpty()) {
			LOG.info("Could not find any sets in database. Fetching from web instead.");
			return false;
		}
		LOG.info("Loaded sets from database.");
		Grimoire.getInstance().getDBManager().getJongo().getCollection(MtgCard.COLLECTION).find().as(MtgCard.class).forEach(card -> this.cards.add(card));
		if (cards.isEmpty()) {
			LOG.info("Could not find any cards in database. Fetching from web instead.");
			return false;
		}

		// Add foreign cards
		this.cards.addAll(this.cards.parallelStream()
				.map(c -> Arrays.stream(c.getForeignNames()).parallel().map(fv -> new MtgCard(c, fv)))
				.flatMap(o -> o)
				.collect(Collectors.toList()));
		LOG.info("Loaded cards from database.");

		// Sort the data
		sortData();
		return true;
	}

	@Override
	void saveToDB() {
		LOG.info("Saving sets to database...");
		this.sets.stream().forEach(MtgSet::save);
		LOG.info("Saving cards to database...");
		this.cards.stream().forEach(MtgCard::save);
		LOG.info("Saved sets and cards to database.");
	}

	@Override
	public void loadFromSource() {
		// Load data from ScryfallRetriever
		List<ScryfallCard> scryfallCards;
		List<ScryfallSet> scryfallSets;
		try {
			scryfallCards = ScryfallRetriever.retrieveCards();
			scryfallSets = ScryfallRetriever.retrieveSets();
		} catch (IOException | InterruptedException e) {
			LOG.log(Level.SEVERE, "Could not fetch data from ScryfallRetriever", e);
			return;
		}

		// Load data from MTGJSON
		List<MtgJsonCard> mtgJsonCards;
		List<MtgJsonSet> mtgJsonSets;
		try {
			Map<MtgJsonSet, List<MtgJsonCard>> mtgJsonSetListMap = MtgJsonRetriever.retrieveData();
			mtgJsonSets = new ArrayList<>(mtgJsonSetListMap.keySet());
			mtgJsonCards = new ArrayList<>(mtgJsonSetListMap.values().parallelStream().map(Collection::parallelStream).flatMap(o -> o).collect(Collectors.toList()));
		} catch (IOException e) {
			e.printStackTrace();
			LOG.log(Level.SEVERE, "Could not fetch data from MTGJSON", e);
			return;
		}

		LOG.info("Merging set models...");

		// Merge set models
		this.sets = scryfallSets.parallelStream()
				.map(scryfallSet -> new MtgSet(
						scryfallSet,
						mtgJsonSets.parallelStream()
								.filter(mtgJsonSet -> mtgJsonSet.getCode().equalsIgnoreCase(scryfallSet.getCode()))
								.findFirst().orElse(null)
				))
				.collect(Collectors.toList());

		LOG.info("Merging card models...");

		// Merge card models
		this.cards = scryfallCards.parallelStream()
				.map(scryfallCard -> new MtgCard(
						scryfallCard,
						mtgJsonCards.parallelStream().filter(mtgJsonCard -> mtgJsonCard.getMultiverseid() > 0 && scryfallCard.getMultiverseId() > 0 && mtgJsonCard.getMultiverseid() == scryfallCard.getMultiverseId() || mtgJsonCard.getName().equalsIgnoreCase(scryfallCard.getName()) && scryfallCard.getSet().equalsIgnoreCase(mtgJsonCard.getSetCode())).findFirst().orElse(null)
				))
				.collect(Collectors.toList());

		// Validate models
		this.sets.parallelStream().forEach(MtgSet::assertValidity);
		this.cards.parallelStream().forEach(MtgCard::assertValidity);
		assertValidity();

		// Save models to database
		saveToDB();

		// Add foreign cards
		LOG.info("Adding foreign cards...");
		this.cards.addAll(this.cards.parallelStream()
				.map(c -> Arrays.stream(c.getForeignNames()).parallel().map(fv -> new MtgCard(c, fv)))
				.flatMap(o -> o)
				.collect(Collectors.toList()));

		// Sort the data
		sortData();
	}

	private void sortData() {
		if (!"1".equals(System.getenv("DONT_SORT_CARDDATA"))) {
			LOG.info("Sorting sets...");
			this.sets.sort((o1, o2) -> {
				if (o1.getReleaseDate() == null && o2.getReleaseDate() == null) return 0;
				if (o1.getReleaseDate() == null) return 1;
				if (o2.getReleaseDate() == null) return -1;
				return o1.getReleaseDate().compareTo(o2.getReleaseDate()) * -1;
			});
			LOG.info("Sorting cards...");
			this.cards.sort((o1, o2) -> {
				if (o1.getReleaseDate() == null && o2.getReleaseDate() == null) return 0;
				if (o1.getReleaseDate() == null) return 1;
				if (o2.getReleaseDate() == null) return -1;
				return o1.getReleaseDate().compareTo(o2.getReleaseDate()) * -1;
			});
			LOG.info("Set & Card sorting complete");
		}
	}

	private void assertValidity() {
		assert this.sets.parallelStream().map(MtgSet::getCode).count() == this.sets.parallelStream().map(MtgSet::getCode).distinct().count();
	}

	public List<String> getAllSupertypes() {
		return cards.parallelStream()
				.filter(card -> card.getMtgJsonCard() != null)
				.map(MtgCard::getMtgJsonCard)
				.map(card -> Arrays.stream(card.getSupertypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<String> getAllTypes() {
		return cards.parallelStream()
				.filter(card -> card.getMtgJsonCard() != null)
				.map(MtgCard::getMtgJsonCard)
				.map(card -> Arrays.stream(card.getTypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<String> getAllSubtypes() {
		return cards.parallelStream()
				.filter(card -> card.getMtgJsonCard() != null)
				.map(MtgCard::getMtgJsonCard)
				.map(card -> Arrays.stream(card.getSubtypes()))
				.flatMap(o -> o)
				.distinct()
				.collect(Collectors.toList());
	}

	public class MultipleSetResultsException extends Exception {

		private List<MtgSet> results;

		MultipleSetResultsException(List<MtgSet> sets) {
			results = sets;
		}

		public List<MtgSet> getResults() {
			return results;
		}
	}

	public static class SearchQuery extends ArrayList<MtgCard> {

		public SearchQuery() {
			this(Grimoire.getInstance().getCardProvider().getCards());
		}

		SearchQuery(Collection<? extends MtgCard> c) {
			super(c);
		}

		public SearchQuery containsName(String name) {
			return new SearchQuery(this.parallelStream().filter(card -> {
				String reducedCard = card.getName().toLowerCase().replaceAll("[^a-z0-9- ]", "");
				String reducedInput = name.toLowerCase().replaceAll("[^a-z0-9- ]", "");
				return reducedCard.contains(reducedInput) || reducedCard.replaceAll("-", "").contains(reducedInput.replaceAll("-", "")) || reducedCard.replaceAll("-", " ").contains(reducedInput.replaceAll("-", " "));
			}).collect(Collectors.toList()));
		}

		public SearchQuery hasName(String name) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getName().equalsIgnoreCase(name)).collect(Collectors.toList()));
		}

		public SearchQuery hasSupertype(String supertype) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getSupertypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(supertype))).collect(Collectors.toList()));
		}

		public SearchQuery hasType(String type) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getTypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(type))).collect(Collectors.toList()));
		}

		public SearchQuery hasSubtype(String subtype) {
			return new SearchQuery(this.parallelStream().filter(card -> Arrays.stream(card.getSubtypes()).parallel().anyMatch(t -> t.equalsIgnoreCase(subtype))).collect(Collectors.toList()));
		}

		public SearchQuery inSet(@Nullable MtgSet set) {
			if (set == null) return this;
			return new SearchQuery(this.parallelStream().filter(card -> card.getSet().getCode().equalsIgnoreCase(set.getCode())).collect(Collectors.toList()));
		}

		public SearchQuery isOfRarity(MtgCard.Rarity rarity) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getRarity().equals(rarity)).collect(Collectors.toList()));
		}

		public SearchQuery distinctCards() {
			return new SearchQuery(this.stream().filter(StreamUtils.distinctByKey(MtgCard::getName)).collect(Collectors.toList()));
		}

		public SearchQuery hasMultiverseId(int multiverseId) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getMultiverseid() == multiverseId).collect(Collectors.toList()));
		}

		public SearchQuery notLayout(MtgCard.Layout layout) {
			return new SearchQuery(this.parallelStream().filter(card -> !card.getLayout().equals(layout)).collect(Collectors.toList()));
		}

		public SearchQuery hasLayout(MtgCard.Layout layout) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getLayout().equals(layout)).collect(Collectors.toList()));
		}

		public SearchQuery inLanguage(String language) {
			return new SearchQuery(this.parallelStream().filter(card -> card.getLanguage().equalsIgnoreCase(language)).collect(Collectors.toList()));
		}

		public SearchQuery noTokens() {
			return notLayout(MtgCard.Layout.TOKEN);
		}

		public SearchQuery noEmblems() {
			return notLayout(MtgCard.Layout.EMBLEM);
		}

	}
}
