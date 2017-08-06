package net.bemacized.grimoire.model.models;

import net.bemacized.grimoire.utils.AlphanumComparator;

public class ComprehensiveRule implements Comparable<ComprehensiveRule> {

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

	@Override
	public int compareTo(ComprehensiveRule o) {
		return new AlphanumComparator().compare(paragraphId, o.getParagraphId());
	}
}
