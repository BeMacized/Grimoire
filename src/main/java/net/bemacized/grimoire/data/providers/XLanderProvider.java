package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;
import net.bemacized.grimoire.data.retrievers.XLanderRetriever;
import net.bemacized.grimoire.utils.TimedValue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XLanderProvider {

	private static final long TIMEOUT = 6 * 60 * 60 * 1000;

	private static final Logger LOG = Logger.getLogger(XLanderProvider.class.getName());
	private TimedValue<List<MtgCard>> highlanderBanlist;

	public XLanderProvider() {
		highlanderBanlist = new TimedValue<List<MtgCard>>(TIMEOUT) {
			@Override
			public List<MtgCard> refresh() {
				try {
					return XLanderRetriever.retrieveHighlanderBanlist().parallelStream()
							.map(name -> {
								try {
									return Grimoire.getInstance().getCardProvider().getCardsByScryfallQuery(name).parallelStream().findFirst().orElse(null);
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
	}

	@Nullable
	public List<MtgCard> getHighlanderBanlist() {
		return highlanderBanlist.get();
	}
}
