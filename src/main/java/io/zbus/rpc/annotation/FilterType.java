package io.zbus.rpc.annotation;

public enum FilterType {
	GlobalBefore, //Global filter in first chain
	GlobalAfter,  //Global filter in last chain
	Local,         //Simple filter, depends on the location of filter, class or method
	Exception
}
