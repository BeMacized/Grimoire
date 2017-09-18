package net.bemacized.grimoire.data.retrievers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.bemacized.grimoire.data.models.scryfall.ScryfallCard;
import net.bemacized.grimoire.data.models.scryfall.ScryfallError;
import net.bemacized.grimoire.data.models.scryfall.ScryfallList;
import net.bemacized.grimoire.data.models.scryfall.ScryfallSet;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ScryfallRetriever {

	private final static Logger LOG = Logger.getLogger(ScryfallRetriever.class.getName());

	@Nonnull
	public static ScryfallCard getCardByScryfallId(String scryfallId) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		return new Gson().fromJson(new ScryfallRequest("/cards/" + scryfallId).makeRequest(), ScryfallCard.class);
	}

	@Nonnull
	public static ScryfallCard getCardByMultiverseId(int multiverseId) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		return new Gson().fromJson(new ScryfallRequest("/cards/multiverse/" + multiverseId).makeRequest(), ScryfallCard.class);
	}

	@Nonnull
	public static ScryfallSet getSet(String setCode) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		return new Gson().fromJson(new ScryfallRequest("/sets/" + setCode.toLowerCase()).makeRequest(), ScryfallSet.class);
	}

	@Nonnull
	public static List<ScryfallCard> getCardsFromQuery(String query) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		return getCardsFromQuery(query, -1);
	}

	@Nonnull
	public static List<ScryfallCard> getCardsFromQuery(String query, int maxResults) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		try {
			Gson gson = new Gson();
			List<ScryfallCard> cards = new ListRetriever("/cards/search?q=" + URLEncoder.encode(query, "UTF-8")).getListContent(maxResults).parallelStream().map(e -> gson.fromJson(e, ScryfallCard.class)).collect(Collectors.toList());
			return cards;
		} catch (UnsupportedEncodingException e) {
			LOG.log(Level.SEVERE, "UTF-8 is not a supported encoding", e);
			throw new ScryfallRequest.UnknownResponseException(e);
		}
	}


	public static List<ScryfallSet> getSets() throws ScryfallRequest.UnknownResponseException, ScryfallRequest.ScryfallErrorException {
		try {
			Gson gson = new Gson();
			return new ListRetriever("/sets").getListContent().parallelStream().map(e -> gson.fromJson(e, ScryfallSet.class)).collect(Collectors.toList());
		} catch (ScryfallRequest.NoResultException e) {
			LOG.log(Level.SEVERE, "Scryfall returned a 404 on sets. Should not happen.", e);
			throw new ScryfallRequest.UnknownResponseException(e);
		}
	}

	@Nullable
	public static ScryfallCard getRandomCardFromQuery(String query) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
		try {
			Gson gson = new Gson();
			Random random = new Random();
			ScryfallList list = gson.fromJson(new ScryfallRequest("/cards/search?q=" + URLEncoder.encode(query, "UTF-8")).makeRequest(), ScryfallList.class);
			int page = random.nextInt((int) Math.ceil(((double) list.getTotalCards()) / ((double) list.getData().size()))) + 1;
			if (!list.hasMore() || list.getNextPage() == null || page == 1)
				return gson.fromJson(list.getData().get(random.nextInt(list.getData().size())), ScryfallCard.class);
			list = gson.fromJson(new ScryfallRequest(list.getNextPage().substring(list.getNextPage().indexOf("/", "https://".length())).replace("page=1", "page=" + page)).makeRequest(), ScryfallList.class);
			return gson.fromJson(list.getData().get(random.nextInt(list.getData().size())), ScryfallCard.class);
		} catch (UnsupportedEncodingException e) {
			LOG.log(Level.SEVERE, "UTF-8 is not a supported encoding", e);
			throw new ScryfallRequest.UnknownResponseException(e);
		}
	}

	private static class ListRetriever {

		private String endpoint;

		ListRetriever(String endpoint) {
			this.endpoint = endpoint;
		}

		List<JsonElement> getListContent() throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
			return getListContent(-1);
		}

		List<JsonElement> getListContent(int maxResults) throws ScryfallRequest.UnknownResponseException, ScryfallRequest.NoResultException, ScryfallRequest.ScryfallErrorException {
			Gson gson = new Gson();
			List<JsonElement> content = new ArrayList<>();
			ScryfallList list = null;
			while (list == null || list.hasMore()) {
				list = gson.fromJson(new ScryfallRequest(list == null ? endpoint : list.getNextPage().substring(list.getNextPage().indexOf("/", "https://".length()))).makeRequest(), ScryfallList.class);
				content.addAll(StreamSupport.stream(list.getData().spliterator(), false).collect(Collectors.toList()));
				if (content.size() >= maxResults && maxResults != -1) break;
			}
			return content.parallelStream().limit(maxResults > 0 ? maxResults : content.size()).collect(Collectors.toList());
		}
	}

	public static class ScryfallRequest {

		private final static String HOST = "https://api.scryfall.com";

		private String endpoint;

		ScryfallRequest(String endpoint) {
			this.endpoint = endpoint;
		}

		JsonElement makeRequest() throws ScryfallErrorException, NoResultException, UnknownResponseException {
			try {
				Gson gson = new Gson();
				HttpResponse<String> req = Unirest.get(HOST + endpoint).header("accept", "application/json").asString();
				if (req.getStatus() == 404) throw new NoResultException();
				if (req.getStatus() != 200)
					throw new ScryfallErrorException(gson.fromJson(req.getBody(), ScryfallError.class));
				return new JsonParser().parse(req.getBody());
			} catch (UnirestException e) {
				throw new UnknownResponseException(e);
			}
		}

		public static class ScryfallErrorException extends Exception {
			private ScryfallError error;

			public ScryfallErrorException(ScryfallError error) {
				super(error.getCode() + " - " + error.getDetails());
				this.error = error;
			}

			public ScryfallError getError() {
				return error;
			}

			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("error", error)
						.toString();
			}
		}

		public static class NoResultException extends Exception {
		}

		public static class UnknownResponseException extends Exception {

			private Exception exception;

			UnknownResponseException(Exception exception) {
				this.exception = exception;
			}

			public Exception getException() {
				return exception;
			}
		}
	}

}
