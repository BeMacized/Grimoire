package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.commands.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;
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
				new TokenCommand()
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
		String[] args = Arrays.copyOfRange(data, 1, data.length);

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
		command.exec(args, e);
	}
}
