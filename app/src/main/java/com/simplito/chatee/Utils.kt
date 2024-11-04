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

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.serialization.json.Json

object Utils {

    val ChateeJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun getMimeType(contentResolver: ContentResolver, uri: Uri): String? =
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            contentResolver.getType(uri)
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(
                    uri.toString().split(".").lastOrNull()
                )
            )
        }

    fun calculateFormattedFileSize(fileSize: Long): String {
        var countOfSizes = 0
        var dividedSize = fileSize
        while (dividedSize > 1024 && countOfSizes <= 3) {
            dividedSize /= 1024
            countOfSizes++
        }

        val sizeType =
            when (countOfSizes) {
                1 -> "KB"
                2 -> "MB"
                3 -> "GB"
                4 -> "TB"
                else -> "B"
            }
        return "$dividedSize $sizeType"
    }

}