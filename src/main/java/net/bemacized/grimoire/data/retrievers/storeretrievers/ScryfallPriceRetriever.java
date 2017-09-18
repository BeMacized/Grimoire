package net.bemacized.grimoire.data.retrievers.storeretrievers;

import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.retrievers.ScryfallRetriever;

import java.util.HashMap;
import java.util.logging.Level;

public class ScryfallPriceRetriever extends StoreRetriever {


	@Override
	public String getStoreName() {
		return "Scryfall";
	}

	@Override
	public String getStoreId() {
		return "SCF";
	}

	@Override
	public String[] supportedLanguages() {
		return new String[]{"English"};
	}

	@Override
	public long timeout() {
		return 1000 * 60 * 60 * 6; // 6 hours
	}

	@Override
	protected StoreCardPriceRecord _retrievePrice(MtgCard card) throws StoreAuthException, StoreServerErrorException, UnknownStoreException {
		try {
			ScryfallCard sc = ScryfallRetriever.getCardByScryfallId(card.getScryfallId());
			return new StoreCardPriceRecord(card.getName(), card.getSet().getCode(), null, System.currentTimeMillis(), getStoreId(), new HashMap<String, Price>() {{
				if (sc.getEur() != null)
					put("Paper (EUR)", new Price(Double.parseDouble(sc.getEur()), Currency.EUR));
				if (sc.getTix() != null)
					put("MTGO", new Price(Double.parseDouble(sc.getTix()), Currency.TIX));
				if (sc.getUsd() != null)
					put("Paper (USD)", new Price(Double.parseDouble(sc.getUsd()), Currency.USD));
			}});
		} catch (ScryfallRetriever.ScryfallRequest.UnknownResponseException | ScryfallRetriever.ScryfallRequest.NoResultException | ScryfallRetriever.ScryfallRequest.ScryfallErrorException e) {
			LOG.log(Level.SEVERE, "Could not retrieve price for known scryfall card", e);
			throw new UnknownStoreException();
		}


	}

}
