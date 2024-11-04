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


package com.simplito.chatee.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ui.theme.DarkGrey
import com.simplito.chatee.ui.theme.LightGrey

@Composable
fun ChateeCircularProgressIndicator(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
) {
    CircularProgressIndicator(
        color = LightGrey,
        trackColor = DarkGrey,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        modifier = modifier
            .size(20.dp)
    )
}