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

import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * @author stw
 */
public class Runner {

	static Logger logger = LogManager.getLogger(Runner.class);

	private Runner() {
		// Only static methods here
	}

	/**
	 * @param verticleID
	 * @param debugMode
	 */
	public static void runVerticle(final String verticleID, final boolean debugMode) {

		// That's just for IDE testing...
		final Consumer<Vertx> runner = vertx -> {
			try {
				final DeploymentOptions depOpt = new DeploymentOptions();
				vertx.deployVerticle(verticleID, depOpt, res -> {
					if (res.succeeded()) {
						Runner.logger.info("{} deployed as {}", verticleID, res.result());
					} else {
						Runner.logger.error("Deployment failed for {} {}", verticleID, res.cause());
					}
				});
			} catch (final Exception e) {
				Runner.logger.error(e.getMessage(), e);
			}
		};

		final VertxOptions options = new VertxOptions();
		if (Runner.isDebug(debugMode)) {
			options.setBlockedThreadCheckInterval(1000 * 60 * 60L);
		}

		final Vertx vertx = Vertx.vertx(options);
		runner.accept(vertx);
	}

	private static boolean isDebug(boolean debugMode) {
		// Debug was given as parameter or there's an environment variable
		// set to true so we go debugging
		return debugMode || Boolean.parseBoolean(System.getenv("Debug"));
	}
}
