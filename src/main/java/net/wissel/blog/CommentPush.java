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
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Notifies on new comments via Push
 *
 * @author swissel
 *
 */
public class CommentPush extends AbstractVerticle {

	WebClient client = null;

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Future<Void> startFuture) throws Exception {
		// We only bother if we have configuration values
		if (Config.INSTANCE.getPushToken() != null && Config.INSTANCE.getPushUser() != null) {
			final EventBus eb = this.getVertx().eventBus();
			eb.consumer(Parameters.MESSAGE_PUSH_COMMENT, this::processNewMessages);
		}
		startFuture.complete();

	}

	private WebClient getWebClient() {
		if (this.client == null) {
			final WebClientOptions options = new WebClientOptions().setUserAgent("CommentService 1.0").setSsl(true)
					.setKeepAlive(true);
			this.client = WebClient.create(this.vertx, options);
		}
		return this.client;
	}

	private void processNewMessages(final Message<JsonObject> incoming) {

		final JsonObject message = incoming.body();
		final JsonObject body = new JsonObject();

		body.put("token", Config.INSTANCE.getPushToken());
		body.put("user", Config.INSTANCE.getPushUser());
		body.put("title", "Comment from " + message.getString("Commentor"));
		body.put("url", "https://bitbucket.org/" + Config.INSTANCE.getRepositoryURL() + "/src");
		body.put("url_title", "See in bitbucket");
		final String payload = message.getString("Body");
		body.put("message", payload.substring(0, Math.min(100, payload.length())));

		final WebClient wc = this.getWebClient();
		final String target = Parameters.HTTP_PUSHAPI;
		wc.post(443, "api.pushover.net", target).ssl(true).putHeader("Content-Type", "application/json")
				.sendJsonObject(body, res -> {
					if (res.failed()) {
						System.err.println("Failed to send push notification");
					} else {
						System.out.println("PushOver notified");
					}
				});

	}
}
