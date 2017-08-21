package net.bemacized.grimoire.data.retrievers.storeretrievers;

import net.bemacized.grimoire.data.models.card.MtgCard;

import java.util.HashMap;

public class ScryfallRetriever extends StoreRetriever {
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
		card.updateScryfall();
		return new StoreCardPriceRecord(card.getName(), card.getSet().getCode(), null, System.currentTimeMillis(), getStoreId(), new HashMap<String, Price>() {{
			if (card.getScryfallCard().getEur() != null) put("", new Price(Double.parseDouble(card.getScryfallCard().getEur()), Currency.EUR));
			if (card.getScryfallCard().getTix() != null) put("", new Price(Double.parseDouble(card.getScryfallCard().getTix()), Currency.TIX));
			if (card.getScryfallCard().getUsd() != null) put("", new Price(Double.parseDouble(card.getScryfallCard().getUsd()), Currency.USD));
		}});
	}

}
