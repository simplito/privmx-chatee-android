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


package com.simplito.chatee.ui.component.basic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.simplito.chatee.classes.async.Promise
import kotlinx.coroutines.launch

@Composable
fun Button(
    onClick: () -> Promise<*>?,
    modifier: Modifier = Modifier,
    isAsync: Boolean = false,
    processingState: MutableState<Boolean> = rememberProcessingState(),
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    processingContent: @Composable () -> Unit = { BasicCircularProgressIndicator() },
    content: @Composable RowScope.() -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    Button(
        onClick = {
            if (!processingState.value) {
                if (!isAsync) {
                    onClick()
                    return@Button
                }

                onClick().also {
                    processingState.value = true
                }?.then {
                    coroutineScope.launch {
                        processingState.value = false
                    }
                }?.fail {
                    coroutineScope.launch {
                        processingState.value = false
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (processingState.value) {
            processingContent()
        } else {
            content(this)
        }
    }
}

@Composable
private fun rememberProcessingState(): MutableState<Boolean> = remember {
    mutableStateOf(false)
}