/*
 * Copyright 2021 The Fufile Project
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
package cn.fufile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Filepigger {

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(args[0]));
        System.out.println(properties.get("data.dir"));



//        FileTree dirTree = new FileTree();
//
//        FilepiggerSelector selector = new FilepiggerSelector(Selector.open());
//        selector.bind(new InetSocketAddress(9000));
//        while (true) {
//            selector.pool();
//        }
    }
}
