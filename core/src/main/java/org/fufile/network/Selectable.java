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

package org.fufile.network;

import java.io.IOException;

public interface Selectable extends AutoCloseable {

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O operations,
     * then handle these events.
     * @param timeout If positive, block for up to <tt>timeout</tt> milliseconds,
     *                If it's less than or equal to 0, block indefinitely.
     */
    void doPool(long timeout) throws IOException;
}
