package it.unina.androidripper.filters;

import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;

public class SimpleEventFilter extends ButtonFilter {

	@Override
	public boolean isValidItem(View v) {
		return (super.isValidItem(v) || (v instanceof TabHost) || (v instanceof ListView) || (v.getClass().getSimpleName().endsWith("IconMenuItemView")));
	}

}
