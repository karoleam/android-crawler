package com.nofatclips.crawler.automation;

import static com.nofatclips.crawler.Resources.*;
import static com.nofatclips.androidtesting.model.InteractionType.*;
import static com.nofatclips.androidtesting.model.SimpleType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
//import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.jayway.android.robotium.solo.Solo;
import com.nofatclips.androidtesting.model.*;
import com.nofatclips.crawler.model.*;

// Automation implements the methods to interact with the application via the Instrumentation (Robot)
// and to extract informations from it (Extractor); the Robotium framework is used where possible

public class Automation implements Robot, Extractor, TaskProcessor {
	
//	private Instrumentation inst;
	@SuppressWarnings("rawtypes")
	private ActivityInstrumentationTestCase2 test; // The test case used to crawl the application
	private Activity theActivity; // Current Activity
	private Map<Integer,View> theViews = new HashMap<Integer,View> (); // A list of widgets with an id
	private ArrayList<View> allViews = new ArrayList<View>(); // A list of all widgets
	private Solo solo; // Robotium
	private Extractor extractor;
	private Restarter restarter;
	private TabHost	tabs; // Reference to the TabHost widget if present
	private int tabNum; // Number of tabs used by the Activity
	private Robot theRobot;
	
	// A Trivial Extractor is provided if none is assigned
	public Automation () {
		setExtractor (new TrivialExtractor());
		setRobot (this);
	}

	public Automation (Extractor e) {
		setExtractor (e);
	}
	
	// Initializations
	@SuppressWarnings("rawtypes")
	public void bind (ActivityInstrumentationTestCase2 test) {
		this.test = test;
//		this.theActivity = this.test.getActivity();
		this.solo = new Solo (test.getInstrumentation(), test.getActivity());
		afterRestart();
		refreshCurrentActivity();
		Log.w ("nofatclips","--->" + theActivity.getLocalClassName());
	}
	
	@Override
	public void execute (Trace t) {
		this.theRobot.process (t);
	}
	
	@Override
	public void process (Trace t) {
		Log.i ("nofatclips", "Restarting");
		this.restarter.restart();
		afterRestart();
		extractState();
		Log.i ("nofatclips", "Playing Trace " + t.getId());
		for (Transition step: t) {
			for (UserInput i: step) {
				setInput(i);
			}
			fireEvent (step.getEvent());
		}
	}

	@Override
	public void finalize() {
		try {
			solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		theActivity.finish();
	}

	@Override
	public void fireEvent(UserEvent e) {
		String eventType = e.getType();
		if (eventType.equals(BACK) || eventType.equals(SCROLL_DOWN)) { // Special events
			Log.d("nofatclips", "Firing event: type= " + eventType);
			fireEventOnView(null, eventType, null);
		} else if (e.getWidgetId().equals("-1")) {
			Log.d("nofatclips", "Firing event: type= " + e.getType() + " name=" + e.getWidgetName() + " widget="+ e.getWidgetType());
			fireEvent (e.getWidgetName(), e.getWidget().getSimpleType(), e.getType(), e.getValue());			
		} else {
			Log.d("nofatclips", "Firing event: type= " + e.getType() + " id=" + e.getWidgetId() + " widget="+ e.getWidgetType());
			fireEvent (Integer.parseInt(e.getWidgetId()), e.getWidgetName(), e.getWidget().getSimpleType(), e.getType(), e.getValue());
		}
	}

	@Override
	public void setInput(UserInput i) {
		Log.d("nofatclips", "Setting input: type= " + i.getType() + " id=" + i.getWidgetId() + " value="+ i.getValue());
		setInput (Integer.parseInt(i.getWidgetId()), i.getType(), i.getValue());
	}
	
	public void swapTab (String tab) {
		swapTab (this.tabs, Integer.valueOf(tab));
	}

	public void swapTab (int tab) {
		swapTab (this.tabs, tab);
	}
	
	private void fireEvent (int widgetId, String widgetName, String widgetType, String eventType, String value) {
		View v = getWidget(widgetId, widgetType, widgetName);
		if (v == null) {
			v = getWidget(widgetId);
		}
		if (v == null) {
			v = theActivity.findViewById(widgetId);
		}
		fireEventOnView(v, eventType, value);
	}

	private void fireEvent (String widgetName, String widgetType, String eventType, String value) {
		View v = null;
		if (widgetType.equals(BUTTON)) {
			v = solo.getButton(widgetName);
		}
		if (v == null) {
			for (View w: getAllWidgets()) {
				if (w instanceof Button) {
					Button candidate = (Button) w;
					if (candidate.getText().equals(widgetName)) {
						v = candidate;
					}
				}
				if (v!=null) break;
			}
		}
		fireEventOnView(v, eventType, value);
	}
	
	private void fireEventOnView (View v, String eventType, String value) {
		if (eventType == CLICK) {
			TouchUtils.clickView(test, v);
		} else if (eventType == BACK) {
			solo.goBack();
		} else if (eventType == SCROLL_DOWN) {
			solo.scrollDown();
		} else if (eventType == SWAP_TAB && value!=null) {
			if (v instanceof TabHost) {
				swapTab ((TabHost)v, value);
			} else {
				swapTab (value);
			}
		} else {
			return;
		}
		refreshCurrentActivity();
		solo.sleep(SLEEP_AFTER_EVENT);
		extractState();
	}

	private void refreshCurrentActivity() {
		this.theActivity = solo.getCurrentActivity();
		Log.i("nofatclips", "Current activity is " + getActivity().getLocalClassName());
	}

	private void setInput (int widgetId, String inputType, String value) {
		View v = getWidget(widgetId);
		if (v == null) {
			v = theActivity.findViewById(widgetId);
		}
		if (inputType == TYPE_TEXT) {
			solo.enterText((EditText)v, value);
		} else if (inputType == CLICK) {
			TouchUtils.clickView(test, v);
		}
	}

	private void swapTab (TabHost t, String tab) {
		swapTab (t, Integer.valueOf(tab));
	}

	private void swapTab (final TabHost t, int num) {
		final int n = Math.min(this.tabNum, Math.max(1,num))-1;
		Log.i("nofatclips", "Swapping to tab " + num);
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				t.setCurrentTab(n);
			}
		});
		this.test.getInstrumentation().waitForIdleSync();
	}
	
	public void clearWidgetList() {
		theViews.clear();
		allViews.clear();		
	}
	
	public void retrieveWidgets () {
		clearWidgetList();
		Log.i("nofatclips", "Retrieving widgets");
//		solo.clickInList(3);
//		this.test.getInstrumentation().waitForIdleSync();
//		solo.sleep(1000);
//		solo.clickInList(22);
//		this.test.getInstrumentation().waitForIdleSync();
//		solo.sleep(1000);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.DOWN);
//		solo.sendKey(Solo.ENTER);
//		this.test.getInstrumentation().waitForIdleSync();
//		solo.sleep(1000);
		for (View w: solo.getCurrentViews()) {
			String text = (w instanceof TextView)?": "+((TextView)w).getText().toString():"";
			int xy[] = new int[2];
			int xy2[] = new int[2];
			w.getLocationInWindow(xy);
			w.getLocationOnScreen(xy2);
			Log.d("nofatclips", "Found widget: id=" + w.getId() + " ("+ w.toString() + ")" + text + " in window at [" + xy[0] + "," + xy[1] + "] on screen at [" + xy2[0] + "," + xy2[1] +"]");
//			if (w.getId() == 16908298) {
//				ListView l = (ListView)w;
//				ListAdapter a = l.getAdapter();
//				Log.w("nofatclips","count=" + l.getCount() + " childCount=" + l.getChildCount());
//			}
//			if (!theViews.containsKey(w.getId())) {
			allViews.add(w);
//			}
			if (w.getId()>0) {
				theViews.put(w.getId(), w); // Add only if the widget has a valid ID
			}
			if (w instanceof TabHost) {
				setTabs((TabHost)w);
				Log.d("nofatclips", "Found tabhost: id=" + w.getId());
			}
		}
	}
	
	public void setRobot (Robot r) {
		this.theRobot = r;
	}

	public Map<Integer,View> getWidgets () {
		return this.theViews;
	}

	public ArrayList<View> getAllWidgets () {
		return this.allViews;
	}

	public Activity getActivity() {
		return this.theActivity;
	}

	public void setExtractor (Extractor e) {
		this.extractor = e;
	}

	public void setRestarter (Restarter r) {
		this.restarter = r;
	}
	
	public void setTabs (TabHost t) {
		this.tabs = t;
		this.tabNum = t.getTabWidget().getTabCount();
	}

	public void afterRestart() {
		solo.sleep(SLEEP_AFTER_RESTART);
		Log.d("nofatclips", "Ready to operate after restarting...");
	}
	
	public String getAppName () {
		return solo.getCurrentActivity().getApplicationInfo().toString();
	}

	@Override
	public View getWidget (int id) {
		return this.extractor.getWidget(id);
	}
	
	public View getWidget (int theId, String theType, String theName) {
		for (View testee: getWidgetsById(theId)) {
			Log.i("nofatclips", "Retrieved from return list id=" + testee.getId());
			String testeeType = testee.getClass().getName();
			Log.i("nofatclips", "Testing for type (" + testeeType + ") against the original (" + theType);
			String testeeName = (testee instanceof TextView)?((TextView) testee).getText().toString():"";
			Log.i("nofatclips", "Testing for name (" + testeeName + ") against the original (" + theName);
			if ( (theType.equals(testeeType)) && (theName.equals(testeeName)) ) {
				return testee;
			}
		}
		return null;
	}
	
	public ArrayList<View> getWidgetsById (int id) {
		ArrayList<View> theList = new ArrayList<View>();
		for (View theView: getAllWidgets()) {
			if (theView.getId() == id) {
				Log.i("nofatclips", "Added to return list id=" + id);
				theList.add(theView);
			}
		}
		return theList;
	}

	@Override
	public ActivityDescription describeActivity() {
		return this.extractor.describeActivity();
	}

	@Override
	public void extractState() {
		this.extractor.extractState();
	}
	
	// The TrivialExtractor uses the same methods available in Automation to create
	// a description of the Activity, which is basically the name and a list of widgets
	// in the Activity.
	
	public class TrivialExtractor implements Extractor {

		@Override
		public void extractState() {
			retrieveWidgets();
		}

		@Override
		public View getWidget (int key) {
			return getWidgets().get(key);
		}

		@Override
		public ActivityDescription describeActivity() {
			return new ActivityDescription() {
				
				@Override
				public Iterator<View> iterator() {
					return getAllWidgets().iterator();
				}
				
				@Override
				public int getWidgetIndex(View v) {
					return getAllWidgets().indexOf(v);
				}

				@Override
				public String getActivityName() {
//					return getActivity().getLocalClassName();
					return getActivity().getClass().getSimpleName();
				}
				
				@Override
				public String toString() {
					return getActivityName();
				}

			};
		}

	}
	
}
