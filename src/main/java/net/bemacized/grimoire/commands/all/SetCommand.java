package net.bemacized.grimoire.commands.all;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.commands.BaseCommand;
import net.bemacized.grimoire.data.models.preferences.GuildPreferences;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;

public class SetCommand extends BaseCommand {

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
	public String[] usages() {
		return new String[]{
				"<set code>",
				"<set name>"
		};
	}

	@Override
	public String[] examples() {
		return new String[]{
				"ORI",
				"Magic Origins"
		};
	}

	@Override
	public void exec(String[] args, String rawArgs, MessageReceivedEvent e, GuildPreferences guildPreferences) {
		// Quit and error out if none provided
		if (args.length == 0) {
			sendErrorEmbed(e.getChannel(), "Please provide a set name.");
			return;
		}

		ScryfallSet set = Grimoire.getInstance().getCardProvider().getSetByNameOrCode(rawArgs);
		if (set == null) {
			sendErrorEmbedFormat(e.getChannel(), "I couldn't find any sets with **'%s'** as its name or code.", rawArgs);
			return;
		}

		EmbedBuilder eb = new EmbedBuilder(set.getEmbed());

		try {
			// Attempt sending with set symbol
			ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
			TranscoderInput transcoderInput = new TranscoderInput(set.getIconSvgUri());
			TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);
			PNGTranscoder pngTranscoder = new PNGTranscoder();
			pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, 64f);
			pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 64f);
			pngTranscoder.transcode(transcoderInput, transcoderOutput);
			resultByteStream.flush();
			e.getChannel().sendFile(resultByteStream.toByteArray(), "set.png", new MessageBuilder().setEmbed(eb.build()).build()).submit();
		} catch (Exception ex) {
			// Fall back to no set symbol if needed
			e.getChannel().sendMessage(eb.build());
		}
	}

}
