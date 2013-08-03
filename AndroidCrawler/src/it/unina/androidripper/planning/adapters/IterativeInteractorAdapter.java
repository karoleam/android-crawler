package it.unina.androidripper.planning.adapters;

import it.unina.androidripper.model.Abstractor;
import it.unina.androidripper.planning.adapters.InteractorAdapter;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.nofatclips.androidtesting.model.UserEvent;
import com.nofatclips.androidtesting.model.WidgetState;

public abstract class IterativeInteractorAdapter extends InteractorAdapter {

	private int maxEventsPerWidget;

	public IterativeInteractorAdapter (String ... simpleTypes) {
		super (simpleTypes);
	}

	public IterativeInteractorAdapter (int maxItems) {
		setMaxEventsPerWidget(maxItems);
	}

	public IterativeInteractorAdapter (Abstractor theAbstractor) {
		super (theAbstractor);
	}

	public IterativeInteractorAdapter (int maxItems, String ... simpleTypes) {
		this (simpleTypes);
		setMaxEventsPerWidget(maxItems);
	}

	public IterativeInteractorAdapter (Abstractor theAbstractor, String ... simpleTypes) {
		super (theAbstractor, simpleTypes);
	}

	public IterativeInteractorAdapter (Abstractor theAbstractor, int maxItems) {
		this (theAbstractor);
		setMaxEventsPerWidget(maxItems);
	}

	public IterativeInteractorAdapter (Abstractor theAbstractor, int maxItems, String ... simpleTypes) {
		this (theAbstractor, simpleTypes);
		setMaxEventsPerWidget(maxItems);
	}

	@Override
	public List<UserEvent> getEvents (WidgetState w) {
		ArrayList<UserEvent> events = new ArrayList<UserEvent>();
		if (canUseWidget(w)) {
			final int fromItem = 1; 
			final int toItem = getToItem(w, fromItem, w.getCount()); 
			if (toItem<fromItem) return events;
			Log.d("androidripper", "Handling event " + getInteractionType() + " for items [" + fromItem + "," + toItem + "] on " + w.getSimpleType() + " #" + w.getId() + " count=" + w.getCount());
			for (int i=fromItem; i<=toItem; i++) {
				events.add(generateEvent(w, String.valueOf(i)));
			}
		}
		return events;
	}

	public int getToItem(WidgetState w, int fromItem, int toItem) {
		return (getMaxEventsPerWidget(w)>0)?Math.min (fromItem + getMaxEventsPerWidget(w) - 1, toItem):toItem;
	}

	public int getToItem(int fromItem, int toItem) {
		return (getMaxEventsPerWidget()>0)?Math.min (fromItem + getMaxEventsPerWidget() - 1, toItem):toItem;
	}

	public int getMaxEventsPerWidget() {
		return this.maxEventsPerWidget;
	}

	public int getMaxEventsPerWidget(WidgetState w) {
		return getMaxEventsPerWidget();
	}

	public void setMaxEventsPerWidget(int maxEventsPerWidget) {
		this.maxEventsPerWidget = maxEventsPerWidget;
	}

}