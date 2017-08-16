package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.commands.SetBaseCommand;
import net.bemacized.grimoire.data.models.MtgSet;
import net.bemacized.grimoire.utils.LoadMessage;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;

public class SetCommand extends SetBaseCommand {

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
	protected String getInitialLoadLine() {
		return "Loading set...";
	}

	@Override
	protected void execForSet(MtgSet set, LoadMessage loadMsg, MessageReceivedEvent e) {

		// Update load text
		loadMsg.setLineFormat("Loading set '%s, (%s)'...", set.getName(), set.getCode());

		try {
			// Attempt sending with set symbol
			ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
			TranscoderInput transcoderInput = new TranscoderInput(set.getScryfallSet().getIconSvgUri());
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
