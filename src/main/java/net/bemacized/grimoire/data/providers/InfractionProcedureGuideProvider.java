package net.bemacized.grimoire.data.providers;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.data.models.rules.InfractionProcedureGuideSection;
import net.bemacized.grimoire.data.retrievers.InfractionProcedureGuideRetriever;

import java.util.ArrayList;
import java.util.List;

public class InfractionProcedureGuideProvider extends Provider {

	private static final String COLLECTION = "InfractionProcedureGuideSections";

	private List<InfractionProcedureGuideSection> sections;

	public InfractionProcedureGuideProvider() {
		sections = new ArrayList<>();
	}

	public List<InfractionProcedureGuideSection> getSections() {
		return sections;
	}

	@Override
	boolean loadFromDB() {
		LOG.info("Attempting to load Infraction Procedure Guide from database...");
		sections.clear();
		Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).find().as(InfractionProcedureGuideSection.class).forEach(section -> sections.add(section));
		boolean success = !sections.isEmpty();
		if (success) {
			LOG.info("Loaded Infraction Procedure Guide from database.");
		} else {
			LOG.info("Could not load Infraction Procedure Guide from database. Retrieving from web instead.");
		}
		return success;
	}

	@Override
	void saveToDB() {
		sections.forEach(section -> Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).save(section));
	}

	@Override
	public void loadFromSource() {
		this.sections = InfractionProcedureGuideRetriever.retrieve();
		saveToDB();
	}
}
