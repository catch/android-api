
package com.snaptic.api;
/*
 * Copyright (c) 2010 Snaptic, Inc
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * http://snaptic.com Java Android library
 * 
 * Hugh Johnson   <hugh@snaptic.com>
 * Harry Tormey   <harry@snaptic.com>
 */

/**
 * @author Hugh Johnson
 * @author Harry Tormey
 * @version 0.1
 * 
 * Data structure that represents a Snaptic users account.
 */
public class SnapticAccount {
	public long id;
	public String email;
	public String username;
	public long accountCreatedOn;

	public SnapticAccount() {
		id = -1;
		email = "";
		username = "";
		accountCreatedOn = -1;
	}

	public SnapticAccount(long id, String email, String username,
			long accountCreatedOn) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.accountCreatedOn = accountCreatedOn;
	}	
}
