package net.bemacized.grimoire.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.logging.Logger;

public abstract class BaseCommand {

    final Logger LOG;

    BaseCommand() {
        LOG = Logger.getLogger(this.getClass().getName());
    }

    public abstract String name();

    public abstract String[] aliases();

    public abstract String description();

    public abstract String paramUsage();

    public abstract void exec(String[] args, MessageReceivedEvent e);
}
