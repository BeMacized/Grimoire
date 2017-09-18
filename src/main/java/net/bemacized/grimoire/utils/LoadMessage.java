package net.bemacized.grimoire.utils;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoadMessage {

	private final static Logger LOG = Logger.getLogger(LoadMessage.class.getName());
	private final static String[][] SPINNERS = new String[][]{
			new String[]{
					Grimoire.getInstance().getEmojiParser().parseEmoji("{T}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{Q}", null)
			},
			new String[]{
					Grimoire.getInstance().getEmojiParser().parseEmoji("{W}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{R}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{G}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{B}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{U}", null)
			},
			new String[]{
					Grimoire.getInstance().getEmojiParser().parseEmoji("{1}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{2}", null),
					Grimoire.getInstance().getEmojiParser().parseEmoji("{3}", null)
			}
	};
	private final static long EXPIRE_TIME = 1000 * 90;
	private final static int SPINNER_INTERVAL = 1000;

	private List<Message> messages;
	private List<String> lines;
	private int spinnerStage;
	private RunnableQueue taskQueue;
	private boolean finished;
	private Timer spinnerTimer;
	private boolean showSpinner;
	private long startTime;
	private MessageChannel channel;
	private String[] spinner;
	private boolean disabled;

	public LoadMessage(MessageChannel channel, String msg, boolean showSpinner, boolean disabled) {
		// Initialize fields
		this.spinner = SPINNERS[new Random().nextInt(SPINNERS.length)];
		this.showSpinner = showSpinner;
		this.lines = new ArrayList<>();
		this.messages = new ArrayList<>();
		this.finished = false;
		this.spinnerStage = new Random().nextInt(this.spinner.length);
		this.taskQueue = new RunnableQueue(disabled ? 0 : SPINNER_INTERVAL);
		this.spinnerTimer = new Timer();
		this.startTime = System.currentTimeMillis();
		this.channel = channel;
		this.disabled = disabled;

		// Add line(s) to list
		this.lines.addAll(Arrays.stream(msg.split("[\n\r]")).collect(Collectors.toList()));

		// Start timer for spinner & autofinish
		if (!disabled) {
			this.spinnerTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - startTime >= EXPIRE_TIME) LoadMessage.this.finish();
					if (spinnerStage + 1 == spinner.length) spinnerStage = 0;
					else spinnerStage++;
					if (taskQueue.isEmpty()) taskQueue.queue(LoadMessage.this::render);
				}
			}, 0, SPINNER_INTERVAL);
		}
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public void setLineFormat(String template, Object... objs) {
		setLine(String.format(template, objs));
	}

	public LoadMessage setLine(String line) {
		this.lines.clear();
		return this.addLine(line);
	}

	public void addLineFormat(String template, Object... objs) {
		addLine(String.format(template, objs));
	}

	public LoadMessage addLine(String line) {
		this.lines.addAll(Arrays.stream(line.split("[\n\r]")).collect(Collectors.toList()));
		taskQueue.queue(this::render);
		return this;
	}

	public LoadMessage showSpinner(boolean enabled) {
		this.showSpinner = enabled;
		if (!enabled) spinnerStage = 0;
		return this;
	}

	private void render() {
		// Don't ever render on fast mode
		if (disabled) return;
		try {
			final StringBuilder sb = new StringBuilder();
			if (showSpinner) sb.append(spinner[spinnerStage]).append(" ");
			sb.append(String.join("\n", this.lines));
			String[] messageTexts = MessageUtils.splitMessage(sb.toString().trim(), false);
			for (int i = 0; i < messageTexts.length; i++) {
				// Delete messages that exceed amount of messages required
				for (int j = messages.size() - 1; j >= messageTexts.length; j--) {
					messages.get(j).delete().submit();
					messages.remove(j);
				}

				// Update old messages if they exist, otherwise send new ones.
				String newText = messageTexts[i];
				MessageEmbed newEmbed = new EmbedBuilder().setDescription(newText).setColor(Globals.EMBED_COLOR_PRIMARY).build();
				Message oldMsg = (messages.size() > i) ? messages.get(i) : null;
				if (oldMsg == null) {
					oldMsg = channel.sendMessage(newEmbed).submit().get();
					messages.add(oldMsg);
				} else if (!oldMsg.getEmbeds().get(0).getDescription().equals(newText)) {
					oldMsg = oldMsg.editMessage(newEmbed).submit().get();
					messages.remove(i);
					messages.add(i, oldMsg);
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			LOG.log(Level.SEVERE, "Could not render LoadMessage", e);
		}
	}

	private void finish() {
		this.finished = true;
		spinnerTimer.cancel();
	}

	public void complete() {
		if (this.finished) throw new IllegalStateException("Object exceeded its purpose");
		taskQueue.queue(() -> {
			messages.forEach(msg -> msg.delete().queue());
			messages.clear();
			finish();
		});
	}

	public void completeFormat(String template, Object... objs) {
		complete(String.format(template, objs));
	}

	public void complete(String msg) {
		this.complete((Object) msg);
	}

	public void complete(Message msg) {
		this.complete((Object) msg);
	}

	public void complete(MessageEmbed msg) {
		this.complete((Object) msg);
	}

	private void complete(Object msg) {
		if (this.finished) throw new IllegalStateException("Object exceeded its purpose");
		taskQueue.queue(() -> {
			Message edit = messages.isEmpty() ? null : messages.get(0);
			if (msg instanceof String) {
				if (edit != null) edit.editMessage((String) msg);
				else channel.sendMessage((String) msg).submit();
			} else if (msg instanceof Message) {
				if (edit != null) edit.editMessage((String) msg);
				else channel.sendMessage((Message) msg).submit();
			} else if (msg instanceof MessageEmbed) {
				if (edit != null) edit.editMessage((String) msg);
				else channel.sendMessage((MessageEmbed) msg).submit();
			} else {
				throw new InvalidParameterException("Msg parameter must be a String, Message, or MessageEmbed object.");
			}
			for (int i = 1; i < messages.size(); i++) {
				try {
					messages.get(i).delete().submit();
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Could not remove message object.", e);
				}
			}
			finish();
		});
	}

}
