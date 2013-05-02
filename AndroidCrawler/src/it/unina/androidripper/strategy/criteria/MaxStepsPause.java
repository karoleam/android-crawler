package it.unina.androidripper.strategy.criteria;

import it.unina.androidripper.model.Strategy;
import android.util.Log;

public class MaxStepsPause implements PauseCriteria {
	
	private int max;
	private int current;

	public MaxStepsPause () {
		this(1000);
	}

	public MaxStepsPause (int maxSteps) {
		this.max=maxSteps;
		reset();
	}

	public boolean pause () {
		this.current--;
		Log.i("androidripper", "Check for pause: " + current + " steps left of " + max);
		return (this.current==0);
	}
	
	public void reset() {
		this.current=max;
	}
	
	public void setStrategy(Strategy theStrategy) {}
	
}