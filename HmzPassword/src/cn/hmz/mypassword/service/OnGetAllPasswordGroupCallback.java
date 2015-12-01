package cn.hmz.mypassword.service;

import java.util.List;

import cn.hmz.mypassword.model.PasswordGroup;

public interface OnGetAllPasswordGroupCallback {
	public void onGetAllPasswordGroup(List<PasswordGroup> passwordGroups);
}
