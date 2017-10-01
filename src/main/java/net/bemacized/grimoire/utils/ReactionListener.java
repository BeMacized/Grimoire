package net.bemacized.grimoire.utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;

public class ReactionListener extends ListenerAdapter {

	private static final long MAX_LIFE = 150 * 1000;

	private JDA jda;

	private Map<String, ReactionCallback> actionMap = new HashMap<>();
	private boolean oneTimeUse = false;
	private long expireTimeout = 0;
	private long startTime;
	private Timer expireTimer;
	private Message message;
	private Set<String> controllers = new HashSet<>();

	public ReactionListener(JDA jda, Message message, boolean oneTimeUse, long expireTimeout) {
		this.jda = jda;
		this.message = message;
		this.oneTimeUse = oneTimeUse;
		this.actionMap = new HashMap<>();
		this.expireTimeout = expireTimeout;
		this.expireTimer = new Timer();
		this.startTime = System.currentTimeMillis();
		enable();

		// Force disable after max life expiry
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				disable();
			}
		}, MAX_LIFE);
	}

	public void addResponse(String reaction, ReactionCallback cb) {
		actionMap.put(reaction, cb);
	}

	@Override
	public void onGenericMessageReaction(GenericMessageReactionEvent event) {
		if (message == null || event.getMessageIdLong() != message.getIdLong() || !controllers.contains(event.getUser().getId()))
			return;
		ReactionCallback cb = actionMap.getOrDefault(event.getReactionEmote().getName(), null);
		if (cb != null) {
			cb.exec(event);
			if (oneTimeUse) disable();
			else resetTimer();
		}
	}

	private void enable() {
		this.jda.addEventListener(this);
		if (this.expireTimeout > 0) resetTimer();
	}

	public void disable() {
		this.jda.removeEventListener(this);
		this.expireTimer.cancel();
		if (message.getGuild().getSelfMember().hasPermission((TextChannel) message.getChannel(), Permission.MESSAGE_MANAGE))
			this.message.clearReactions().queue(s -> {
			}, e -> {
			});
	}

	private void resetTimer() {
		if (System.currentTimeMillis() - startTime >= MAX_LIFE) return;
		if (this.expireTimeout > 0) {
			this.expireTimer.cancel();
			this.expireTimer = new Timer();
			this.expireTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					disable();
				}
			}, expireTimeout);
		}
	}

	public void addController(User author) {
		controllers.add(author.getId());
	}

	public interface ReactionCallback {

		void exec(GenericMessageReactionEvent event);
	}
}
