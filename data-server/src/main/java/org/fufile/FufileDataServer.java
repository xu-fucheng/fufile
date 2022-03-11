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

package org.fufile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class FufileDataServer {

    private final static Logger logger = LoggerFactory.getLogger(FufileDataServer.class);

    public static void main(String[] args) throws IOException {

        logger.info(logger.getName());

//        FileTree dirTree = new FileTree();
//
//        FufileSelector selector = new FufileSelector(Selector.open());
//        selector.bind(new InetSocketAddress(9000));
//        while (true) {
//            selector.pool();
//        }
    }
}
