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


package com.simplito.chatee.server.model.request

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequestData(
    val domainName: String,
    val username: String,
    val sign: String
)
