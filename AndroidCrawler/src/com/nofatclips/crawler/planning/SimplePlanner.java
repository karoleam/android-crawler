package com.nofatclips.crawler.planning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import android.util.Log;

import com.nofatclips.androidtesting.model.*;
import com.nofatclips.crawler.model.*;

import static com.nofatclips.androidtesting.model.InteractionType.*;
//import static com.nofatclips.androidtesting.model.SimpleType.*;

public class SimplePlanner implements Planner {

	public final static boolean ALLOW_SWAP_TAB = true;
	public final static boolean NO_SWAP_TAB = false;
	public final static boolean ALLOW_GO_BACK = true;
	public final static boolean NO_GO_BACK = false;
	
	public Plan getPlanForActivity (ActivityState a) {
		return getPlanForActivity(a, !Resources.TAB_EVENTS_START_ONLY, ALLOW_GO_BACK);
	}

	public Plan getPlanForBaseActivity (ActivityState a) {
		return getPlanForActivity(a, ALLOW_SWAP_TAB, NO_GO_BACK);
	}

	public void addPlanForActivityWidgets (Plan p, ActivityState a, boolean allowSwapTabs, boolean allowGoBack) {		
		Log.i("nofatclips", "Planning for new Activity " + a.getName());
		for (WidgetState w: getEventFilter()) {
			Collection<UserEvent> events = getUser().handleEvent(w);			
			for (UserEvent evt: events) {
				if (evt == null) continue;
				Collection<UserInput> inputs = new ArrayList<UserInput>();
				for (WidgetState formWidget: getInputFilter()) {
					List<UserInput> alternatives = getFormFiller().handleInput(formWidget);
					if (alternatives.size()==0) continue;
					UserInput inp = alternatives.get(alternatives.size()-1);
					if ( (inp != null) && !((inp.getWidget().getUniqueId().equals(evt.getWidget().getUniqueId())) && (inp.getType().equals(evt.getType()))) ) {
						inputs.add(inp);
					}
				}
				Transition t = getAbstractor().createStep(a, inputs, evt);
				p.addTask(t);
			}
		}
	}
	
	public Plan getPlanForActivity (ActivityState a, boolean allowSwapTabs, boolean allowGoBack) {
		Plan p = new Plan();
		addPlanForActivityWidgets(p, a, allowSwapTabs, allowGoBack);
		
		UserEvent evt;
		Transition t;
		
		// Special handling for pressing back button
		if (Resources.BACK_BUTTON_EVENT && allowGoBack) {
			evt = getAbstractor().createEvent(null, BACK);
			t = getAbstractor().createStep(a, new HashSet<UserInput>(), evt);
			Log.i("nofatclips", "Created trace to press the back button");
			p.addTask(t);
		}
		
		if (Resources.MENU_EVENTS) {
			evt = getAbstractor().createEvent(null, OPEN_MENU);
			t = getAbstractor().createStep(a, new HashSet<UserInput>(), evt);
			Log.i("nofatclips", "Created trace to press the menu button");
			p.addTask(t);
		}

		// Special handling for scrolling down
		if (Resources.SCROLL_DOWN_EVENT) {
			evt = getAbstractor().createEvent(null, SCROLL_DOWN);
			t = getAbstractor().createStep(a, new HashSet<UserInput>(), evt);
			Log.i("nofatclips", "Created trace to perform scrolling down");
			p.addTask(t);
		}

		if (Resources.ORIENTATION_EVENTS) {
			evt = getAbstractor().createEvent(null, CHANGE_ORIENTATION);
			t = getAbstractor().createStep(a, new HashSet<UserInput>(), evt);
			Log.i("nofatclips", "Created trace to change orientation");
			p.addTask(t);
		}
		
		if (Resources.KEY_EVENTS.length>0) {
			for (int keyCode: Resources.KEY_EVENTS) {
				evt = getAbstractor().createEvent(null, PRESS_KEY);
				evt.setValue(String.valueOf(keyCode));
				t = getAbstractor().createStep(a, new HashSet<UserInput>(), evt);
				Log.i("nofatclips", "Created trace to perform key press (key code: " + keyCode + ")");
				p.addTask(t);
			}
		}
		
		return p;
	}
	
	public Filter getEventFilter() {
		return this.eventFilter;
	}

	public void setEventFilter(Filter eventFilter) {
		this.eventFilter = eventFilter;
	}
	
	public Filter getInputFilter() {
		return this.inputFilter;
	}
	
	public void setInputFilter(Filter inputFilter) {
		this.inputFilter = inputFilter;
	}

	public EventHandler getUser() {
		return this.user;
	}

	public void setUser(EventHandler user) {
		this.user = user;
	}

	public InputHandler getFormFiller() {
		return this.formFiller;
	}

	public void setFormFiller(InputHandler formFiller) {
		this.formFiller = formFiller;
	}

	public Abstractor getAbstractor() {
		return this.abstractor;
	}

	public void setAbstractor(Abstractor abstractor) {
		this.abstractor = abstractor;
	}

	private Filter eventFilter;
	private Filter inputFilter;
	private EventHandler user;
	private InputHandler formFiller;
	private Abstractor abstractor;

}
