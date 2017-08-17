/**
 * 
 */
package com.sc.camera;

import android.app.Application;
import android.content.Context;


public class MainApp extends Application {

	private static Context sCtx;

	@Override
	public void onCreate() {
		super.onCreate();
		sCtx = getApplicationContext();

	}

	public static Context getContext() {
		return sCtx;
	}

}
