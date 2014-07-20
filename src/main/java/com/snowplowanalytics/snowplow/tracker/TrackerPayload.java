/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */


package com.snowplowanalytics.snowplow.tracker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TrackerPayload implements Payload {

    private final ObjectMapper objectMapper = Util.defaultMapper();
    private final Logger logger = LoggerFactory.getLogger(TrackerPayload.class);
    private ObjectNode objectNode;

    public TrackerPayload() {
        objectNode = objectMapper.createObjectNode();
    }

    @Override
    public void add(String key, String value) {
        objectNode.put(key, value);
    }

    @Override
    public void add(String key, Object value) {
        objectNode.putPOJO(key, objectMapper.valueToTree(value));
    }

    @Override
    public void addMap(Map map) {
        // Return if we don't have a map
        if (map == null)
            return;

        Set<String> keys = map.keySet();
        for(String key : keys) {
            objectNode.putPOJO(key, objectMapper.valueToTree(map.get(key)));
        }
    }

    @Override
    public void addMap(Map map, Boolean base64_encoded, String type_encoded, String type_no_encoded) {
        // Return if we don't have a map
        if (map == null)
            return;

        String mapString;
        try {
            mapString = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return; // Return because we can't continue
        }

        if (base64_encoded) { // base64 encoded data
            objectNode.put(type_encoded, Util.base64Encode(mapString));
        } else { // add it as a child node
            add(type_no_encoded, map);
        }
    }

    public void setData(Object data) {
        if (data instanceof ArrayList || data.getClass().isArray()) {
            // If object passed is an array, add ArrayNode
            logger.debug("Object recognized as an array.");
            objectNode.putPOJO(Parameter.DATA, objectMapper.valueToTree(data));

        } else if (data instanceof Map || data instanceof Payload ){
            // Else it's just an ObjectNode with data to put in or a Payload
            logger.debug("Object recognized as a map (or payload).");
            objectNode.putPOJO(Parameter.DATA, objectMapper.valueToTree(data));

        }
    }

    public void setSchema(String schema) {
        // Always sets schema with key "schema"
        objectNode.put(Parameter.SCHEMA, schema);
    }

    @Override
    public JsonNode getNode() {
        return objectNode;
    }

    @Override
    public Map getMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            map = objectMapper.readValue(objectNode.toString(), new TypeReference<HashMap<String,String>>(){});
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String toString() {
        return objectNode.toString();
    }
}
