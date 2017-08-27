package net.bemacized.grimoire.eventlogger;

import net.bemacized.grimoire.Grimoire;
import net.bemacized.grimoire.eventlogger.events.LogEntry;

public class EventLogger {

	public static final String COLLECTION = "Logs";

	public static void saveLog(LogEntry entry) {
		if (entry._id() != null) throw new IllegalArgumentException("Do not save existing logs");
		Grimoire.getInstance().getDBManager().getJongo().getCollection(COLLECTION).save(entry);
	}


}
