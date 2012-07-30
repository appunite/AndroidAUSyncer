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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

public class Downloader {
	
	private Parser mParser;
	private ProviderParserResult mParserResult;

	public Downloader(Context context) {
		mParserResult = new ProviderParserResult(context);
		mParser = new Parser(mParserResult);
	}

	public boolean download(String url) {
		try {
			JSONObject data = downloadWithException(url);
			mParser.parse(data);
			mParserResult.apply();
			return true;
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (URISyntaxException e) {
			return false;
		} catch (JSONException e) {
			return false;
		} catch (RemoteException e) {
			return false;
		} catch (OperationApplicationException e) {
			return false;
		}
	}

	private JSONObject downloadWithException(String url) throws ClientProtocolException,
			IOException, URISyntaxException, JSONException {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		// adding params
		List<NameValuePair> params = new LinkedList<NameValuePair>();
//		params.add(new BasicNameValuePair(LAST_SYNC, userPreferences
//				.getLastSync()));
//		params.add(new BasicNameValuePair(JSON_AUTH_TOKEN, userPreferences
//				.getUserAuthToken()));
		url += "?" + URLEncodedUtils.format(params, "utf-8");
		request.setURI(new URI(url));

		HttpResponse response = client.execute(request);
		HttpEntity httpEntity = response.getEntity();
		InputStream is = httpEntity.getContent();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
		}

		return new JSONObject(stringBuilder.toString());
	}

}
