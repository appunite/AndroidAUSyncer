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

package com.example.exampleausyncer.adapter;

import com.example.exampleausyncer.provider.ExampleContract;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class MainAdapter extends CursorAdapter {
	
	public static final String[] projection = new String[] {ExampleContract.Example._ID, ExampleContract.Example.TITLE};
//	private static final int ID = 0;
	private static final int TITLE = 1;
	
	private static class ViewHolder {
		TextView textView;
	}
	
	private LayoutInflater mLayoutInflater;

	public MainAdapter(Context context, Cursor c) {
		super(context, c, false);
		
		mLayoutInflater = LayoutInflater.from(context);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		
		String title = cursor.getString(TITLE);
		viewHolder.textView.setText(title);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        assert view != null;
		
		ViewHolder viewHolder = new ViewHolder();
		view.setTag(viewHolder);
		
		viewHolder.textView = (TextView) view;
		
		return view;
	}

}
