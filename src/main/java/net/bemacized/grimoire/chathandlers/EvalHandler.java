package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class EvalHandler extends ChatHandler {

	public EvalHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, GuildPreferences guildPreferences,  ChatHandler next) {
		String code = e.getMessage().getRawContent();

		// Only allow dev to execute code, and only when enabled with an env variable
		boolean moduleEnabled = System.getenv("ENABLE_EVAL_MODULE") != null && (System.getenv("ENABLE_EVAL_MODULE").equalsIgnoreCase("true") || System.getenv("ENABLE_EVAL_MODULE").equalsIgnoreCase("1"));
		if (!e.getMessage().getAuthor().getId().equalsIgnoreCase(Grimoire.DEV_ID) || !moduleEnabled || (!code.matches("^[`]{3}(javascript|javascript)[\\r\\n]([^\\r\\n]*[\\r\\n])+[`]{3}[\\r\\n]?$") && !code.matches("!eval .*"))) {
			next.handle(e);
			return;
		}

		e.getMessage().addReaction("\uD83D\uDD04").submit();

		//Extract from code block
		code = (code.startsWith("!eval ")) ? code.substring(6).trim() : code.substring(13, code.length() - 3).trim();

		//Wrap for imports
		code = String.format(String.join("\n", new String[]{
				"load(\"nashorn:mozilla_compat.js\");",
				"importPackage(Packages.net.bemacized.grimoire.data.models.card);",
				"importPackage(Packages.net.bemacized.grimoire.data.models.mtgjson);",
				"importPackage(Packages.net.bemacized.grimoire.data.models.rules);",
				"importPackage(Packages.net.bemacized.grimoire.data.models.scryfall);",
				"importPackage(java.util.stream);",
				"importClass(java.lang.String);",
				"%s"
		}), code);

		ScriptEngine se = new ScriptEngineManager().getEngineByName("Nashorn");

		Grimoire.getInstance().getDBManager().getJongo().getCollection("MtgCards").drop();
		Grimoire.getInstance().getDBManager().getJongo().getCollection("MtgSets").drop();

		se.put("event", e);
		se.put("grimoire", Grimoire.getInstance());
		se.put("jda", e.getJDA());
		se.put("guild", e.getGuild());
		se.put("channel", e.getChannel());
		se.put("query", new CardProvider.SearchQuery());
//		se.put("_", new UtilMethods());
		try {
			String result = se.eval(code).toString();
			new MessageBuilder().appendCodeBlock(result, "javascript");
			for (String s : MessageUtils.splitMessage(result))
				e.getChannel().sendMessage(new MessageBuilder().appendCodeBlock(s, "javascript").build()).queue();
			e.getMessage().addReaction("✅").queue();
		} catch (Exception ex) {
			ex.printStackTrace();
			new MessageBuilder().appendCodeBlock(ex.toString(), "javascript").buildAll(MessageBuilder.SplitPolicy.ANYWHERE).forEach(msg -> e.getChannel().sendMessage(msg).queue());
			e.getMessage().addReaction("❌").queue();
		}
	}

//	public class UtilMethods {
//
//	}
}
