/*
 * Copyright 2022 The Fufile Project
 *
 * The Fufile Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.fufile.config;

public enum ConfigDefinition {
    HEARTBEAT_INTERVAL(ConfigKeys.HEARTBEAT_INTERVAL, Type.LONG, 2000, ""),
    HEARTBEAT_TIMEOUT(ConfigKeys.HEARTBEAT_TIMEOUT, Type.LONG, 10000, "");

    public final String name;
    public final Type type;
    public final Object defaultValue;
    public final String doc;

    ConfigDefinition(String name, Type type, Object defaultValue, String doc) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.doc = doc;
    }

    enum Type {
        INT,
        LONG,
        STRING,
        LIST;

    }
}
