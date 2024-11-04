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


package com.simplito.chatee.server

import android.util.Log
import com.simplito.chatee.Utils.ChateeJson
import com.simplito.chatee.server.base.AccessToken
import com.simplito.chatee.server.base.ApiError
import com.simplito.chatee.server.base.Response
import com.simplito.chatee.server.model.CloudData
import com.simplito.chatee.server.model.InviteToken
import com.simplito.chatee.server.model.request.InviteRequestData
import com.simplito.chatee.server.model.request.SignInRequestData
import com.simplito.chatee.server.model.request.SignUpRequestData
import com.simplito.chatee.server.model.response.ContactsResponseData
import com.simplito.chatee.server.model.response.DefaultResponseData
import com.simplito.chatee.server.model.response.SignInResponseData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@OptIn(ExperimentalSerializationApi::class)
class ServerApi(
    val domain: String
) {

    private var token: AccessToken? = null
    private val url = "https://$domain"
    var cloudData: CloudData? = null
        private set

    @Throws(Exception::class)
    fun signIn(signInRequestData: SignInRequestData): Response<Boolean> {
        return call<SignInRequestData, SignInResponseData>(
            "POST",
            "api/sign-in",
            signInRequestData
        ).also {
            if (it.error == null && it.result != null) {
                token = AccessToken("Bearer", it.result.token)
                cloudData = it.result.cloudData
            }
        }.resultMap { it?.isStaff }
    }

    @kotlin.jvm.Throws(Exception::class)
    fun signUp(signUpRequestData: SignUpRequestData): Response<DefaultResponseData> {
        return call("POST", "api/sign-up", signUpRequestData)
    }

    @kotlin.jvm.Throws(Exception::class)
    fun contacts(): Response<ContactsResponseData> {
        return call<Unit, ContactsResponseData>("GET", "api/contacts", authToken = token)
    }

    @Throws(Exception::class)
    fun invite(inviteData: InviteRequestData): Response<InviteToken> {
        return call("POST", "api/invite-token", inviteData, token)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(Exception::class)
    private inline fun <reified T, reified U> call(
        operation: String = "GET",
        method: String,
        inputData: T? = null,
        authToken: AccessToken? = null
    ): Response<U> {
        with(URL("${url}/$method").openConnection() as HttpsURLConnection) {
            try {
                // Uncomment lines below to send requests to host with not verified certificates
                /*
                sslSocketFactory = SSLCertificateSocketFactory.getInsecure(0, null)
                setHostnameVerifier { s, sslSession -> true }
                 */
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = operation
                doInput = true
                setRequestProperty("Content-Type", "application/json")
                authToken?.let {
                    setRequestProperty("Authorization", "${authToken.type} ${authToken.token}")
                }
                inputData?.let {
                    ChateeJson.encodeToStream(inputData, outputStream)
                }

                println(responseCode)
                return if (responseCode in (400..500))
                    Response(
                        null,
                        ChateeJson.decodeFromStream<ApiError>(errorStream),
                        responseCode
                    )
                else if (responseCode in (200..<300)) {
                    Response(
                        ChateeJson.decodeFromStream<U>(inputStream),
                        null,
                        responseCode
                    )
                } else if (responseCode < 0) {
                    Response(
                        null,
                        ApiError("Incorrect status code"),
                        responseCode
                    )
                } else Response(null, ApiError("Unknown Error"), responseCode)
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("[ServerApi]", "timeout")
                return Response(null, ApiError("Timeout"), responseCode)
            }
        }
    }
}