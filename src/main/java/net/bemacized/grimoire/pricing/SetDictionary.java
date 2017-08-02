package net.bemacized.grimoire.pricing;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.oid.MongoId;

import java.util.*;
import java.util.logging.Logger;

public class SetDictionary {

	private static final Logger LOG = Logger.getLogger(SetDictionary.class.getName());

	private List<SetDictionaryItem> dictionary;

	SetDictionary() {
		dictionary = new ArrayList<>();

		// Load initial data
		Grimoire.getInstance().getSets().getSets().forEach(set -> dictionary.add(new SetDictionaryItem(set.getCode(), set.getName())));

		// Load from database
		MongoCursor<SetDictionaryItem> items = Grimoire.getInstance().getDBManager().getJongo().getCollection("SetDictionaryItems").find("{}").as(SetDictionaryItem.class);

		items.forEach(item -> {
			// Remove old entry
			new ArrayList<>(dictionary).parallelStream().filter(i -> i.getCode().equalsIgnoreCase(item.getCode())).forEach(i -> dictionary.remove(i));
			// Insert new entry
			dictionary.add(item);
		});

		LOG.info("Loaded " + items.count() + " sets for the dictionary.");

		save();
	}

	void save() {
		MongoCollection collection = Grimoire.getInstance().getDBManager().getJongo().getCollection("SetDictionaryItems");
		dictionary.forEach(collection::save);
	}

	/**
	 * Finds an item based on the store-specific set name
	 *
	 * @param storeId - The ID of the store to look up the item for
	 * @param setName - The store-specific name for the set we're looking up
	 * @return - The found item
	 */
	public SetDictionaryItem getItemByStore(String storeId, String setName) {
		return dictionary.parallelStream().filter(sdi -> setName.equalsIgnoreCase(sdi.getStoreSetName(storeId))).findFirst().orElse(null);
	}

	/**
	 * Find an item based on its set code
	 *
	 * @param setCode - The set code to look for
	 * @return The item with the specified set code
	 */
	public SetDictionaryItem getItem(String setCode) {
		return dictionary.parallelStream().filter(sdi -> sdi.getCode().equalsIgnoreCase(setCode)).findFirst().orElse(null);
	}

	/**
	 * Fuzzily matches a set item to a rough name provided using Levenshteins.
	 * Useful for matching almost identical, store-specific set names to set codes.
	 *
	 * @param roughName - The rough name to look for
	 * @return - The best matching item
	 */
	public SetDictionaryItem findItem(String roughName) {
		return dictionary.parallelStream()
				.sorted(Comparator.comparingInt(o -> StringUtils.getLevenshteinDistance(roughName, o.getDisplayName())))
				.findFirst().orElse(null);
	}

	public static class SetDictionaryItem {
		@MongoId
		private String code;
		private String displayName;
		private Map<String, String> storeNames;

		public SetDictionaryItem() {

		}

		public SetDictionaryItem(String code, String displayName) {
			this.displayName = displayName;
			this.storeNames = new HashMap<>();
			this.code = code;
		}

		public SetDictionaryItem setStoreSetName(String storeId, String setName) {
			if (setName != null && !setName.isEmpty()) storeNames.put(storeId, setName);
			else storeNames.remove(storeId);
			return this;
		}

		public String getStoreSetName(String storeId) {
			return storeNames.get(storeId);
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getCode() {
			return code;
		}
	}


}
