package com.floyd.bukkit.kit;

import java.util.HashMap;


public class KitObject {
	String raw = "";
	String name = "";
	Integer cooldown = 0;
	Integer cost = 0;
	HashMap<String, Integer> components = new HashMap<String, Integer>(); 
	
	public KitObject(String newraw) {
		raw = newraw;
		String[] parts = raw.split(";");
		name = parts[0];
		try {
			for (Integer index=1; index<parts.length; index++) {
				String[] pair = parts[index].split(" ", 2);
				if (pair[0].startsWith("-")) {
					// Negative integer = cooldown
					cooldown = Integer.parseInt(pair[0]) * -1;
					continue;
				}
				if (pair[0].startsWith("$")) {
					// iConomy price tag
					cost = Integer.parseInt(pair[0].substring(1));
					continue;
				}
				if (pair.length == 1) {
					// Just an item ID
					components.put(pair[0], 1);
				} else {
					// Item ID and amount
					components.put(pair[0], Integer.valueOf(pair[1]));
				}
			}
		}
		catch (Exception e) {
			System.out.println("[Kit] The definition of kit '" + name + "' contains an error, please fix kits.txt");
			e.printStackTrace();
		}
	}
	
	public String Name() {
		return name;
	}
	
	public Integer Cooldown() {
		return cooldown;
	}
	
	public Integer Cost() {
		return cost;
	}
	
	public HashMap<String, Integer> Components() {
		return components;
	}
	
	public Integer ComponentId(String item) {
		String[] parts = item.split(":");
		return Integer.parseInt(parts[0]);
	}
	
	public Byte ComponentData(String item) {
		String[] parts = item.split(":");
		if (parts.length > 1 && !parts[1].equals("")) {
			return Byte.parseByte(parts[1]);
		}
		return 0;
	}
	
	public Short ComponentDurability(String item) {
		String[] parts = item.split(":");
		if (parts.length > 2 && !parts[2].equals("")) {
			return Short.parseShort(parts[2]);
		}
		return 0;
	}

}

