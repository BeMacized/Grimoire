package net.bemacized.grimoire.model.models.imageproviders;

import net.bemacized.grimoire.model.models.Card;

public class Scryfall extends ImageProvider {

	@Override
	public String getUrl(Card card) {
		String scryfallURL = card.getMultiverseid() > 0 ?
				String.format("https://api.scryfall.com/cards/multiverse/%s?format=image", card.getMultiverseid()) :
				String.format("https://api.scryfall.com/cards/%s/%s?format=image", card.getSet().getCode().toLowerCase(), card.getNumber());
		return imageAvailable(scryfallURL) ? scryfallURL : null;
	}
}
