package net.bemacized.grimoire.model.models;

import java.util.ArrayList;
import java.util.List;

public class InfractionProcedureGuideSection {

	private String sectionId;
	private String title;
	private String content;
	private List<InfractionProcedureGuideSection> subSections;

	public InfractionProcedureGuideSection(String sectionId, String title, String content) {
		this.sectionId = sectionId;
		this.title = title;
		this.content = content;
		this.subSections = new ArrayList<>();
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

}
