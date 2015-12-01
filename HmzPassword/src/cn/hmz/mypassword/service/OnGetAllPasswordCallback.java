package cn.hmz.mypassword.service;

import java.util.List;

import cn.hmz.mypassword.model.Password;

public interface OnGetAllPasswordCallback {
	public void onGetAllPassword(String froupName, List<Password> passwords);
}
