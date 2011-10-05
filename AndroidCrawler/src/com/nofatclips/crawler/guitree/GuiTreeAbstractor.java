package com.nofatclips.crawler.guitree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nofatclips.androidtesting.guitree.*;
import com.nofatclips.androidtesting.model.*;
import com.nofatclips.crawler.model.Abstractor;
import com.nofatclips.crawler.model.ActivityDescription;
import com.nofatclips.crawler.model.Filter;
import com.nofatclips.crawler.model.FilterHandler;

public class GuiTreeAbstractor implements Abstractor, FilterHandler {

	private GuiTree theSession;
	private StartActivity baseActivity;
	private HashSet<Filter> filters;
	private int eventId=0;
	private int activityId=0;

	public GuiTreeAbstractor () throws ParserConfigurationException {
		this (new GuiTree());
	}
	
	public GuiTreeAbstractor(GuiTree s) {
		super();
		this.filters = new HashSet<Filter>();
		setTheSession(s);
	}

	public GuiTree getTheSession() {
		return this.theSession;
	}

	public void setTheSession(GuiTree theSession) {
		this.theSession = theSession;
	}

	@Override
	public ActivityState createActivity (ActivityDescription desc) {
		return createActivity (desc,false);
	}
	
	// If the boolean parameter is omitted, the overloading method will default to a Final Activity
	public ActivityState createActivity (ActivityDescription desc, boolean start) {
		ActivityState newActivity = (start)?StartActivity.createActivity(getTheSession()):FinalActivity.createActivity(getTheSession());
		newActivity.setName(desc.getActivityName());
		newActivity.setId(getUniqueActivityId());
		for (Filter f: this.filters) {
			f.clear();
		}
		for (View v: desc) {
			TestCaseWidget w = TestCaseWidget.createWidget(getTheSession());
			String id = String.valueOf(v.getId());
			String text = (v instanceof TextView)?((TextView)v).getText().toString():"";
			w.setIdNameType(id, text, v.toString());
			if (v instanceof TextView) {
				int type = ((TextView)v).getInputType();
				if (type!=0) {
					w.setTextType("" + type);
				}
			}
			newActivity.addWidget(w);
			for (Filter f: this.filters) {
				f.loadItem(v, w);
			}
		}
		return newActivity;
	}
	
	public void setBaseActivity (ActivityDescription desc) {
		this.baseActivity = (StartActivity) createActivity(desc,true);
	}
	
	public ActivityState getBaseActivity () {
		return this.baseActivity;
	}

	@Override
	public Iterator<Filter> iterator() {
		return this.filters.iterator();
	}

	@Override
	public void addFilter(Filter f) {
		this.filters.add(f);
	}

	@Override
	public UserEvent createEvent(WidgetState target, String type) {
		// TODO Auto-generated method stub
		TestCaseEvent newEvent = TestCaseEvent.createEvent(getTheSession());
		newEvent.setWidget(target.clone());
		newEvent.setType(type);
		newEvent.setId(getUniqueEventId());
		return newEvent;
	}

	@Override
	public UserInput createInput(WidgetState target, String text) {
		return createInput(target, text, "editText");
	}
	
	public UserInput createInput(WidgetState target, String text, String type) {
		TestCaseInput newInput = TestCaseInput.createInput(getTheSession());
		newInput.setWidget(target);
		newInput.setValue(text);
		newInput.setType(type);
		return newInput;
	}

	@Override
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
	
	public Transition createStep (ActivityState start, Collection<UserInput> inputs, UserEvent event) {
		Transition t = TestCaseTransition.createTransition(start.getElement().getOwnerDocument());
//		Log.i("nofatclips",StartActivity.createActivity(start).getElement().getNodeName());
		try {
			t.setStartActivity(StartActivity.createActivity(start));
		}
		catch (DOMException e) {
			Log.i("nofatclips", "Abstractor->createStep(activity): " + ((e.code==DOMException.HIERARCHY_REQUEST_ERR)?"HIERARCHY_REQUEST_ERR":String.valueOf(e.code)));
		}
		for (UserInput inPut: inputs) {
			t.addInput(inPut);
		}
//		Log.i("nofatclips",t.getElement().getNodeName());
//		Log.i("nofatclips",t.getEvent().getElement().getNodeName());
//		Log.i("nofatclips",event.getElement().getNodeName());
		try {
			t.setEvent (event);
		}
		catch (DOMException e) {
			Log.i("nofatclips", "Abstractor->createStep(event): " + ((e.code==DOMException.HIERARCHY_REQUEST_ERR)?"HIERARCHY_REQUEST_ERR":String.valueOf(e.code)));
		}

		return t;
	}
	
	public String getUniqueEventId () {
		this.eventId++;
		return "e" + this.eventId;
	}

	public String getUniqueActivityId () {
		this.activityId++;
		return "a" + this.activityId;
	}

}