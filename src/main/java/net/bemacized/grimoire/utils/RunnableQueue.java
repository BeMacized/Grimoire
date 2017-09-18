package net.bemacized.grimoire.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RunnableQueue {

	private int interval;
	private long lastAction;
	private List<Timer> timers;

	public RunnableQueue(int interval) {
		this.interval = interval;
		lastAction = 0;
		timers = new ArrayList<>();
	}

	public void queue(Runnable r) {
		if (isEmpty()) {
			lastAction = System.currentTimeMillis();
			r.run();
		} else if (interval <= 0) {
			lastAction = System.currentTimeMillis();
			r.run();
		} else {
			Timer timer = new Timer();
			lastAction += interval;
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					timers.remove(timer);
					r.run();
				}
			}, lastAction - System.currentTimeMillis());
		}
	}

	public void cancel() {
		timers.forEach(Timer::cancel);
		timers.clear();
	}

	public boolean isEmpty() {
		return System.currentTimeMillis() - lastAction >= interval;
	}
}
