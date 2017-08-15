package net.bemacized.grimoire.data.controllers;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.jongo.Jongo;

public class DBManager {

	private Jongo jongo;

	public DBManager(String host, int port, String dbname, String username, String password) {
		// Construct mongo url
		if (dbname == null || dbname.isEmpty()) dbname = "Grimoire";
		if (host == null || host.isEmpty()) host = "127.0.0.1";
		if (port <= 0 || port >= 65535) port = 27017;
		String mongoURL = host + ":" + port + "/" + dbname;
		if (username != null && !username.isEmpty()) {
			String auth = username;
			if (password != null && !password.isEmpty()) auth += ":" + password;
			mongoURL = auth + "@" + mongoURL;
		}
		mongoURL = "mongodb://" + mongoURL;

		// Construct client
		MongoClient client = new MongoClient(new MongoClientURI(mongoURL));

		// Wrap with jongo
		jongo = new Jongo(client.getDB(dbname));
	}


	public Jongo getJongo() {
		return jongo;
	}
}
