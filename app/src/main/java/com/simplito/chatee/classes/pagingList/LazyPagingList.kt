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


package com.simplito.chatee.classes.pagingList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <Value : Any> LazyPagingColumn(
    pagingList: PagingList<Value>,
    itemKey: (index: Int, item: Value?) -> Any,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    listState: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    errorMessage: (error: Throwable) -> String = { "Cannot load list ${it.message}" },
    emptyListMessage: () -> String = { "List is empty" },
    loadingComponent: @Composable BoxScope.() -> Unit = { LoadingComponent(Modifier.align(Alignment.Center)) },
    errorComponent: @Composable BoxScope.(error: Throwable) -> Unit = {
        ErrorComponent(
            errorMessage(
                it
            ), Modifier.align(Alignment.Center)
        )
    },
    emptyListComponent: @Composable BoxScope.(message: String) -> Unit = {
        EmptyListComponent(
            emptyListMessage(),
            Modifier.align(Alignment.Center)
        )
    },
    itemContent: @Composable LazyItemScope.(index: Int, item: Value) -> Unit
) {
    Box(modifier) {
        when (pagingList.loadState) {
            PagingList.LoadingState.Loading -> {
                loadingComponent()
            }

            is PagingList.LoadingState.Error -> {
                errorComponent(pagingList.loadState.error!!)
            }

            PagingList.LoadingState.NotLoading -> {
                if (pagingList.isEmpty()) {
                    emptyListComponent(emptyListMessage())
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = verticalArrangement,
                        reverseLayout = reverseLayout,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            pagingList.size,
                            key = { index ->
                                pagingList.getId(
                                    index
                                ) {
                                    itemKey(index, it)
                                }
                            }
                        ) { index ->
                            val item = remember(index, pagingList.snapshot[index]) {
                                pagingList[index]
                            }
                            if (pagingList.prependLoadState == PagingList.LoadingState.Loading && index == 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.LightGray)
                                }
                            }
                            itemContent(index, item)
                        }
                        if (pagingList.appendLoadState == PagingList.LoadingState.Loading) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingComponent(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(color = Color.LightGray, modifier = modifier)
}

@Composable
private fun ErrorComponent(
    error: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = error,
        color = Color.Red,
        modifier = modifier
    )
}

@Composable
private fun EmptyListComponent(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = Color.White,
        modifier = modifier
    )
}