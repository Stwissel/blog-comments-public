/** ========================================================================= *
 * Copyright (C)  2017, 2021 Stephan Wissel                                   *
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

public class Parameters {
	public static final String SUCCESS_MESSAGE = "You comment has been received, it will appear after review";
	public static final String FAILURE_MESSAGE = "Something went wrong, we are sooo sorry";
	public static final String HTTP_REFERER = "Referer";
	public static final String HTTP_CLIENTIP = "ClientIP";
	public static final String HTTP_RREQUESTED_WITH = "X-Requested-With";
	public static final String HTTP_USER_AGENT = "User-Agent";
	public static final String HTTP_CONTENTTYPE = "Content-Type";
	public static final String HTTP_PUSHAPI = "/1/messages.json";

	public static final String EXPECTED_REQUESTED_WITH = "XMLHttpRequest";
	public static final String EXPECTED_CONTENT_TYPE = "application/json";

	public static final String ID_COMMENT = "commentId";
	public static final String ID_REPOSITORYPATH = "RepositoryPath";

	public static final String MESSAGE_NEW_COMMENT = "comment.new";
	public static final String MESSAGE_PUSH_COMMENT = "comment.pushnotification";
	public static final String MESSAGE_PULLREQUEST = "comment.pullrequest";
	public static final String CREATED = "created";
	public static final String IMPORT_DATE_FORMAT = "MMMM dd, yyyy HH:mm:ss a";

	private Parameters() {
		// Prevent instance
	}
}
