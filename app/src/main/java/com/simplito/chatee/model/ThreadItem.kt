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

package com.simplito.chatee.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.simplito.chatee.Utils.ChateeJson
import com.simplito.java.privmx_endpoint.model.Thread

data class ThreadItem(
    val thread: Thread,
) {
    val decodedPrivateMeta: ThreadPrivateMeta =
        ChateeJson.decodeFromString(thread.privateMeta.decodeToString())


    @Composable
    fun rememberTitle(index: Int) = remember(
        index,
        thread.threadId
    ) {
        decodedPrivateMeta.name
    }
}