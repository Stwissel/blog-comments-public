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

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import de.triology.recaptchav2java.ReCaptcha;

/**
 * HTML filter utility.
 * 
 * @author Craig R. McClanahan
 * @author Tim Tye
 * @version $Id: HTMLFilter.java 939315 2010-04-29 14:11:01Z kkolinko $
 */

public final class HTMLFilter {

	/**
	 * Filter the specified message string for characters that are sensitive in
	 * HTML. This avoids potential attacks caused by including JavaScript codes
	 * in the request URL that is often reported in error messages.
	 * 
	 * @param message
	 *            The message string to be filtered
	 */
	public static String filter(String message) {
		boolean didFilter = false;
		if (message == null)
			return (null);

		char content[] = new char[message.length()];
		message.getChars(0, message.length(), content, 0);
		StringBuilder result = new StringBuilder(content.length + 50);
		for (int i = 0; i < content.length; i++) {
			switch (content[i]) {
			case '<':
				result.append("&lt;");
				didFilter = true;
				break;
			case '>':
				result.append("&gt;");
				didFilter = true;
				break;
			case '&':
				result.append("&amp;");
				didFilter = true;
				break;
			case '"':
				result.append("&quot;");
				didFilter = true;
				break;
			default:
				result.append(content[i]);
			}
		}

		if (didFilter) {
			System.out.print(message);
			System.out.print(" -> ");
			System.out.println(result.toString());
		}

		return (result.toString());

	}

	/**
	 * Gets a plain text equivalent of potentially HTML containing String
	 * Designed for single line input fields, terminates on the first chr(10) or
	 * chr(13)
	 * 
	 * @param message
	 *            The message string to be stripped of HTML
	 */
	public static String strip(String message) {
		boolean ampersandMode = false;
		boolean tagMode = false;

		if (message == null) {
			return (null);
		}
		char content[] = new char[message.length()];
		message.getChars(0, message.length(), content, 0);
		StringBuilder result = new StringBuilder(content.length);
		for (int i = 0; i < content.length; i++) {
			if (ampersandMode) {
				// End of encoded char reached
				if (";".equals(content[i]) || " ".equals(content[i])) {
					ampersandMode = false;
				}
			} else if (tagMode) {
				if (">".equals(content[i])) {
					tagMode = false;
				}
			} else {
				if ("<".equals(content[i])) {
					tagMode = true;
					ampersandMode = false;
				} else if ("&".equals(content[i])) {
					tagMode = false;
					ampersandMode = true;
				} else if ("\n".equals(content[i]) || "\r".equals(content[i])) {
					break;
				} else {
					// Only here we accept chars
					result.append(content[i]);
				}
			}
		}
		return (result.toString());

	}

	/**
	 * Checks that a string looks like a valid eMail address
	 * 
	 * @param candidate
	 *            The string to be checked
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
	 * @param candidate
	 *            The string to be checked
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
	public static boolean isValidCaptcha(String captchaKey, String challenge, String response) {
		boolean result = true;
		// We only test if we have a remote address and the captcha switch is on
		if (captchaKey != null) {
			ReCaptcha reCaptcha = new ReCaptcha(captchaKey);
			return reCaptcha.isValid(response);
		}

		return result;
	}

}
