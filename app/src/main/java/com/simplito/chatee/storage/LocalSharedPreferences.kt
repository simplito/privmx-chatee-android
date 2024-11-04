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


package com.simplito.chatee.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object LocalSharedPreferences {
    private const val TAG = "[SecureSharedPreferences]"
    private const val SP_LOCAL_NAME = "Chatee-localPreferences"
    private var localStorageInstance: SharedPreferences? = null
    fun getInstance(context: Context): SharedPreferences =
        localStorageInstance ?: MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            .let {
                try {
                    EncryptedSharedPreferences.create(
                        context,
                        SP_LOCAL_NAME,
                        it,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                } catch (e: Exception) {
                    context.deleteSharedPreferences(SP_LOCAL_NAME)
                    EncryptedSharedPreferences.create(
                        context,
                        SP_LOCAL_NAME,
                        it,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                }

            }.also {
                localStorageInstance = it
            }

}
