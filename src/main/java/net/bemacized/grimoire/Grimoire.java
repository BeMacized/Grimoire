package net.bemacized.grimoire;

import net.bemacized.grimoire.database.DBManager;
import net.bemacized.grimoire.eventhandlers.MainChatProcessor;
import net.bemacized.grimoire.parsers.ComprehensiveRules;
import net.bemacized.grimoire.parsers.InfractionProcedureGuide;
import net.bemacized.grimoire.parsers.Tokens;
import net.bemacized.grimoire.parsers.TournamentRules;
import net.bemacized.grimoire.pricing.PricingManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Grimoire {

	private final static Logger LOG = Logger.getLogger(Grimoire.class.getName());
	private static Grimoire instance;

	public static Grimoire getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		new Grimoire(System.getenv("BOT_TOKEN"));
	}

	private JDA discord;
	private DBManager dbManager;
	private PricingManager pricingManager;
	private Tokens tokens;
	private ComprehensiveRules comprehensiveRules;
	private TournamentRules tournamentRules;
	private InfractionProcedureGuide infractionProcedureGuide;

	private Grimoire(String bot_token) {
		instance = this;

		// Verify existence of token
		if (bot_token == null) {
			LOG.severe("No discord bot token was set in the BOT_TOKEN environment variable! Quitting...");
			System.exit(1);
		}

		// Connect to Mongo database
		int MONGO_PORT = -1;
		try {
			MONGO_PORT = Integer.parseInt(System.getenv("MONGO_PORT"));
		} catch (Exception ignored) {
		}
		this.dbManager = new DBManager(
				System.getenv("MONGO_HOST"),
				MONGO_PORT,
				System.getenv("MONGO_DB"),
				System.getenv("MONGO_USER"),
				System.getenv("MONGO_PASSWORD")
		);

		// Instantiate pricing manager
		this.pricingManager = new PricingManager();
		this.pricingManager.init();

		// Load tokens
		this.tokens = new Tokens();

		// Load comprehensive rules
		comprehensiveRules = new ComprehensiveRules();

		// Load tournament rules
		tournamentRules = new TournamentRules();

		// Load infraction procedure guide
		infractionProcedureGuide = new InfractionProcedureGuide();


		// Log in to Discord
		try {
			LOG.info("Logging in to Discord...");
			discord = new JDABuilder(AccountType.BOT)
					.setAutoReconnect(true)
					.setToken(bot_token)
					.buildBlocking();
			LOG.info("Discord login complete.");
		} catch (LoginException e) {
			LOG.log(Level.SEVERE, "Could not log in to Discord. Quitting...", e);
			System.exit(1);
		} catch (RateLimitedException e) {
			LOG.log(Level.SEVERE, "Walked into a rate limit while logging in. Please try again later. Quitting...", e);
			System.exit(1);
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Login procedure was interrupted. Quitting...", e);
			System.exit(1);
		}

		// Register EventHandlers
		discord.addEventListener(new MainChatProcessor());
	}

	public JDA getDiscord() {
		return discord;
	}

	public DBManager getDBManager() {
		return dbManager;
	}

	public PricingManager getPricingManager() {
		return pricingManager;
	}

	public Tokens getTokens() {
		return tokens;
	}

	public ComprehensiveRules getComprehensiveRules() {
		return comprehensiveRules;
	}

	public TournamentRules getTournamentRules() {
		return tournamentRules;
	}

	public InfractionProcedureGuide getInfractionProcedureGuide() {
		return infractionProcedureGuide;
	}
}
