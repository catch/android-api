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

import java.io.File;

/**
 * Represents a media item.
 */
public class CatchMedia {
	public String id;
	public String note_id;
	public long created_at;
	public String content_type;
	public String src;
	public long size;
	public String filename;
	public boolean voice_hint;
	public File data;
	
	public CatchMedia() {
		id = "";
		note_id = "";
		created_at = 0;
		content_type = "";
		src = "";
		size = 0;
		data = null;
		filename = "";
		voice_hint = false;
	}
	
	public String toString() {
		return "apiId=" + id + " nodeId=" + note_id + " created_at=" + created_at +
			   " type=" + content_type + " src=" + src + " size=" + size +
			   " filename=" + filename + " voice_hint=" + voice_hint + " data=" +
			   ((data == null) ? "null" : data.getPath());
	}
}
