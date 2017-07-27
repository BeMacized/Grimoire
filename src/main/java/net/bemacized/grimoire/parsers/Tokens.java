package net.bemacized.grimoire.parsers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tokens {

	private static final Logger LOG = Logger.getLogger(Tokens.class.getName());

	private List<Token> tokens;

	public Tokens() {
		LOG.info("Loading tokens...");
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

	public List<Token> getTokens() {
		return new ArrayList<>(tokens);
	}

	public static class Token {

		private String name;
		private List<TokenSetArt> tokenSetArt;
		private String type;
		private List<String> reverseRelated;
		//Optional fields
		private String pt;
		private String color;

		public Token(String name, List<TokenSetArt> tokenSetArt, String type, List<String> reverseRelated, String pt, String color) {
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

	public static class TokenSetArt {
		private String url;
		private String setCode;

		public TokenSetArt(String url, String setCode) {
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
