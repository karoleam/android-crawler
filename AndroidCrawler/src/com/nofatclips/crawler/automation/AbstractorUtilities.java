package com.nofatclips.crawler.automation;

import static com.nofatclips.androidtesting.model.InteractionType.PRESS_KEY;

import java.lang.reflect.Field;

import com.nofatclips.androidtesting.model.UserEvent;
import com.nofatclips.androidtesting.model.WidgetState;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;

public class AbstractorUtilities {

	public static String detectName (View v) {
		String name = "";
		if (v instanceof TextView) {
			TextView t = (TextView)v;
			name = t.getText().toString();
			if (v instanceof EditText) {
				CharSequence hint = ((EditText)v).getHint();
				name = (hint==null)?"":hint.toString();
			}
		} else if (v instanceof RadioGroup) {
			RadioGroup g = (RadioGroup)v;
			int max=g.getChildCount();
			String text = "";
			for (int i=0; i<max; i++) {
				View c = g.getChildAt(i);
				text = detectName (c);
				if (!text.equals("")) {
					name = text;
					break;
				}
			}
		}
		return name;
	}
	
	@SuppressWarnings("rawtypes")
	public static void setCount (View v, WidgetState w) {
		// For lists, the count is set to the number of rows in the list (inactive rows - e.g. separators - count as well)
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
		
		// For grids, the count is set to the number of icons, for RadioGroups it's set to the number of RadioButtons
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

	public static void setValue (View v, WidgetState w) {
		
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
	
	public static String getType (View v) {
		return v.getClass().getName();
	}
	
	// Event description methods, used by Automation - the description property is only used in graphs
	
	// Special handling for Press Key event: there is no target widget to describe
	public static boolean describeKeyEvent (UserEvent e) {
		int val = Integer.parseInt(e.getValue());
		String name;
		for (Field f: android.view.KeyEvent.class.getFields()) {
			name = f.getName();
			if (f.getType().equals(Integer.TYPE)) {
				try {
					if (name.startsWith("KEYCODE_") && (f.getInt(null) == val)) {
						Log.i("nofatclips", "Event Description: " + name);
						e.setDescription(name.replaceAll("KEYCODE_", ""));
						return true;
					}
				} catch (IllegalArgumentException iae) {
				} catch (IllegalAccessException iae) {}
			}
		}
		return false;		
	}
	
	public static boolean describeCurrentEvent (UserEvent e, View v) {
		if (e == null) return false; // This is probably an input, not an event
		if (e.getType().equals(PRESS_KEY)) {
			return describeKeyEvent(e);
		}
		
		// Get text from the target widget
		if (v instanceof TextView) {
			String s = ((TextView)v).getText().toString();
			e.setDescription(s);
			Log.d ("nofatclips", "Event description: " + s);
			return true;
		} else if (v instanceof TabHost) {
			e.setDescription(((TabHost)v).getCurrentTabTag());
		} else if (v instanceof ViewGroup) {
			int childNum = ((ViewGroup)v).getChildCount();
			for (int i = 0; i<childNum; i++) {
				View child =  ((ViewGroup)v).getChildAt(i);
				if (describeCurrentEvent(e, child)) return true;
			}
		}
		return false;
	}

	/** @author nicola */
	/* NOTA:
	 * l'esecuzione di questa funziona puo' essere ottimizzata creando al momento
	 * della descrizione dell'activity una hashmap temporanea <id,nome> (<integer,string>)
	 * per cui la reflection interviene una sola volta
	 */
	public static String reflectTextualIDbyNumericalID(int id)
	{
		try {
			Class c = Class.forName(com.nofatclips.crawler.Resources.PACKAGE_NAME+".R");
			Class idClass = null; 
			for (Class c1 : c.getClasses())
			{
				if (c1.getCanonicalName().endsWith(".id"))
				{
					idClass = c1;
					break;
				}
			}
			
			if (idClass != null)
			{
				for (Field f: idClass.getFields())
				{
					if (f.getInt(null) == id)
						return f.getName();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e("nicola", e.toString());
		}
		
		return null;
	}
	/** @author nicola */
}
