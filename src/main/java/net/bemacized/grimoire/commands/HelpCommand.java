package net.bemacized.grimoire.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class HelpCommand extends BaseCommand {
	@Override
	public String name() {
		return "help";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Shows the help text, containing all of the command references.";
	}

	@Override
	public String paramUsage() {
		return "";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		//TODO: REVAMP
		// Construct message
		final StringBuilder sb = new StringBuilder();
		// Add header
		sb.append("Hi there! This is the help page for Mac's Grimoire.").append("\n");
		sb.append("Below, you will find all usable commands with what they do and how to use them.").append("\n");
		sb.append("You can use both `/` or `!` as the command prefix.").append("\n");
		sb.append("\n");

		// Add command data
		this.getCommandClasses().forEach(cmd -> {
			try {
				BaseCommand c = cmd.getDeclaredConstructor().newInstance();
				sb.append(String.format("**!%s** %s", c.name(), c.paramUsage())).append("\n");
				if (c.aliases() != null && c.aliases().length > 0)
					sb.append(String.format("**Aliases:** _%s_", String.join(", ", c.aliases()))).append("\n");
				sb.append(c.description()).append("\n");
				sb.append("\n");
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
				LOG.log(Level.SEVERE, "An error occurred listing the help text", ex);
			}
		});

		// Add footer
		sb.append("Mac's Grimoire is being developed by BeMacized(<https://bemacized.net/>)").append("\n");
		sb.append("\n");
		sb.append("You can find the source code over at GitHub: <https://github.com/BeMacized/Grimoire>").append("\n");
		sb.append("Contributors are always welcome!").append("\n");
		sb.append("For feature suggestions or bug reports, please submit an issue with the issue tracker: <https://github.com/BeMacized/Grimoire/issues>").append("\n");
		sb.append("").append("\n");
		sb.append("In case you have any questions regarding Mac's Grimoire, feel free to contact my developer").append("\n");
		sb.append("via Twitter: <https://twitter.com/BeMacized>").append("\n");
		sb.append("or via e-mail: info@bemacized.net").append("\n");

		e.getChannel().sendMessage(sb.toString()).submit();
	}

	private List<Class<? extends BaseCommand>> getCommandClasses() {
		List<ClassLoader> classLoadersList = new LinkedList<>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());
		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
				.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(this.getClass().getPackage().getName()))));
		return new ArrayList<>(reflections.getSubTypesOf(BaseCommand.class));
	}
}
