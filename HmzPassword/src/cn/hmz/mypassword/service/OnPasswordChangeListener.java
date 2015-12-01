package cn.hmz.mypassword.service;

import cn.hmz.mypassword.model.Password;

/**
 * 密码变化监听器
 * 
 * @author zengdexing
 * 
 */
public interface OnPasswordChangeListener {
	/**
	 * 用户增加了新的密码
	 */
	public void onNewPassword(Password password);

	/**
	 * 密码被删除了
	 */
	public void onDeletePassword(int id);

	/**
	 * 密码的属性发生变化了
	 */
	public void onUpdatePassword(Password password);
}
