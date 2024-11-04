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

sealed class ServerException(
    message: String,
    val statusCode: Int?
) : RuntimeException(message) {

    companion object {
        fun parseException(message: String, statusCode: Int?): ServerException {
            if (statusCode == InvalidCredentials.statusCode && message == InvalidCredentials.message) {
                return InvalidCredentials
            }
            if (statusCode == BadRequest.statusCode && message == BadRequest.message) {
                return BadRequest
            }
            if (statusCode == UsernameOrPublicKeyInUse.statusCode && message == UsernameOrPublicKeyInUse.message) {
                return UsernameOrPublicKeyInUse
            }
            if (statusCode == InvalidToken.statusCode && message == InvalidToken.message) {
                return InvalidToken
            }
            return UnknownException(message, statusCode)
        }
    }

    data object InvalidCredentials : ServerException("Invalid credentials", 400)
    data object InvalidToken : ServerException("Invalid token", 400)
    data object UsernameOrPublicKeyInUse : ServerException("Username or public key in use", 409)
    data object BadRequest : ServerException("Bad request", 400)
    class UnknownException(message: String, statusCode: Int?) :
        ServerException("Unknown exception: $message", statusCode)
}