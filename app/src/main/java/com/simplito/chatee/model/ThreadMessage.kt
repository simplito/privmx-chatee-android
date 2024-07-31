//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.cloud).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.simplito.chatee.model

import com.simplito.chatee.Utils.ChateeJson
import com.simplito.chatee.classes.FileUploadMessageJson
import com.simplito.chatee.classes.MessageJson
import com.simplito.chatee.classes.TextMessageJson
import com.simplito.java.privmx_endpoint.model.Message
import com.simplito.java.privmx_endpoint.model.ServerInfo

open class ThreadMessage(
    val privateMeta: ThreadPrivateMeta,
    val data: MessageJson,
    val publicMeta: ByteArray = ByteArray(0),
    val info: ServerInfo? = null,
    val statusCode: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThreadMessage

        if (!publicMeta.contentEquals(other.publicMeta)) return false
        if (privateMeta != other.privateMeta) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicMeta.contentHashCode()
        result = 31 * result + privateMeta.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromCoreMessage(message: Message): ThreadMessage {
            val privateMeta: ThreadPrivateMeta = try {
                ChateeJson.decodeFromString(String(message.privateMeta))
            } catch (e: Exception) {
                throw IllegalArgumentException()
            }
            val data: MessageJson = try {
                ChateeJson.decodeFromString<TextMessageJson>(String(message.data))
            } catch (e: Exception) {
                ChateeJson.decodeFromString<FileUploadMessageJson>(String(message.data))
            }

            return ThreadMessage(
                privateMeta,
                data,
                message.publicMeta,
                message.info,
                message.statusCode,
            )
        }
    }
}