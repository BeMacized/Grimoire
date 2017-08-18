package net.bemacized.grimoire.data.models.rules;

import net.bemacized.grimoire.utils.AlphanumComparator;
import org.apache.commons.codec.digest.DigestUtils;
import org.jongo.marshall.jackson.oid.MongoId;

public class ComprehensiveRule implements Comparable<ComprehensiveRule> {

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@MongoId
	private String _id;

	private String paragraphId;
	private String text;

	public ComprehensiveRule() {
	}

	public ComprehensiveRule(String paragraphId, String text) {
		this.paragraphId = paragraphId;
		this.text = text;
		this._id = generateId();
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

	private String generateId() {
		return DigestUtils.sha1Hex(paragraphId);
	}
}
