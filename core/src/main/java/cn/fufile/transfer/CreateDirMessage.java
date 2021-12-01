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

package cn.fufile.transfer;

/**
 * 创建目录的一些字段
 */
public class CreateDirMessage extends FufileMessage {

    public static RequestMessage createRequestMessage() {
        return new CreateDirRequestMessage();
    }

    public static ResponseMessage createResponseMessage() {
        return new CreateDirResponseMessage();
    }

    public static class CreateDirRequestMessage extends RequestMessage {


    }

    public static class CreateDirResponseMessage extends ResponseMessage {

    }
}
