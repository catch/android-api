//
//  Copyright 2011 Catch.com, Inc.
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//      http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.catchnotes.api;

/**
 * Represents a Catch account.
 * 
 * Provides information such as user name, email address, account creation date, etc.
 */
public class CatchAccount {
	public long id;
	public String email;
	public String username;
	public long accountCreatedOn;
	public String auth_token;
	public long periodLimit;
	public long periodUsage;
	public long periodStart;
	public long periodEnd;
	public long subscriptionEnd;
	public int accountLevel;
	public String accountDescription;

	public CatchAccount() {
		id = -1;
		email = "";
		username = "";
		accountCreatedOn = -1;
		auth_token = "";
		periodLimit = -1;
		periodUsage = -1;
		periodStart = -1;
		periodEnd = -1;
		subscriptionEnd = -1;
		accountLevel = -1;
		accountDescription = "";
	}

	public CatchAccount(long id, String email, String username, long accountCreatedOn, String auth_token, int accountLevel, String accountDescription, long monthlyLimit, long usage, long usageStart, long usageEnd, long subscriptionEnd) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.accountCreatedOn = accountCreatedOn;
		this.auth_token = auth_token;
		this.periodLimit = monthlyLimit;
		this.periodUsage = usage;
		this.periodStart = usageStart;
		this.periodEnd = usageEnd;
		this.subscriptionEnd = subscriptionEnd;
		this.accountLevel = accountLevel;
		this.accountDescription = accountDescription;
	}
	
	@Override
	public String toString() {
		return
			"email=\"" + email + "\" " +
			"id=" + id + ' ' +
			"username=\"" + username + "\" " +
			"accountCreatedOn=" + accountCreatedOn + ' ' +
			"accountLevel=" + accountLevel + ' ' +
			"accountDescription=\"" + accountDescription + "\" " +
			"periodLimit=" + periodLimit + ' ' +
			"periodUsage=" + periodUsage + ' ' +
			"periodStart=" + periodStart + ' ' +
			"periodEnd=" + periodEnd + ' ' +
			"subscriptionEnd=" + subscriptionEnd + ' ' +
			"auth_token=" + auth_token;
	}
}
