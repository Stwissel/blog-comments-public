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
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Saves received comments into Bitbucket
 *
 * @author swissel
 *
 */
public class CommentStore extends AbstractVerticle {

    WebClient            client = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Queue<JsonObject> retryMessages = new LinkedList<>();

    /**
     * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
     */
    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        final EventBus eb = this.getVertx().eventBus();
        eb.consumer(Parameters.MESSAGE_NEW_COMMENT, this::processNewMessages);
        // For messages not gotung through
        this.getVertx().setPeriodic(5000L, this::retryHandler);

        startFuture.complete();

    }

    private String getAuthorEmail(final JsonObject message) {
        return "\"" + message.getString("Commentor") + "\"" + " <" + message.getString("eMail") + ">";
    }

    private String getMessage(final JsonObject message) {
        return "Comment from " + message.getString("Commentor");
    }

    private String getMessagePath(final JsonObject message) {
        return "/src/comments/" + message.getString("parentId") + "/" + message.getString(Parameters.ID_COMMENT)
                + ".json";
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

        // ADD an id if needed
        if (!message.containsKey(Parameters.ID_COMMENT)) {
            message.put(Parameters.ID_COMMENT, UUID.randomUUID().toString());
        }

        this.logger.info("Processing " + this.getMessagePath(message));

        final Future<AccessToken> userToken = Future.future();
        userToken.setHandler(handler -> {
            if (handler.succeeded()) {
                // We are good to go

                // this.backendAccess = handler.result();
                final User u = handler.result();
                final String accessToken = u.principal().getString("access_token");

                this.storeMessageInBitbucket(message, accessToken);

            } else {
                this.logger.error("Failed to get access Token:" + this.getMessagePath(message), handler.cause());
            }
        });
        OauthHelper.INSTANCE.getOauthSessionToken(userToken, this.getVertx());
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
                    this.logger.error("Retry count exceeded:" + this.getMessagePath(candidate));
                } else {
                    // Put them back on the bus
                    this.logger.info("Retry: " + this.getMessagePath(candidate));
                    eb.publish(Parameters.MESSAGE_NEW_COMMENT, candidate);
                }
            }
        }
    }

    private void storeMessageInBitbucket(final JsonObject message, final String accessToken) {

        // Convert to HTTP Form format as used by Bitbucket API
        final String commentBranch = "comments-" + UUID.randomUUID().toString();
        message.put("branch", commentBranch);
        message.put(Parameters.ID_REPOSITORYPATH, this.getMessagePath(message));
        final MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("author", this.getAuthorEmail(message));
        form.set("message", this.getMessage(message));
        form.set("branch", commentBranch);
        form.set(this.getMessagePath(message), message.encodePrettily());

        final WebClient wc = this.getWebClient();
        final String target = "/2.0/repositories/" + Config.INSTANCE.getRepositoryURL() + "/src";
        wc.post(443, "api.bitbucket.org", target).ssl(true)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .putHeader("Authorization", "Bearer " + accessToken).sendForm(form, res -> {
                    if (res.failed()) {
                        this.logger.error("Failed to send (will retry):" + this.getMessagePath(message), res.cause());
                        this.retryMessages.offer(message);
                    } else {                      
                        this.getVertx().eventBus().publish(Parameters.MESSAGE_PUSH_COMMENT, message);
                        this.getVertx().eventBus().publish(Parameters.MESSAGE_PULLREQUEST, message);
                        this.logger.info("Posted to " + this.getMessagePath(message));
                    }
                });
    }

}
