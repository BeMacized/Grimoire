package net.bemacized.grimoire.utils;

public abstract class TimedValue<T> {

	private long max_age;
	private T value;
	private long timestamp;

	public TimedValue(long max_age) {
		this.max_age = max_age;
	}

	public T get() {
		if (value == null || System.currentTimeMillis() - timestamp > max_age) {
			timestamp = System.currentTimeMillis();
			value = refresh();
		}
		return value;
	}

	public abstract T refresh();
}
