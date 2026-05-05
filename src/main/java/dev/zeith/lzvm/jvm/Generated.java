package dev.zeith.lzvm.jvm;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
public @interface Generated
{
	String expression();
	
	int argCount() default 0;
}