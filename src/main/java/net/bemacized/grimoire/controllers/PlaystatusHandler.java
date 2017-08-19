package net.bemacized.grimoire.controllers;

import com.google.gson.Gson;
import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.entities.Game;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlaystatusHandler {

	private static final Logger LOG = Logger.getLogger(PlaystatusHandler.class.getName());
	private static final int CHANGE_INTERVAL = 30 * 1000;

	private List<PlaystatusLines> statuses;
	private Random random;

	public PlaystatusHandler() {
		String json;
		try {
			json = IOUtils.toString(getClass().getResourceAsStream("/Playstatuses.json"), "UTF-8");
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Cannot load play statuses. Disabling playstatus handler.");
			return;
		}

		this.random = new Random();

		Gson gson = new Gson();
		this.statuses = gson.fromJson(json, PlaystatusFile.class).getData();

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Grimoire.getInstance().getDiscord().getPresence().setGame(Game.of(getRandomStatus()));
			}
		}, 0, CHANGE_INTERVAL);
	}

	private String getRandomStatus() {
		int set = random.nextInt(statuses.parallelStream().mapToInt(PlaystatusLines::getRarity).sum());
		int total = 0;
		for (PlaystatusLines status : statuses) {
			total += status.getRarity();
			if (set < total) return status.getTexts()[random.nextInt(status.getTexts().length)];
		}
		return "";
	}

	public class PlaystatusFile {
		private List<PlaystatusLines> data;

		public List<PlaystatusLines> getData() {
			return data;
		}
	}

	public class PlaystatusLines {
		private int rarity;
		private String[] texts;

		public int getRarity() {
			return rarity;
		}

		public String[] getTexts() {
			return texts;
		}
	}
}
