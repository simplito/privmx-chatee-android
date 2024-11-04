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


package com.simplito.chatee.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import com.simplito.chatee.extension.mExecutor
import com.simplito.chatee.server.base.ServerException
import com.simplito.chatee.server.model.User
import com.simplito.chatee.server.model.request.SignInRequestData
import com.simplito.chatee.server.model.request.SignUpRequestData
import com.simplito.chatee.server.model.response.DefaultResponseData
import com.simplito.chatee.ui.component.basic.Button
import com.simplito.chatee.ui.component.basic.TextField
import com.simplito.chatee.ui.theme.LightGrey
import com.simplito.java.privmx_endpoint_extra.model.Modules

class LoginActivity : BasicActivity() {
    companion object {
        const val TAG = "[LoginActivity]"

        enum class ViewState {
            DOMAIN_INPUT, LOGIN, REGISTER
        }
    }

    private val currentViewState: MutableState<ViewState> = mutableStateOf(ViewState.DOMAIN_INPUT)
    private var currentDomainName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDomainName = ChateeSession.domainNameFull
        if (currentDomainName != null) {
            currentViewState.value = ViewState.LOGIN
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentViewState.value == ViewState.REGISTER) {
                    currentViewState.value = ViewState.LOGIN
                }
            }
        })

    }

    @Composable
    override fun Content() {
        val pageScrollState = rememberScrollState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp)
                .contentPadding()
                .verticalScroll(pageScrollState)
                .height(IntrinsicSize.Max)
        ) {
            Image(
                ImageBitmap.imageResource(id = R.drawable.chatee_icon),
                contentDescription = "Chatee_icon",
                Modifier.clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(25.dp))
            when (currentViewState.value) {
                ViewState.DOMAIN_INPUT -> {
                    DomainInputView {
                        currentDomainName = it
                        currentViewState.value = ViewState.LOGIN
                    }
                }

                ViewState.LOGIN -> {
                    LoginView(
                        currentDomainName = ChateeSession.domainNameFull ?: currentDomainName,
                        onLogin = { username, password ->
                            logIn(username, password)
                        },
                        onChangeDomain = {
                            ChateeSession.domainNameFull = null
                            currentViewState.value = ViewState.DOMAIN_INPUT
                        },
                        onCreateNewAccount = {
                            currentViewState.value = ViewState.REGISTER
                        }
                    )
                }

                ViewState.REGISTER -> {
                    RegisterView { invitateToken, username, password ->
                        register(
                            invitateToken,
                            username,
                            password
                        )?.then(mExecutor) {
                            currentViewState.value = ViewState.LOGIN
                        }?.fail(mExecutor) {
                            when (it) {
                                is ServerException.InvalidToken -> {
                                    showError(getString(R.string.login_activity_register_snackbar_error_invalid_invite_token))
                                }

                                else -> {
                                    showError(
                                        getString(R.string.login_activity_register_snackbar_error_cannot_create_account)
                                    )
                                }
                            }
                            Log.e(TAG, "Cannot create account [REASON]: ${it.message}")
                        }
                    }
                }
            }

        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun logIn(username: String, password: String): Promise<*>? {
        if (ChateeSession.currentUser != null) return null
        initDomainServer()
        return currentDomainName?.let {
            privmxEndpointContainer?.cryptoApi?.let { cryptoApi ->
                AsyncCall {
                    val privKey = cryptoApi.derivePrivateKey(
                        username,
                        password
                    )
                    val sign = cryptoApi.signData(username.toByteArray(), privKey)
                    val response = ChateeSession.domainNameShort?.let { domain ->
                        ChateeSession
                            .serverConnection
                            ?.signIn(
                                SignInRequestData(
                                    domain,
                                    username,
                                    sign.toHexString(HexFormat.Default)
                                )
                            )
                    }

                    response?.error?.let {
                        throw ServerException.parseException(
                            it.message ?: "",
                            response.responseCode
                        )
                    }
                    logInPlatform(privKey)
                    return@AsyncCall Pair(
                        cryptoApi.derivePublicKey(privKey),
                        response?.result ?: false
                    )
                }.then(mExecutor) {
                    Log.d(TAG, "Success log in")
                    ChateeSession.currentUser = User(username, it.first, it.second)
                    val mainActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(mainActivityIntent)
                }.fail(mExecutor) {
                    when (it) {
                        is ServerException.InvalidCredentials -> {
                            showError(getString(R.string.login_activity_login_snackbar_error_invalid_credentials))
                        }

                        else -> {
                            showError(getString(R.string.login_activity_login_snackbar_error_cannot_log_in))
                        }
                    }
                    Log.e(TAG, "Cannot sign in ${it.javaClass} ${it.message}")
                    it.printStackTrace()
                } as Promise<*>
            }
        }
    }

    private fun logInPlatform(privKey: String) {
        val cloudData = ChateeSession.serverConnection?.cloudData
        cloudData?.let {
            privmxEndpointContainer?.connect(
                setOf(
                    Modules.THREAD,
                    Modules.STORE
                ),
                privKey,
                cloudData.solutionId,
                cloudData.platformUrl
            )
        }

    }


    private fun register(
        inviteToken: String,
        username: String,
        password: String
    ): Promise<DefaultResponseData?>? {
        return privmxEndpointContainer?.cryptoApi?.let { cryptoApi ->
            initDomainServer()
            val privateKey = cryptoApi.derivePrivateKey(
                username,
                password
            )
            val publicKey = cryptoApi.derivePublicKey(privateKey)
            AsyncCall {
                val response = ChateeSession
                    .serverConnection
                    ?.signUp(
                        SignUpRequestData(
                            inviteToken,
                            username,
                            publicKey
                        )
                    )
                if (response == null) throw Exception("Domain Server is not initialized")
                response.error?.let {
                    throw ServerException.parseException(it.message ?: "", response.responseCode)
                }
                response.result
            }
        }
    }

    private fun initDomainServer() {
        currentDomainName?.let {
            ChateeSession.domainNameFull = currentDomainName
            initServer(it)
        }
    }

    @Preview(
        showBackground = true,
        showSystemUi = true,
    )
    @Composable
    fun ContentPreview() {
        Content()
    }
}

@Composable
private fun DomainInputView(
    onNext: (domainName: String) -> Unit
) {
    var domainName by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(
                R.string.login_activity_domain_enter_header_question
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(25.dp))
        TextField(
            value = domainName,
            label = context.getString(
                R.string.login_activity_domain_enter_input_label
            ),
            onValueChange = { newValue ->
                domainName = newValue
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(25.dp))
        Button(
            onClick = {
                onNext(domainName)
                null
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.login_activity_domain_input_button_label_next))
        }
    }
}


@Composable
private fun LoginView(
    currentDomainName: String?,
    onLogin: (username: String, password: String) -> Promise<*>?,
    onChangeDomain: () -> Unit,
    onCreateNewAccount: () -> Unit
) {
    var username by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = context.getString(
                    R.string.login_activity_login_header_login
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(25.dp))
            TextField(
                value = username,
                label = context.getString(
                    R.string.login_activity_login_input_label_username
                ),
                onValueChange = { newValue ->
                    username = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(15.dp))
            TextField(
                value = password,
                label = context.getString(
                    R.string.login_activity_login_input_label_password
                ),
                onValueChange = { newValue ->
                    password = newValue.filterNot { it.isWhitespace() }
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(25.dp))
            Button(
                onClick = {
                    onLogin(username, password)
                },
                isAsync = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context.getString(
                        R.string.login_activity_login_button_login
                    ),
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context.getString(R.string.login_activity_login_label_no_account),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clickable {
                            onCreateNewAccount()
                        }
                        .sizeIn(
                            minWidth = 48.dp,
                            minHeight = 48.dp
                        )
                ) {
                    Text(
                        text = context.getString(R.string.login_activity_login_label_create_new_account),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentDomainName ?: "Unknown domain",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable {
                        onChangeDomain()
                    }
                    .sizeIn(
                        minWidth = 48.dp,
                        minHeight = 48.dp
                    )
            ) {
                Text(
                    text = stringResource(R.string.login_activity_login_button_label_change_domain),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RegisterView(
    onRegister: (
        invitationKey: String,
        username: String,
        password: String
    ) -> Promise<*>?,
) {
    val context = LocalContext.current
    val requirementsList = remember {
        listOf(
            R.string.login_activity_register_label_password_size_requirement,
            R.string.login_activity_register_label_password_numbers_requirement,
            R.string.login_activity_register_label_password_lowercase_requirement,
            R.string.login_activity_register_label_password_uppercase_requirement,
            R.string.login_activity_register_label_password_special_characters_requirement,
            R.string.login_activity_register_label_password_same_requirement
        )
    }
    val requirementsMap = remember {
        mutableStateMapOf(
            *requirementsList.map { it to false }.toTypedArray()
        )
    }
    var invitationKey by remember {
        mutableStateOf("")
    }
    var username by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var repeatedPassword by remember {
        mutableStateOf("")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(
                R.string.login_activity_register_header_new_account
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(25.dp))
        TextField(
            value = invitationKey,
            label = context.getString(
                R.string.login_activity_register_input_label_invitation_key
            ),
            hint = stringResource(R.string.login_activity_register_input_field_hint_invitation_key),
            onValueChange = { newValue ->
                invitationKey = newValue
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextField(
            value = username,
            label = context.getString(
                R.string.login_activity_login_input_label_username
            ),
            onValueChange = { newValue ->
                username = newValue
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextField(
            value = password,
            label = context.getString(
                R.string.login_activity_register_input_label_password
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            onValueChange = { newValue ->
                password = newValue.filterNot { it.isWhitespace() }
                checkPasswords(requirementsMap, password, repeatedPassword)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextField(
            value = repeatedPassword,
            label = context.getString(
                R.string.login_activity_register_input_field_repeat_password
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            onValueChange = { newValue ->
                repeatedPassword = newValue.filterNot { it.isWhitespace() }
                if (password.isNotBlank()) {
                    checkPasswords(requirementsMap, password, repeatedPassword)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(15.dp))
        val inactive by remember(password) {
            derivedStateOf { password.isBlank() }
        }
        requirementsList.forEach {
            PasswordCorrectnessRow(
                text = context.getString(it),
                correct = requirementsMap[it] ?: false,
                inactive = inactive,
                modifier = Modifier.align(Alignment.Start)
            )
        }
        Spacer(modifier = Modifier.height(25.dp))
        Button(
            onClick = {
                checkPasswords(
                    requirementsMap,
                    password,
                    repeatedPassword
                )
                if (requirementsMap.none { !it.value }) {
                    return@Button onRegister(invitationKey, username, password)
                }
                return@Button null
            },
            isAsync = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = context.getString(
                    R.string.login_activity_register_button_create_account
                ),
            )
        }
    }
}

fun checkPasswords(
    map: SnapshotStateMap<Int, Boolean>,
    password1: String,
    password2: String
) {
    map[R.string.login_activity_register_label_password_size_requirement] = password1.length >= 6
    map[R.string.login_activity_register_label_password_same_requirement] = password1 == password2
    map[R.string.login_activity_register_label_password_numbers_requirement] =
        password1.contains(Regex("[0-9]"))
    map[R.string.login_activity_register_label_password_uppercase_requirement] =
        password1.contains(Regex("[A-Z]"))
    map[R.string.login_activity_register_label_password_lowercase_requirement] =
        password1.contains(Regex("[a-z]"))
    map[R.string.login_activity_register_label_password_special_characters_requirement] =
        password1.contains(Regex("\\W"))
}

@Composable
private fun PasswordCorrectnessRow(
    text: String,
    correct: Boolean,
    inactive: Boolean,
    modifier: Modifier = Modifier
) {
    val icon = remember(correct, inactive) {
        if (correct && !inactive) R.drawable.baseline_check_24 else R.drawable.baseline_close_24
    }
    val color = remember(correct, inactive) {
        if (inactive) LightGrey else if (correct) Color.Green else Color.Red
    }
    val textStyle = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current

    val iconSize = remember(density, textStyle) {
        with(density) {
            textStyle.lineHeight.toDp()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            ImageVector.vectorResource(
                id = icon
            ),
            tint = color,
            contentDescription = "Password correctness icon",
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = color,
            style = textStyle
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun DomainPreview() {
    DomainInputView {}
}

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun LoginPreview() {
    LoginView(null, onLogin = { username, password -> null }, onChangeDomain = {}) {}
}

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun RegisterPreview() {
    RegisterView { invitationKey, username, password -> null }
}

@Preview
@Composable
private fun PasswordCorrectnessRowPreview() {
    PasswordCorrectnessRow(text = "Conajmniej sześć znaków", correct = false, inactive = true)
}