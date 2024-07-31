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

import com.simplito.chatee.classes.MessageJson
import kotlinx.serialization.Serializable

@Serializable
data class ThreadPrivateMeta(
    val msgId: String,
    val type: String,
    val author: Author,
    val createDate: Long,
    val text: MessageJson
)