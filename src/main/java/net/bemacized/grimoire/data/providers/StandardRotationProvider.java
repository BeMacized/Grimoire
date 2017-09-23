package net.bemacized.grimoire.data.providers;

import com.mashape.unirest.http.exceptions.UnirestException;
import net.bemacized.grimoire.data.models.standard.StandardSet;
import net.bemacized.grimoire.data.retrievers.StandardSetsRetriever;
import net.bemacized.grimoire.utils.TimedValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StandardRotationProvider {

	private static final long TIMEOUT = 6 * 60 * 60 * 1000;

	private static final Logger LOG = Logger.getLogger(StandardRotationProvider.class.getName());
	private TimedValue<List<StandardSet>> sets;

	public StandardRotationProvider() {
		sets = new TimedValue<List<StandardSet>>(TIMEOUT) {
			@Override
			public List<StandardSet> refresh() {
				try {
					return StandardSetsRetriever.retrieveStandardSets();
				} catch (UnirestException e) {
					LOG.log(Level.SEVERE, "Standard rotation could not be fetched", e);
				}
				return null;
			}
		};
	}

	@Nullable
	public List<StandardSet> getSets() {
		return sets.get();
	}
}
