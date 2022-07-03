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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FufileConfig {
    private final Properties properties;
    private final  Map<String, Object> configs = new HashMap<>();

    public FufileConfig(Properties properties) {
        this.properties = properties;
    }

    public FufileConfig config(ConfigDefinition configDefinition) {
        String value = properties.getProperty(configDefinition.name);


        if (value != null) {
            configs.put(configDefinition.name, value);
        }

        return this;
    }

    public Integer getInt(String key) {
        return (Integer) configs.get(key);
    }

    public String getString(String key) {
        return (String) configs.get(key);
    }

    public Long getLong(String key) {
        return (Long) configs.get(key);
    }
}
