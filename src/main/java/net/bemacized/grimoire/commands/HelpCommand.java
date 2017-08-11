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
		sendEmbed(e.getChannel(), ":worried: The help command is currently being rewritten. Please try again later!");
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
