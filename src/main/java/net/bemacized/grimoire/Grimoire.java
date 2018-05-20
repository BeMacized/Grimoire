package net.bemacized.grimoire;

import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import net.bemacized.grimoire.controllers.DBManager;
import net.bemacized.grimoire.controllers.EmojiParser;
import net.bemacized.grimoire.controllers.ListReporter;
import net.bemacized.grimoire.controllers.PlaystatusHandler;
import net.bemacized.grimoire.data.providers.*;
import net.bemacized.grimoire.utils.AppProperties;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;

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
		new Grimoire();
	}

	// API Instances
	private JDA discord;
	private GeoApiContext geoAPI;
	private RedditClient redditAPI;

	// Controllers
	private EmojiParser emojiParser;
	private DBManager dbManager;
	private PlaystatusHandler playstatusHandler;
	private ListReporter listReporter;

	// Providers
	private CardProvider cardProvider;
	private ComprehensiveRuleProvider comprehensiveRuleProvider;
	private TournamentRuleProvider tournamentRuleProvider;
	private InfractionProcedureGuideProvider infractionProcedureGuideProvider;
	private StandardRotationProvider standardRotationProvider;
	private PricingProvider pricingProvider;
	private GuildPreferenceProvider guildPreferenceProvider;
	private XLanderProvider xLanderProvider;

	private Grimoire() {
		instance = this;
		LOG.info("Starting Grimoire v" + AppProperties.getInstance().getVersion());

		configureRedditAPI();
		configureUnirest();
		configureGeocodingAPI();
		configureDiscord();
		configureMongoDB();

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
		this.standardRotationProvider.getSets();

		// Load XLander Properties
		this.xLanderProvider = new XLanderProvider();
		this.xLanderProvider.getHighlanderBanlist();

		// Instantiate pricing provider
		this.pricingProvider = new PricingProvider();

		// Load emoji references
		this.emojiParser = new EmojiParser();

		// Remove starting message
		discord.getPresence().setGame(null);

		// Register EventHandlers
		discord.addEventListener(new MainEventHandler());

		// Random Playstatuses
		this.playstatusHandler = new PlaystatusHandler();

		// Assert nickname
		if (!discord.getSelfUser().getName().equals(BOT_NAME))
			discord.getSelfUser().getManager().setName(BOT_NAME).submit();

		// Start bot list reporters
		this.listReporter = new ListReporter();
	}

	private void configureMongoDB() {
		int MONGO_PORT = -1;
		try {
			MONGO_PORT = Integer.parseInt(System.getenv("MONGO_PORT"));
		} catch (NumberFormatException e) {
			LOG.log(Level.SEVERE, "Invalid MONGO_PORT specified. Not a valid int!", e);
			System.exit(1);
		}
		this.dbManager = new DBManager(
				System.getenv("MONGO_HOST"),
				MONGO_PORT,
				System.getenv("MONGO_DB"),
				System.getenv("MONGO_USER"),
				System.getenv("MONGO_PASSWORD")
		);
	}

	private void configureDiscord() {
		if (System.getenv("BOT_TOKEN") == null) {
			LOG.severe("No discord bot token was set in the BOT_TOKEN environment variable! Quitting...");
			System.exit(1);
		}
		try {
			LOG.info("Logging in to Discord...");
			discord = new JDABuilder(AccountType.BOT)
					.setAutoReconnect(true)
					.setToken(System.getenv("BOT_TOKEN"))
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
	}

	private void configureGeocodingAPI() {
		geoAPI = (System.getenv("GOOGLE_API_KEY") == null) ? null : new GeoApiContext.Builder()
				.apiKey(System.getenv("GOOGLE_API_KEY"))
				.build();
	}

	private void configureRedditAPI() {
		String user = System.getenv("REDDIT_USER");
		String password = System.getenv("REDDIT_PASSWORD");
		String clientId = System.getenv("REDDIT_CLIENT_ID");
		String secret = System.getenv("REDDIT_SECRET");
		if (user == null) {
			LOG.log(Level.WARNING, "Environment variable REDDIT_USER was not specified. Disabling all Reddit based functionality.");
			return;
		}
		if (password == null) {
			LOG.log(Level.WARNING, "Environment variable REDDIT_PASSWORD was not specified. Disabling all Reddit based functionality.");
			return;
		}
		if (clientId == null) {
			LOG.log(Level.WARNING, "Environment variable REDDIT_CLIENT_ID was not specified. Disabling all Reddit based functionality.");
			return;
		}
		if (secret == null) {
			LOG.log(Level.WARNING, "Environment variable REDDIT_SECRET was not specified. Disabling all Reddit based functionality.");
			return;
		}
		UserAgent userAgent = new UserAgent("bot", "net.bemacized.grimoire", AppProperties.getInstance().getVersion(), user);
		Credentials credentials = Credentials.script(user, password, clientId, secret);
		NetworkAdapter adapter = new OkHttpNetworkAdapter(userAgent);
		redditAPI = OAuthHelper.automatic(adapter, credentials);
		redditAPI.setLogHttp(false);
	}

	private void configureUnirest() {
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
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
		HttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		Unirest.setHttpClient(httpclient);
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

	public RedditClient getRedditAPI() {
		return redditAPI;
	}

	public XLanderProvider getXLanderProvider() {
		return xLanderProvider;
	}
}
