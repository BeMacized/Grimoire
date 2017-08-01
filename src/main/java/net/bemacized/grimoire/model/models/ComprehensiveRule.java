package net.bemacized.grimoire.model.models;

public class ComprehensiveRule {

	private String paragraphId;
	private String text;

	public ComprehensiveRule(String paragraphId, String text) {
		this.paragraphId = paragraphId;
		this.text = text;
	}

	public String getParagraphId() {
		return paragraphId;
	}

	public String getText() {
		return text;
	}
}
