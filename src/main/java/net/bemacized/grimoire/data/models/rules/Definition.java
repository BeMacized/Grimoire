package net.bemacized.grimoire.data.models.rules;

import org.apache.commons.codec.digest.DigestUtils;
import org.jongo.marshall.jackson.oid.MongoId;

public class Definition {

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@MongoId
	private String _id;

	private String keyword;
	private String explanation;

	public Definition() {
	}

	public Definition(String keyword, String explanation) {
		this.keyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1).toLowerCase();
		this.explanation = explanation;
		this._id = generateId();
	}

	public String getKeyword() {
		return keyword;
	}

	public String getExplanation() {
		return explanation;
	}

	private String generateId() {
		return DigestUtils.sha1Hex(keyword);
	}
}
