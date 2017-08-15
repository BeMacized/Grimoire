package net.bemacized.grimoire.data.retrievers.imageproviders;

import net.bemacized.grimoire.data.models.MtgCard;

import javax.annotation.Nullable;

public class Scryfall extends ImageService {

	@Nullable
	@Override
	public String getUrl(MtgCard card) {
		String url;
		if (card.getMultiverseid() > 0)
			url = String.format("https://api.scryfall.com/cards/multiverse/%s?format=image", card.getMultiverseid());
		else if (card.getNumber() != null && !card.getName().isEmpty())
			url = String.format("https://api.scryfall.com/cards/%s/%s?format=image", card.getSet().getCode().toLowerCase(), card.getNumber());
		else url = String.format("https://api.scryfall.com/cards/%s?format=image", card.getScryfallId());
		return imageAvailable(url) ? url : null;
	}
}
