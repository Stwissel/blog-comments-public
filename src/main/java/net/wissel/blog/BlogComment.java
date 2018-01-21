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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlogComment {
	public String Commentor;
	public String eMail;
	public String webSite;
	public String Body;
	public String rChallenge;
	public String rResponse;
	public String parentId;
	public final Map<String, String> parameters = new HashMap<>();

	/**
	 * We want: a message, a name and an eMail
	 * 
	 * @throws InvalidContentException
	 */
	public void checkForMandatoryFields(final String captchaKey) throws InvalidContentException {
		boolean canProceed = true; // innocent until proven guilty

		final StringBuilder result = new StringBuilder();
		final Collection<String> problems = new ArrayList<String>();
		if ((this.eMail == null) || !HTMLFilter.isEmail(this.eMail)) {
			problems.add(String.valueOf(this.eMail) + " seems not to be a valid eMail");
			canProceed = false;
		}

		if ((this.Commentor == null) || this.Commentor.trim().equals("")) {
			problems.add("Please provide a name");
			canProceed = false;
		}

		if ((this.Body == null) || this.Body.trim().equals("")) {
			problems.add("Some content for your comment is required!");
			canProceed = false;
		}

		if ((this.webSite != null) && !this.webSite.trim().equals("") && !HTMLFilter.isURL(this.webSite)) {
			problems.add("Please provide a valid URL or none");
			canProceed = false;
		}
		
		if (this.parentId == null || this.parentId.trim().equals("")) {
			problems.add("I can't identify which post you try to comment, Sorry");
			canProceed = false;
		}

		if (!HTMLFilter.isValidCaptcha(captchaKey, this.rChallenge, this.rResponse)) {
			problems.add("Sorry, the ReCaptchaCode wasn't valid, you might want to try again");
			canProceed = false;
		}

		if (!canProceed) {

			result.append("Sorry submitting your comment didn't work, check this:\n");
			for (final String p : problems) {
				result.append(p);
				result.append("\n");
			}
			throw new InvalidContentException(result.toString());
		}
	}
}
