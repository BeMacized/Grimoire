package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.card.MtgCard;
import net.bemacized.grimoire.data.providers.CardProvider;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvalHandler extends ChatHandler {

	public EvalHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
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
		code = String.format(String.join("\n",new String[]{
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
		se.put("event", e);
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
