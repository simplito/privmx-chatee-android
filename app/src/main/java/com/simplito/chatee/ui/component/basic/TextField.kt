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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.simplito.chatee.R

@Composable
fun TextField(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    hintMaxLines: Int = Int.MAX_VALUE,
    error: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    contentPadding: PaddingValues = PaddingValues(
        vertical = 12.dp,
        horizontal = 13.dp
    ),
    fontSize: TextUnit = TextUnit.Unspecified,
    suffix: @Composable () -> Unit = {},
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }
    val iconId by remember(visualTransformation, passwordVisible) {
        derivedStateOf {
            if (passwordVisible) R.drawable.outline_visibility_off_24 else R.drawable.outline_visibility_24
        }
    }
    val density = LocalDensity.current
    val materialTypo = MaterialTheme.typography
    val textHeight = remember(density, materialTypo) {
        with(density) {
            materialTypo.bodySmall.lineHeight.toDp()
        }
    }
    val visibilityInteractionSource = remember {
        MutableInteractionSource()
    }
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.width(IntrinsicSize.Max)
    ) {
        label?.let {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(5.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = if (fontSize == TextUnit.Unspecified) MaterialTheme.typography.bodySmall.fontSize else fontSize
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .wrapContentHeight(),
            singleLine = singleLine,
            visualTransformation = if (passwordVisible) VisualTransformation.None else visualTransformation
        ) { innerTextField ->
            Box(
                Modifier
                    .border(
                        1.dp,
                        if (error.isNullOrBlank()) MaterialTheme.colorScheme.primary else Color.Red,
                        MaterialTheme.shapes.medium,
                    )
                    .padding(contentPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                ) {
                    Box(
                        Modifier.weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint ?: "",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = hintMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = if (fontSize == TextUnit.Unspecified) MaterialTheme.typography.bodySmall.fontSize else fontSize,
                            )
                        }
                        innerTextField()
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    suffix()
                    if (visualTransformation is PasswordVisualTransformation) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            ImageVector.vectorResource(id = iconId),
                            contentDescription = "input_visibility_icon",
                            Modifier
                                .size(textHeight)
                                .clickable(
                                    visibilityInteractionSource,
                                    null
                                ) {
                                    passwordVisible = !passwordVisible
                                }
                        )
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                error,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = Color.Red,
            )
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
fun ChateeTextFieldPreview() {
    Column {
        TextField(
            value = "username",
            label = "123",
            onValueChange = { newValue ->
            },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = "",
            label = "123",
            hint = "Hello",
            singleLine = true,
            onValueChange = { newValue ->
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = "",
            label = "123",
            hint = "Hello",
            onValueChange = { newValue ->
            },
        )
        TextField(
            value = "",
            label = "123",
            onValueChange = { newValue ->
            },
        )

        TextField(
            value = "",
            label = "123",
            hint = "Hello",
            error = "",
            singleLine = true,
            onValueChange = { newValue ->
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = "",
            label = "123",
            hint = "Hello",
            onValueChange = { newValue ->
            },
        )
        TextField(
            value = "",
            label = "123",
            error = "Wrong name",
            onValueChange = { newValue ->
            },
        )
    }
}