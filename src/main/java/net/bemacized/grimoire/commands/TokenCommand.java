package net.bemacized.grimoire.commands;

import com.sun.istack.internal.NotNull;
import io.magicthegathering.javasdk.api.SetAPI;
import io.magicthegathering.javasdk.resource.MtgSet;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TokenCommand extends BaseCommand {

	private static final int MAX_TOKEN_RESULTS = 15;

	private List<Token> tokens;

	public TokenCommand() {
		try {
			tokens = new ArrayList<>();

			// Parse token.xml
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(getClass().getResourceAsStream("/tokens.xml"));

			// Extract card nodes
			for (Element card : new ArrayList<Element>() {{
				NodeList cardNodeList = doc.getElementsByTagName("card");
				for (int i = 0; i < cardNodeList.getLength(); i++) add((Element) cardNodeList.item(i));
			}}) {
				tokens.add(new Token(
						card.getElementsByTagName("name").item(0).getTextContent().trim(),
						new ArrayList<TokenSetArt>() {{
							NodeList arts = card.getElementsByTagName("set");
							for (int i = 0; i < arts.getLength(); i++) {
								add(new TokenSetArt(
										(arts.item(i).getAttributes().getNamedItem("picURL") == null) ? null : arts.item(i).getAttributes().getNamedItem("picURL").getTextContent().trim(),
										arts.item(i).getTextContent().trim()
								));
							}
						}},
						card.getElementsByTagName("type").item(0).getTextContent().trim(),
						new ArrayList<String>() {{
							NodeList cards = card.getElementsByTagName("reverse-related");
							for (int i = 0; i < cards.getLength(); i++) {
								add(cards.item(i).getTextContent().trim());
							}
						}},
						(card.getElementsByTagName("pt").getLength() > 0) ? card.getElementsByTagName("pt").item(0).getTextContent().trim() : null,
						(card.getElementsByTagName("color").getLength() > 0) ? colourIdToName(card.getElementsByTagName("color").item(0).getTextContent().trim()) : "Colourless"
				));
			}
			LOG.info("Loaded " + tokens.size() + " tokens");
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			LOG.log(Level.SEVERE, "Could not parse tokens.xml", ex);
		}
	}

	@Override
	public String name() {
		return "token";
	}

	@Override
	public String[] aliases() {
		return new String[0];
	}

	@Override
	public String description() {
		return "Retrieve the art of a token.";
	}

	@Override
	public String paramUsage() {
		return "<token_name> [choice]";
	}

	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		try {
			// Verify token name presence
			if (args.length == 0) {
				e.getChannel().sendMessage(String.format(
						"<@%s>, Please provide a valid token name",
						e.getAuthor().getId()
				)).submit();
				return;
			}

			// Send load message
			RequestFuture<Message> loadMsg = e.getChannel().sendMessage("```\n" + "Searching for token..." + "\n```").submit();

			// Extract card name and optional choice id
			int choice = -1;
			String cardName;
			if (args.length >= 2 && isNumber(args[args.length - 1])) {
				choice = Integer.parseInt(args[args.length - 1]);
				cardName = String.join(" ", Arrays.copyOf(args, args.length - 1));
			} else {
				cardName = String.join(" ", args);
			}

			// Find token(s)
			List<Token> matches = tokens.parallelStream().filter(token -> token.getName().replaceAll("[^a-zA-Z0-9 ]+", "").toLowerCase().contains(cardName.toLowerCase())).collect(Collectors.toList());
			// filter down to exact matches only if available
			List<Token> exactMatches = matches.parallelStream().filter(token -> token.getName().trim().equalsIgnoreCase(cardName.toLowerCase())).collect(Collectors.toList());
			if (!exactMatches.isEmpty()) matches = exactMatches;

			// Sort matches
			matches.sort((o1, o2) -> {
				if (o1.getName().equalsIgnoreCase(o2.getName()) && o1.getPt() != null && o2.getPt() != null)
					return o1.getPt().compareTo(o2.getPt());
				else return o1.getName().compareTo(o2.getName());
			});

			// Test for no matches
			if (matches.isEmpty()) {
				loadMsg.get().editMessage(String.format(
						"<@%s>, I couldn't find any tokens called **'%s'**",
						e.getAuthor().getId(),
						cardName
				)).submit();
				return;
			}

			// Get 1st match
			Token match = matches.get(0);

			// Test for multiple matches
			if (matches.size() > 1) {
				// Check if choice # was set
				if (choice != -1) {
					// Check if choice # is in range
					if (choice < 1 || choice > matches.size()) {
						loadMsg.get().editMessage(String.format(
								"<@%s>, The choice number you provided is not within range. Please only pick a valid option.",
								e.getAuthor().getId()
						)).submit();
						return;
					}
					// Replace match with choice
					match = matches.get(choice - 1);
				} else if (matches.size() > MAX_TOKEN_RESULTS) {
					// List options
					loadMsg.get().editMessage(String.format(
							"<@%s>, There are too many tokens matching your search. Please be more specific.",
							e.getAuthor().getId(),
							cardName
					)).submit();
					return;
				} else {
					// List options
					StringBuilder sb = new StringBuilder(String.format(
							"<@%s>, There are multiple tokens matching your search. Please pick any of the following using `!token %s [#]`:\n",
							e.getAuthor().getId(),
							cardName
					));
					for (int i = 0; i < matches.size(); i++) {
						Token m = matches.get(i);
						boolean hasArts = m.getTokenSetArt().stream().anyMatch(art -> art.getUrl() != null && !art.getUrl().isEmpty());
						sb.append(String.format(
								"\n**%s.**%s%s%s%s",
								i + 1,
								(m.getColor() == null) ? "" : " " + m.getColor(),
								(m.getPt() == null) ? "" : " _" + m.getPt() + "_",
								" " + m.getName(),
								(hasArts) ? "" : " __[NO ART AVAILABLE]__"

						));
					}

					loadMsg.get().editMessage(sb.toString()).submit();
					return;
				}

				// Check if match has image
				List<TokenSetArt> arts = match.getTokenSetArt().stream().filter(art -> art.getUrl() != null && !art.getUrl().isEmpty()).collect(Collectors.toList());
				if (arts.isEmpty()) {
					loadMsg.get().editMessage(String.format(
							"<@%s>, I sadly do not know of any art for this token. Please try a different one!",
							e.getAuthor().getId(),
							cardName
					)).submit();
					return;
				}

				// Update load message
				loadMsg.get().editMessage(String.format("```\n" + "Loading '%s' token..." + "\n```", match.getName())).submit();

				// Pick random art
				TokenSetArt art = arts.get(new Random().nextInt(arts.size()));

				// Attempt finding set
				MtgSet set = SetAPI.getSet(art.getSetCode());

				// Show card
				try {
					// Obtain stream
					InputStream artStream = new URL(art.getUrl()).openStream();
					// Upload art
					RequestFuture<Message> artMsg = e.getChannel().sendFile(artStream, "token.png", null).submit();
					// Attach card name & set name + code
					artMsg.get().editMessage(String.format(
							"**%s%s**\n%s%s",
							(match.getPt() != null) ? match.getPt() + " " : "",
							match.getName(),
							match.getType(),
							(set != null) ? String.format("\n%s (%s)", set.getName(), set.getCode()) : ""
					)).submit();
					// Delete loading message
					loadMsg.get().delete().submit();
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, "Could not upload card art", ex);
					loadMsg.get().editMessage(String.format(
							"<@%s>, An error occurred while uploading the card art! Please try again later.",
							e.getAuthor().getId()
					)).submit();
				}
			}

		} catch (InterruptedException | ExecutionException ex) {
			LOG.log(Level.SEVERE, "An error occurred getting a token", ex);
			e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, An unknown error occurred getting the token. Please notify my developer to fix me up!").submit();
		}
	}

	private static class Token {

		private String name;
		private List<TokenSetArt> tokenSetArt;
		private String type;
		private List<String> reverseRelated;
		//Optional fields
		private String pt;
		private String color;

		public Token(@NotNull String name, List<TokenSetArt> tokenSetArt, @NotNull String type, List<String> reverseRelated, String pt, String color) {
			this.name = name;
			this.tokenSetArt = (tokenSetArt != null) ? tokenSetArt : new ArrayList<>();
			this.type = type;
			this.reverseRelated = (reverseRelated != null) ? reverseRelated : new ArrayList<>();
			this.pt = pt;
			this.color = color;
		}

		public String getName() {
			return name;
		}

		public List<TokenSetArt> getTokenSetArt() {
			return tokenSetArt;
		}

		public String getType() {
			return type;
		}

		public List<String> getReverseRelated() {
			return reverseRelated;
		}

		public String getPt() {
			return pt;
		}

		public String getColor() {
			return color;
		}
	}

	private static class TokenSetArt {
		private String url;
		private String setCode;

		public TokenSetArt(@NotNull String url, @NotNull String setCode) {
			this.url = url;
			this.setCode = setCode;
		}

		public String getUrl() {
			return url;
		}

		public String getSetCode() {
			return setCode;
		}
	}

	private boolean isNumber(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String colourIdToName(String id) {
		switch (id.toUpperCase()) {
			case "R":
				return "Red";
			case "B":
				return "Black";
			case "U":
				return "Blue";
			case "W":
				return "White";
			case "G":
				return "Green";
			default:
				return "Colourless";
		}
	}
}
