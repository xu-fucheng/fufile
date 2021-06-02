/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.fufile.example;

import cn.fufile.network.FilepiggerSelector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;

public class UploadFile {

    public static void main(String[] args) throws IOException {
        FilepiggerSelector selector = new FilepiggerSelector(Selector.open());
        selector.connect(new InetSocketAddress("localhost", 9000));
        while (true){
            selector.pool();
        }

//        Properties properties = new Properties();
//        properties.setProperty("server", "localhost:9000");
//        FileInputStream fileInputStream = new FileInputStream(new File("config/server.properties"));
//
//        FilepiggerClient filepiggerClient = new FilepiggerClient(properties);
//
//        filepiggerClient.upload(fileInputStream);


    }
}
