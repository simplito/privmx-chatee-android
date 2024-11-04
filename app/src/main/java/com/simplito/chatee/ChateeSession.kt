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


package com.simplito.chatee

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.simplito.chatee.server.ServerApi
import com.simplito.chatee.server.model.User
import com.simplito.chatee.storage.LocalSharedPreferences

object ChateeSession {
    private enum class KEYS(val path: String) {
        DOMAIN_NAME("ChateeSession::domainName")
    }

    var domainNameFull: String? = null
        set(value) {
            field = value
            sharedPreferences?.edit {
                putString(KEYS.DOMAIN_NAME.path, value)
            }
        }

    val domainNameShort get() = domainNameFull?.split(".")?.firstOrNull()


    var serverConnection: ServerApi? = null
        set(value) {
            if (value == null) field = null
            if (field == null) field = value
        }
    var currentUser: User? = null
        set(value) {
            if (value == null) field = null
            if (field == null) field = value
        }
    private var sharedPreferences: SharedPreferences? = null

    fun loadFromCache(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = LocalSharedPreferences.getInstance(context)
        domainNameFull = sharedPreferences?.getString(KEYS.DOMAIN_NAME.path, null)
    }

    fun logout() {
        serverConnection = null
        currentUser = null
    }
}