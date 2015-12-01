/*
 * Copyright (c) 2014 zengdexing
 */
package cn.zdx.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * �ؼ�����ע��
 * 
 * @author zengdexing
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface FindViewById {
	int value();
}
