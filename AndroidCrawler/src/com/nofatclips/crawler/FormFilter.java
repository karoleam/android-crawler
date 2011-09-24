package com.nofatclips.crawler;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

public class FormFilter extends ListFilter {

	@Override
	public boolean isValidItem(View v) {
		return ((v instanceof CompoundButton) || (v instanceof TextView));
	}

}
