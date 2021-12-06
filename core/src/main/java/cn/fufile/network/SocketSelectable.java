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
package cn.fufile.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;

public interface SocketSelectable extends Selectable {

    void connect(String channelId, InetSocketAddress address) throws IOException;

    void doPool(long timeout) throws IOException;

    void send(Sender sender) throws IOException;

    int connectedChannelsSize();

    void getSends();

    void registerNewConnections();

    Collection<FufileSocketChannel> getReceive();
}
