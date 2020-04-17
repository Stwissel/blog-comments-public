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

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Creates a new Pull Request for a just posted comment
 *
 * @author swissel
 *
 */
public class CommentPullRequest extends AbstractVerticle {
    private final Logger            logger        = LoggerFactory.getLogger(this.getClass());
    private WebClient               client        = null;
    private final Queue<JsonObject> retryMessages = new LinkedList<>();

    /**
     * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
     */
    @Override
    public void start(final Promise<Void> startFuture) {

        final EventBus eb = this.getVertx().eventBus();
        eb.consumer(Parameters.MESSAGE_PULLREQUEST, this::processNewMessages);
        // For messages not going through - retry after 30 seconds
        this.getVertx().setPeriodic(30000L, this::retryHandler);
        startFuture.complete();
    }

    private void createPullRequest(final JsonObject message, final String accessToken) {

        // Compose pull request data
        final JsonObject source = new JsonObject();
        source.put("branch", new JsonObject().put("name", message.getString("branch", "unknown")));
        source.put("repository", new JsonObject().put("full_name", Config.INSTANCE.getRepositoryURL()));

        final JsonObject destination = new JsonObject();
        destination.put("branch", new JsonObject().put("name", "master"));

        // Final assembly
        final JsonObject body = new JsonObject();
        body.put("title", "Comment from " + message.getString("Commentor", "Anonymous"));
        body.put("source", source);
        body.put("destination", destination);
        body.put("close_source_branch", true);

        final WebClient wc = this.getWebClient();
        final String target = "/2.0/repositories/" + Config.INSTANCE.getRepositoryURL() + "/pullrequests/";

        wc.post(443, "api.bitbucket.org", target).ssl(true)
                .putHeader("Content-Type", "application/json")
                .putHeader("Authorization", "Bearer " + accessToken).sendJson(body, res -> {
                    if (res.failed()) {
                        this.logger.error(body.encode(), res.cause());
                        this.retryMessages.offer(message);
                    } else {
                        this.logger.info("Pullrequest deployed");
                    }
                });
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

        final Future<String> userToken = OauthHelper.getAccessToken(this.getVertx());
        userToken.setHandler(handler -> {
            if (handler.succeeded()) {
                final String accessToken = handler.result();
                this.createPullRequest(message, accessToken);

            } else {
                this.logger.error(handler.cause().getMessage(), handler.cause());
            }
        });
    }

    private void retryHandler(final Long interval) {
        final EventBus eb = this.getVertx().eventBus();
        while (!this.retryMessages.isEmpty()) {
            final JsonObject candidate = this.retryMessages.poll();
            if (candidate != null) {
                final int retryCount = (candidate.containsKey("retryCount")) ? (candidate.getInteger("retryCount") + 1)
                        : 1;
                candidate.put("retryCount", retryCount);
                if (retryCount > 10) {
                    this.logger.error(
                            "Pull request Retry count exceeded from user:" + candidate.getString("eMail", "n/a"));
                    this.getVertx().eventBus().publish(Parameters.MESSAGE_PUSH_COMMENT,
                            candidate.put("Failure", "Pull request Retry count exceeded from user:"
                                    + candidate.getString("eMail", "n/a")));
                } else {
                    // Put them back on the bus
                    this.logger.info("Retry from user: " + candidate.getString("eMail", "n/a"));
                    eb.publish(Parameters.MESSAGE_PULLREQUEST, candidate);
                }
            }
        }
    }
}
