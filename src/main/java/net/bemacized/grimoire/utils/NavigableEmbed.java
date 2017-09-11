package net.bemacized.grimoire.utils;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
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
    private long controlTimeout;
    private Set<String> controlUsers;
    private List<List<Supplier<MessageEmbed>>> embeds;
    private MessageChannel channel;

    // Internals
    private long creationTime;
    private int xindex;
    private int yindex;
    private Message message;

    NavigableEmbed(@Nonnull Set<String> controlUsers, @Nonnull List<List<Supplier<MessageEmbed>>> embeds, long controlTimeout, @Nonnull MessageChannel channel) {
        this.controlUsers = new HashSet<>();
        this.controlUsers.addAll(controlUsers);
        this.embeds = new ArrayList<>();
        this.embeds.addAll(embeds);
        this.channel = channel;
        xindex = 0;
        yindex = 0;
        creationTime = System.currentTimeMillis();
    }

    public List<String> getControlUsers() {
        return new ArrayList<>(controlUsers);
    }

    public List<List<Supplier<MessageEmbed>>> getEmbeds() {
        return new ArrayList<>(embeds);
    }

    private void render() {
        //TODO: WRITE RENDER MECHANISM
        MessageEmbed embed = embeds.get(xindex).get(yindex).get();
        try {
            if (message == null)
                message = channel.sendMessage(embed).submit().get();
            else {
                message.clearReactions().submit().get();
                message = message.editMessage(embed).submit().get();
            }
        } catch (InterruptedException | ExecutionException e) {
            //TODO: HANDLE EXCEPTION
            e.printStackTrace();
        }
        //TODO: ADD REACTIONS TO MESSAGE
    }

    private void next() {
        if (xindex < embeds.size() - 1) {
            xindex++;
            yindex = 0;
            render();
        }
    }

    private void previous() {
        if (xindex > 0) {
            xindex--;
            yindex = 0;
            render();
        }
    }

    private void down() {
        if (yindex < embeds.get(xindex).size() - 1) {
            yindex++;
            render();
        } else if (yindex != 0) {
            yindex = 0;
            render();
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        //TODO: CONNECT TO next(), previous() and down();
    }

    public static class Builder {
        private long controlTimeout = 30 * 1000;
        private Set<String> controlUsers;
        private List<List<Supplier<MessageEmbed>>> embeds;
        private MessageChannel channel;

        public Builder(@Nonnull MessageChannel channel) {
            controlUsers = new HashSet<>();
            embeds = new ArrayList<>();
            this.channel = channel;
        }

        public Builder setControlTimeout(long value) {
            if (value < 0) throw new IllegalArgumentException("Value cannot be negative.");
            this.controlTimeout = value;
            return this;
        }

        public Builder addUser(@Nonnull String user) {
            this.controlUsers.add(user);
            return this;
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
            return new NavigableEmbed(controlUsers, embeds, controlTimeout, channel);
        }

    }
}
