package net.bemacized.grimoire.data.providers;

import java.util.logging.Logger;

abstract class CachedProvider {

	protected final Logger LOG;

	public CachedProvider() {
		LOG = Logger.getLogger(this.getClass().getName());
	}

	public void load() {
		if (!loadFromDB()) loadFromSource();
	}

	abstract boolean loadFromDB();

	abstract void saveToDB();

	public abstract void loadFromSource();
}
