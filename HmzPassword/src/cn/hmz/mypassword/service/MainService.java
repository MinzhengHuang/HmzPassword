package cn.hmz.mypassword.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import cn.hmz.mypassword.app.MyApplication;

public class MainService extends Service {
	private Mainbinder mainbinder;
	
	public IBinder onBind(Intent intent) {
		return mainbinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mainbinder = new Mainbinder(this, (MyApplication) getApplicationContext());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mainbinder.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			handlerIntent(intent);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void handlerIntent(Intent intent) {
		//
	}
}
