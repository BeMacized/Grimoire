package net.bemacized.grimoire.commands.all;

import com.google.maps.GeocodingApi;
import com.google.maps.errors.*;
import com.google.maps.model.GeocodingResult;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LocatorCommand extends BaseCommand {
	@Override
	public String name() {
		return "locator";
	}

	@Override
	public String[] aliases() {
		return new String[]{
				"locate"
		};
	}

	@Override
	public String description() {
		return "Find game stores and events via the Wizards Locator";
	}

	@Override
	public String[] usages() {
		return new String[]{
				"<location>"
		};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"Sydney, Australia",
				"1027 Newport Avenue Pawtucket, RI 02862"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {


		// Make sure that the module is enabled
		if (Grimoire.getInstance().getGeoAPI() == null) {
			sendErrorEmbed(e.getChannel(), "This command has been disabled as no API key has been provided for the Google Geocoding API. Please set environment variable `GOOGLE_API_KEY`.");
			return;
		}

		// Make sure a location was entered
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please enter a location to check.");
			return;
		}

		// Send initial load message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Searching Wizards Locator....", true);

		try {
			// Get location
			final GeocodingResult[] results = GeocodingApi.geocode(Grimoire.getInstance().getGeoAPI(), rawArgs).await();

			// Get results
			if (results.length == 0) {
				loadMsg.complete(errorEmbed("There were no results for your query.").get(0));
				return;
			}

			// Construct locator request body
			final JSONObject body = new JSONObject() {{
				put("language", "en-us");
				put("page", 1);
				put("count", 8);
				put("filter_mass_markets", true);
				put("request", new JSONObject() {{
					put("LatestEventStartDate", JSONObject.NULL);
					put("EarliestEventStartDate", JSONObject.NULL);
					put("MarketingProgramCodes", new JSONArray());
					put("SalesBrandCodes", new JSONArray() {{
						put("MG");
					}});
					put("PlayFormatCodes", new JSONArray());
					put("EventTypeCodes", new JSONArray());
					put("ProductLineCodes", new JSONArray() {{
						put("MG");
					}});
					put("LocalTime", "/Date(" + System.currentTimeMillis() + ")/");
					put("North", Arrays.stream(results).mapToDouble(r -> r.geometry.location.lat).max().orElse(Double.NaN));
					put("East", Arrays.stream(results).mapToDouble(r -> r.geometry.location.lng).max().orElse(Double.NaN));
					put("South", Arrays.stream(results).mapToDouble(r -> r.geometry.location.lat).min().orElse(Double.NaN));
					put("West", Arrays.stream(results).mapToDouble(r -> r.geometry.location.lng).min().orElse(Double.NaN));
				}});
			}};

			// Define locator URL
			final String locatorServiceURL = "http://locator.wizards.com/Service/LocationService.svc/GetLocations";

			// Request locations
			HttpResponse<JsonNode> resp = Unirest.post(locatorServiceURL).header("Content-Type", "application/json").body(body).asJson();

			// Convert locations to stream
			List<JSONObject> responseList = StreamSupport.stream(Spliterators.spliteratorUnknownSize(resp.getBody().getObject().getJSONObject("d").getJSONArray("Results").iterator(), Spliterator.ORDERED), false).map(JSONObject.class::cast).collect(Collectors.toList());

			// Get map image
			HttpResponse<InputStream> mapData = Unirest.get("https://maps.googleapis.com/maps/api/staticmap")
					.queryString(new HashMap<String, Object>() {{
						put("key", System.getenv("GOOGLE_API_KEY"));
						put("size", "960x360");
						put("markers", String.format(
								"color:0x%s|%s",
								String.format("%02x%02x%02x", Globals.EMBED_COLOR_PRIMARY.getRed(), Globals.EMBED_COLOR_PRIMARY.getGreen(), Globals.EMBED_COLOR_PRIMARY.getBlue()).toUpperCase(),
								String.join("|", responseList.parallelStream().map(json -> {
									JSONObject address = json.getJSONObject("Address");
									return address.getDouble("Latitude") + "," + address.getDouble("Longitude");
								}).collect(Collectors.toList()))
						));
					}}).asBinary();

			if (mapData.getStatus() != 200) {
				switch (mapData.getStatus()) {
					case 403: {
						sendErrorEmbed(e.getChannel(), "Could not generate map image as I was not permitted to by the Google Static Map API. Please make sure the Static Map API is enabled for your API Token.");
						break;
					}
				}
			}

			// Build Embed
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Wizards Locator Results", String.format("http://locator.wizards.com/#brand=magic&a=search&p=%s&massmarket=no", URLEncoder.encode(rawArgs, "UTF-8")));
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);
			eb.appendDescription("The following results were found for **\"" + rawArgs + "\"**");

			// Insert stores into embed
			for (JSONObject store : responseList) {
				JSONObject organization = store.getJSONObject("Organization");
				JSONObject address = store.getJSONObject("Address");
				StringBuilder description = new StringBuilder();
				if (address != null) {
					String addrStr = address.getString("Format");
					for (String key : address.keySet().parallelStream().filter(k -> address.get(k) instanceof String).collect(Collectors.toList()))
						addrStr = addrStr.replaceAll("\\{" + key + "\\}", address.getString(key));
					addrStr = addrStr
							.replaceAll("<BR/>", "\n")
							.replaceAll("\\{Country\\}", address.getString("CountryName"))
							.replaceAll("\\{Region\\}", "")
							.replaceAll("[\n]{2,}", "\n");
					description.append("\n").append(addrStr);
				}
				if (organization != null) {
					if (organization.getString("Phone") != null)
						description.append("\n").append("**Tel.** ").append(organization.getString("Phone"));
					if (organization.getString("Email") != null)
						description.append("\n").append("**Email:** ").append(organization.getString("Email"));
				}
				if (organization != null && address != null)
					description.append("\n").append(String.format("[Locator Page](http://locator.wizards.com/#brand=magic&a=location&p=%s&massmarket=no&loc=%s&orgid=%s&addrid=%s)", URLEncoder.encode(rawArgs, "UTF-8"), address.getInt("Id"), organization.getInt("Id"), address.getInt("Id")));
				String name = (organization != null && organization.getString("Name") != null) ? organization.getString("Name") : (address != null && address.getString("Name") != null) ? address.getString("Name") : String.valueOf(store.getInt("Id"));
				eb.addField(name, description.toString().trim(), true);
			}

			// Send embed
			if (mapData.getStatus() == 200)
				e.getChannel().sendFile(mapData.getBody(), "map.png", new MessageBuilder().setEmbed(eb.build()).build()).submit();
			else e.getChannel().sendMessage(eb.build()).submit();
			loadMsg.complete();
		} catch (ApiException | InterruptedException | IOException | UnirestException ex) {
			if (ex instanceof RequestDeniedException) {
				loadMsg.complete(errorEmbed("Could not contact the Google Geocoding API: " + ex.getMessage()).get(0));
				return;
			}
			if (ex instanceof OverDailyLimitException || ex instanceof OverQueryLimitException) {
				loadMsg.complete(errorEmbed("The locator command has been used too often and Google started blocking our requests. Please try again later!").get(0));
				return;
			}
			if (ex instanceof ZeroResultsException || ex instanceof NotFoundException) {
				loadMsg.complete(errorEmbed("There were no results for your query.").get(0));
				return;
			}
			LOG.log(Level.SEVERE, "An unknown error occurred when contacting the Google Geocoding API", ex);
			loadMsg.complete(errorEmbed("An unknown error occurred while contacting the Google Geocoding API. This incident has been reported.").get(0));
		}
	}
}
