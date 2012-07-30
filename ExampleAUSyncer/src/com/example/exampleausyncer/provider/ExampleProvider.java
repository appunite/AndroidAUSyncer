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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ExampleProvider extends ContentProvider {

	private static final Map<String, String> sExampleProjectionMap;

	private static final int EXAMPLE = 0;

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;
    private static final long SLEEP_AFTER_YIELD_DELAY = 4000;

	static {
		sURIMatcher.addURI(ExampleContract.AUTHORITY,
				ExampleContract.Example.CONTENT_PATH, EXAMPLE);

		sExampleProjectionMap = new HashMap<String, String>();
		sExampleProjectionMap.put(ExampleContract.Example._ID,
				ExampleContract.Example.EXAMPLE_ID + " AS "
						+ ExampleContract.Example._ID);
		sExampleProjectionMap.put(ExampleContract.Example.TITLE,
				ExampleContract.Example.TITLE);
		sExampleProjectionMap.put(ExampleContract.Example.TIME_STAMP,
				ExampleContract.Example.TIME_STAMP);
		sExampleProjectionMap.put(ExampleContract.Example.GUID,
				ExampleContract.Example.GUID);
		sExampleProjectionMap.put(ExampleContract.Example.EXAMPLE_ID,
				ExampleContract.Example.EXAMPLE_ID);
	}

	private DBHelper mDatabase;

	private SQLiteDatabase db;

	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		int ypCount = 0;
		int opCount = 0;
		db = mDatabase.getWritableDatabase();
		db.beginTransaction();
		try {
			final int numOperations = operations.size();
			final ContentProviderResult[] results = new ContentProviderResult[numOperations];
			for (int i = 0; i < numOperations; i++) {
				if (++opCount >= MAX_OPERATIONS_PER_YIELD_POINT) {
					throw new OperationApplicationException(
							"Too many content provider operations between yield points. "
									+ "The maximum number of operations per yield point is "
									+ MAX_OPERATIONS_PER_YIELD_POINT, ypCount);
				}
				final ContentProviderOperation operation = operations.get(i);
				if (i > 0 && operation.isYieldAllowed()) {
					opCount = 0;
					if (db.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY)) {
						db = mDatabase.getWritableDatabase();
						ypCount++;
					}
				}

				results[i] = operation.apply(this, results, i);
			}
			db.setTransactionSuccessful();
			return results;
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public boolean onCreate() {
		mDatabase = new DBHelper(getContext());
		db = mDatabase.getWritableDatabase();
		return true;
	}

	@Override
	public String getType(Uri uri) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case EXAMPLE:
			return ExampleContract.Example.CONTENT_TYPE;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		int result;
		switch (uriType) {
		case EXAMPLE:
			result = db.delete(ExampleContract.Example.DB_TABLE, selection,
					selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return result;
		default:
			throw new IllegalArgumentException();
		}
	}

	private Uri updateThis(SQLiteDatabase db, Uri uri, String table,
			String idFieldName, String guidFieldName, ContentValues values) {

		String whereClause = String.format("$s = ?", guidFieldName);
		String id = null;
		String guid = values.getAsString(guidFieldName);

		boolean inTransaction = db.inTransaction();
		if (!inTransaction) {
			db.beginTransaction();
		}
		try {
			if (guid != null) {
				Cursor query = db.query(false, table,
						new String[] { idFieldName }, whereClause,
						new String[] { guid }, null, null, null, "1");
				if (query.moveToFirst()) {
					id = query.getString(0);
				}
			}

			if (id == null) {
				id = String.valueOf(db.insert(table, null, values));
			} else {
				db.update(table, values, whereClause, new String[] { guid });
			}
		} finally {
			if (!inTransaction)
				db.endTransaction();
		}
		return Uri.withAppendedPath(ExampleContract.Example.CONTENT_URI, id);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case EXAMPLE:
			Uri itemUid = updateThis(db, uri, ExampleContract.Example.DB_TABLE,
					ExampleContract.Example.EXAMPLE_ID,
					ExampleContract.Example.GUID, values);
			getContext().getContentResolver().notifyChange(itemUid, null);
			return itemUid;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		StringBuilder tables = new StringBuilder();
		Cursor cursor = null;

		int uriType = sURIMatcher.match(uri);

		switch (uriType) {
		case EXAMPLE:
			tables.append(ExampleContract.Example.DB_TABLE);
			queryBuilder.setTables(tables.toString());

			queryBuilder.setProjectionMap(sExampleProjectionMap);

			cursor = queryBuilder.query(db, projection, selection,
					selectionArgs, null, null, sortOrder);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			return cursor;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		int result;
		switch (uriType) {
		case EXAMPLE:
			result = db.update(ExampleContract.Example.DB_TABLE, values,
					selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return result;
		default:
			throw new IllegalArgumentException();
		}
	}

}
