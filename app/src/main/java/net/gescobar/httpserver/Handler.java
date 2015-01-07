package net.gescobar.httpserver;

/**
 * Implementations of this interface handle requests. It must be thread-safe.
 * 
 * @author German Escobar
 */
public interface Handler {

	/**
	 * This method is called when a new request is received. Use the request argument to extract information from the
	 * request; use the response argument to write the response back to the client.
	 * 
	 * @param request the HTTP request wrapper.
	 * @param response the HTTP response wrapper.
	 */
	void handle(Request request, Response response);
	
}
