package net.bemacized.grimoire.controllers.quiz;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.utils.MessageUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.*;
import java.util.stream.Collectors;

public class AvatarQuizManager extends MessageUtils {

	private static final int QUESTION_LIMIT = 5;
	private static final int ANSWER_LIMIT = 3;

	private HashMap<Long, AvatarQuiz> activeAvatarQuizzes = new HashMap<>();

	public AvatarQuizManager() {
	}

	void finishQuiz(long userid) {
		try {
			Grimoire.getInstance().getDiscord().removeEventListener(activeAvatarQuizzes.remove(userid));
		} catch (Exception ignored) {}
	}

	public void startAvatarQuiz(MessageReceivedEvent e) {
		if (activeAvatarQuizzes.containsKey(e.getAuthor().getIdLong())) {
			sendErrorEmbed(e.getChannel(), "Avatar creation already started.");
			return;
		}
		final AvatarQuiz quiz = createAvatarQuiz(e.getAuthor(), e.getChannel());
		final long authorId = e.getAuthor().getIdLong();
		activeAvatarQuizzes.put(authorId, quiz);
		Grimoire.getInstance().getDiscord().addEventListener(quiz);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Grimoire.getInstance().getDiscord().removeEventListener(quiz);
				activeAvatarQuizzes.remove(authorId);
			}
		}, 300L * 1000L);
	}

	private AvatarQuiz createAvatarQuiz(User user, MessageChannel channel) {
		List<AvatarQuiz.Question> quizQuestions = new ArrayList<>(questions);
		Collections.shuffle(quizQuestions);
		quizQuestions = quizQuestions.parallelStream().limit(QUESTION_LIMIT).map(q -> {
			List<AvatarQuiz.Answer> answers = new ArrayList<>(q.getAnswers());
			Collections.shuffle(answers);
			answers = answers.parallelStream().limit(ANSWER_LIMIT).collect(Collectors.toList());
			return new AvatarQuiz.Question(q.getQuestion(), answers);
		}).collect(Collectors.toList());
		return new AvatarQuiz(user, quizQuestions, channel);
	}

	private List<AvatarQuiz.Question> questions = new ArrayList<AvatarQuiz.Question>(){{
		add(new AvatarQuiz.Question(
				"You're facing down a 5/5 Beast in 1-on-1 combat. How do you deal with him?",
				new ArrayList<AvatarQuiz.Answer>() {{
					add(new AvatarQuiz.Answer(
							"Swiftly blast him with a barrage of magic, and prevent getting physical. Why dirty my hands?",
							qr -> {
								qr.addAbility(AvatarQuizResults.Ability.PINGER);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, -1);
							}
					));
					add(new AvatarQuiz.Answer(
							"GRUUL SMASH!",
							qr -> {
								qr.modPower(2);
								qr.modToughness(2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.WHITE, -1);
							}
					));
					add(new AvatarQuiz.Answer(
							"Engage him as a distraction to protect my allies above all else.",
							qr -> {
								qr.modToughness(3);
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
							}
					));
					add(new AvatarQuiz.Answer(
							"Drop a smoke bomb and get the heck outta there!",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, -1);
								qr.addAbility(AvatarQuizResults.Ability.EVADE);
							}
					));
					add(new AvatarQuiz.Answer(
							"Overpower him with my tactical prowess and weapons and claim victory",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, -1);
								qr.addAbility(AvatarQuizResults.Ability.COMBAT);
							}
					));
					add(new AvatarQuiz.Answer(
							"By surviving through sheer willpower to live another day",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLACK, -1);
								qr.addAbility(AvatarQuizResults.Ability.INDESCRUCTIBLE);
							}
					));
					add(new AvatarQuiz.Answer(
							"I don't",
							qr -> {
								qr.modPower(-2);
								qr.modToughness(-2);
								qr.modCmc(-2);
							}
					));
				}}
		));
		add(new AvatarQuiz.Question(
				"You're facing down a 5/5 Beast in 1-on-1 combat. How do you deal with him?",
				new ArrayList<AvatarQuiz.Answer>() {{
					add(new AvatarQuiz.Answer(
							"Swiftly blast him with a barrage of magic, and prevent getting physical. Why dirty my hands?",
							qr -> {
								qr.addAbility(AvatarQuizResults.Ability.PINGER);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, -1);
							}
					));
					add(new AvatarQuiz.Answer(
							"GRUUL SMASH!",
							qr -> {
								qr.modPower(2);
								qr.modToughness(2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.WHITE, -1);
							}
					));
					add(new AvatarQuiz.Answer(
							"Engage him as a distraction to protect my allies above all else.",
							qr -> {
								qr.modToughness(3);
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
							}
					));
					add(new AvatarQuiz.Answer(
							"Drop a smoke bomb and get the heck outta there!",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, -1);
								qr.addAbility(AvatarQuizResults.Ability.EVADE);
							}
					));
					add(new AvatarQuiz.Answer(
							"Overpower him with my tactical prowess and weapons and claim victory",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.RED, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLUE, -1);
								qr.addAbility(AvatarQuizResults.Ability.COMBAT);
							}
					));
					add(new AvatarQuiz.Answer(
							"By surviving through sheer willpower to live another day",
							qr -> {
								qr.modDevotion(AvatarQuizResults.Color.WHITE, 2);
								qr.modDevotion(AvatarQuizResults.Color.GREEN, 2);
								qr.modDevotion(AvatarQuizResults.Color.BLACK, -1);
								qr.addAbility(AvatarQuizResults.Ability.INDESCRUCTIBLE);
							}
					));
					add(new AvatarQuiz.Answer(
							"I don't",
							qr -> {
								qr.modPower(-2);
								qr.modToughness(-2);
								qr.modCmc(-2);
							}
					));
				}}
		));
	}};


}
