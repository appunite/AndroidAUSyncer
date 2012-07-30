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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Parser {
	
	private final ParserResult mParserResult;

	public Parser(ParserResult parserResult) {
		this.mParserResult = parserResult;
	}
	
	public void parse(JSONObject object) throws JSONException {
		String type = object.getString("type");
		if (type.equals("FeatureCollection")) {
			parseFeatureCollection(object);
		} else if (type.equals("Feature")) {
			parseFeautre(object);
		}
	}

	private void parseFeautre(JSONObject object) throws JSONException {
		String guid = object.getString("id");
		JSONObject geometry = object.getJSONObject("geometry");
		String geometry_type = geometry.getString("type");
		if (!"Point".equals(geometry_type))
			throw new JSONException("geometry should be type Point");
		JSONArray coordinates = geometry.getJSONArray("coordinates");
		if (coordinates.length() != 2)
			throw new JSONException("wrong length of coordinates");
		double latitude = coordinates.getDouble(0);
		double longitude = coordinates.getDouble(1);
		
		JSONObject properties = object.getJSONObject("properties");
		String description = properties.getString("opis");
		String longDescription = properties.getString("opis_long");
		mParserResult.addFeature(guid, description, longDescription, latitude, longitude);
	}

	private void parseFeatureCollection(JSONObject object) throws JSONException {
		JSONArray features = object.getJSONArray("features");
		for (int i = 0; i < features.length(); i++) {
			JSONObject feature = features.getJSONObject(i);
			parse(feature);
		}
	}
}
