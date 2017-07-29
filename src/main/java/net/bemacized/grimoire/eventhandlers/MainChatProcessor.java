package net.bemacized.grimoire.eventhandlers;

import net.bemacized.grimoire.chathandlers.CardRetrieveHandler;
import net.bemacized.grimoire.chathandlers.ChatHandler;
import net.bemacized.grimoire.chathandlers.CommandHandler;
import net.bemacized.grimoire.chathandlers.PriceRetrieveHandler;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainChatProcessor extends ListenerAdapter {

	private List<ChatHandler> chatHandlers;

	public MainChatProcessor() {
		// Make sure chatHandlers is initialized
		if (chatHandlers == null) chatHandlers = new ArrayList<>();
		// Register handlers
		List<Class<? extends ChatHandler>> handlerClasses = Arrays.asList(
				CommandHandler.class,
				PriceRetrieveHandler.class,
				CardRetrieveHandler.class
		);
		Collections.reverse(handlerClasses);
		handlerClasses.forEach(this::registerChatHandler);
	}

	private void registerChatHandler(Class<? extends ChatHandler> handler) {
		// Obtain top handler
		ChatHandler lastHandler = (!chatHandlers.isEmpty()) ? chatHandlers.get(0) : new ChatHandler(null) {
			@Override
			protected void handle(MessageReceivedEvent e, ChatHandler next) {
			}
		};

		// Instantiate & Register handler with top handler
		try {
			chatHandlers.add(0, handler.getDeclaredConstructor(ChatHandler.class).newInstance(lastHandler));
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!chatHandlers.isEmpty() && !e.getAuthor().isBot())
			new Thread(() -> this.chatHandlers.get(0).handle(e)).start();
	}


}
