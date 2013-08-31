package it.unina.androidripper;

import it.unina.androidripper.model.ResourceFile;

public class Resources implements ResourceFile {

	public static String PACKAGE_NAME = "app.package";
	public static String CLASS_NAME = "app.package.class";
	
	public final static String CRAWLER_PACKAGE = "it.unina.androidripper";
	public final static String PREFERENCES_FILE = "preferences.xml";
	
	public static long RANDOM_SEED = 5466516511651561651L; // 0 = Random

	public static Class<?> theClass;
	static {
		Prefs.setMainNode(CRAWLER_PACKAGE);
		Prefs.updateMainNode();

		try {
			theClass = Class.forName(CLASS_NAME);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
