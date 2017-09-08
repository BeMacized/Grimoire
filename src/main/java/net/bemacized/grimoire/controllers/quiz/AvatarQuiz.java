package net.bemacized.grimoire.controllers.quiz;

import net.bemacized.grimoire.Globals;
import net.bemacized.grimoire.Grimoire;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AvatarQuiz extends ListenerAdapter {

	private static final String[] ALPHABET = "\uD83C\uDDE6 \uD83C\uDDE7 \uD83C\uDDE8 \uD83C\uDDE9 \uD83C\uDDEA \uD83C\uDDEB \uD83C\uDDEC \uD83C\uDDED \uD83C\uDDEE \uD83C\uDDEF \uD83C\uDDF0 \uD83C\uDDF1 \uD83C\uDDF2 \uD83C\uDDF3 \uD83C\uDDF4 \uD83C\uDDF5 \uD83C\uDDF6 \uD83C\uDDF7 \uD83C\uDDF8 \uD83C\uDDF9 \uD83C\uDDFA \uD83C\uDDFB \uD83C\uDDFC \uD83C\uDDFD \uD83C\uDDFE \uD83C\uDDFF".split("\\s+");

	// Stats
	private User user;
	private MessageChannel channel;
	private AvatarQuizResults results;
	private Stack<Question> questions;
	private Message msg;

	public AvatarQuiz(User user, List<Question> questions, MessageChannel channel) {
		this.user = user;
		this.channel = channel;
		this.results = new AvatarQuizResults(user.getName(), user.getEffectiveAvatarUrl());
		this.questions = new Stack<>();
		questions.forEach(q -> this.questions.push(q));
		showQuestion();
	}

	private void showQuestion() {
		EmbedBuilder eb = new EmbedBuilder();
		Question q = questions.peek();
		eb.setAuthor(user.getName() + ": Avatar Card Creation", null, null);
		eb.setFooter("Questions Remaining: " + (questions.size() - 1), null);
		eb.setDescription("**" + q.getQuestion() + "**\n");
		eb.setColor(Globals.EMBED_COLOR_PRIMARY);
		for (int i = 0; i < q.getAnswers().size(); i++)
			eb.appendDescription(String.format("\n%s %s", ALPHABET[i], q.getAnswers().get(i).getText()));
		if (msg == null) {
			try {
				msg = this.channel.sendMessage(eb.build()).submit().get();
			} catch (InterruptedException | ExecutionException e) {
				//TODO: HANDLE
				e.printStackTrace();
			}
		} else {
			try {
				msg.clearReactions().submit().get();
			} catch (InterruptedException | ExecutionException e) {
				//TODO: HANDLE
				e.printStackTrace();
			}
			msg.editMessage(eb.build()).queue();
		}
		for (int i = 0; i < q.getAnswers().size(); i++)
			msg.addReaction(ALPHABET[i]).queue();
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent e) {
		if (msg == null || e.getMessageIdLong() != msg.getIdLong() || e.getUser().getIdLong() != user.getIdLong())
			return;
		int index = Stream.of(ALPHABET).collect(Collectors.toList()).indexOf(e.getReactionEmote().getName());
		if (index >= 0) {
			questions.pop().getAnswers().get(index).getExec().exec(results);
			if (!questions.empty()) showQuestion();
			else {
				msg.clearReactions().queue();
				msg.editMessage(new EmbedBuilder().setDescription("```javascript\n" + results.getCard().toString() + "\n```").build()).queue();
				Grimoire.getInstance().getAvatarQuizManager().finishQuiz(user.getIdLong());
			}
		}
	}

	public static class Answer {

		private String text;
		private ModCallback exec;

		public Answer(String text, ModCallback exec) {
			this.text = text;
			this.exec = exec;
		}

		public String getText() {
			return text;
		}

		public ModCallback getExec() {
			return exec;
		}

		interface ModCallback {
			void exec(AvatarQuizResults qr);
		}
	}

	public static class Question {

		private String question;
		private List<Answer> answers;

		public Question(String question, List<Answer> answers) {
			this.question = question;
			this.answers = answers;
		}

		public String getQuestion() {
			return question;
		}

		public List<Answer> getAnswers() {
			return answers;
		}
	}

}
