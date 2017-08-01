package net.bemacized.grimoire.model.models;

import java.util.ArrayList;
import java.util.List;

public class Token {

	private String name;
	private List<SetArt> setArt;
	private String type;
	private List<String> reverseRelated;
	//Optional fields
	private String pt;
	private String color;

	public Token(String name, List<SetArt> setArt, String type, List<String> reverseRelated, String pt, String color) {
		this.name = name;
		this.setArt = (setArt != null) ? setArt : new ArrayList<>();
		this.type = type;
		this.reverseRelated = (reverseRelated != null) ? reverseRelated : new ArrayList<>();
		this.pt = pt;
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public List<SetArt> getSetArt() {
		return setArt;
	}

	public String getType() {
		return type;
	}

	public List<String> getReverseRelated() {
		return reverseRelated;
	}

	public String getPt() {
		return pt;
	}

	public String getColor() {
		return color;
	}

	public static class SetArt {
		private String url;
		private String setCode;

		public SetArt(String url, String setCode) {
			this.url = url;
			this.setCode = setCode;
		}

		public String getUrl() {
			return url;
		}

		public String getSetCode() {
			return setCode;
		}
	}


}

