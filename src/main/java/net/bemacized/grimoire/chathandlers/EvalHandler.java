package net.bemacized.grimoire.chathandlers;

import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.JDA;
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

public class ExecHandler extends ChatHandler {

	private static final String SCRIPT_DIR = "execscripts";

	public ExecHandler(ChatHandler next) {
		super(next);
	}

	@Override
	protected void handle(MessageReceivedEvent e, ChatHandler next) {
		String code = e.getMessage().getRawContent();

		// Only allow dev to execute code, and only when enabled with an env variable
		boolean moduleEnabled = System.getenv("ENABLE_EXEC_MODULE") != null && (System.getenv("ENABLE_EXEC_MODULE").equalsIgnoreCase("true") || System.getenv("ENABLE_EXEC_MODULE").equalsIgnoreCase("1"));
		if (!e.getMessage().getAuthor().getId().equalsIgnoreCase(Grimoire.DEV_ID) || !moduleEnabled || !code.matches("^[`]{3}(java|JAVA)[\\r\\n]([^\\r\\n]*[\\r\\n])+[`]{3}[\\r\\n]?$")) {
			next.handle(e);
			return;
		}

		e.getMessage().addReaction("\uD83D\uDD04").submit();

		//Extract from code block
		code = code.substring(7, code.length() - 3).trim();

		// Write script to disk
		File scriptFile;
		try {
			scriptFile = writeScriptToDisk(code);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not write script to disk", ex);
			e.getMessage().getReactions().parallelStream().filter(MessageReaction::isSelf).forEach(r -> r.removeReaction().submit());
			e.getMessage().addReaction("❌").submit();
			return;
		}

		// Compile into classfile
		File classFile;
		try {
			classFile = compileScript(scriptFile);
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "Could not compile script", ex);
			e.getMessage().getReactions().parallelStream().filter(MessageReaction::isSelf).forEach(r -> r.removeReaction().submit());
			e.getMessage().addReaction("❌").submit();
			return;
		}

		//Load classfile
		Debugger d;
		try {
			d = (Debugger) new URLClassLoader(
					new URL[]{new File(classFile.getAbsolutePath().substring(0, classFile.getAbsolutePath().length() - classFile.getName().length())).toURI().toURL()},
					this.getClass().getClassLoader())
					.loadClass(classFile.getName().substring(0, classFile.getName().length() - 6))
					.newInstance();
		} catch (InstantiationException | IllegalAccessException | MalformedURLException | ClassNotFoundException ex) {
			LOG.log(Level.WARNING, "Could not load classfile", ex);
			e.getMessage().getReactions().parallelStream().filter(MessageReaction::isSelf).forEach(r -> r.removeReaction().submit());
			e.getMessage().addReaction("❌").submit();
			return;
		}

		try {
			d.exec(Grimoire.getInstance());
		} catch (Exception ex) {
			e.getMessage().getReactions().parallelStream().filter(MessageReaction::isSelf).forEach(r -> r.removeReaction().submit());
			e.getMessage().addReaction("❌").submit();
			sendEmbed(e.getChannel(), ":x: \n```\n" + ex.toString() + "\n```\n");
			return;
		}

		e.getMessage().addReaction("✅").submit();

		scriptFile.delete();
		classFile.delete();
	}


	private File compileScript(File scriptFile) throws Exception {
		// Get compiler
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		// Build classpath
		String classpath = String.join("", Stream.of(
				Grimoire.class,
				JDA.class
		).map(c -> {
			try {
				return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
			} catch (URISyntaxException e) {
				LOG.log(Level.WARNING, "Could not parse URI", e);
			}
			return null;
		}).map(path -> {
			String base = (!path.endsWith("*")) ? path + System.getProperty("path.separator") : "";
			path = path.substring(0, path.length() - 1);
			File pathf = new File(path);
			String finalPath = path;
			return base + String.join("", Arrays.stream(((pathf.listFiles() == null) ? new File[0] : pathf.listFiles()))
					.filter(f -> f.isFile() && f.getName().endsWith(".jar"))
					.map(f -> finalPath + f.getName() + System.getProperty("path.separator"))
					.collect(Collectors.toList()));
		}).collect(Collectors.toList()));
		// Refer compiler to script file
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		JavaCompiler.CompilationTask task = compiler.getTask(
				null,
				fileManager,
				null,
				Arrays.asList("-classpath", classpath),
				null,
				fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(scriptFile))
		);
		// Execute compilation
		if (!task.call()) throw new Exception("Could not compile file");
		return new File(scriptFile.getAbsolutePath().substring(0, scriptFile.getAbsolutePath().length() - 5) + ".class");
	}

	private String getClasspath(String... paths) {
		// Build classpath string
		final StringBuilder sb = new StringBuilder();
		Arrays.stream(paths).forEach(path -> {
			if (!path.endsWith("*"))
				sb.append(path + System.getProperty("path.separator"));
			path = path.substring(0, path.length() - 1);
			File pathf = new File(path);
			String finalPath = path;
			Arrays.stream(pathf.listFiles()).filter(f -> f.isFile() && f.getName().endsWith(".jar")).forEach(file -> sb.append(finalPath + file.getName() + System.getProperty("path.separator")));
		});
		return sb.toString();
	}

	private File writeScriptToDisk(String code) throws IOException {
		// Generate random name
		String randomName = RandomStringUtils.randomAlphabetic(1).toUpperCase() + RandomStringUtils.randomAlphabetic(11).toLowerCase();
		// Insert into template code
		code = autocompleteCode(code, randomName);
		// Make sure script folder exists
		new File(SCRIPT_DIR).mkdirs();
		// Create empty file
		String fileName = randomName + ".java";
		File outputFile = new File(SCRIPT_DIR + File.separator + fileName);
		outputFile.createNewFile();
		// Write code to it
		PrintWriter pw = new PrintWriter(outputFile);
		pw.println(code);
		pw.close();
		// Return the created file
		return outputFile;
	}

	private String autocompleteCode(String code, String classname) {
		// Define standard imports
		String[] standardImports = new String[0];
		standardImports = ArrayUtils.addAll(standardImports, getPackagesInPackage("java.util").toArray(new String[0]));
		standardImports = ArrayUtils.addAll(standardImports, getPackagesInPackage("java.net").toArray(new String[0]));
		standardImports = ArrayUtils.addAll(standardImports, getPackagesInPackage("net.bemacized.grimoire").toArray(new String[0]));
		standardImports = ArrayUtils.addAll(standardImports, getPackagesInPackage("net.dv8tion.jda").toArray(new String[0]));
		// Check if full class was provided, if not wrap it
		if (Arrays.stream(code.split("[\\r\\n]")).noneMatch(l -> l.matches("((public|private|protected)\\s)class\\s[^\\s\\n\\r]+\\simplements.*[\\n]")))
			code = String.format("public class Exec implements ExecHandler.Debugger { @Override public void exec(Grimoire grimoire) throws Exception { %s } }", code);
		// Attach standard imports
		for (String i : standardImports)
			code = String.format("import %s;\n%s", i, code);
		// Insert classname
		code = code.replaceAll("public class ([^\\n\\s]+) implements", "public class " + classname + " implements");
		return code;
	}

	private List<String> getPackagesInPackage(String p) {
		ConfigurationBuilder config = new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(
						Stream.of(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader())
								.collect(Collectors.toList()).toArray(new ClassLoader[0])))
				.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(p)));
		Reflections reflections = new Reflections(config);
		return reflections.getSubTypesOf(Object.class).parallelStream()
				.filter(c -> (c.getModifiers() & Modifier.PUBLIC) != 0)
				.map(c -> c.getPackage().getName() + ".*")
				.distinct()
				.collect(Collectors.toList());
	}

	public static interface Debugger {
		void exec(Grimoire grimoire) throws Exception;
	}
}
