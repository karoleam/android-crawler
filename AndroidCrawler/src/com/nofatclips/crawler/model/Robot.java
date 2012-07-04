package com.nofatclips.crawler.model;

import android.test.ActivityInstrumentationTestCase2;

import com.nofatclips.androidtesting.model.Trace;
import com.nofatclips.androidtesting.model.Transition;
import com.nofatclips.androidtesting.model.UserEvent;
import com.nofatclips.androidtesting.model.UserInput;

public interface Robot {

	@SuppressWarnings("rawtypes")
	public void bind (ActivityInstrumentationTestCase2 engine);
	public void fireEvent (UserEvent e);
	public void setInput (UserInput i);
	public void process (Trace t);
	public void process (Transition tr);
	public void finalize();
	public void swapTab(String tab);
	public void wait (int milli);
	
}
