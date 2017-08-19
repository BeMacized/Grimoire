package net.bemacized.grimoire.eventlogger.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jongo.marshall.jackson.oid.MongoId;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_class")
public abstract class LogEntry {

	@MongoId
	private String _id;

	public String _id() {
		return _id;
	}

	public static class User {
		private String userName;
		private long userId;

		public User() {
		}

		public User(String userName, long userId) {
			this.userName = userName;
			this.userId = userId;
		}

		public String getUserName() {
			return userName;
		}

		public long getUserId() {
			return userId;
		}
	}

	public static class Guild {
		private long guildId;
		private String guildName;

		public Guild() {
		}

		public Guild(long guildId, String guildName) {
			this.guildId = guildId;
			this.guildName = guildName;
		}

		public long getGuildId() {
			return guildId;
		}

		public String getGuildName() {
			return guildName;
		}
	}

	public static class Channel {
		private long channelId;
		private String channelName;

		public Channel() {
		}

		public Channel(long channelId, String channelName) {
			this.channelId = channelId;
			this.channelName = channelName;
		}

		public long getChannelId() {
			return channelId;
		}

		public String getChannelName() {
			return channelName;
		}
	}

}
