package cn.hmz.mypassword.app;

import cn.hmz.mypassword.model.SettingKey;

/**
 * 用户设置变化监听器
 */
public interface OnSettingChangeListener {
	void onSettingChange(SettingKey key);
}
