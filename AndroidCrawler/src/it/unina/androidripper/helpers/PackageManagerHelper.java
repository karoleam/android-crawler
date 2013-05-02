package it.unina.androidripper.helpers;

import it.unina.androidripper.Resources;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;


public class PackageManagerHelper
{
	public static final String TAG = "androidripper";
	
	public PackageInfo packageInfo = null;
	private ArrayList<String> permissionCache = null;
	
	public PackageManagerHelper (Context ctx) throws Exception
	{
		try
		{
		    PackageManager pm = ctx.getPackageManager();
		    packageInfo = pm.getPackageInfo(Resources.PACKAGE_NAME, PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS | PackageManager.GET_ACTIVITIES);
		}
		catch (Exception ex)
		{
			Log.v(TAG, ex.toString());
			throw ex;
		}
	}
	
	public ArrayList<String> getPackagePermissions()
	{
		if (permissionCache == null)
		{
		
			ArrayList<String> ret = new ArrayList<String>();
			
			for (String p : packageInfo.requestedPermissions)
			{
				ret.add(p);
			}
			
			permissionCache = ret;
		}
		
		return permissionCache;
	}
	
	public boolean hasPermission(String permission)
	{
		ArrayList<String> perms = this.getPackagePermissions();
		
		for (String perm : perms)
			if (perm.equals(permission))
				return true;
		
		return false;
	}

	public boolean hasInternetPermission()
	{
		return this.hasPermission("android.permission.INTERNET");
	}
	
	public boolean hasAccessMockLocationPermission()
	{
		return this.hasPermission("android.permission.ACCESS_MOCK_LOCATION");
	}

	public boolean reactsToSMSReceive()
	{
		return this.hasPermission("android.permission.RECEIVE_SMS");
	}
	
	public boolean canSendSMS()
	{
		return this.hasPermission("android.permission.SEND_SMS");
	}
	
	public boolean activityCanRotate(String activityCanonicalName)
	{
		for (ActivityInfo ai : packageInfo.activities)
		{
			if (ai.name.equals(activityCanonicalName))
			{
				//TODO
			}
		}
		
		return false;
	}
}
