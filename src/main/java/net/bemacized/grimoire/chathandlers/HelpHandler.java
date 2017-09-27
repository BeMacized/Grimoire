package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.commands.all.HelpCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HelpHandler extends ChatHandler {

	public HelpHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, GuildPreferences guildPreferences, ChatHandler next) {
		// Only enable in PM
		if (e.getGuild() != null) {
			next.handle(e);
			return;
		}

		final String msg = e.getMessage().getContent();

		// Resend regular help
		if (msg.equalsIgnoreCase("help") || msg.equalsIgnoreCase("g!help") || msg.equalsIgnoreCase("!help") || msg.equalsIgnoreCase("_help") || msg.equalsIgnoreCase("@" + Grimoire.BOT_NAME + " help")) {
			new HelpCommand().exec(null, "", e, guildPreferences);
			return;
		}

		// List commands
		if (msg.equalsIgnoreCase("commands")) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setAuthor("Mac's Grimoire: Commands", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);
			eb.appendDescription("\n\n:link: [**Command Reference**](" + Grimoire.WEBSITE + "/reference)");
			eb.appendDescription("\n\nThe following commands are available:");
			Reflections reflections = new Reflections("net.bemacized.grimoire.commands.all");
			List<List<String>> lists = new ArrayList<>(Stream.of(new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()).collect(Collectors.toList()));
			Set<Class<? extends BaseCommand>> cmdClasses = reflections.getSubTypesOf(BaseCommand.class).parallelStream().filter(c -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
			List<? extends BaseCommand> commands = cmdClasses.parallelStream()
					.map(c -> {
						try {
							return c.newInstance();
						} catch (InstantiationException | IllegalAccessException ex) {
							LOG.log(Level.SEVERE, "Cannot instantiate command class: " + c.getName(), ex);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.filter(cmd -> !cmd.unlisted())
					.sorted(Comparator.comparing(BaseCommand::name))
					.collect(Collectors.toList());
			IntStream.range(0, commands.size()).forEach(i -> lists.get(i % 3).add("[`" + commands.get(i).name() + "`](" + Grimoire.WEBSITE + "/reference/" + commands.get(i).name() + ")"));

			lists.forEach(l -> eb.addField("", String.join("\n", l), true));

			eb.setFooter("Get more information about any of them using \"help <command>\".", null);

			e.getChannel().sendMessage(eb.build()).submit();
			return;
		}

		// Explain inline references
		if (msg.equalsIgnoreCase("inline references")) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setAuthor("Mac's Grimoire: Inline References", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);

			eb.appendDescription("Inline references allow you to trigger commands straight from a regular message.\n");
			eb.appendDescription("Two commands currently have support for these references:\n`card` and `pricing`.\n");

			eb.appendDescription("\n\nTo trigger the `card` command, you can use either\n`<<card>>` or `[[card]]`");
			eb.appendDescription("\nYou can too specify the set by adding it after a spacer (`|`) like so:\n`[[card|set]]`");
			eb.appendDescription("\nBoth set names and set codes are usable!");

			eb.appendDescription("\n\nTo trigger the `pricing` command, simply use the same, but prefix the card name with a dollar sign: `<<$card>>`");

			eb.appendDescription("\n\nAn example of these in use would be as follows:");

			eb.appendDescription("\n\n_\"I like <<Mighty Leap>> a lot, but the old <<Mighty Leap|OGW>> art is much prettier. What does <<$Mighty Leap|ORI>> cost?\"_");

			e.getChannel().sendMessage(eb.build()).submit();
			return;
		}

		// Show command help
		if (msg.matches("help [a-zA-Z]+")) {
			Reflections reflections = new Reflections("net.bemacized.grimoire.commands.all");
			Set<Class<? extends BaseCommand>> cmdClasses = reflections.getSubTypesOf(BaseCommand.class).parallelStream().filter(c -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
			BaseCommand command = cmdClasses.parallelStream()
					.map(c -> {
						try {
							return c.newInstance();
						} catch (InstantiationException | IllegalAccessException ex) {
							LOG.log(Level.SEVERE, "Cannot instantiate command class: " + c.getName(), ex);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.filter(c -> c.name().equalsIgnoreCase(msg.substring(5)) || Arrays.stream(c.aliases()).parallel().anyMatch(a -> a.equalsIgnoreCase(msg.substring(5))))
					.findFirst().orElse(null);
			if (command == null) {
				sendErrorEmbed(e.getChannel(), "**\"" + msg.substring(5) + "\"** is not a valid command.");
				return;
			}
			EmbedBuilder eb = new EmbedBuilder();
			eb.setAuthor("Mac's Grimoire: " + command.name() + " command", Grimoire.WEBSITE, e.getJDA().getSelfUser().getAvatarUrl());
			eb.setColor(Globals.EMBED_COLOR_PRIMARY);
			eb.addField("Command", "`g!" + command.name() + "`", false);
			eb.addField("Description", command.description(), false);
			if (command.scryfallSyntax())
				eb.addField("", "This command supports the full [Scryfall Syntax](https://scryfall.com/docs/reference)!", false);
			if (command.usages().length > 0)
				eb.addField("Usage" + (command.usages().length > 1 ? "s" : ""), String.join("\n", Arrays.stream(command.usages()).parallel().map(u -> "`g!" + command.name() + " " + u + "`").collect(Collectors.toList())), false);
			if (command.examples().length > 0)
				eb.addField("Example" + (command.examples().length > 1 ? "s" : ""), String.join("\n", Arrays.stream(command.examples()).parallel().map(ex -> "`g!" + command.name() + " " + ex + "`").collect(Collectors.toList())), false);
			e.getChannel().sendMessage(eb.build()).submit();
			return;
		}

		// Send help on first attempt
		try {
			if (e.getChannel().getHistory().retrievePast(1).submit().get().isEmpty()) {
				new HelpCommand().exec(null, "", e, guildPreferences);
				return;
			}
		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "could not retrieve history", ex);
			return;
		}

		// Send error
		sendErrorEmbed(e.getChannel(), "I did not understand what you mean. Please only choose one of the options given:\n`help`, `commands`, `inline references`, `info`, or `help <command>`.");

	}
}
