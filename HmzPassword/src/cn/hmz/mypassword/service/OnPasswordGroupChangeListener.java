package cn.hmz.mypassword.service;

import cn.hmz.mypassword.model.PasswordGroup;

/**
 * ����仯������
 * 
 * @author zengdexing
 * 
 */
public interface OnPasswordGroupChangeListener {

	/**
	 * �û��������µ�����
	 */
	public void onNewPasswordGroup(PasswordGroup passwordGroup);

	/**
	 * �����뱻ɾ����
	 */
	public void onDeletePasswordGroup(String passwordGroupName);

	
	public void onUpdateGroupName(String oldGroupName, String newGroupName);
}
