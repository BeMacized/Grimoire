package net.bemacized.grimoire.data.retrievers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bemacized.grimoire.Grimoire;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;

public class GuildPreferenceRetriever {

	private final static String SOURCE = Grimoire.WEBSITE + "/api/guildpreferences/";

	@Nullable
	public static String retrieveSettings(String guildId) {
		try {
			JsonObject json = new JsonParser().parse(IOUtils.toString(new URL(SOURCE + guildId),"UTF-8")).getAsJsonObject();
			return json.get("preferences").getAsString();
		} catch (IOException e) {
			return null;
		}
	}

}
