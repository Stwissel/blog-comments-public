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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration settings from the environment
 * 
 * @author swissel
 *
 */
public enum Config {
	
	INSTANCE;
	
	private static final String CLIENT_SECRET = "ClientSecret";
	private static final String CLIENT_TOKEN = "ClientToken";
	private static final String PORT = "PORT";
	private static final String CAPTCHA_SECRET = "CaptchaSecret";
	private static final String REPOSITORY_URL = "RepositoryURL";
	private static final String OAUTH_URL= "OauthURL";
	
	private boolean isInit = false;
	private final Map<String, String> configValues = new HashMap<>();
	
	public String getClientSecret() {
		init();
		return this.configValues.get(CLIENT_SECRET);
	}
	
	public String getClientToken() {
		init();
		return this.configValues.get(CLIENT_TOKEN);
	}
	
	public String getCaptchSecret() {
		init();
		return this.configValues.get(CAPTCHA_SECRET);
	}
	
	public int getPort() {
		init();
		return Integer.valueOf(this.configValues.get(PORT));
	}
	
	public String getRepositoryURL() {
		init();
		return this.configValues.get(REPOSITORY_URL);
	}
	
	public String getOauthURL() {
		init();
		return this.configValues.get(OAUTH_URL);
	}
	
	/**
	 * Loads the configuration values from the environment
	 * or elsewhere
	 */
	private void init() {
		if (this.isInit) {
			return;
		}

		final String portCandidate = System.getenv(PORT);
		this.configValues.put(PORT, (portCandidate == null || "".equals(portCandidate)) ? "5000" : portCandidate);
		this.addParam(CLIENT_SECRET);
		this.addParam(CLIENT_TOKEN);
		this.addParam(CAPTCHA_SECRET);
		this.addParam(REPOSITORY_URL);
		this.addParam(OAUTH_URL);
		
		this.isInit = true;
	}

	private void addParam(String clientToken) {
		String candidate = System.getenv(clientToken);
		this.configValues.put(clientToken, candidate);
	}
	
}
