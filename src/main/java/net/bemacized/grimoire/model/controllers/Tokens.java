package net.bemacized.grimoire.model.controllers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.models.Dependency;
import net.bemacized.grimoire.model.models.Token;
import net.bemacized.grimoire.utils.MTGUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tokens {

	private static Logger LOG = Logger.getLogger(Tokens.class.getName());

	private List<Token> tokens;

	public Tokens() {
		tokens = new ArrayList<>();
	}

	public List<Token> getTokens() {
		return new ArrayList<>(tokens);
	}

	public void load() {
		tokens.clear();
		LOG.info("Loading tokens...");

		try {
			tokens = new ArrayList<>();

			// Fetch xml
			Dependency d = Grimoire.getInstance().getDependencyManager().getDependency("TOKENS");
			String xml = d.getString();
			if (xml == null) {
				LOG.severe("Could not load tokens!");
				return;
			}
			d.release(); // Release dependency from memory after loading

			// Parse token.xml
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

			// Extract card nodes
			for (Element card : new ArrayList<Element>() {{
				NodeList cardNodeList = doc.getElementsByTagName("card");
				for (int i = 0; i < cardNodeList.getLength(); i++) add((Element) cardNodeList.item(i));
			}}) {
				tokens.add(new Token(
						card.getElementsByTagName("name").item(0).getTextContent().trim(),
						new ArrayList<Token.SetArt>() {{
							NodeList arts = card.getElementsByTagName("set");
							for (int i = 0; i < arts.getLength(); i++) {
								add(new Token.SetArt(
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
						(card.getElementsByTagName("color").getLength() > 0) ? MTGUtils.colourIdToName(card.getElementsByTagName("color").item(0).getTextContent().trim()) : "Colourless"
				));
			}
			LOG.info("Loaded " + tokens.size() + " tokens");
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			LOG.log(Level.SEVERE, "Could not parse tokens.xml", ex);
		}
	}
}
