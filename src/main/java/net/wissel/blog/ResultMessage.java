/** ========================================================================= *
 * Copyright (C)  2017, 2018 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel@net>                  *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.blog;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

/**
 * Message that gets send back to to the browser
 * @author swissel
 *
 */
public class ResultMessage {
	public final String message;
	public final int status;
	
	public ResultMessage(final String message, final int errorNum) {
		this.message = message;
		this.status = errorNum;
	}
	
	@SuppressWarnings("unused")
	private ResultMessage() {
		// Don't construct one
		this.message = "Unknown";
		this.status = 500;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		JsonObject o = JsonObject.mapFrom(this);
		return o.encodePrettily();
	}
	
	/**
	 * Shortcut to end a request with a simple JSON message!
	 * 
	 * @param response The HTTP response
	 * @param message the textual message
	 * @param status the http status code
	 */
	public static void end(HttpServerResponse response, String message, int status) {
		ResultMessage m = new ResultMessage(message, status);
		response.setStatusCode((status == 0) ? 200 : status);
		response.putHeader(Parameters.HTTP_CONTENTTYPE, Parameters.EXPECTED_CONTENT_TYPE);
		response.end(m.toString());
	}
	
}
