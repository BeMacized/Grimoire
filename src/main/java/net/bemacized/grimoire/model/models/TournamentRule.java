package net.bemacized.grimoire.model.models;

import java.util.ArrayList;
import java.util.List;

public class TournamentRule {

	private List<SubSection> subsections;
	private String paragraphNr;
	private String title;

	public TournamentRule(String paragraphNr, String title, List<SubSection> subsections) {
		this.subsections = new ArrayList<>(subsections);
		this.paragraphNr = paragraphNr;
		this.title = title;
	}

	private TournamentRule(String paragraphNr, String title) {
		this(paragraphNr, title, new ArrayList<>());
	}

	public List<SubSection> getSubsections() {
		return subsections;
	}

	public String getParagraphNr() {
		return paragraphNr;
	}

	public String getTitle() {
		return title;
	}

	public static class SubSection extends TournamentRule {

		private String content;

		public SubSection(String paragraphNr, String title, String content) {
			super(paragraphNr, title);
			this.content = content;
		}

		public String getContent() {
			return content;
		}
	}


}
