package net.bemacized.grimoire.data.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.retrievers.GuildPreferenceRetriever;
import net.dv8tion.jda.core.entities.Guild;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuildPreferenceProvider {

	private static final Logger LOG = Logger.getLogger(GuildPreferenceProvider.class.getName());

	private static final long MAX_PREFERENCE_AGE = 5 * 60 * 1000;

	private JsonArray defaultPreferences;
	private String defaultPreferenceString;

	private Set<GuildPreferences> guildPreferenceList;

	public GuildPreferenceProvider() {
		this.guildPreferenceList = new HashSet<>();
	}

	public GuildPreferences getPreferences(Guild guild) {
		return getPreferences(guild, false);
	}

	public GuildPreferences getPreferences(Guild guild, boolean forceReload) {
		if (guild == null) return new GuildPreferences(null, getDefaultPreferenceString());
		GuildPreferences preferences = this.guildPreferenceList.parallelStream().filter(p -> p.getGuildId().equals(guild.getId())).findFirst().orElse(null);
		if (!forceReload && preferences != null && System.currentTimeMillis() - preferences.getTimestamp() < MAX_PREFERENCE_AGE)
			return preferences;
		if (preferences != null) guildPreferenceList.remove(preferences);
		preferences = new GuildPreferences(guild.getId(), GuildPreferenceRetriever.retrieveSettings(guild.getId()));
		guildPreferenceList.add(preferences);
		return preferences;
	}

	@Nullable
	public JsonArray getDefaultPreferences() {
		if (defaultPreferences == null) {
			try {
				defaultPreferences = new JsonParser().parse(IOUtils.toString(GuildPreferences.class.getResourceAsStream("/GuildPreferences.json"))).getAsJsonArray();
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Cannot load default guild preferences!", e);
			}
		}
		return defaultPreferences;
	}

	@Nullable
	public String getDefaultPreferenceString() {
		if (defaultPreferenceString == null) {
			defaultPreferences = getDefaultPreferences();
			if (defaultPreferences == null) return null;
			final StringBuilder sb = new StringBuilder();
			defaultPreferences.forEach(p -> {
				String key = Integer.toHexString(p.getAsJsonObject().get("id").getAsInt());
				String value = Integer.toHexString(p.getAsJsonObject().get("default").getAsInt());
				if (key.length() == 1) key = "0" + key;
				if (value.length() == 1) value = "0" + value;
				sb.append(key);
				sb.append(value);
			});
			defaultPreferenceString = sb.toString();
		}
		return defaultPreferenceString;
	}

	public boolean validPreferenceString(String str) {
		try {
			new GuildPreferences("", str);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
