package net.bemacized.grimoire.controllers;

import net.bemacized.grimoire.Grimoire;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
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
					HttpsURLConnection connection;
					try {
						connection = (HttpsURLConnection) new URL("https://bots.discordlist.net/api.php").openConnection();
						connection.setDoOutput(true);
						connection.setUseCaches(false);
						connection.setRequestMethod("POST");
						connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Could not post server count to bots.discordlist.net, 1", e);
						return;
					}
					String encodedToken = URLEncoder.encode(getToken());
					String body = "token=" + encodedToken + "&servers=" + serverCount;
					try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
						outputStream.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
						outputStream.flush();
						if (connection.getResponseCode() != 200)
							LOG.log(Level.WARNING, "Could not post server count bots.discordlist.net, 2. Status " + connection.getResponseCode());
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Could not post server count to bots.discordlist.net, 3", e);
					}
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
					HttpsURLConnection connection;
					try {
						connection = (HttpsURLConnection) new URL("https://bots.discord.pw/api/bots/" + Grimoire.getInstance().getDiscord().getSelfUser().getId() + "/stats").openConnection();
						connection.setDoOutput(true);
						connection.setRequestMethod("POST");
						connection.addRequestProperty("Authorization", getToken());
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Could not post server count to bots.discord.pw, 1", e);
						return;
					}

					JSONObject data = new JSONObject() {{
						//TODO: ENABLE WHEN IMPLEMENTING SHARDING
//						put("shard_id", Grimoire.getInstance().getDiscord().getShardInfo().getShardId());
//						put("shard_count", Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal());
						put("server_count", serverCount);
					}};

					try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
						outputStream.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
						outputStream.flush();
						if (connection.getResponseCode() != 200)
							LOG.log(Level.WARNING, "Could not post server count to bots.discord.pw, 2. Status " + connection.getResponseCode());
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Could not post server count to bots.discord.pw, 3", e);
					}
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
					JSONObject data = new JSONObject() {{
						//TODO: ENABLE WHEN IMPLEMENTING SHARDING
//						put("shard_id", Grimoire.getInstance().getDiscord().getShardInfo().getShardId());
//						put("shard_count", Grimoire.getInstance().getDiscord().getShardInfo().getShardTotal());
						put("server_count", serverCount);
					}};

					try {
						HttpURLConnection conn = (HttpURLConnection) new URL("https://discordbots.org/api/bots/" + Grimoire.getInstance().getDiscord().getSelfUser().getId() + "/stats").openConnection();
						conn.setDoOutput(true);
						conn.setRequestProperty("Accept-Charset", java.nio.charset.StandardCharsets.UTF_8.name());
						conn.setRequestProperty("Content-Type", "application/json");
						conn.setRequestProperty("Authorization", getToken());

						OutputStream output = conn.getOutputStream();
						output.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
						output.close();
						if (conn.getResponseCode() != 200)
							LOG.log(Level.WARNING, "Could not post server count to discordbots.org, 1. Status " + conn.getResponseCode());
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Could not post server count to discordbots.org, 2", e);

					}
				}
			});
		}

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				reporters.forEach(r -> r.report(Grimoire.getInstance().getDiscord().getGuilds().size()));
			}
		}, 0, REPORT_INTERVAL);

	}

	public abstract class Reporter {

		public abstract String getToken();

		public abstract void report(int serverCount);

	}
}

