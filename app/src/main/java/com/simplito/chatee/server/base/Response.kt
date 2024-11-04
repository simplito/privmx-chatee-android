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


package com.simplito.chatee.server.base

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(val result: T?, val error: ApiError?, val responseCode: Int) {
    fun <U> resultMap(map: (v: T?) -> U?): Response<U> = Response(map(result), error, responseCode)
}