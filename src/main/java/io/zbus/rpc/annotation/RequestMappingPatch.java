package io.zbus.rpc.annotation;

import java.lang.annotation.Annotation;

public class RequestMappingPatch implements Route {
	private RequestMapping r;
	public RequestMappingPatch(RequestMapping r) {
		this.r = r;
	}
	
	@Override
	public Class<? extends Annotation> annotationType() { 
		return Route.class;
	}

	@Override
	public String value() { 
		return r.value();
	}

	@Override
	public String path() { 
		return r.path();
	}

	@Override
	public String[] method() { 
		return r.method();
	}

	@Override
	public boolean exclude() { 
		return r.exclude();
	}

	@Override
	public boolean docEnabled() { 
		return r.docEnabled();
	}

}
