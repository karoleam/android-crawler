package it.unina.androidripper.model;

import com.nofatclips.androidtesting.model.ActivityState;

public interface Comparator {

	public boolean compare (ActivityState a, ActivityState b);
	public String describe();
	
}
