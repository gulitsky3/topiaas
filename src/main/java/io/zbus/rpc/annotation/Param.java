package io.zbus.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
	String value() default "";         //Name of parameter
	String name() default "";          //Alias to value
	boolean ctx() default  false;      //is from context
	String defaultValue() default "";  //Default value of parameter 
}
