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


package com.simplito.chatee.classes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MessageDataSerializer::class)
sealed class MessageJson

@Serializable
data class TextMessageJson(
    val text: String,
) : MessageJson()

@Serializable
@SerialName("file")
data class FileUploadMessageJson(
    val fileId: String,
    val fileName: String,
) : MessageJson()

object MessageDataSerializer : KSerializer<MessageJson> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor(
            "com.simplito.chatee.classes.MessageDataSerializer",
            MessageJson.serializer().descriptor
        )

    override fun deserialize(decoder: Decoder): MessageJson {
        return try {
            TextMessageJson.serializer().deserialize(decoder)
        } catch (e: Exception) {
            FileUploadMessageJson.serializer().deserialize(decoder)
        }
    }

    override fun serialize(encoder: Encoder, value: MessageJson) {
        when (value) {
            is TextMessageJson -> TextMessageJson.serializer().serialize(encoder, value)
            is FileUploadMessageJson -> FileUploadMessageJson.serializer().serialize(encoder, value)
        }
    }
}