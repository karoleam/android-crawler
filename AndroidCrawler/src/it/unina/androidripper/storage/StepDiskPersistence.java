package it.unina.androidripper.storage;

import it.unina.androidripper.model.SaveStateListener;
import it.unina.androidripper.model.SessionParams;
import android.content.ContextWrapper;
import android.util.Log;

import com.nofatclips.androidtesting.model.Session;
import com.nofatclips.androidtesting.model.Trace;

import static it.unina.androidripper.storage.Resources.*;

public class StepDiskPersistence extends DiskPersistence implements SaveStateListener {

	public StepDiskPersistence () {
		super();
		PersistenceFactory.registerForSavingState(this);
	}
	
	public StepDiskPersistence (Session theSession) {
		super(theSession);
		PersistenceFactory.registerForSavingState(this);
	}

	public StepDiskPersistence (int theStep) {
		this();
		setStep(theStep);
	}
	
	public StepDiskPersistence (Session theSession, int theStep) {
		this (theSession);
		setStep (theStep);
	}
	
	public void setStep (int theStep) {
		this.step = theStep;		
	}
	
	@Override
	public void addTrace (Trace t) {
		super.addTrace (t);
		this.count++;
		Log.i("androidripper", "Session count is " + this.count +". Will dump to disk at " + this.step);
		if (this.count == this.step) {
			saveStep();
			this.count=0;
		}
	}
	
	@Override
	public String generate() {
		return generateXML() + System.getProperty("line.separator");
	}
	
	public String generateXML () {
		String graph = super.generate();
		
		// Session is smaller than the step: fall back to DiskPersistence behavior and save all
		if (isFirst() && isLast()) {
			return graph;
		}
		
		int bodyBegin = graph.indexOf(XML_BODY_BEGIN);
		int bodyEnd = graph.lastIndexOf(XML_BODY_END) + XML_BODY_END.length();
		
		// First step: return header and body, save the footer for the final step
		if (isFirst()) {
			this.footer = graph.substring(bodyEnd);
			return graph.substring(0,bodyEnd);
		}
		
		// Final step: return the body (if any) and the footer
		if (isLast()) {
			return (bodyBegin == -1)?(this.footer):graph.substring(bodyBegin);
		}
		
		if ( (bodyBegin == -1) || (bodyEnd == -1) ) { // Empty body
			return "";
		}
		
		// Return the body of the XML graph
		return graph.substring(bodyBegin,bodyEnd);
	}
	
	public void saveStep () {
		
		if (ENABLE_MODEL) {
			
			if (isFirst()) {
				Log.i ("androidripper", "Saving the session on disk. This is the first batch: the file will be created.");					
			} else {
				Log.i ("androidripper", "Saving the session on disk.");
			}
			save(isLast());
			
		}
		
		for (Trace t: getSession()) {
			getSession().removeTrace(t);
		}
		
		setNotFirst();
		System.gc();
	}
	
	@Override
	public void save () {
		save (true);
	}
	
	public void save (boolean last) {
		
			if (!isFirst()) this.mode = ContextWrapper.MODE_APPEND;
			
			if (last) {
				setLast();
				Log.i ("androidripper", "Saving the session on disk. This is the last batch. The session will be terminated.");			
			}
			
			if (ENABLE_MODEL) super.save();
			
	}
	
	public boolean isFirst () {
		return this.first;
	}

	public boolean isLast () {
		return this.last;
	}
	
	public void setNotFirst () {
		this.first = false;
	}
	
	public void setLast () {
		this.last = true;
	}

	@Override
	public String getListenerName() {
		return ACTOR_NAME;
	}

	@Override
	public SessionParams onSavingState() {
		return new SessionParams(PARAM_NAME, this.footer);
	}

	@Override
	public void onLoadingState(SessionParams sessionParams) {
		this.footer = sessionParams.get(PARAM_NAME);
		Log.d("androidripper", "Backup session footer restored to " + this.footer);
	}

	private int step = 1;
	private int count = 0;
	private String footer = "";
	private boolean first = true;
	private boolean last = false;
	
	public final static String ACTOR_NAME = "StepDiskPersistence";
	public final static String PARAM_NAME = "footer";

}