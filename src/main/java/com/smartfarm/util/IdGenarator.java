package com.smartfarm.util;

public class IdGenarator {

	public static String generateId(String name, long count) {
		String prefix = (name != null && !name.trim().isEmpty())
				? name.trim().substring(0, 1).toUpperCase()
				: "REC";
		return prefix + String.format("%03d", count + 1);
	}
}
