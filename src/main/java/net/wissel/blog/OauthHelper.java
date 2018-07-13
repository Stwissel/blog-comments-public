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

import java.util.Base64;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class OauthHelper {

    public final static String  AUTH_PATH  = "/site/oauth2/authorize";
    public final static String  TOKEN_PATH = "/site/oauth2/access_token";
    public final static String  TOKEN_NAME = "access_token";
    private final static Logger LOGGER     = LoggerFactory.getLogger("OauthHelper");

    /**
     * Creates a manual Session to retrieve an access token
     *
     * @param result
     * @param vertx
     */
    public static void getAccessToken(final Future<String> result, final Vertx vertx) {
        final WebClientOptions options = new WebClientOptions().setUserAgent("CommentService 1.0.2").setSsl(true)
                .setKeepAlive(true);
        final WebClient wc = WebClient.create(vertx, options);
        final String accessBasic = Base64.getEncoder().encodeToString(
                (Config.INSTANCE.getClientToken() + ":" + Config.INSTANCE.getClientSecret()).getBytes());
        final MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("grant_type", "client_credentials");
        form.set("scope", "");
        form.set("client_id", Config.INSTANCE.getClientToken());

        wc.post(443, Config.INSTANCE.getOauthURL(), OauthHelper.TOKEN_PATH).ssl(true)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .putHeader("Authorization", "Basic " + accessBasic).sendForm(form, res -> {
                    if (res.failed()) {
                        OauthHelper.LOGGER.error("Failed to obtain OAuth token:", res.cause());
                        result.fail(res.cause());
                    } else {
                        try {
                            final JsonObject authJson = res.result().bodyAsJsonObject();
                            if (authJson.containsKey(OauthHelper.TOKEN_NAME)) {
                                result.complete(authJson.getString(OauthHelper.TOKEN_NAME));
                            } else {
                                result.fail("HTTP did not contain access token:" + authJson.encode());
                            }
                        } catch (final Throwable t) {
                            OauthHelper.LOGGER.error(t);
                            result.fail(t);
                        }
                    }
                });

    }

}
