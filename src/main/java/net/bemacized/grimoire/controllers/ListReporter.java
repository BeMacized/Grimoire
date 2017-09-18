package net.bemacized.grimoire.controllers;

import com.mashape.unirest.http.Unirest;
import net.bemacized.grimoire.Grimoire;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class ListReporter {

	private static final Logger LOG = Logger.getLogger(ListReporter.class.getName());
	private static final long REPORT_INTERVAL = 5 * 60 * 1000;

	private List<Reporter> reporters;

	public ListReporter() {

		reporters = new ArrayList<>();

		if (System.getenv("BOTSDISCORDLISTNET_API_TOKEN") != null) {
			reporters.add(new Reporter() {
				@Override
				public String getToken() {
					return System.getenv("BOTSDISCORDLISTNET_API_TOKEN");
				}

				@Override
				public void report(int serverCount) {
					Unirest.post("https://bots.discordlist.net/api.php")
							.header("Authorization", getToken())
							.header("Content-Type", "application/x-www-form-urlencoded")
							.body("token=" + URLEncoder.encode(getToken()) + "&servers=" + serverCount)
							.asJsonAsync();
				}
			});
		}

		if (System.getenv("BOTSDISCORDPW_API_TOKEN") != null) {
			reporters.add(new Reporter() {
				@Override
				public String getToken() {
					return System.getenv("BOTSDISCORDPW_API_TOKEN");
				}

				@Override
				public void report(int serverCount) {
					JSONObject data = new JSONObject();
					data.put("server_count", serverCount);
					if (Grimoire.getInstance().getDiscord().getShardInfo() != null && Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal() > 1) {
						data.put("shard_id", Grimoire.getInstance().getDiscord().getShardInfo().getShardId());
						data.put("shard_count", Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal());
					}
					Unirest.post("https://bots.discord.pw/api/bots/" + Grimoire.getInstance().getDiscord().getSelfUser().getId() + "/stats")
							.header("Authorization", getToken())
							.header("Content-Type", "application/json")
							.body(data.toString())
							.asJsonAsync();
				}
			});
		}

		if (System.getenv("DISCORDBOTSORG_API_TOKEN") != null) {
			reporters.add(new Reporter() {
				@Override
				public String getToken() {
					return System.getenv("DISCORDBOTSORG_API_TOKEN");
				}

				@Override
				public void report(int serverCount) {
					JSONObject data = new JSONObject();
					data.put("server_count", serverCount);
					if (Grimoire.getInstance().getDiscord().getShardInfo() != null && Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal() > 1) {
						data.put("shard_id", Grimoire.getInstance().getDiscord().getShardInfo().getShardId());
						data.put("shard_count", Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal());
					}
					Unirest.post("https://discordbots.org/api/bots/" + Grimoire.getInstance().getDiscord().getSelfUser().getId() + "/stats")
							.header("Authorization", getToken())
							.header("Accept-Charset", java.nio.charset.StandardCharsets.UTF_8.name())
							.header("Content-Type", "application/json")
							.body(data.toString())
							.asJsonAsync();
				}
			});
		}

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				reporters.forEach(r -> r.report(Grimoire.getInstance().getDiscord().getGuilds().size()));
			}
		}, 20 * 1000, REPORT_INTERVAL);

	}

	public abstract class Reporter {

		public abstract String getToken();

		public abstract void report(int serverCount);

	}
}

