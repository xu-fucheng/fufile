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

package org.fufile.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileHeader implements Serialize {

    private int magic;

    private int version;

    public FileHeader() {
    }

    public FileHeader(int magic, int version) {
        this.magic = magic;
        this.version = version;
    }

    @Override
    public void serialize(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(magic);
        dataOutputStream.writeInt(version);
    }

    @Override
    public void deserialize(DataInputStream dataInputStream) throws IOException {
        magic = dataInputStream.readInt();
        version = dataInputStream.readInt();
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
