package cn.hmz.mypassword.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Binder;
import cn.hmz.mypassword.app.MyApplication;
import cn.hmz.mypassword.app.OnSettingChangeListener;
import cn.hmz.mypassword.database.PasswordDatabase;
import cn.hmz.mypassword.model.AsyncResult;
import cn.hmz.mypassword.model.AsyncSingleTask;
import cn.hmz.mypassword.model.Password;
import cn.hmz.mypassword.model.PasswordGroup;
import cn.hmz.mypassword.model.SettingKey;
import cn.hmz.mypassword.service.task.GetAllPasswordTask;

public class Mainbinder extends Binder {
	private MyApplication myApplication;
	private PasswordDatabase passwordDatabase;

	/** 密码变化监听器 */
	private List<OnPasswordChangeListener> onPasswordListeners = new ArrayList<OnPasswordChangeListener>();

	/** 密码分组变化监听 */
	private List<OnPasswordGroupChangeListener> onPasswordGroupListeners = new ArrayList<OnPasswordGroupChangeListener>();

	private OnSettingChangeListener onSettingChangeListener = new OnSettingChangeListener() {
		@Override
		public void onSettingChange(SettingKey key) {
			// 用户密码变化了，重新解密后再加密
			encodePasswd(myApplication.getSetting(SettingKey.LOCK_PATTERN, "[]"));
		}
	};

	/** 重新解密 */
	private void encodePasswd(final String newPasswd) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				passwordDatabase.encodePasswd(newPasswd);
				return asyncResult;
			}
		}.execute();
	}

	public Mainbinder(Context context, MyApplication myApplication) {
		passwordDatabase = new PasswordDatabase(context);
		this.myApplication = myApplication;
		final String passwd = myApplication.getSetting(SettingKey.LOCK_PATTERN, "[]");
		myApplication.registOnSettingChangeListener(SettingKey.LOCK_PATTERN, onSettingChangeListener);
		// 线程安全
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				passwordDatabase.setCurrentPasswd(passwd);
				passwordDatabase.getWritableDatabase();
				return asyncResult;
			}
		}.execute();
	}

	void onDestroy() {
		passwordDatabase.close();
		myApplication.unregistOnSettingChangeListener(SettingKey.LOCK_PATTERN, onSettingChangeListener);
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				onPasswordListeners.clear();
			}
		}.execute();
	}

	public void registOnPasswordGroupListener(final OnPasswordGroupChangeListener onPasswordGroupListener) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				onPasswordGroupListeners.add(onPasswordGroupListener);
			}
		}.execute();
	}

	public void unregistOnPasswordGroupListener(final OnPasswordGroupChangeListener onPasswordGroupListener) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				onPasswordGroupListeners.remove(onPasswordGroupListener);
			}
		}.execute();
	}

	public void registOnPasswordListener(final OnPasswordChangeListener onPasswordListener) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				onPasswordListeners.add(onPasswordListener);
			}
		}.execute();
	}

	public void unregistOnPasswordListener(final OnPasswordChangeListener onPasswordListener) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				onPasswordListeners.remove(onPasswordListener);
			}
		}.execute();
	}

	public void getAllPassword(OnGetAllPasswordCallback onGetAllPasswordCallback, String groupName) {
		GetAllPasswordTask getAllPasswordTask = new GetAllPasswordTask(passwordDatabase, onGetAllPasswordCallback,
				groupName);
		getAllPasswordTask.execute();
	}

	public void getAllPassword(OnGetAllPasswordCallback onGetAllPasswordCallback) {
		GetAllPasswordTask getAllPasswordTask = new GetAllPasswordTask(passwordDatabase, onGetAllPasswordCallback, null);
		getAllPasswordTask.execute();
	}

	/**
	 * 删除密码
	 * 
	 * @param id
	 *            密码ID
	 * @param onDeletePasswordResultListener
	 *            结果监听器
	 */
	public void deletePassword(final int id) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				int result = passwordDatabase.deletePasssword(id);
				asyncResult.setResult(result);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				for (OnPasswordChangeListener onPasswordListener : onPasswordListeners) {
					onPasswordListener.onDeletePassword(id);
				}
			}
		}.execute();
	}

	public void getPassword(final int id, final OnGetPasswordCallback onGetPasswordCallback) {
		new AsyncSingleTask<Password>() {
			@Override
			protected AsyncResult<Password> doInBackground(AsyncResult<Password> asyncResult) {
				Password password = passwordDatabase.getPassword(id);
				asyncResult.setData(password);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Password> asyncResult) {
				onGetPasswordCallback.onGetPassword(asyncResult.getData());
			}
		}.execute();
	}

	public void updatePassword(final Password password) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				int result = passwordDatabase.updatePassword(password);
				asyncResult.setResult(result);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				for (OnPasswordChangeListener onPasswordListener : onPasswordListeners) {
					onPasswordListener.onUpdatePassword(password);
				}
			}
		}.execute();
	}

	public void insertPassword(final Password password) {
		new AsyncSingleTask<Password>() {
			@Override
			protected AsyncResult<Password> doInBackground(AsyncResult<Password> asyncResult) {
				String newGroupName = password.getGroupName();

				/** 是否是新的分组 */
				boolean isNew = true;
				List<PasswordGroup> passwordGroups = passwordDatabase.getAllPasswordGroup();
				for (int i = 0; i < passwordGroups.size(); i++) {
					PasswordGroup passwordGroup = passwordGroups.get(i);
					if (passwordGroup.getGroupName().equals(newGroupName)) {
						isNew = false;
						break;
					}
				}

				if (isNew) {
					// 不存在的分组，添加
					PasswordGroup passwordGroup = new PasswordGroup();
					passwordGroup.setGroupName(newGroupName);
					passwordDatabase.addPasswordGroup(passwordGroup);
				}
				asyncResult.getBundle().putBoolean("isNew", isNew);

				int result = (int) passwordDatabase.insertPassword(password);
				password.setId(result);
				asyncResult.setData(password);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Password> asyncResult) {
				if (asyncResult.getBundle().getBoolean("isNew")) {
					PasswordGroup passwordGroup = new PasswordGroup();
					passwordGroup.setGroupName(asyncResult.getData().getGroupName());

					for (OnPasswordGroupChangeListener onPasswordGroupListener : onPasswordGroupListeners) {
						onPasswordGroupListener.onNewPasswordGroup(passwordGroup);
					}
				}

				for (OnPasswordChangeListener onPasswordListener : onPasswordListeners) {
					onPasswordListener.onNewPassword(asyncResult.getData());
				}
			}
		}.execute();
	}

	public void insertPasswordGroup(final PasswordGroup passwordGroup) {
		new AsyncSingleTask<PasswordGroup>() {
			@Override
			protected AsyncResult<PasswordGroup> doInBackground(AsyncResult<PasswordGroup> asyncResult) {
				String newGroupName = passwordGroup.getGroupName();

				boolean isNew = true;
				List<PasswordGroup> passwordGroups = passwordDatabase.getAllPasswordGroup();
				for (int i = 0; i < passwordGroups.size(); i++) {
					PasswordGroup passwordGroup = passwordGroups.get(i);
					if (passwordGroup.getGroupName().equals(newGroupName)) {
						isNew = false;
						break;
					}
				}

				if (isNew) {
					PasswordGroup passwordGroup = new PasswordGroup();
					passwordGroup.setGroupName(newGroupName);
					passwordDatabase.addPasswordGroup(passwordGroup);
				}
				asyncResult.getBundle().putBoolean("isNew", isNew);
				asyncResult.setData(passwordGroup);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<PasswordGroup> asyncResult) {
				if (asyncResult.getBundle().getBoolean("isNew")) {
					for (OnPasswordGroupChangeListener onPasswordGroupListener : onPasswordGroupListeners) {
						onPasswordGroupListener.onNewPasswordGroup(asyncResult.getData());
					}
				}
			}
		}.execute();
	}

	/**
	 * 删除密码分组，包括密码分住下的所有密码都会被删除
	 * 
	 * @param passwordGroupName
	 *            分组名
	 */
	public void deletePasswordgroup(final String passwordGroupName) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				int count = passwordDatabase.deletePasswordGroup(passwordGroupName);
				asyncResult.setResult(count);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				if (asyncResult.getResult() > 0) {
					for (OnPasswordGroupChangeListener onPasswordGroupListener : onPasswordGroupListeners) {
						onPasswordGroupListener.onDeletePasswordGroup(passwordGroupName);
					}
				}
			}
		}.execute();
	}

	/**
	 * 更新组名字
	 * 
	 * @param oldGroupName
	 * @param newGroupName
	 */
	public void updatePasswdGroupName(final String oldGroupName, final String newGroupName) {
		new AsyncSingleTask<Void>() {
			@Override
			protected AsyncResult<Void> doInBackground(AsyncResult<Void> asyncResult) {
				passwordDatabase.updatePasswdGroupName(oldGroupName, newGroupName);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<Void> asyncResult) {
				for (OnPasswordGroupChangeListener onPasswordGroupListener : onPasswordGroupListeners) {
					onPasswordGroupListener.onUpdateGroupName(oldGroupName, newGroupName);
				}
			}
		}.execute();
	}

	public void getAllPasswordGroup(final OnGetAllPasswordGroupCallback onGetAllPasswordGroupCallback) {
		new AsyncSingleTask<List<PasswordGroup>>() {
			@Override
			protected AsyncResult<List<PasswordGroup>> doInBackground(AsyncResult<List<PasswordGroup>> asyncResult) {
				List<PasswordGroup> list = passwordDatabase.getAllPasswordGroup();
				asyncResult.setData(list);
				return asyncResult;
			}

			@Override
			protected void runOnUIThread(AsyncResult<List<PasswordGroup>> asyncResult) {
				onGetAllPasswordGroupCallback.onGetAllPasswordGroup(asyncResult.getData());
			}
		}.execute();
	}
}
