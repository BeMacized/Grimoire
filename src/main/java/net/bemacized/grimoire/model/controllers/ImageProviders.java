package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.model.models.Card;
import net.bemacized.grimoire.model.models.imageproviders.Gatherer;
import net.bemacized.grimoire.model.models.imageproviders.ImageProvider;
import net.bemacized.grimoire.model.models.imageproviders.Scryfall;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImageProviders {

	private List<ImageProvider> imageProviders;

	public ImageProviders() {
		this.imageProviders = Stream.of(
			new Scryfall(),
			new Gatherer()
		).collect(Collectors.toList());
	}

	public String findUrl(Card card) {
		return imageProviders.parallelStream().map(p -> p.getUrl(card)).filter(Objects::nonNull).findFirst().orElse(null);
	}
}
