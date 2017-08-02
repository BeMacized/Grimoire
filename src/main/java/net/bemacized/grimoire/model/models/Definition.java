package net.bemacized.grimoire.model.models;

public class Definition {

	private String keyword;
	private String explanation;

	public Definition(String keyword, String explanation) {
		this.keyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1).toLowerCase();
		this.explanation = explanation;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getExplanation() {
		return explanation;
	}
}
