package net.bemacized.grimoire.data.retrievers;

import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.imageproviders.Gatherer;
import net.bemacized.grimoire.data.retrievers.imageproviders.ImageService;
import net.bemacized.grimoire.data.retrievers.imageproviders.Scryfall;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CardImageRetriever {

	private List<ImageService> imageServices;

	public CardImageRetriever() {
		this.imageServices = Stream.of(
				new Scryfall(),
				new Gatherer()
		).collect(Collectors.toList());
	}

	@Nullable
	public String findUrl(MtgCard card, GuildPreferences guildPreferences) {
		List<ImageService> s = new ArrayList<>(imageServices);
		if (guildPreferences.preferLQImages()) Collections.reverse(s);
		return s.parallelStream().map(p -> p.getUrl(card, !guildPreferences.disableImageVerification())).filter(Objects::nonNull).findFirst().orElse(null);
	}
}
