package net.bemacized.grimoire.controllers;

import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class EmojiParser {

	private static final Logger LOG = Logger.getLogger(EmojiParser.class.getName());

	private Map<String, Emote> emojiMap;

	public EmojiParser() {
		this.emojiMap = new HashMap<>();

		// Get guild id
		long emojiGuildId;
		try {
			if (System.getenv("EMOJI_GUILD") == null || System.getenv("EMOJI_GUILD").isEmpty()) {
				LOG.warning("Environment variable EMOJI_GUILD has NOT BEEN SET. Emoji parsing will be disabled.");
				return;
			} else {
				emojiGuildId = Long.parseLong(System.getenv("EMOJI_GUILD"));
			}
		} catch (NumberFormatException nfe) {
			LOG.warning("Environment variable EMOJI_GUILD is NOT AN ID. Emoji parsing will be disabled.");
			return;
		}

		// Get guild
		Guild g = Grimoire.getInstance().getDiscord().getGuildById(emojiGuildId);

		// Verify guild
		if (g == null) {
			LOG.warning("Bot is not a member of guild specified in environment variable EMOJI_GUILD. Emoji parsing will be disabled.");
			return;
		}

		// Populate emoji map
		symbolTable.forEach((key, value) -> {
			if (value == null || value.isEmpty()) return;
			g.getEmotesByName(value, true).stream().findFirst().ifPresent(em -> emojiMap.put(key, em));
		});

		LOG.info("Loaded " + emojiMap.size() + "/" + symbolTable.size() + " guild emoji.");
	}

	public String parseEmoji(String text, @Nullable Guild guild) {
		// Private chat or guild which allows external emoji
		if (guild == null || guild.getSelfMember().hasPermission(Permission.MESSAGE_EXT_EMOJI)) {
			for (Map.Entry<String, Emote> e : emojiMap.entrySet())
				text = text.replaceAll(Pattern.quote(e.getKey()), e.getValue().getAsMention());
		}
		// Otherwise try using the guild's own emoji
		else {
			for (Map.Entry<String, String> e : symbolTable.entrySet()) {
				Emote emote = guild.getEmotesByName(e.getValue(), true).parallelStream().findAny().orElse(null);
				if (emote == null) continue;
				text = text.replaceAll(Pattern.quote(e.getKey()), emote.getAsMention());
			}
		}
		return text;
	}

	private static Map<String, String> symbolTable = new HashMap<String, String>() {{
		put("{G/P}", "managp");
		put("{T}", "manat");
		put("{R/W}", "manarw");
		put("{C}", "manac");
		put("{U}", "manau");
		put("{2/W}", "mana2w");
		put("{R/G}", "manarg");
		put("{G}", "manag");
		put("{U/B}", "manaub");
		put("{4}", "mana4");
		put("{R}", "manar");
		put("{3}", "mana3");
		put("{Q}", "manaq");
		put("{2/R}", "mana2r");
		put("{G/U}", "managu");
		put("{16}", "mana16");
		put("{9}", "mana9");
		put("{1}", "mana1");
		put("{15}", "mana15");
		put("{8}", "mana8");
		put("{2/B}", "mana2b");
		put("{2/G}", "mana2g");
		put("{B}", "manab");
		put("{20}", "mana20");
		put("{B/G}", "manabg");
		put("{E}", "manae");
		put("{B/R}", "manabr");
		put("{S}", "manas");
		put("{U/R}", "manaur");
		put("{G/W}", "managw");
		put("{R/P}", "manarp");
		put("{CHAOS}", "manachaos");
		put("{W/B}", "manawb");
		put("{5}", "mana5");
		put("{7}", "mana7");
		put("{B/P}", "manabp");
		put("{U/P}", "manaup");
		put("{W/U}", "manawu");
		put("{10}", "mana10");
		put("{0}", "mana0");
		put("{14}", "mana14");
		put("{11}", "mana11");
		put("{X}", "manax");
		put("{13}", "mana13");
		put("{W/P}", "manawp");
		put("{2}", "mana2");
		put("{W}", "manaw");
		put("{12}", "mana12");
		put("{2/U}", "mana2u");
		put("{6}", "mana6");
		// The values below will be unsupported for now
		put("{P}", "");
		put("{½}", "");
		put("{hw}", "");
		put("{hr}", "");
		put("{1000000}", "");
		put("{∞}", "");
		put("{100}", "");
		put("{Y}", "");
		put("{Z}", "");
	}};


}
