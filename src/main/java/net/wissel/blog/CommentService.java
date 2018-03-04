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

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author swissel
 *
 */
public class CommentService extends AbstractVerticle {

	private static final String COMMENT_PATH = "/blogcomments/*";

	/**
	 * Convenience method to allow IDE Testing
	 *
	 * @param args
	 *            - Not used here
	 */
	public static void main(final String[] args) {
		Runner.runVerticle(CommentService.class.getName(), true);
	}

	private final List<String> corsValues = new ArrayList<>();

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Future<Void> startFuture) throws Exception {
	    // Use v4 only
	    System.setProperty("java.net.preferIPv4Stack" , "true");
		this.loadCors();
		this.getVertx().deployVerticle("net.wissel.blog.CommentPush");
		this.getVertx().deployVerticle("net.wissel.blog.CommentPullRequest");
		this.getVertx().deployVerticle("net.wissel.blog.CommentStore", result -> {
			if (result.succeeded()) {
				final int port = Config.INSTANCE.getPort();

				final HttpServer server = this.vertx.createHttpServer();
				final Router router = Router.router(this.vertx);
				router.route().handler(BodyHandler.create());

				final Route allowCORS = router.route(HttpMethod.OPTIONS, CommentService.COMMENT_PATH);
				allowCORS.handler(ctx -> {
					this.addCors(ctx);
					ctx.response().end();
				});
				final Route incomingCommentRoute = router.route(HttpMethod.POST, CommentService.COMMENT_PATH)
						.consumes("application/json").produces("application/json");
				incomingCommentRoute.handler(this::newComment);
				incomingCommentRoute.failureHandler(this::commentFailure);

				router.route("/*").handler(StaticHandler.create());

				server.requestHandler(router::accept).listen(port, listenResult -> {
					if (listenResult.failed()) {
						System.out.println("Could not start HTTP server on port " + String.valueOf(port));
						listenResult.cause().printStackTrace();
						startFuture.fail(listenResult.cause());
					} else {
						System.out.println("Server started on port " + String.valueOf(port));
						startFuture.complete();
					}
				});
			} else {
				startFuture.fail(result.cause());
			}
		});
	}

	private void addCors(final RoutingContext ctx) {
		final HttpServerResponse response = ctx.response();
		final String origin = ctx.request().getHeader("Origin");
		if (origin != null) {
			if (this.corsValues.contains(origin)) {
				response.putHeader("Access-Control-Allow-Origin", origin);
				response.putHeader("Access-Control-Allow-Methods", "OPTIONS, POST");
				response.putHeader("Access-Control-Allow-Headers", "Content-Type");
			}
		}
	}

	private JsonObject addParametersFromHeader(final MultiMap headers, final String remoteHost) {
		final JsonObject result = new JsonObject();
		result.put(Parameters.HTTP_CLIENTIP, remoteHost);
		headers.entries().forEach(entry -> {
			// We overwrite duplicate values here -> never mind for our purpose!
			result.put(entry.getKey(), entry.getValue());
		});
		return result;
	}

	/**
	 * If we don't like the comment
	 *
	 * @param ctx
	 *            Routing context
	 */
	private void commentFailure(final RoutingContext ctx) {
		ResultMessage.end(ctx.response(), "Something went wrong", 500);
	}

	private void loadCors() {
		this.corsValues.add("http://localhost");
		this.corsValues.add("https://wissel.net");
		this.corsValues.add("https://www.wissel.net");
		this.corsValues.add("https://stwissel.github.io");
		this.corsValues.add("https://notessensei.com");
		this.corsValues.add("https://www.notessensei.com");
	}

	/**
	 * Captures incoming new comments to be routed to forwarder
	 *
	 * @param ctx
	 *            Routing context
	 */
	private void newComment(final RoutingContext ctx) {
		final HttpServerRequest request = ctx.request();
		final HttpServerResponse response = ctx.response();
		this.addCors(ctx);
		final MultiMap headers = request.headers();
		final JsonObject comment = ctx.getBodyAsJson();
		comment.put("parameters", this.addParametersFromHeader(headers, request.remoteAddress().host()));
		try {
			// Incoming comments are Markdown, legacy might be HTML, so we flag it here
			comment.put("markdown", true);
			// We check if we have everything
			final BlogComment blogComment = comment.mapTo(BlogComment.class);
			blogComment.checkForMandatoryFields(Config.INSTANCE.getCaptchSecret());
			final EventBus eb = this.getVertx().eventBus();
			eb.publish(Parameters.MESSAGE_NEW_COMMENT, comment);
			ResultMessage.end(response, Parameters.SUCCESS_MESSAGE, 200);
		} catch (final Exception e) {
			e.printStackTrace();
			// TODO: better status code!
			ResultMessage.end(response, e.getMessage(), 400);
		}
	}

}
