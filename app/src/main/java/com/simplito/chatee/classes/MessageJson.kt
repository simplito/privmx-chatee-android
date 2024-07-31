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


package com.simplito.chatee.classes

import com.simplito.chatee.Utils.ChateeJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class MessageJson(
    @Transient val type: String = ""
)

@Serializable
@SerialName(TextMessageJson.DEFAULT_TYPE)
data class TextMessageJson(
    val content: String,
): MessageJson(DEFAULT_TYPE){
    companion object {
        const val DEFAULT_TYPE: String = "text"
        fun toTextMessageOrNull(json: String): TextMessageJson? = try {
            ChateeJson.decodeFromString<TextMessageJson>(json)
        } catch (e: Exception){
            null
        }
    }
}

@Serializable
@SerialName(FileUploadMessageJson.DEFAULT_TYPE)
data class FileUploadMessageJson(
    val storeId: String,
    val fileId: String,
    val fileName: String,
    val fileMimeType: String,
): MessageJson(DEFAULT_TYPE){
    companion object {
        const val DEFAULT_TYPE: String = "fileupload"
        fun toFileUploadMessageOrNull(json: String): FileUploadMessageJson? = try {
            ChateeJson.decodeFromString<FileUploadMessageJson>(json)
        } catch (e: Exception){
            null
        }
    }
}