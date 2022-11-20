/** ========================================================================= *
 * Copyright (C)  2017, 2021 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel.net>                  *
 *                                       @notessensei                         *
 * @version     1.1                                                           *
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Saves received comments into Bitbucket
 *
 * @author swissel
 */
public class CommentStore extends AbstractVerticle {

    private static final String RETRY_COUNT = "retryCount";

    WebClient client = null;
    private final Logger logger = LogManager.getLogger(this.getClass());

    private final Queue<JsonObject> retryMessages = new LinkedList<>();

    /**
     * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
     */
    @Override
    public void start(final Promise<Void> startFuture) {

        final EventBus eb = this.getVertx().eventBus();
        eb.consumer(Parameters.MESSAGE_NEW_COMMENT, this::processNewMessages);
        // For messages not going through retry after 10 seconds
        this.getVertx().setPeriodic(10000L, this::retryHandler);
        logger.info("Verticle {} deployed", this.getClass().getName());
        startFuture.complete();

    }

    private String getAuthorEmail(final JsonObject message) {
        return "\"" + message.getString("Commentor") + "\"" + " <" + message.getString("eMail")
                + ">";
    }

    private String getDateYear() {
        final Date today = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/");
        return sdf.format(today);
    }

    private String getMessage(final JsonObject message) {
        return "Comment from " + message.getString("Commentor");
    }

    private String getMessagePath(final JsonObject message) {

        return "/src/comments/" + this.getDateYear() + message.getString(Parameters.ID_COMMENT)
                + ".json";
    }

    private WebClient getWebClient() {
        if (this.client == null) {
            final WebClientOptions options =
                    new WebClientOptions().setUserAgent("CommentService 1.0.4").setSsl(true)
                            .setKeepAlive(true);
            this.client = WebClient.create(this.vertx, options);
        }
        return this.client;
    }

    private void processNewMessages(final Message<JsonObject> incoming) {

        final JsonObject message = incoming.body();
        if (!message.containsKey(Parameters.CREATED)) {
            final SimpleDateFormat sdf =
                    new SimpleDateFormat(Parameters.IMPORT_DATE_FORMAT, Locale.US);
            message.put(Parameters.CREATED, sdf.format(new Date()));
        }

        // ADD an id if needed
        if (!message.containsKey(Parameters.ID_COMMENT)) {
            message.put(Parameters.ID_COMMENT, UUID.randomUUID().toString());
        }

        this.logger.info("Processing {}", this.getMessagePath(message));

        OauthHelper.getAccessToken(this.getVertx()).onFailure(err -> {
            this.logger.error("Failed to get access Token: {}, {}", this.getMessagePath(message),
                    err);
            this.getVertx().eventBus().publish(Parameters.MESSAGE_PUSH_COMMENT,
                    message.put("Failure", "Failed to get access token"));
        }).onSuccess(accessToken -> this.storeMessageInBitbucket(message, accessToken));
    }

    private void retryHandler(final Long interval) {
        final EventBus eb = this.getVertx().eventBus();
        while (!this.retryMessages.isEmpty()) {
            final JsonObject candidate = this.retryMessages.poll();
            if (candidate != null) {
                final int retryCount = (candidate.getInteger(RETRY_COUNT, 0) + 1);
                candidate.put(RETRY_COUNT, retryCount);
                if (retryCount > 20) {
                    this.logger.error("Retry count exceeded: {}", this.getMessagePath(candidate));
                    this.getVertx().eventBus().publish(Parameters.MESSAGE_PUSH_COMMENT,
                            candidate.put("Failure", "Retry count exceeded"));

                } else {
                    // Put them back on the bus
                    this.logger.info("Retry: {}", this.getMessagePath(candidate));
                    eb.publish(Parameters.MESSAGE_NEW_COMMENT, candidate);

                }
            }
        }
    }

    private void storeMessageInBitbucket(final JsonObject message, final String accessToken) {

        // Convert to HTTP Form format as used by Bitbucket API
        final String commentBranch = "comments-" + UUID.randomUUID().toString().substring(0, 5);
        // .or. Single Branch, so multiple comments can be approved in one go
        // change commentBranch to "comments" for that
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
                        this.logger.error(
                                "Failed to send (will retry): {}, {}", this.getMessagePath(message),
                                res.cause());
                        this.retryMessages.offer(message);
                    } else {
                        this.getVertx().eventBus().publish(Parameters.MESSAGE_PUSH_COMMENT,
                                message);
                        this.getVertx().eventBus().publish(Parameters.MESSAGE_PULLREQUEST, message);
                        this.logger.info("Posted to {}", this.getMessagePath(message));
                    }
                });
    }

}
