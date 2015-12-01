package cn.hmz.mypassword.activity;

import java.util.HashMap;

import android.app.ActionBar;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import cn.hmz.mypassword.R;
import cn.hmz.mypassword.activity.PasswordGroupFragment.OnPasswordGroupSelected;
import cn.hmz.mypassword.app.BaseActivity;
import cn.hmz.mypassword.dialog.ExportDialog;
import cn.hmz.mypassword.dialog.ImportDialog;
import cn.hmz.mypassword.model.SettingKey;
import cn.hmz.mypassword.service.MainService;
import cn.hmz.mypassword.service.Mainbinder;
import cn.zdx.lib.annotation.FindViewById;

import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

/**
 * 主界面
 * 
 * @author zengdexing
 * 
 */
public class MainActivity extends BaseActivity {
	/** 数据源 */
	private Mainbinder mainbinder;
	private long lastBackKeyTime;

	@FindViewById(R.id.drawer_layout)
	private DrawerLayout drawerLayout;

	private ActionBarDrawerToggle mDrawerToggle;

	private PasswordListFragment passwordListFragment;
	private PasswordGroupFragment passwordGroupFragment;

	@FindViewById(R.id.navigation_drawer)
	private View drawerView;

	private OnPasswordGroupSelected onPasswordGroupSelected = new OnPasswordGroupSelected() {
		@Override
		public void onPasswordGroupSelected(String passwordGroupName) {
			drawerLayout.closeDrawer(drawerView);
			if (passwordListFragment != null)
				passwordListFragment.showPasswordGroup(passwordGroupName);
		}
	};

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mainbinder = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mainbinder = (Mainbinder) service;
			initFragment();
		}
	};

	private void initFragment() {
		FragmentManager fragmentManager = getFragmentManager();

		passwordListFragment = (PasswordListFragment) fragmentManager.findFragmentByTag("PasswordListFragment");
		if (passwordListFragment == null)
			passwordListFragment = new PasswordListFragment();
		passwordListFragment.setDataSource(mainbinder);

		passwordGroupFragment = (PasswordGroupFragment) fragmentManager.findFragmentByTag("PasswordGroupFragment");
		if (passwordGroupFragment == null)
			passwordGroupFragment = new PasswordGroupFragment();
		passwordGroupFragment.setDataSource(mainbinder, onPasswordGroupSelected);

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.navigation_drawer, passwordGroupFragment, "PasswordGroupFragment");
		fragmentTransaction.replace(R.id.container, passwordListFragment, "PasswordListFragment");
		fragmentTransaction.commitAllowingStateLoss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		setContentView(R.layout.activity_main);

		initDrawer();

		Intent intent = new Intent(this, MainService.class);
		this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

		// 友盟自动升级
		UmengUpdateAgent.update(this);
	}

	private void initDrawer() {
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, R.drawable.ic_drawer,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActivity().invalidateOptionsMenu();
				getActionBar().setTitle(R.string.app_name);
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				getActivity().invalidateOptionsMenu();
				if (passwordListFragment != null && !passwordListFragment.getPasswordGroupName().equals(""))
					getActionBar().setTitle(passwordListFragment.getPasswordGroupName());
				else {
					getActionBar().setTitle(R.string.app_name);
				}
			}
		};

		drawerLayout.post(new Runnable() {
			@Override
			public void run() {
				mDrawerToggle.syncState();
			}
		});
		drawerLayout.setDrawerListener(mDrawerToggle);

		if (getSetting(SettingKey.IS_SHOWED_DRAWER, "false").equals("false")) {
			putSetting(SettingKey.IS_SHOWED_DRAWER, "true");
			drawerLayout.openDrawer(drawerView);
		} else {
			String lastGroupName = getSetting(SettingKey.LAST_SHOW_PASSWORDGROUP_NAME, "");
			if (lastGroupName.equals(""))
				lastGroupName = getString(R.string.app_name);
			getActionBar().setTitle(lastGroupName);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}

	private boolean isExistSDCard() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else
			return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		int id = item.getItemId();
		switch (id) {
			case R.id.action_add_password:
				if (mainbinder == null)
					break;
				Intent intent = new Intent(this, EditPasswordActivity.class);
				if (passwordListFragment != null)
					intent.putExtra(EditPasswordActivity.PASSWORD_GROUP, passwordListFragment.getPasswordGroupName());
				startActivity(intent);
				break;

			case R.id.action_import:
				// 密码导入
				if (mainbinder == null)
					break;
				ImportDialog importDialog = new ImportDialog(getActivity(), mainbinder);
				importDialog.show();
				break;

			case R.id.action_export:
				// 密码导出
				if (mainbinder == null)
					break;
				if (!isExistSDCard()) {
					showToast(R.string.export_no_sdcard);
					break;
				}
				ExportDialog exportDialog = new ExportDialog(this, mainbinder);
				exportDialog.show();
				break;

			case R.id.action_set_lock_pattern:
				// 软件锁
				startActivity(new Intent(this, SetLockpatternActivity.class));
				break;
			case R.id.action_set_effect:
				// 列表特效
				onEffectClick();
				break;
			case R.id.action_about:
				// 关于
				startActivity(new Intent(this, AboutActivity.class));
				break;
			case R.id.action_exit:
				// 退出
				finish();
				break;

			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!drawerLayout.isDrawerOpen(drawerView)) {
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		} else {
			return super.onCreateOptionsMenu(menu);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				long delay = Math.abs(System.currentTimeMillis() - lastBackKeyTime);
				if (delay > 4000) {
					// 双击退出程序
					showToast(R.string.toast_key_back);
					lastBackKeyTime = System.currentTimeMillis();
					return true;
				}
				break;

			default:
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void onEffectClick() {
		if (getSetting(SettingKey.JAZZY_EFFECT_INTRODUCTION, "false").equals("false")) {
			putSetting(SettingKey.JAZZY_EFFECT_INTRODUCTION, "true");
			Builder builder = new Builder(this);
			builder.setMessage(R.string.action_jazzy_effect_introduction);
			builder.setNeutralButton(R.string.i_known, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onEffectClick();
				}
			});
			builder.show();
		} else {
			Builder builder = new Builder(this);
			builder.setTitle(R.string.action_jazzy_effect);

			final String[] effectArray = getResources().getStringArray(R.array.jazzy_effects);
			builder.setItems(effectArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getActivity().putSetting(SettingKey.JAZZY_EFFECT, which + "");
					onEventEffect(effectArray[which]);
				}
			});
			builder.show();
		}
	}

	/**
	 * 友盟的事件统计“effect”
	 * 
	 * @param effect
	 */
	private void onEventEffect(String effect) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("effect", effect);
		MobclickAgent.onEvent(getActivity(), "effect", map);
	}
}
