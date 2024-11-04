//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.simplito.chatee.model;

public class StoreFileData {
    public String mimetype;
    public String name;
    public byte[] data;
    public Long size;

    public StoreFileData(
            String mimetype,
            String name,
            byte[] data,
            Long size
    ) {
        this.mimetype = mimetype;
        this.name = name;
        this.data = data;
        this.size = size;
    }
}
