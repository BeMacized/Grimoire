package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.commands.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler extends ChatHandler {

	private List<BaseCommand> commands;

	public CommandHandler(ChatHandler next) {
		super(next);
		commands = Stream.of(
				new HelpCommand(),
				new RulingsCommand(),
				new PrintsCommand(),
				new OracleCommand(),
				new RandomCommand(),
				new PricingCommand(),
				new TokenCommand(),
				new ArtCommand(),
				new CompRulesCommand(),
				new DefineCommand(),
				new TournamentRulesCommand(),
				new InfractionProcedureGuideCommand(),
				new NamesCommand(),
				new CardCommand(),
				new LegalityCommand()
		).collect(Collectors.toList());
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		// If no command prefix is found, quit here.
		if (!e.getMessage().getContent().startsWith("!") && !e.getMessage().getContent().startsWith("/")) {
			next.handle(e);
			return;
		}

		// Extract command and arguments
		String[] data = e.getMessage().getContent().substring(1).split("\\s+");
		String cmd = data[0];
		Matcher argsMatcher = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|[^\\s\"]+").matcher(String.join(" ", Arrays.copyOfRange(data, 1, data.length)));
		List<String> args = new ArrayList<>();
		while (argsMatcher.find()) {
			String arg = argsMatcher.group();
			if (arg.startsWith("\"") && arg.endsWith("\"") && arg.length() > 1)
				arg = arg.substring(1, arg.length() - 1);
			arg = arg.replaceAll("\\\\\"", "\"");
			args.add(arg);
		}

		// Search for command
		BaseCommand command = commands
				.stream()
				.filter(c ->
						c.name().equalsIgnoreCase(cmd) ||
								Arrays.stream(c.aliases())
										.anyMatch(a -> a.equalsIgnoreCase(cmd))
				)
				.findAny()
				.orElse(null);

		// Quit here if command was not found
		if (command == null) {
			next.handle(e);
			return;
		}

		// Execute command otherwise
		new Thread(() -> command.exec(args.toArray(new String[0]), e)).start();
	}
}
