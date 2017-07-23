package net.bemacized.grimoire.chathandlers;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.logging.Logger;

public abstract class ChatHandler {

    final Logger LOG;
    private final ChatHandler next;

    protected ChatHandler(ChatHandler next) {
        this.next = next;
        this.LOG = Logger.getLogger(this.getClass().getName());
    }

    public void handle(MessageReceivedEvent e) {
        this.handle(e, next);
    }

    protected abstract void handle(MessageReceivedEvent e, ChatHandler next);
}
