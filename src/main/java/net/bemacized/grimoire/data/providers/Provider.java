package net.bemacized.grimoire.data.providers;

import java.util.logging.Logger;

abstract class Provider {

	protected final Logger LOG = Logger.getLogger(this.getClass().getName());

	public void load() {
		if (!loadFromDB()) loadFromSource();
	}

	abstract boolean loadFromDB();

	abstract void saveToDB();

	public abstract void loadFromSource();

}
