package com.nofatclips.crawler.automation;

import static com.nofatclips.crawler.Resources.*;
import static com.nofatclips.androidtesting.model.InteractionType.*;
import static com.nofatclips.androidtesting.model.SimpleType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
//import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import com.jayway.android.robotium.solo.Solo;
import com.nofatclips.androidtesting.model.*;
import com.nofatclips.crawler.model.*;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.Surface.*;

// Automation implements the methods to interact with the application via the Instrumentation (Robot)
// and to extract informations from it (Extractor); the Robotium framework is used where possible

public class Automation implements Robot, Extractor, TaskProcessor, ImageCaptor {
	
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
	private UserEvent currentEvent;
	private ImageCaptor imageCaptor;
		
	// A Trivial Extractor is provided if none is assigned
	public Automation () {
		TrivialExtractor te = new TrivialExtractor(); 
		setExtractor (te);
		this.imageCaptor = te;
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
	
	public void execute (Trace t) {
		this.theRobot.process (t);
	}
	
	public void process (Trace t) {
		Log.i ("nofatclips", "Restarting");
		if (FORCE_RESTART) {
			this.restarter.restart();
		}
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

	public void finalize() {
		try {
			solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		theActivity.finish();
	}

	public void fireEvent(UserEvent e) {
		this.currentEvent = e;
		String eventType = e.getType();
		if (eventType.equals(BACK) || eventType.equals(SCROLL_DOWN)) { // Special events
			Log.d("nofatclips", "Firing event: type= " + eventType);
			fireEventOnView(null, eventType, null);
		} else {
			View v = null;
			if (e.getWidget().getIndex()<getAllWidgets().size()) {
				v = getAllWidgets().get(e.getWidget().getIndex()); // Search widget by index
			}
			if ((v!=null) && checkWidgetEquivalence(v, Integer.parseInt(e.getWidgetId()), e.getWidgetType(), e.getWidgetName())) { // Widget found
				Log.i("nofatclips", "Firing event: type= " + eventType + " index=" + e.getWidget().getIndex() + " widget="+ e.getWidgetType());
				fireEventOnView (v, eventType, e.getValue());
			} else if (e.getWidgetId().equals("-1")) { // Widget not found. Search widget by name
				Log.i("nofatclips", "Firing event: type= " + eventType + " name=" + e.getWidgetName() + " widget="+ e.getWidgetType());
				fireEvent (e.getWidgetName(), e.getWidget().getSimpleType(), eventType, e.getValue());			
			} else { // Widget not found. Search widget by id
				Log.i("nofatclips", "Firing event: type= " + eventType + " id=" + e.getWidgetId() + " widget="+ e.getWidgetType());
				fireEvent (Integer.parseInt(e.getWidgetId()), e.getWidgetName(), e.getWidget().getSimpleType(), eventType, e.getValue());
			}
		}
		this.currentEvent = null;
	}

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
		injectInteraction(v, eventType, value);
		wait(SLEEP_AFTER_EVENT);
		waitOnThrobber();
		refreshCurrentActivity();
		extractState();
	}
	
	private void injectInteraction (View v, String interactionType, String value) {
		if (v!=null) {
			requestView(v);
		}
		if (interactionType.equals(CLICK)) {
			click (v);
		} else if (interactionType.equals(LONG_CLICK)) {
			longClick(v);
		} else if (interactionType.equals(BACK)) {
			solo.goBack();
		} else if (interactionType.equals(OPEN_MENU)) {
			solo.sendKey(Solo.MENU);
		} else if (interactionType.equals(SCROLL_DOWN)) {
			solo.scrollDown();
		} else if (interactionType.equals(CHANGE_ORIENTATION)) {
			changeOrientation();
		} else if (interactionType.equals(SWAP_TAB) && (value!=null)) {
			if (v instanceof TabHost) {
				swapTab ((TabHost)v, value);
			} else {
				swapTab (value);
			}
		} else if (interactionType.equals(LIST_SELECT)) {
			selectListItem((ListView)v, value);
		} else if (interactionType.equals(LIST_LONG_SELECT)) {
			selectListItem((ListView)v, value, true);
		} else if (interactionType.equals(SPINNER_SELECT)) {
			selectSpinnerItem((Spinner)v, value);
		} else if (interactionType.equals(TYPE_TEXT)) {
			solo.enterText((EditText)v, value);
		} else if (interactionType.equals(SET_BAR)) {
			solo.setProgressBar((ProgressBar)v, Integer.parseInt(value));
		} else {
			return;
		}		
	}

	// Scroll the view to the top. Only works for ListView and ScrollView. Support for GridView and others must be added
	public void home () {
		
		// Scroll listviews up
		final ArrayList<ListView> viewList = solo.getCurrentListViews();
		if (viewList.size() > 0) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					viewList.get(0).setSelection(0);
				}
			});
		}
		
		// Scroll scrollviews up
		final ArrayList<ScrollView> viewScroll = solo.getCurrentScrollViews();
		if (viewScroll.size() > 0) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					viewScroll.get(0).fullScroll(ScrollView.FOCUS_UP);
				}
			});
		}
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
		injectInteraction(v, inputType, value);
	}

	private void swapTab (TabHost t, String tab) {
		swapTab (t, Integer.valueOf(tab));
	}

	private void swapTab (final TabHost t, int num) {
		assertNotNull(t, "Cannon swap tab: the tab host does not exist");
		final int n = Math.min(this.tabNum, Math.max(1,num))-1;
		Log.i("nofatclips", "Swapping to tab " + num);
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				t.setCurrentTab(n);
			}
		});
		sync();
		describeCurrentEvent(t.getTabWidget().getChildAt(n));
	}

	private void selectListItem (ListView l, String item) {
		selectListItem (l, item, false);
	}

	private void selectListItem (ListView l, String item, boolean longClick) {
		selectListItem (l, Integer.valueOf(item), longClick);
	}

	private void selectListItem (final ListView l, int num, boolean longClick) {
		assertNotNull(l, "Cannon select list item: the list does not exist");
		final int n = Math.min(l.getCount(), Math.max(1,num))-1;
		requestFocus(l);
		Log.i("nofatclips", "Swapping to listview item " + num);
		solo.sendKey(Solo.DOWN);
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				l.setSelection(n);
			}
		});
		sync();
		if (n<l.getCount()/2) {
			solo.sendKey(Solo.DOWN);
			solo.sendKey(Solo.UP);
		} else {
			solo.sendKey(Solo.UP);			
			solo.sendKey(Solo.DOWN);
		}
		sync();
		View v = l.getSelectedView();
		if (longClick) {
			longClick(v);
		} else {
			click (v);
		}
//		describeCurrentEvent(v);
	}

	private void selectSpinnerItem (Spinner l, String item) {
		selectSpinnerItem (l, Integer.valueOf(item));
	}

	private void selectSpinnerItem (final Spinner s, int num) {
		assertNotNull(s, "Cannon press spinner item: the spinner does not exist");
//		requestFocus(s);
		Log.i("nofatclips", "Clicking the spinner view");
		click(s);
		sync();
		selectListItem(solo.getCurrentListViews().get(0), num, false);
	}

	protected void assertNotNull (final View v) {
		ActivityInstrumentationTestCase2.assertNotNull(v);
	}

	protected void assertNotNull (final View v, String errorMessage) {
		ActivityInstrumentationTestCase2.assertNotNull(errorMessage, v);
	}

	public static boolean isInAndOutFocusEnabled() {
		return (IN_AND_OUT_FOCUS);
	}		

	protected void requestFocus (final View v) {
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				v.requestFocus();		
			}
		});
		sync();
	}
	
	// Scroll until the view is on the screen if IN_AND_OUT_OF_FOCUS is enabled or if the force parameter is true 
	protected void requestView (final View v, boolean force) {
		if (force || isInAndOutFocusEnabled()) {
			home();
			solo.sendKey(Solo.UP); // Solo.waitForView() requires a widget to be focused		
			solo.waitForView(v, 1000, true);
		}
		requestFocus(v);
	}		

	protected void requestView (final View v) {
		requestView(v, false);
	}
	
	protected void click (View v) {
		assertNotNull(v,"Cannot click: the widget does not exist");
//		android.test.TouchUtils.clickView(this.test, v);
		describeCurrentEvent(v);
		solo.clickOnView(v);
	}
	
	protected void longClick (View v) {
		assertNotNull(v, "Cannot longClick: the widget does not exist");
		describeCurrentEvent(v);
		solo.clickLongOnView(v);
	}
	
	protected void wait (int milli) {
		Log.i("nofatclips", "Waiting for " + milli/1000 + " sec.");
		solo.sleep(milli);
	}

	private boolean describeCurrentEvent (View v) {
		if (this.currentEvent == null) return false; // This is probably an input, not an event
		if (v instanceof TextView) {
			String s = ((TextView)v).getText().toString();
			this.currentEvent.setDescription(s);
			Log.d ("nofatclips", "Event description: " + s);
			return true;
		} else if (v instanceof TabHost) {
			this.currentEvent.setDescription(((TabHost)v).getCurrentTabTag());
		} else if (v instanceof ViewGroup) {
			int childNum = ((ViewGroup)v).getChildCount();
			for (int i = 0; i<childNum; i++) {
				View child =  ((ViewGroup)v).getChildAt(i);
				if (describeCurrentEvent(child)) return true;
			}
		}
		return false;
	}

	public void clearWidgetList() {
		theViews.clear();
		allViews.clear();		
	}
	
	public void retrieveWidgets () {
		home();
		clearWidgetList();
		Log.i("nofatclips", "Retrieving widgets");
		ArrayList<View> viewList = (isInAndOutFocusEnabled())?solo.getViews():solo.getCurrentViews();
		for (View w: viewList) {
			String text = (w instanceof TextView)?": "+((TextView)w).getText().toString():"";
			Log.d("nofatclips", "Found widget: id=" + w.getId() + " ("+ w.toString() + ")" + text); // + " in window at [" + xy[0] + "," + xy[1] + "] on screen at [" + xy2[0] + "," + xy2[1] +"]");
			allViews.add(w);
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
		solo.setActivityOrientation(Solo.PORTRAIT);
		wait(SLEEP_AFTER_RESTART);
		waitOnThrobber();
		Log.d("nofatclips", "Ready to operate after restarting...");
	}
	
	public void changeOrientation() {
		Display display = ((WindowManager) this.test.getInstrumentation().getContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		int angle = display.getRotation();
		int newAngle = ((angle==ROTATION_0)||(angle==ROTATION_180))?Solo.LANDSCAPE:Solo.PORTRAIT;
		solo.setActivityOrientation(newAngle);
	}
	
	public void waitOnThrobber() {
		int sleepTime = SLEEP_ON_THROBBER;
		if (sleepTime==0) return;
		
		boolean flag;
		do {
			flag = false;
			int oldId = 0;
			ArrayList<ProgressBar> bars = solo.getCurrentProgressBars();
			for (ProgressBar b: bars) {
				if (b.isShown() && b.isIndeterminate()) {
					int newId = b.getId();
					if (newId != oldId) { // Only log if the throbber changed since the last time
						Log.d("nofatclips", "Waiting on Progress Bar #" + newId);
						oldId = newId;
					}
					flag = true;
					wait(500);
					sleepTime-=500;
				}
			}
		} while (flag && (sleepTime>0));
		sync();
	}
	
	public String getAppName () {
		return solo.getCurrentActivity().getApplicationInfo().toString();
	}

	public View getWidget (int id) {
		return this.extractor.getWidget(id);
	}
	
	public View getWidget (int theId, String theType, String theName) {
		for (View testee: getWidgetsById(theId)) {
			if (checkWidgetEquivalence(testee, theId, theType, theName)) {
				return testee;
			}
		}
		return null;
	}
	
	public boolean checkWidgetEquivalence (View testee, int theId, String theType, String theName) {
		Log.i("nofatclips", "Retrieved from return list id=" + testee.getId());
		String testeeType = testee.getClass().getName();
		Log.d("nofatclips", "Testing for type (" + testeeType + ") against the original (" + theType + ")");
		String testeeName = (testee instanceof TextView)?((TextView) testee).getText().toString():"";
		Log.d("nofatclips", "Testing for name (" + testeeName + ") against the original (" + theName + ")");
		if ( (theType.equals(testeeType)) && (theName.equals(testeeName)) && (theId == testee.getId()) ) {
			return true;
		}
		return false;
	}
	
	public ArrayList<View> getWidgetsById (int id) {
		ArrayList<View> theList = new ArrayList<View>();
		for (View theView: getAllWidgets()) {
			if (theView.getId() == id) {
				Log.d("nofatclips", "Added to return list id=" + id);
				theList.add(theView);
			}
		}
		return theList;
	}

	public ActivityDescription describeActivity() {
		return this.extractor.describeActivity();
	}

	public void extractState() {
		this.extractor.extractState();
	}
	
	public Bitmap captureImage() {
		return this.imageCaptor.captureImage();
	}
	
	public void sync() {
		this.test.getInstrumentation().waitForIdleSync();
	}
	
	// The TrivialExtractor uses the same methods available in Automation to create
	// a description of the Activity, which is basically the name and a list of widgets
	// in the Activity.
	
	public class TrivialExtractor implements Extractor, ImageCaptor {

		public void extractState() {
			retrieveWidgets();
		}

		public View getWidget (int key) {
			return getWidgets().get(key);
		}

		public Activity getActivity() {
			return theActivity;
		}

		public ActivityDescription describeActivity() {
			return new ActivityDescription() {
				
				public Iterator<View> iterator() {
					return getAllWidgets().iterator();
				}
				
				public int getWidgetIndex(View v) {
					return getAllWidgets().indexOf(v);
				}

				public String getActivityName() {
//					return getActivity().getLocalClassName();
					return getActivity().getClass().getSimpleName();
				}

				public String getActivityTitle() {
					return getActivity().getTitle().toString();
				}

				public String toString() {
					return getActivityName();
				}

			};
		}
		
		public Bitmap captureImage() {
			final View view = solo.getViews().get(0);
			final boolean flag = view.isDrawingCacheEnabled();
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (!flag) {
						view.setDrawingCacheEnabled(true);
					}
		            view.buildDrawingCache();
				}
			});
			sync();
			Bitmap b = view.getDrawingCache();
            b = b.copy(b.getConfig(), false);
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (!flag) {
						view.setDrawingCacheEnabled(false);
					}
				}
			});
			return b;
		}

	}
	
}
