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

public interface Parameters {
	public String SUCCESS_MESSAGE = "You comment has been received and will appear shortly";
	public String FAILURE_MESSAGE = "Something went wrong, we are sooo sorry";
	public String HTTP_REFERER = "Referer";
	public String HTTP_CLIENTIP = "ClientIP";
	public String HTTP_RREQUESTED_WITH = "X-Requested-With";
	public String HTTP_USER_AGENT = "User-Agent";
	public String HTTP_CONTENTTYPE = "Content-Type";
	
	public String EXPECTED_REQUESTED_WITH = "XMLHttpRequest";
	public String EXPECTED_CONTENT_TYPE = "application/json";
	public String MESSAGE_NEW_COMMENT = "comment.new";
	public String ID_COMMENT = "commentId";
	
}
