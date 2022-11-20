/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wissel.blog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import de.triology.recaptchav2java.ReCaptcha;

/**
 * HTML filter utility. Java 8 mods by Stephan
 *
 * @author Craig R. McClanahan
 * @author Tim Tye
 * @author Stephan Wissel
 * @version $Id: HTMLFilter.java 939315 2010-04-29 14:11:01Z kkolinko $
 */

public final class HTMLFilter {

	private static final Character OPEN_TAG = Character.valueOf('<');
	private static final Character CLOSE_TAG = Character.valueOf('>');
	private static final Character AMPERSAND = Character.valueOf('&');
	private static final Character SPACE = Character.valueOf(' ');
	private static final Character SEMICOLON = Character.valueOf(';');
	private static final Character CR = Character.valueOf('\r');
	private static final Character LF = Character.valueOf('\n');
	private static final Character QUOTE = Character.valueOf('"');

	private static final Logger LOGGER = LogManager.getLogger(HTMLFilter.class);

	private static final Map<Character, String> FILTER_CHARS = new HashMap<>();

	static {
		FILTER_CHARS.put(OPEN_TAG, "&lt;");
		FILTER_CHARS.put(CLOSE_TAG, "&gt;");
		FILTER_CHARS.put(AMPERSAND, "&amp;");
		FILTER_CHARS.put(QUOTE, "&quot;");
	}

	/**
	 * Filter the specified message string for characters that are sensitive in
	 * HTML. This avoids potential attacks caused by including JavaScript codes in
	 * the request URL that is often reported in error messages.
	 *
	 * @param message The message string to be filtered
	 */
	public static String filter(String message) {

		if (message == null) {
			return (null);
		}
		final AtomicBoolean didFilter = new AtomicBoolean(false);
		final StringBuilder result = new StringBuilder(message.length() + 50);

		message.codePoints().mapToObj(char.class::cast).filter(c -> {
			final boolean isFilterChar = FILTER_CHARS.containsKey(c);
			if (isFilterChar) {
				result.append(FILTER_CHARS.get(c));
				didFilter.set(true);
			}
			return !isFilterChar;
		}).forEach(result::append);

		if (didFilter.get()) {
			LOGGER.info("{}  -> {}", message, result);
		}

		return (result.toString());

	}

	/**
	 * Gets a plain text equivalent of potentially HTML containing String Designed
	 * for single line input fields, terminates on the first chr(10) or chr(13)
	 *
	 * @param message The message string to be stripped of HTML
	 */
	public static String strip(String message) {

		if (message == null) {
			return (null);
		}

		final AtomicBoolean ampersandMode = new AtomicBoolean(false);
		final AtomicBoolean tagMode = new AtomicBoolean(false);
		final AtomicBoolean done = new AtomicBoolean(false);
		final StringBuilder result = new StringBuilder(message.length());

		message.codePoints().mapToObj(char.class::cast).filter(c -> {
			final boolean stopit = ampersandMode.get();
			final boolean isAmpersandMode = stopit && !(c.equals(SEMICOLON) || c.equals(SPACE));
			ampersandMode.set(isAmpersandMode);
			return !stopit;
		}).filter(c -> {
			final boolean stopit = tagMode.get();
			final boolean isTagMode = stopit && !c.equals(CLOSE_TAG);
			tagMode.set(isTagMode);
			return !stopit;
		}).filter(c -> {
			final boolean startAmpersand = c.equals(AMPERSAND);
			ampersandMode.set(startAmpersand);
			return !startAmpersand;
		}).filter(c -> {
			final boolean startTagMode = c.equals(OPEN_TAG);
			tagMode.set(startTagMode);
			return !startTagMode;
		}).filter(c -> !done.getAndSet(c.equals(LF) || c.equals(CR))).forEach(result::append);

		return result.toString();

	}

	/**
	 * Checks that a string looks like a valid eMail address
	 *
	 * @param candidate The string to be checked
	 */
	public static boolean isEmail(String candidate) {
		// Get an EmailValidator
		EmailValidator validator = EmailValidator.getInstance();

		// Validate an email address
		return validator.isValid(candidate);

	}

	/**
	 * Checks that a string looks like a valid URL
	 *
	 * @param candidate The string to be checked
	 */
	public static boolean isURL(String candidate) {
		// Get an EmailValidator
		UrlValidator validator = UrlValidator.getInstance();

		// Validate an URL
		return validator.isValid(candidate);

	}

	/**
	 * Call to the reCaptcha
	 *
	 * @param remoteAddress
	 * @param challenge
	 * @param response
	 * @return
	 */
	public static boolean isValidCaptcha(String captchaKey, String response) {
		boolean result = true;
		// We only test if we have a remote address and the captcha switch is on
		if (captchaKey != null) {
			ReCaptcha reCaptcha = new ReCaptcha(captchaKey);
			return reCaptcha.isValid(response);
		}

		return result;
	}

	private HTMLFilter() {
		// Static methods only
	}

}
