package net.bemacized.grimoire.eventlogger.events;

import javax.annotation.Nullable;
import java.util.Date;

public class UserCommandInvocation extends LogEntry {

	private User user;
	private Guild guild;
	private Channel channel;
	private String command;
	private String[] args;
	private String rawArgs;
	private String aliasUsed;
	private Date timestamp;
	private boolean inline;

	public UserCommandInvocation() {
	}

	public UserCommandInvocation(User user, @Nullable Guild guild, Channel channel, String command, String[] args, String rawArgs, String aliasUsed, Date timestamp, boolean inline) {
		this.user = user;
		this.guild = guild;
		this.channel = channel;
		this.command = command;
		this.args = args;
		this.rawArgs = rawArgs;
		this.aliasUsed = aliasUsed;
		this.timestamp = timestamp;
		this.inline = inline;
	}

	public User getUser() {
		return user;
	}

	public Guild getGuild() {
		return guild;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getCommand() {
		return command;
	}

	public String[] getArgs() {
		return args;
	}

	public String getRawArgs() {
		return rawArgs;
	}

	public String getAliasUsed() {
		return aliasUsed;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public boolean isInline() {
		return inline;
	}
}
