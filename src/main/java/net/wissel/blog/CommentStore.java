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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Saves received comments into Bitbucket
 * @author swissel
 *
 */
public class CommentStore extends AbstractVerticle {
	
	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Future<Void> startFuture) throws Exception {
		
		EventBus eb = this.getVertx().eventBus();
		eb.consumer(Parameters.MESSAGE_NEW_COMMENT,this::processNewMessages);
		
		
	}
	
	private void processNewMessages(Message<JsonObject> message) {
		
		System.out.println(message.body().encodePrettily());
		
	}

}
