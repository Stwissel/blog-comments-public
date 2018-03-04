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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;

public enum OauthHelper {
    INSTANCE;
    
    private AccessToken backendAccess = null;
    private OAuth2Auth oauth2 = null;
    
    private OAuth2Auth getOauth(Vertx vertx) {
        if (this.oauth2 == null) {
            final OAuth2ClientOptions credentials = new OAuth2ClientOptions()
                    .setClientID(Config.INSTANCE.getClientToken()).setClientSecret(Config.INSTANCE.getClientSecret())
                    .setSite(Config.INSTANCE.getOauthURL()).setAuthorizationPath("/site/oauth2/authorize")
                    .setTokenPath("/site/oauth2/access_token");

            this.oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.CLIENT, credentials);
        }
        return this.oauth2;
    }
    
    public void getOauthSessionToken(final Future<AccessToken> result, final Vertx vertx) {

        if (this.backendAccess != null) {
            if (this.backendAccess.expired()) {
                this.backendAccess.refresh(h -> {
                    if (h.succeeded()) {
                        result.complete(this.backendAccess);
                    } else {
                        result.fail(h.cause());
                    }
                });
            } else {
                result.complete(this.backendAccess);
            }
        } else {
            final OAuth2Auth oauth = this.getOauth(vertx);
            final JsonObject tokenConfig = new JsonObject();
            oauth.authenticate(tokenConfig, res -> {
                if (res.failed()) {
                    System.err.println("Access Token Error: " + res.cause().getMessage());
                    result.fail(res.cause());
                } else {
                    this.backendAccess = (AccessToken) res.result();
                    result.complete((AccessToken) res.result());
                }
            });
        }
    }
}
