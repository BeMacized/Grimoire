package net.bemacized.grimoire.utils;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class NavigableEmbed extends ListenerAdapter {

	// Preferences
	private List<List<Supplier<MessageEmbed>>> embeds;
	private MessageChannel channel;

	// Internals
	private int xindex;
	private int yindex;
	private Message message;

	NavigableEmbed(@Nonnull List<List<Supplier<MessageEmbed>>> embeds, @Nonnull MessageChannel channel) {
		this.embeds = new ArrayList<>();
		this.embeds.addAll(embeds);
		this.channel = channel;
		xindex = 0;
		yindex = 0;
		render();
	}

	public Message getMessage() {
		return message;
	}

	public int getX() {
		return xindex;
	}

	public int getY() {
		return yindex;
	}

	public int getWidth() {
		return embeds.size();
	}

	public int getHeight() {
		return embeds.parallelStream().mapToInt(List::size).max().orElse(0);
	}

	public int getHeightAt(int x) {
		if (x < 0 || x >= embeds.size()) throw new IllegalArgumentException("X is out of bounds.");
		return embeds.get(x).size();
	}

	public List<List<Supplier<MessageEmbed>>> getEmbeds() {
		return new ArrayList<>(embeds);
	}

	private void render() {
		MessageEmbed embed = embeds.get(xindex).get(yindex).get();
		try {
			if (message == null)
				message = channel.sendMessage(embed).submit().get();
			else {
				message = message.editMessage(embed).submit().get();
			}
		} catch (InterruptedException | ExecutionException e) {
			//TODO: HANDLE EXCEPTION
			e.printStackTrace();
		}
		//TODO: ADD REACTIONS TO MESSAGE
	}

	public void setX(int x) {
		int newX = Math.min(Math.max(x, 0), getWidth() - 1);
		if (newX != xindex) {
			xindex = newX;
			render();
		}
	}

	public void setY(int y) {
		int newY = Math.min(Math.max(y, 0), embeds.get(xindex).size() - 1);
		if (newY != yindex) {
			yindex = newY;
			render();
		}
	}

	public void modX(int mod) {
		setX(getX() + mod);
	}

	public void modY(int mod) {
		setY(getY() + mod);
	}

	public void right() {
		modX(1);
	}

	public void left() {
		modX(-1);
	}

	public void up() {
		modY(-1);
	}

	public void down() {
		modY(1);
	}

	public static class Builder {
		private List<List<Supplier<MessageEmbed>>> embeds;
		private MessageChannel channel;

		public Builder(@Nonnull MessageChannel channel) {
			embeds = new ArrayList<>();
			this.channel = channel;
		}

		public Builder addEmbed(@Nonnull Supplier<MessageEmbed> embedSupplier) {
			embeds.add(new ArrayList<Supplier<MessageEmbed>>() {{
				add(embedSupplier);
			}});
			return this;
		}

		public Builder addEmbed(Supplier<MessageEmbed> embedSupplier, int xIndex) {
			if (xIndex >= embeds.size())
				throw new IllegalArgumentException("xIndex is not within current bounds of the navigatable embed. " + xIndex + " >= " + embeds.size());
			List<Supplier<MessageEmbed>> xList = embeds.get(xIndex);
			xList.add(embedSupplier);
			return this;
		}

		public NavigableEmbed build() {
			return new NavigableEmbed(embeds, channel);
		}

	}
}
