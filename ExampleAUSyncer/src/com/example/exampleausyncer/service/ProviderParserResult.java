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

package com.example.exampleausyncer.service;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.example.exampleausyncer.provider.ExampleContract;

public class ProviderParserResult implements ParserResult {

	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
	private String mTimeStamp;
	private ContentResolver mContentResolver;

	public ProviderParserResult(Context context) {
		mContentResolver = context.getContentResolver();
		mTimeStamp = String.valueOf(System.currentTimeMillis());
	}

	public void addFeature(String guid, String description,
			String longDescription, double latitude, double longitude) {
		ContentProviderOperation operation = ContentProviderOperation
				.newInsert(ExampleContract.Example.CONTENT_URI)
				.withValue(ExampleContract.Example.TITLE, description)
				.withValue(ExampleContract.Example.GUID, guid)
				.withValue(ExampleContract.Example.TIME_STAMP, mTimeStamp)
				.build();
		ops.add(operation);
	}

	public void apply() throws RemoteException, OperationApplicationException {
		String selection = String.format("%s != ?",
				ExampleContract.Example.TIME_STAMP);
		ContentProviderOperation deleteOldOperation = ContentProviderOperation
				.newDelete(ExampleContract.Example.CONTENT_URI)
				.withSelection(selection, new String[] { mTimeStamp }).build();
		ops.add(deleteOldOperation);
		mContentResolver.applyBatch(ExampleContract.AUTHORITY, ops);
	}

}
