package com.nofatclips.crawler.guitree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AbsSpinner;
import android.widget.TabHost;

import com.nofatclips.androidtesting.guitree.*;
import com.nofatclips.androidtesting.model.*;
import com.nofatclips.crawler.model.*;
import com.nofatclips.crawler.storage.PersistenceFactory;

import static com.nofatclips.crawler.Resources.*;

public class GuiTreeAbstractor implements Abstractor, FilterHandler, SaveStateListener {

	private GuiTree theSession;
	private StartActivity baseActivity;
	private HashSet<Filter> filters;
	private int eventId=0;
	private int inputId=0;
	private int activityId=0;
	private TypeDetector detector;
	private List<AbstractorListener> theListeners = new ArrayList<AbstractorListener>();
	public final static String ACTOR_NAME = "GuiTreeAbstractor";
	private static final String EVENT_PARAM_NAME = "eventId";
	private static final String INPUT_PARAM_NAME = "inputId";
	private static final String ACTIVITY_PARAM_NAME = "activityId";

	public GuiTreeAbstractor () throws ParserConfigurationException {
		this (new GuiTree());
	}
	
	public GuiTreeAbstractor(GuiTree s) {
		super();
		this.filters = new HashSet<Filter>();
		setTheSession(s);
		PersistenceFactory.registerForSavingState(this);
	}

	public GuiTree getTheSession() {
		return this.theSession;
	}

	public void setTheSession(GuiTree theSession) {
		this.theSession = theSession;
	}
	
	public TypeDetector getTypeDetector () {
		return this.detector;
	}
	
	public void setTypeDetector (TypeDetector t) {
		this.detector = t;
	}

	public ActivityState createActivity (ActivityDescription desc) {
		return createActivity (desc,false);
	}
	
	// If the boolean parameter is omitted, the overloading method will default to a Final Activity
	public ActivityState createActivity (ActivityDescription desc, boolean start) {
		ActivityState newActivity = (start)?StartActivity.createActivity(getTheSession()):FinalActivity.createActivity(getTheSession());
		newActivity.setName(desc.getActivityName());
		newActivity.setTitle(desc.getActivityTitle());
		newActivity.setUniqueId(getUniqueActivityId());
		newActivity.setId(newActivity.getUniqueId());
		for (Filter f: this.filters) {
			f.clear();
		}
		boolean hasDescription = updateDescription(newActivity, desc, false);
		if (!hasDescription) newActivity.markAsExit();
		for (AbstractorListener listener: this.theListeners) {
			listener.onNewActivity(newActivity);
		}
		return newActivity;
	}

	public boolean updateDescription (ActivityState newActivity, ActivityDescription desc) {
		return updateDescription  (newActivity, desc, true);
	}

	public boolean updateDescription (ActivityState newActivity, ActivityDescription desc, boolean detectDuplicates) {
		boolean hasDescription = false;
		for (View v: desc) {
			hasDescription = true;
			if (!v.isShown()) continue;
			TestCaseWidget w = TestCaseWidget.createWidget(getTheSession());
			String id = String.valueOf(v.getId());
			String text = "";
			String name = "";
			int type = 0;
			if (v instanceof TextView) {
				TextView t = (TextView)v;
				type = t.getInputType();
				text = t.getText().toString();
				name = (v instanceof EditText)?(t.getHint().toString()):text;
			}
			w.setIdNameType(id, name, v.getClass().getName());
			if (type!=0) {
				w.setTextType("" + type);
			}
			w.setSimpleType(getTypeDetector().getSimpleType(v));
			setCount (v,w);
			setValue (v,w);
			w.setAvailable((v.isEnabled())?"true":"false");
			w.setClickable((v.isClickable())?"true":"false");
			w.setLongClickable((v.isLongClickable())?"true":"false");
			w.setIndex(desc.getWidgetIndex(v));
			if (detectDuplicates && newActivity.hasWidget(w)) continue;
			newActivity.addWidget(w);
			for (Filter f: this.filters) {
				f.loadItem(v, w);
			}
		}
		return hasDescription;
	}
	
	@SuppressWarnings("rawtypes")
	private void setCount (View v, WidgetState w) {
		// For lists, the count is set to the number of rows in the list (inactive rows count as well)
		if (v instanceof AdapterView) {
			w.setCount(((AdapterView)v).getCount());
			return;
		}
		
		// For Spinners, the count is set to the number of options
		if (v instanceof AbsSpinner) {
			w.setCount(((AbsSpinner)v).getCount());
			return;
		}
		
		// For the tab layout host, the count is set to the number of tabs
		if (v instanceof TabHost) {
			w.setCount(((TabHost)v).getTabWidget().getTabCount());
			return;
		}
		
		// For grids, the count is set to the number of icons
		if (v instanceof ViewGroup) {
			w.setCount(((ViewGroup)v).getChildCount());
			return;
		}
		
		// For progress bars, seek bars and rating bars, the count is set to the maximum value allowed
		if (v instanceof ProgressBar) {
			w.setCount(((ProgressBar)v).getMax());
			return;
		}
		
	}
	
	private void setValue (View v, WidgetState w) {
		
		// Checkboxes, radio buttons and toggle buttons -> the value is the checked state (true or false)
		if (v instanceof Checkable) {
			w.setValue(String.valueOf(((Checkable) v).isChecked()));
		}

		// Textview, Editview et al. -> the value is the displayed text
		if (v instanceof TextView) {
			w.setValue(((TextView) v).getText().toString());
//			Log.e("nofatclips", "Hint for " + (((TextView) v).getText().toString()) + " = " + (((TextView) v).getHint()));
			return;
		}
		
		// Progress bars, seek bars and rating bars -> the value is the current progress
		if (v instanceof ProgressBar) {
			w.setValue(String.valueOf(((ProgressBar) v).getProgress()));
		}
				
	}
	
	public void setBaseActivity (ActivityDescription desc) {
		this.baseActivity = (StartActivity) createActivity(desc,true);
	}
	
	public ActivityState getBaseActivity () {
		return this.baseActivity;
	}

	public void setStartActivity (Transition theStep, ActivityState theActivity) {
		theStep.setStartActivity ((ACTIVITY_DESCRIPTION_IN_SESSION)?theActivity:stubActivity(theActivity));
	}

	public void setFinalActivity (Trace theTask, ActivityState theActivity) {
		theTask.setFinalActivity ((ACTIVITY_DESCRIPTION_IN_SESSION)?theActivity:stubActivity(theActivity));
	}
	
	private TestCaseActivity stubActivity (ActivityState theActivity) {
		TestCaseActivity theStub = ((TestCaseActivity)theActivity).clone();
		theStub.resetDescription();
		theStub.setDescriptionId(theActivity.getId());
		return theStub;
	}

	public Iterator<Filter> iterator() {
		return this.filters.iterator();
	}

	public void addFilter(Filter f) {
		this.filters.add(f);
	}

	public UserEvent createEvent(WidgetState target, String type) {
		TestCaseEvent newEvent = TestCaseEvent.createEvent(getTheSession());
		if (target == null) {
			target = TestCaseWidget.createWidget(getTheSession());
			target.setType("null");
			target.setId("-1");
			target.setSimpleType("null");
			newEvent.setWidget (target);
		} else {
			newEvent.setWidget (target.clone());
		}
		newEvent.setType(type);
		newEvent.setId(getUniqueEventId());
		for (AbstractorListener listener: this.theListeners) {
			listener.onNewEvent(newEvent);
		}
		return newEvent;
	}

	public UserInput createInput(WidgetState target, String text, String type) {
		TestCaseInput newInput = TestCaseInput.createInput(getTheSession());
		newInput.setWidget(target);
		newInput.setValue(text);
		newInput.setType(type);
		newInput.setId(getUniqueInputId());
		for (AbstractorListener listener: this.theListeners) {
			listener.onNewInput(newInput);
		}
		return newInput;
	}

	public Trace createTrace(Trace head, Transition tail) {
		TestCaseTrace t;
		if (head!= null) {
			t = ((TestCaseTrace)head).clone();
		} else {
			t = new TestCaseTrace (getTheSession());
		}
		t.addTransition(tail);
		return t;
	}
	
	public Trace importTask (Element fromXml) {
		TestCaseTrace imported = new TestCaseTrace (getTheSession());
		Element task = (Element)getTheSession().getDom().adoptNode(fromXml);
		imported.setElement(task);
		return imported;
	}

	public ActivityState importState (Element fromXml) {
//		Element state = (Element)getTheSession().getDom().adoptNode(fromXml);
//		ActivityState imported = (state.getNodeName().equals(FinalActivity.getTag()))?FinalActivity.createActivity(getTheSession()):StartActivity.createActivity(getTheSession());
//		imported.setElement(state);
//		return imported;
		return getTheSession().importState(fromXml);
	}

	public Transition createStep (ActivityState start, Collection<UserInput> inputs, UserEvent event) {
		Transition t = TestCaseTransition.createTransition(start.getElement().getOwnerDocument());
		try {
			setStartActivity(t, StartActivity.createActivity(start));
			for (UserInput inPut: inputs) {
				t.addInput(inPut);
			}
			t.setEvent (event);
		}
		catch (DOMException e) {
			Log.i("nofatclips", "Abstractor->createStep(activity): " + ((e.code==DOMException.HIERARCHY_REQUEST_ERR)?"HIERARCHY_REQUEST_ERR":String.valueOf(e.code)));
		}
		return t;
	}
	
	public void registerListener(AbstractorListener theListener) {
		this.theListeners.add(theListener);
	}
	
	public String getUniqueEventId () {
		this.eventId++;
		return "e" + this.eventId;
	}

	public String getUniqueActivityId () {
		this.activityId++;
		return "a" + this.activityId;
	}

	public String getUniqueInputId () {
		this.inputId++;
		return "i" + this.inputId;
	}

	public SessionParams onSavingState() {
		SessionParams state = new SessionParams();
		state.store(EVENT_PARAM_NAME, String.valueOf(this.eventId));
		state.store(INPUT_PARAM_NAME, String.valueOf(this.inputId));
		state.store(ACTIVITY_PARAM_NAME, String.valueOf(this.activityId));
		return state;
	}
	
	public void onLoadingState(SessionParams sessionParams) {
		this.eventId = sessionParams.getInt(EVENT_PARAM_NAME);
		this.inputId = sessionParams.getInt(INPUT_PARAM_NAME);
		this.activityId = sessionParams.getInt(ACTIVITY_PARAM_NAME);
		Log.d("nofatclips", "Restored abstractor counters to: event = " + eventId + " - input = " + inputId + " - activity = " + activityId);
	}

	public String getListenerName() {
		return ACTOR_NAME;
	}

}