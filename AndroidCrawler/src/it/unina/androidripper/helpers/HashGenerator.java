package it.unina.androidripper.helpers;

public class HashGenerator {

	public static String generateFromString(String s){
		if (s != null)	return Integer.toString(s.hashCode() % 100);
		else return "null";
	}
	
}
