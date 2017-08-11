package net.bemacized.grimoire.commands;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.model.controllers.Sets;
import net.bemacized.grimoire.model.models.MtgSet;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

public class SetCommand extends BaseCommand {

	private final static int MAX_SET_ALTERNATIVES = 15;

	@Override
	public String name() {
		return "set";
	}

	@Override
	public String[] aliases() {
		return new String[]{"s"};
	}

	@Override
	public String description() {
		return "Fetch information for a set";
	}

	@Override
	public String paramUsage() {
		return "<set name/code>";
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void exec(String[] args, MessageReceivedEvent e) {
		// Quit and error out if none provided
		if (args.length == 0) {
			sendEmbed(e.getChannel(), ":anger: Please provide a card name to look up.");
			return;
		}

		// Obtain card name
		String setname = String.join(" ", args);

		// Send initial status message
		LoadMessage loadMsg = new LoadMessage(e.getChannel(), "Loading set...", true);

		// If a set(code) was provided, check its validity.
		MtgSet set;
		try {
			set = setname != null ? Grimoire.getInstance().getSets().getSingleByNameOrCode(setname) : null;
			if (set == null && setname != null) {
				sendEmbedFormat(loadMsg, ":anger: No set found with **'%s'** as its code or name.", setname);
				return;
			}
		} catch (Sets.MultipleResultsException ex) {
			if (ex.getSets().size() > MAX_SET_ALTERNATIVES)
				sendEmbedFormat(loadMsg, ":anger: There are too many results for a set named **'%s'**. Please be more specific.", setname);
			else
				sendEmbedFormat(loadMsg, "There are multiple sets which match **'%s'**. Did you perhaps mean any of the following?\n\n%s",
						setname, String.join("\n", ex.getSets().parallelStream().map(s -> String.format(":small_orange_diamond: %s _(%s)_", s.getName(), s.getCode())).collect(Collectors.toList())));
			return;
		}

		// Update load text
		loadMsg.setLineFormat("Loading set '%s, (%s)'...", set.getName(), set.getCode());

		try {
			// Attempt sending with set symbol
			ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
			TranscoderInput transcoderInput = new TranscoderInput("https://assets.scryfall.com/assets/sets/" + set.getCode().toLowerCase() + ".svg");
			TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);
			PNGTranscoder pngTranscoder = new PNGTranscoder();
			pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, 64f);
			pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 64f);
			pngTranscoder.transcode(transcoderInput, transcoderOutput);
			resultByteStream.flush();
			e.getChannel().sendFile(resultByteStream.toByteArray(), "set.png", new MessageBuilder().setEmbed(set.getEmbed()).build()).submit();
			// Remove load message
			loadMsg.complete();
		} catch (Exception ex) {
			// Fall back to no set symbol if needed
			loadMsg.complete(set.getEmbed());
		}

	}
}
