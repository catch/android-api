package com.snaptic.api;

import java.util.List;
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
 * Data structure that represents a Snaptic note.
 */
public class SnapticNote {
	public static final String NOT_SET = "NOT_SET";

	public long id;
	public long parentId;
	public CharSequence source;
	public CharSequence sourceUrl;
	public CharSequence owner;
	public long ownerId;
	public long creationTime;
	public long modificationTime;
	public long reminderTime;
	public CharSequence text;
	public CharSequence summary;
	public long depth;
	public long children;
	public CharSequence mode;
	public CharSequence publicUrl;

	public double latitude;
	public double longitude;
	public double altitude;
	public double speed;
	public double bearing;
	public double accuracyPosition;
	public double accuracyAltitude;

	public List<CharSequence> tags;
	public List<Object> mediaList;

	public SnapticNote() {
		id = -1;
		parentId = -1;
		source = NOT_SET;
		sourceUrl = NOT_SET;
		owner = NOT_SET;
		ownerId = -1;
		creationTime = -1;
		modificationTime = -1;
		reminderTime = -1;
		text = NOT_SET;
		summary = NOT_SET;
		depth = -1;
		children = -1;
		mode = "private";
		publicUrl = NOT_SET;

		latitude = 0;
		longitude = 0;
		altitude = 0;
		speed = 0;
		bearing = 0;
		accuracyPosition = 0;
		accuracyAltitude = 0;

		tags = null;
		mediaList = null;
	}

	/**
	 * Return tags contained within note.
	 * 
     * @return CharSequence of tags in note
	 */	
	public CharSequence getTags() {
		if (tags == null) {
			return "";
		}
		
		StringBuffer sb = new StringBuffer();

		for (CharSequence tag : tags) {
			sb.append(tag + " ");
		}

		return sb.toString().trim();
	}
	
	/**
	 * Return md5 code associated with phone.
	 * 
     * @return String md5 code.
	 */
	public String getPhotoMD5() {
		if (mediaList == null || mediaList.isEmpty() ||
				!(mediaList.get(0) instanceof SnapticImage)) {
			return "";
		}
		
		return ((SnapticImage)mediaList.get(0)).md5;
	}

	/**
	 * Copy a given note.
	 * @param SnapticNote note to be copied.
	 */
	public void copy(SnapticNote note) {
		if (note != null) {
			this.id = note.id;
			this.parentId = note.parentId;
			this.owner = note.owner;
			this.ownerId = note.ownerId;
			this.creationTime = note.creationTime;
			this.modificationTime = note.modificationTime;
			this.reminderTime = note.reminderTime;
			this.text = note.text;
			this.summary = note.summary;
			this.depth = note.depth;
			this.children = note.children;
			this.mode = note.mode;
			this.publicUrl = note.publicUrl;
			this.source = note.source;
			this.sourceUrl = note.sourceUrl;

			this.latitude = note.latitude;
			this.longitude = note.longitude;
			this.altitude = note.altitude;
			this.speed = note.speed;
			this.bearing = note.bearing;
			this.accuracyPosition = note.accuracyPosition;
			this.accuracyAltitude = note.accuracyAltitude;

			this.tags = note.tags;
			this.mediaList = note.mediaList;
		}
	}

	/**
	 * Turn note into a string of key : values.
	 * 
	 * @return String of key values in note.
	 */
	@Override
	public String toString() {
		return
			"id:" + id +
			" parent_id:" + parentId +
			" owner:" + owner +
			" ownerId:" + ownerId +
			" creationTime:" + creationTime +
			" modificationTime:" + modificationTime +
			" reminderTime:" + reminderTime +
			" text:" + text +
			" summary:" + summary +
			" depth:" + depth +
			" children:" + children +
			" mode: " + mode +
			" publicUrl: " + publicUrl +
			" source: " + source +
			" sourceUrl: " + sourceUrl +
			" latitude:" + latitude +
			" longitude:" + longitude +
			" altitude:" + altitude +
			" speed:" + speed +
			" bearing:" + bearing +
			" accuracyPosition:" + accuracyPosition +
			" accuracyAltitude:" + accuracyAltitude +
			" tags:" + getTags() +
			" mediaList:" + mediaList;
	}

	public SnapticNote(Builder builder) {
		this.id = builder.id;
		this.parentId = builder.parentId;
		this.owner = builder.owner;
		this.ownerId = builder.ownerId;
		this.creationTime = builder.creationTime;
		this.modificationTime = builder.modificationTime;
		this.reminderTime = builder.reminderTime;
		this.text = builder.text;
		this.summary = builder.summary;
		this.depth = builder.depth;
		this.children = builder.children;
		this.mode = builder.mode;
		this.publicUrl = builder.publicUrl;
		this.source = builder.source;
		this.sourceUrl = builder.sourceUrl;

		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.altitude = builder.altitude;
		this.speed = builder.speed;
		this.bearing = builder.bearing;
		this.accuracyPosition = builder.accuracyPosition;
		this.accuracyAltitude = builder.accuracyAltitude;

		this.tags = builder.tags;
		this.mediaList = builder.mediaList;
	}

	public static class Builder {
		private long id;
		private long parentId;
		private CharSequence source;
		private CharSequence sourceUrl;
		private CharSequence owner;
		private long ownerId;
		private long creationTime;
		private long modificationTime;
		private long reminderTime;
		private CharSequence text;
		private CharSequence summary;
		private long depth;
		private long children;
		private CharSequence mode;
		private CharSequence publicUrl;

		private double latitude;
		private double longitude;
		private double altitude;
		private double speed;
		private double bearing;
		private double accuracyPosition;
		private double accuracyAltitude;

		private List<CharSequence> tags;
		private List<Object> mediaList;

		public Builder id(long id) {
			this.id = id;
			return this;
		}

		public Builder parentId(long parentId) {
			this.parentId = parentId;
			return this;
		}
		
		public Builder source(CharSequence source) {
			this.source = source;
			return this;
		}

		public Builder sourceUrl(CharSequence sourceUrl) {
			this.sourceUrl = sourceUrl;
			return this;
		}
		
		public Builder owner(CharSequence owner) {
			this.owner = owner;
			return this;
		}

		public Builder ownerId(long ownerId) {
			this.ownerId = ownerId;
			return this;
		}

		public Builder creationTime(long creationTime) {
			this.creationTime = creationTime;
			return this;
		}

		public Builder modificationTime(long modificationTime) {
			this.modificationTime = modificationTime;
			return this;
		}

		public Builder reminderTime(long reminderTime) {
			this.reminderTime = reminderTime;
			return this;
		}

		public Builder text(CharSequence text) {
			this.text = text;
			return this;
		}

		public Builder summary(CharSequence summary) {
			this.summary = summary;
			return this;
		}

		public Builder depth(long depth) {
			this.depth = depth;
			return this;
		}

		public Builder children(long children) {
			this.children = children;
			return this;
		}

		public Builder mode(CharSequence mode) {
			this.mode = mode;
			return this;
		}
		
		public Builder publicUrl(CharSequence publicUrl) {
			this.publicUrl = publicUrl;
			return this;
		}
		
		public Builder latitude(double latitude) {
			this.latitude = latitude;
			return this;
		}

		public Builder longitude(double longitude) {
			this.longitude = longitude;
			return this;
		}

		public Builder altitude(double altitude) {
			this.altitude = altitude;
			return this;
		}

		public Builder speed(double speed) {
			this.speed = speed;
			return this;
		}

		public Builder bearing(double bearing) {
			this.bearing = bearing;
			return this;
		}

		public Builder accuracyPosition(double accuracyPosition) {
			this.accuracyPosition = accuracyPosition;
			return this;
		}

		public Builder accuracyAltitude(double accuracyAltitude) {
			this.accuracyAltitude = accuracyAltitude;
			return this;
		}

		public Builder labels(List<CharSequence> tags) {
			this.tags = tags;
			return this;
		}

		public Builder mediaList(List<Object> mediaList) {
			this.mediaList = mediaList;
			return this;
		}

		public SnapticNote build() {
			return new SnapticNote(this);
		}
	}
}
