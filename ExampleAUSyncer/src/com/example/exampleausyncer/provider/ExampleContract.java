/*
 * Copyright (C) 2012 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.example.exampleausyncer.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class ExampleContract {
	public static final String AUTHORITY = "com.example.exampleausyncer";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	public static class Example implements BaseColumns {
		static final String DB_TABLE = "example";

		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.example.exampleausyncer.example";
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.example.exampleausyncer.example";

		public static final String CONTENT_PATH = "example";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, CONTENT_PATH);

		public static final String TITLE = "example_title";
		public static final String TIME_STAMP = "example_time_stamp";
		public static final String GUID = "example_guid";
		public static final String EXAMPLE_ID = "example__id";

	}
}
