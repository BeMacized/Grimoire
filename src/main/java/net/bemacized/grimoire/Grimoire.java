package net.bemacized.grimoire;

import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import net.bemacized.grimoire.controllers.DBManager;
import net.bemacized.grimoire.controllers.EmojiParser;
import net.bemacized.grimoire.controllers.ListReporter;
import net.bemacized.grimoire.controllers.PlaystatusHandler;
import net.bemacized.grimoire.controllers.quiz.AvatarQuizManager;
import net.bemacized.grimoire.data.providers.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Grimoire {

	public final static String DEV_ID = "87551321762172928";
	public final static String BOT_NAME = "Mac's Grimoire";
	public final static String WEBSITE = System.getenv("GRIM_HOST") == null ? "https://grimoirebot.xyz" : System.getenv("GRIM_HOST");

	private final static Logger LOG = Logger.getLogger(Grimoire.class.getName());
	private static Grimoire instance;

	public static Grimoire getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		new Grimoire(System.getenv("BOT_TOKEN"));
	}

	// API Instances
	private JDA discord;
	private GeoApiContext geoAPI;


	// Controllers
	private EmojiParser emojiParser;
	private DBManager dbManager;
	private PlaystatusHandler playstatusHandler;
	private ListReporter listReporter;
	private AvatarQuizManager avatarQuizManager;

	// Providers
	private CardProvider cardProvider;
	private ComprehensiveRuleProvider comprehensiveRuleProvider;
	private TournamentRuleProvider tournamentRuleProvider;
	private InfractionProcedureGuideProvider infractionProcedureGuideProvider;
	private StandardRotationProvider standardRotationProvider;
	private PricingProvider pricingProvider;
	private GuildPreferenceProvider guildPreferenceProvider;

	private Grimoire(String bot_token) {
		instance = this;

		// Verify existence of token
		if (bot_token == null) {
			LOG.severe("No discord bot token was set in the BOT_TOKEN environment variable! Quitting...");
			System.exit(1);
		}

		// Configure UniRest
		Unirest.setObjectMapper(new ObjectMapper() {
			private Gson gson = new Gson();

			public <T> T readValue(String s, Class<T> aClass) {
				try {
					return gson.fromJson(s, aClass);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object o) {
				try {
					return gson.toJson(o);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Configure Google Geocoding API
		geoAPI = (System.getenv("GOOGLE_API_KEY") == null) ? null : new GeoApiContext.Builder()
				.apiKey(System.getenv("GOOGLE_API_KEY"))
				.build();

		// Log in to Discord
		try {
			LOG.info("Logging in to Discord...");
			discord = new JDABuilder(AccountType.BOT)
					.setAutoReconnect(true)
					.setToken(bot_token)
					.buildBlocking();
			discord.getPresence().setGame(Game.of("Starting Up..."));
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

		// Load guild preferences
		this.guildPreferenceProvider = new GuildPreferenceProvider();

		// Load cards and sets
		this.cardProvider = new CardProvider();

		// Load comprehensive rules and definitions
		this.comprehensiveRuleProvider = new ComprehensiveRuleProvider();
		this.comprehensiveRuleProvider.load();

		// Load tournament rules
		this.tournamentRuleProvider = new TournamentRuleProvider();
		this.tournamentRuleProvider.load();

		// Load infraction procedure guide
		this.infractionProcedureGuideProvider = new InfractionProcedureGuideProvider();
		this.infractionProcedureGuideProvider.load();

		// Load standard rotation
		this.standardRotationProvider = new StandardRotationProvider();
		this.standardRotationProvider.load();

		// Instantiate pricing provider
		this.pricingProvider = new PricingProvider();

		// Load emoji references
		this.emojiParser = new EmojiParser();

		// Start avatar quiz handler
		this.avatarQuizManager = new AvatarQuizManager();

		// Remove starting message
		discord.getPresence().setGame(null);

		// Register EventHandlers
		discord.addEventListener(new MainEventHandler());

		// Random Playstatuses
		this.playstatusHandler = new PlaystatusHandler();

		// Assert nickname
		//TODO: Move to on guild join
		if (!discord.getSelfUser().getName().equals(BOT_NAME))
			discord.getSelfUser().getManager().setName(BOT_NAME).submit();

		// Start bot list reporters
		this.listReporter = new ListReporter();
	}

	public JDA getDiscord() {
		return discord;
	}

	public DBManager getDBManager() {
		return dbManager;
	}

	public EmojiParser getEmojiParser() {
		return emojiParser;
	}

	public CardProvider getCardProvider() {
		return cardProvider;
	}

	public ComprehensiveRuleProvider getComprehensiveRuleProvider() {
		return comprehensiveRuleProvider;
	}

	public TournamentRuleProvider getTournamentRuleProvider() {
		return tournamentRuleProvider;
	}

	public InfractionProcedureGuideProvider getInfractionProcedureGuideProvider() {
		return infractionProcedureGuideProvider;
	}

	public StandardRotationProvider getStandardRotationProvider() {
		return standardRotationProvider;
	}

	public PricingProvider getPricingProvider() {
		return pricingProvider;
	}

	public PlaystatusHandler getPlaystatusHandler() {
		return playstatusHandler;
	}

	public GuildPreferenceProvider getGuildPreferenceProvider() {
		return guildPreferenceProvider;
	}

	public ListReporter getListReporter() {
		return listReporter;
	}

	public GeoApiContext getGeoAPI() {
		return geoAPI;
	}

	public AvatarQuizManager getAvatarQuizManager() {
		return avatarQuizManager;
	}
}
