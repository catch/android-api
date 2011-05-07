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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a note.
 */
public class CatchNote {
	public static final String NOT_SET = "NOT_SET";
	public static final int NOT_SET_INT = -1;
	public static final String NODE_ID_NEVER_SYNCED = "-1";
	public static final String MODE_PRIVATE = "";
	public static final String MODE_SET_PRIVATE = "private";
	public static final String MODE_SHARED = "shared";
	public static final String MODE_PUBLIC = "public";
	public static final String DEFAULT_OWNER = "Me";
	public static final	int PENDING_OPERATION_NONE = 0;
	public static final	int PENDING_OPERATION_SYNC = 1;
	public static final	int PENDING_OPERATION_DELETE = 2;
	private static final String STARRED_KEY = "starred";

	public String id;
	public long parentId;
    public String parentNodeId;
	public String source;
	public String sourceUrl;
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
	public CharSequence browserUrl;

	public double latitude;
	public double longitude;
	public double altitude;
	public double speed;
	public double bearing;
	public double accuracyPosition;
	public double accuracyAltitude;
    public long serverModifiedAt;

	public List<CharSequence> tags;
	public List<CatchMedia> mediaList;
	public Map<String, String> annotations;
    public int apiPendingOp;

    public CatchNote() {
		id = NODE_ID_NEVER_SYNCED;
		parentId = NOT_SET_INT;
        parentNodeId = NODE_ID_NEVER_SYNCED;
		source = NOT_SET;
		sourceUrl = NOT_SET;
		owner = NOT_SET;
		ownerId = NOT_SET_INT;
		creationTime = NOT_SET_INT;
		modificationTime = NOT_SET_INT;
		reminderTime = NOT_SET_INT;
		text = NOT_SET;
		summary = NOT_SET;
		depth = NOT_SET_INT;
		children = NOT_SET_INT;
		mode = MODE_PRIVATE;
		browserUrl = NOT_SET;

		latitude = 0;
		longitude = 0;
		altitude = 0;
		speed = 0;
		bearing = 0;
		accuracyPosition = 0;
		accuracyAltitude = 0;
        serverModifiedAt = 0;
        apiPendingOp = PENDING_OPERATION_NONE;

		tags = null;
		mediaList = null;
		annotations = null;
	}

    /**
     * Return tags contained within note.
     * 
     * @return Tags in note separated by spaces.
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
	 * Copy a given note.
	 * 
	 * @param note The note to copy.
	 */
	public void copy(CatchNote note) {
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
			this.browserUrl = note.browserUrl;
			this.source = note.source;
			this.sourceUrl = note.sourceUrl;

			this.latitude = note.latitude;
			this.longitude = note.longitude;
			this.altitude = note.altitude;
			this.speed = note.speed;
			this.bearing = note.bearing;
			this.accuracyPosition = note.accuracyPosition;
			this.accuracyAltitude = note.accuracyAltitude;
            this.serverModifiedAt = note.serverModifiedAt;
            this.apiPendingOp = note.apiPendingOp;

			this.tags = note.tags;
			this.mediaList = note.mediaList;
			this.annotations = note.annotations;
		}
	}

	/**
	 * Turn note into a string of key : values.
	 */
	@Override
	public String toString() {
		return
			"id:" + id +
			" parent_id:" + parentId +
            " parentNodeId:" + parentNodeId +
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
			" browserUrl: " + browserUrl +
			" source: " + source +
			" sourceUrl: " + sourceUrl +
			" latitude:" + latitude +
			" longitude:" + longitude +
			" altitude:" + altitude +
			" speed:" + speed +
			" bearing:" + bearing +
			" accuracyPosition:" + accuracyPosition +
			" accuracyAltitude:" + accuracyAltitude +
            " serverModifiedAt:" + serverModifiedAt +
			" tags:" + getTags() +
			" mediaList:" + mediaList +
			" annotations: " + annotations;
	}

	public CatchNote(Builder builder) {
		this.id = builder.id;
		this.parentId = builder.parentId;
        this.parentNodeId = builder.parentNodeId;
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
		this.browserUrl = builder.publicUrl;
		this.source = builder.source;
		this.sourceUrl = builder.sourceUrl;

		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.altitude = builder.altitude;
		this.speed = builder.speed;
		this.bearing = builder.bearing;
		this.accuracyPosition = builder.accuracyPosition;
		this.accuracyAltitude = builder.accuracyAltitude;
        this.serverModifiedAt = builder.serverModifiedAt;
        this.apiPendingOp = builder.apiPendingOp;

		this.tags = builder.tags;
		this.mediaList = builder.mediaList;
		this.annotations = builder.annotations;
	}

	public static class Builder {
		private String id;
		private long parentId;
        private String parentNodeId;
		private String source;
		private String sourceUrl;
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
        private long serverModifiedAt;

		private List<CharSequence> tags;
		private List<CatchMedia> mediaList;
        private int apiPendingOp;
		private Map<String,String> annotations;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder parentId(long parentId) {
			this.parentId = parentId;
			return this;
		}

        public Builder parentNodeId(String parentNodeId) {
            this.parentNodeId = parentNodeId;
            return this;
        }

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder sourceUrl(String sourceUrl) {
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

        public Builder serverModifiedAt(long serverModifiedAt) {
            this.serverModifiedAt = serverModifiedAt;
            return this;
        }

		public Builder labels(List<CharSequence> tags) {
			this.tags = tags;
			return this;
		}

		public Builder mediaList(List<CatchMedia> mediaList) {
			this.mediaList = mediaList;
			return this;
		}

        public Builder apiPendingOp(int apiPendingOp) {
            this.apiPendingOp = apiPendingOp;
            return this;
        }

		public Builder annotations(Map<String, String> annotations) {
			this.annotations = annotations;
			return this;
		}

		public Builder annotationStarred(int annotationStarred) {
			if (annotationStarred == NOT_SET_INT) {
				return this;
			}
			if (this.annotations == null) {
				annotations = new HashMap<String, String>();
				annotations.put(STARRED_KEY, Boolean.toString(annotationStarred == 1));
			}
			return this;
		}

		public CatchNote build() {
			return new CatchNote(this);
		}
	}
}
