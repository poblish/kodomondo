/**
 * 
 */
package com.andrewregan.kodomondo.ds.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataSourceMeta {

	String[] value();
	Class<?> daggerConfig();
}
