package net.gescobar.httpserver;

import java.util.Map;

/**
 * Holds the information of the request.
 * 
 * @author German Escobar
 */
public interface Request {

	String getMethod();
	
	String getPath();
	
	String getHost();
	
	Map<String,String> getHeaders();
	
	String getHeader(String name);
	
}
