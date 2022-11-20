/** ========================================================================= *
 * Copyright (C)  2017, 2021 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel.net>                  *
 *                                       @notessensei                         *
 * @version     1.2                                                           *
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author swissel
 */
public class CommentService extends AbstractVerticle {

	private static String commentPath;
	private static final Logger LOGGER = LogManager.getLogger(CommentService.class);

	/**
	 * Convenience method to allow IDE Testing
	 *
	 * @param args - Not used here
	 */
	public static void main(final String[] args) {
		CommentService.commentPath = args.length < 1 ? "/blogcomments/*" : args[0];
		Runner.runVerticle(CommentService.class.getName(), true);
	}

	private final List<String> corsValues = new ArrayList<>();
	private final JsonObject mastodonUsers = new JsonObject();

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Promise<Void> startFuture) {
		// Use v4 only
		System.setProperty("java.net.preferIPv4Stack", "true");
		this.loadCors();
		this.getVertx().deployVerticle("net.wissel.blog.CommentPush")
				.compose(v -> this.getVertx().deployVerticle("net.wissel.blog.CommentPullRequest"))
				.compose(v -> this.getVertx().deployVerticle("net.wissel.blog.CommentStore"))
				.compose(v -> this.launchWebListener())
				.onFailure(startFuture::fail)
				.onSuccess(startFuture::complete);
	}

	private Future<Void> launchWebListener() {
		return Future.future(promise -> {
			final int port = Config.INSTANCE.getPort();

			final HttpServer server = this.vertx.createHttpServer();
			final Router router = Router.router(this.vertx);

			router.route().handler(BodyHandler.create());
			final Route allowCORS = router.route(HttpMethod.OPTIONS, CommentService.commentPath);
			allowCORS.handler(ctx -> {
				this.addCors(ctx);
				ctx.response().end();
			});
			final Route incomingCommentRoute =
					router.route(HttpMethod.POST, CommentService.commentPath)
							.consumes("application/json").produces("application/json");
			incomingCommentRoute.handler(this::newComment);
			incomingCommentRoute.failureHandler(this::commentFailure);

			this.addMastodonRoute(router);

			router.route("/*").handler(StaticHandler.create());
			router.route(HttpMethod.GET, CommentService.commentPath)
					.handler(ctx -> ctx.end("The spoken TAO is not the eternal TAO"));

			server.requestHandler(router).listen(port).onFailure(err -> {
				LOGGER.error("Could not start HTTP server on port {}", port);
				err.printStackTrace();
				promise.fail(err);
			}).onSuccess(v -> {
				LOGGER.info("Server started on port {}", port);
				promise.complete();
			});
		});
	}

	private void addMastodonRoute(final Router router) {
		final String routeURL = "/.well-known/webfinger";
		final String sourceName = System.getenv("ACTIVITY_USERS");
		final File sourceFile = new File(sourceName);
		if (sourceFile.exists() && sourceFile.isFile()) {
			try {
				final String raw = Files.readString(sourceFile.toPath());
				final JsonObject users = new JsonObject(raw);
				users.stream()
						.filter(entry -> entry.getValue() instanceof JsonObject)
						.forEach(entry -> this.mastodonUsers.put(entry.getKey(),
								entry.getValue()));
				router.route(HttpMethod.GET, routeURL).handler(this::webfingerHandler);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			LOGGER.error("File {} not found, no webfinger URL loaded", sourceName);
		}
	}

	private void webfingerHandler(final RoutingContext ctx) {
		final String resource = ctx.request().getParam("resource");
		if (resource == null || !resource.startsWith("acct:")) {
			ResultMessage.end(ctx.response(), "Invalid request", 400);
			return;
		}
		final String user = resource.split(":")[1].toLowerCase();
		if (!mastodonUsers.containsKey(user)) {
			ResultMessage.end(ctx.response(), "No such user", 404);
			return;
		}
		this.setReponseHeaders(ctx.response()).end(this.mastodonReply(user));
	}

	private Buffer mastodonReply(final String user) {
		final JsonObject j = this.mastodonUsers.getJsonObject(user);
		final String userid = j.getString("userid", "johndoe");
		final String domain = j.getString("domain", "unknown");
		final JsonObject result = new JsonObject();

		result.put("subject", String.format("acct:%s@%s", userid, domain));
		result.put("aliases", new JsonArray()
				.add(String.format("https://:%s/@%s", domain, userid))
				.add(String.format("https://:%s/users/%s", domain, userid)))
				.put("links", new JsonArray()
						.add(new JsonObject()
								.put("rel", "http://webfinger.net/rel/profile-page")
								.put("type", "text/html")
								.put("href", String.format("https://%s/@%s", domain, userid)))
						.add(new JsonObject()
								.put("rel", "self")
								.put("type", "application/activity+json")
								.put("href", String.format("https://%s/users/%s", domain, userid)))
						.add(new JsonObject()
								.put("rel", "http://ostatus.org/schema/1.0/subscribe")
								.put("template", String.format(
										"https://%s/authorize_interaction?uri={uri}", domain))));

		return result.toBuffer();
	}

	private HttpServerResponse setReponseHeaders(final HttpServerResponse response) {
		response.setStatusCode(200)
				.putHeader("content-type", "application/jrd+json; charset=utf-8")
				.putHeader("x-frame-options", "DENY")
				.putHeader("x-content-type-options", "nosniff")
				.putHeader("content-security-policy",
						"base-uri 'none'; default-src 'none'; frame-ancestors 'none';")
				.putHeader("cache-control", "max-age=259200, public");
		return response;
	}

	private void addCors(final RoutingContext ctx) {
		final HttpServerResponse response = ctx.response();
		final String origin = ctx.request().getHeader("Origin");
		if (origin != null && this.corsValues.contains(origin)) {
			response.putHeader("Access-Control-Allow-Origin", origin);
			response.putHeader("Access-Control-Allow-Methods", "OPTIONS, POST");
			response.putHeader("Access-Control-Allow-Headers", "Content-Type");
		}
	}

	private JsonObject addParametersFromHeader(final MultiMap headers, final String remoteHost) {
		final JsonObject result = new JsonObject();
		result.put(Parameters.HTTP_CLIENTIP, remoteHost);
		// We overwrite duplicate values here -> never mind for our purpose!
		headers.entries().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
		return result;
	}

	/**
	 * If we don't like the comment
	 *
	 * @param ctx Routing context
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
	 * @param ctx Routing context
	 */
	private void newComment(final RoutingContext ctx) {
		final HttpServerRequest request = ctx.request();
		final HttpServerResponse response = ctx.response();
		this.addCors(ctx);
		final MultiMap headers = request.headers();
		final JsonObject comment = ctx.body().asJsonObject();
		comment.put("parameters",
				this.addParametersFromHeader(headers, request.remoteAddress().host()));
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
			ResultMessage.end(response, e.getMessage(), 400);
		}
	}

}
