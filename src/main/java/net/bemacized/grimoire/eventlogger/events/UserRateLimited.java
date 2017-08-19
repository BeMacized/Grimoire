package net.bemacized.grimoire.eventlogger.events;

import javax.annotation.Nullable;
import java.util.Date;

public class UserRateLimited extends UserCommandInvocation {

	public UserRateLimited() {
	}

	public UserRateLimited(User user, @Nullable Guild guild, Channel channel, String command, String[] args, String rawArgs, String aliasUsed, Date timestamp, boolean inline) {
		super(user, guild, channel, command, args, rawArgs, aliasUsed, timestamp, inline);
	}
}
