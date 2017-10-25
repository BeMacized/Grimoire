package net.bemacized.grimoire.data.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.data.retrievers.XLanderRetriever;
import net.bemacized.grimoire.utils.TimedValue;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XLanderProvider {

	private static final long TIMEOUT = 6 * 60 * 60 * 1000;

	private static final Logger LOG = Logger.getLogger(XLanderProvider.class.getName());
	private TimedValue<List<MtgCard>> highlanderBanlist;
	private TimedValue<Map<String, Integer>> canlanderPointsList;
	private TimedValue<Map<String, Integer>> auslanderPointsList;

	public XLanderProvider() {
		highlanderBanlist = new TimedValue<List<MtgCard>>(TIMEOUT) {
			@Override
			public List<MtgCard> refresh() {
				try {
					return XLanderRetriever.retrieveHighlanderBanlist().parallelStream()
							.map(name -> {
								try {
									return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery("!\"" + name + "\"").parallelStream().findFirst().orElse(null);
								} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException | ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
									LOG.log(Level.SEVERE, "An error occurred contacting scryfall for processing the highlander banlist.");
								} catch (ScryfallRetriever.ScryfallRequest.NoResultException e) {
									LOG.log(Level.WARNING, "Could not find card from Highlander banlist on Scryfall: '" + name + "'", e);
								}
								return null;
							})
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Highlander Banlist could not be fetched", e);
				}
				return null;
			}
		};
		canlanderPointsList = new TimedValue<Map<String, Integer>>(TIMEOUT) {
			@Override
			public Map<String, Integer> refresh() {
				JsonObject list;
				try {
					list = new JsonParser().parse(IOUtils.toString(GuildPreferences.class.getResourceAsStream("/CanlanderPoints.json"))).getAsJsonObject();
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Could not parse CanlanderPoints.json", e);
					return new HashMap<>();
				}
				return list.entrySet().parallelStream().collect(Collectors.toMap(p -> p.getKey().toLowerCase(), p -> p.getValue().getAsInt()));
			}
		};

		auslanderPointsList = new TimedValue<Map<String, Integer>>(TIMEOUT) {
			@Override
			public Map<String, Integer> refresh() {
				JsonObject list;
				try {
					list = new JsonParser().parse(IOUtils.toString(GuildPreferences.class.getResourceAsStream("/AuslanderPoints.json"))).getAsJsonObject();
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Could not parse AuslanderPoints.json", e);
					return new HashMap<>();
				}
				return list.entrySet().parallelStream().collect(Collectors.toMap(p -> p.getKey().toLowerCase(), p -> p.getValue().getAsInt()));
			}
		};
	}

	@Nullable
	public List<MtgCard> getHighlanderBanlist() {
		return highlanderBanlist.get();
	}

	@SuppressWarnings("ConstantConditions")
	@Nonnull
	public Map<String, Integer> getCanlanderPointsList() {
		return canlanderPointsList.get();
	}

	@SuppressWarnings("ConstantConditions")
	@Nonnull
	public Map<String, Integer> getAuslanderPointsList() {
		return auslanderPointsList.get();
	}
}
