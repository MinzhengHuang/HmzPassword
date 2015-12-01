/*
 * Copyright (c) 2014 zengdexing
 */
package cn.zdx.lib.annotation;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.View;

/**
 * ע������֧࣬���κζ���ĳ�Ա����ʹ��{@link FindViewById}���ҿؼ�������ʹ��{@link OnClick}�󶨵���¼���
 * <p>
 * ע�⣺�̳���{@link XingBaseActivity}��UI����Ҫ��ʹ�ø��࣬BaseActivity�Ѿ��ṩ�˶�
 * {@link FindViewById}�� {@link OnClick} ��֧�֣���BaseActivity��ֱ��ʹ��
 * </p>
 * 
 * @author zengdexing
 */
public class XingAnnotationHelper {
	/**
	 * ��ʼ��Activity��ʹ����{@link FindViewById}ע��ĳ�Ա����
	 * 
	 * @param target
	 */
	public static void findView(Activity target) {
		findView(target, ViewFinder.create(target));
	}

	/**
	 * ��ʼ��Object��ʹ����{@link FindViewById}ע��ĳ�Ա����
	 * 
	 * @param target
	 */
	public static void findView(Object target, View view) {
		findView(target, ViewFinder.create(view));
	}

	/**
	 * ��ʼ��ʹ����{@link FindViewById}ע������Ա����
	 * 
	 * @param target
	 *            ��Ҫ��ʼ���Ķ��󣬸ö���ĳ�Ա����ʹ����{@link FindViewById}ע��
	 * @param viewFinder
	 *            View������
	 */
	public static void findView(Object target, ViewFinder viewFinder) {
		Class<?> clazz = target.getClass();

		Field[] fields = clazz.getDeclaredFields();
		if (fields != null && fields.length > 0) {
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				findView(target, field, viewFinder);
			}
		}
	}

	/**
	 * ��ʹ��{@link FindViewById}ע��ķ���
	 * 
	 * @param target
	 *            ��Ҫ�󶨵Ķ��󣬸ö����г�Ա����ʹ����{@link FindViewById}
	 * @param field
	 *            ��Ҫ�󶨵ı���
	 * @param viewFinder
	 *            VIew������
	 */
	public static void findView(Object target, Field field, ViewFinder viewFinder) {
		if (field.isAnnotationPresent(FindViewById.class)) {
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}

			int id = field.getAnnotation(FindViewById.class).value();
			View view = viewFinder.findViewById(id);

			checkView(field.getName(), view, field.getType());

			try {
				field.set(target, view);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ������͡�</br>
	 * <p>
	 * �������{@link NullPointerException}�쳣��ͨ����ע���ID���󣬼������ݸ�ID�Ҳ�����Ӧ�Ŀؼ���
	 * </p>
	 * <p>
	 * �������{@link ClassCastException}�쳣��ͨ����ע��ID�Ŀؼ���XML�е����ͺ���Ҫ�󶨵Ķ������Ͳ�һ�¡�
	 * </p>
	 */
	private static void checkView(String msg, View targetView, Class<?> bindType) {
		if (targetView == null) {
			throw new NullPointerException("\"" + msg + "\"Ҫ�󶨵Ŀؼ�������!!");
		} else if (!bindType.isInstance(targetView)) {
			String error = "����ƥ�����\"" + msg + "\"Ҫ�󶨵�����Ϊ��" + bindType + ",��Ŀ������Ϊ��" + targetView.getClass();
			throw new ClassCastException(error);
		}
	}
}
