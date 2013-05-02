package it.unina.androidripper.strategy;

import it.unina.androidripper.model.Comparator;
import it.unina.androidripper.model.Strategy;
import it.unina.androidripper.model.TerminationListener;

import com.nofatclips.androidtesting.model.ActivityState;
import com.nofatclips.androidtesting.model.Trace;

public class NullStrategy implements Strategy {

	public void addState(ActivityState newActivity) {}

	public boolean compareState(ActivityState theActivity) {
		return false; // Always return no match
	}

	public Comparator getComparator() {
		return null;
	}

	public void setComparator(Comparator c) {}

	public boolean checkForTermination() {
		return false;
	}

	public boolean checkForTransition() {
		return true;
	}

	public void setTask(Trace theTask) {}

	public boolean checkForExploration() {
		return !isLastComparationPositive();
	}

	public boolean isLastComparationPositive() {
		return false;
	}

	public Trace getTask() {
		return null;
	}

	public ActivityState getStateBeforeEvent() {
		return null;
	}

	public ActivityState getStateAfterEvent() {
		return null;
	}

	public boolean checkForPause() {
		return false;
	}

	public void registerTerminationListener(TerminationListener theListener) {}

	public int getDepth() {
		return 0;
	}

}
