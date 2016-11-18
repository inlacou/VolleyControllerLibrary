package com.libraries.inlacou.volleycontroller;

/**
 * Created by inlacou on 18/11/16.
 */

public class File {

	private String location, format, name;
	private Type type;

	public File(String location, String format, String name, Type type) {
		this.location = location;
		this.format = format;
		this.name = name;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public String getFormat() {
		return format;
	}

	public enum Type{
		IMAGE("image");

		String text;

		Type(String text){
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}
