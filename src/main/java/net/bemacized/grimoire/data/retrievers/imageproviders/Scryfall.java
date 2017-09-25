package net.bemacized.grimoire.data.retrievers.imageproviders;

import net.bemacized.grimoire.data.models.card.MtgCard;

import javax.annotation.Nullable;

public class Scryfall extends ImageService {

	@Nullable
	@Override
	public String getUrl(MtgCard card, boolean checkAvailability) {
		String url = card.getScryfallImageUrl();
		return (!checkAvailability || imageAvailable(url)) ? url : null;
	}
}
