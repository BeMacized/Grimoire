package net.bemacized.grimoire.data.models.rules;

import org.apache.commons.codec.digest.DigestUtils;
import org.jongo.marshall.jackson.oid.MongoId;

import java.util.ArrayList;
import java.util.List;

public class InfractionProcedureGuideSection {

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@MongoId
	private String _id;

	private String sectionId;
	private String title;
	private String content;
	private List<InfractionProcedureGuideSection> subSections;

	public InfractionProcedureGuideSection() {
	}

	public InfractionProcedureGuideSection(String sectionId, String title, String content) {
		this.sectionId = sectionId;
		this.title = title;
		this.content = content;
		this.subSections = new ArrayList<>();
		this._id = generateId();
	}

	public String getSectionId() {
		return sectionId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public List<InfractionProcedureGuideSection> getSubSections() {
		return new ArrayList<>(subSections);
	}

	public void setSubSections(List<InfractionProcedureGuideSection> subSections) {
		this.subSections = new ArrayList<>(subSections);
	}

	private String generateId() {
		return DigestUtils.sha1Hex(sectionId + title);
	}

}
