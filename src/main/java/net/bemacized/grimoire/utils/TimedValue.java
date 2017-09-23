package net.bemacized.grimoire.utils;

import javax.annotation.Nullable;

public abstract class TimedValue<T> {

	private long max_age;
	private T value;
	private long timestamp;

	private boolean refreshing;

	public TimedValue(long max_age) {
		this.max_age = max_age;
	}

	@Nullable
	public T get() {
		if ((value == null || System.currentTimeMillis() - timestamp > max_age) && !refreshing) {
			refreshing = true;
			timestamp = System.currentTimeMillis();
			T v = refresh();
			if (v != null) value = v;
			refreshing = false;
		}
		return value;
	}

	public abstract T refresh();
}
