package net.bemacized.grimoire.data.retrievers;

import com.google.gson.Gson;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.bemacized.grimoire.data.models.standard.StandardSet;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StandardSetsRetriever {

	private static final String SOURCE = "http://whatsinstandard.com/api/4/sets.json";
	private static final String CHARSET = "UTF-8";

	private static final Logger LOG = Logger.getLogger(StandardSetsRetriever.class.getName());

	public static List<StandardSet> retrieveStandardSets() throws UnirestException {
		LOG.info("Retrieving standard sets...");

		// Parse json
		Gson gson = new Gson();

		Iterator<Object> setIterator = Unirest.get(SOURCE).asJson().getBody().getArray().iterator();
		Iterable<Object> setIterable = () -> setIterator;
		List<StandardSet> sets = StreamSupport.stream(setIterable.spliterator(), true).map(o -> gson.fromJson(o.toString(), StandardSet.class)).collect(Collectors.toList());

		// Sort sets
		sets.sort(Comparator.comparing(StandardSet::getEnterDate));

		LOG.info("Retrieved " + sets.size() + " standard sets");
		return sets;
	}
}
