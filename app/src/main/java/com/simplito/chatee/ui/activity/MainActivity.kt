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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import com.simplito.chatee.classes.pagingList.LazyPagingColumn
import com.simplito.chatee.classes.pagingList.PagingList
import com.simplito.chatee.extension.endpoint
import com.simplito.chatee.model.ThreadItem
import com.simplito.chatee.ui.component.ChateeAvatar
import com.simplito.chatee.ui.component.basic.Button
import com.simplito.chatee.ui.component.basic.TextField
import com.simplito.chatee.ui.component.main.ThreadItemRow
import com.simplito.java.privmx_endpoint.model.Thread
import com.simplito.java.privmx_endpoint.model.events.ThreadStatsEventData
import com.simplito.java.privmx_endpoint_extra.events.EventType
import com.simplito.java.privmx_endpoint_extra.model.SortOrder
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : BasicActivity() {
    companion object {
        const val TAG = "[MainActivity]"
    }

    private val threadsPager: MutableState<PagingList<ThreadItem>?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //Ignore
                }
            }
        )
    }

    override fun onPrivmxEndpointStart() {
        super.onPrivmxEndpointStart()
        ChateeSession.serverConnection?.cloudData?.let { cloudData ->
            threadsPager.value = PagingList(
                this@MainActivity,
                PagingList.PagingListConfig(),
            ) { start, size ->
                if (privmxEndpointContainer?.endpoint == null) {
                    return@PagingList PagingList.LoadResult.Error(
                        Exception("You are not logged in")
                    )
                }
                privmxEndpointContainer?.endpoint!!.let { privmxEndpoint ->
                    if (privmxEndpoint.threadApi == null) {
                        return@PagingList PagingList.LoadResult.Error(
                            Exception("Thread Api is not initialized")
                        )
                    }
                    try {
                        val result = privmxEndpoint.threadApi.listThreads(
                            cloudData.contextId,
                            start,
                            size,
                            SortOrder.DESC
                        )
                        val mappedItems = result.readItems.mapNotNull {
                            try {
                                ThreadItem(it)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        return@PagingList PagingList.LoadResult.Success(
                            result.totalAvailable,
                            mappedItems
                        )
                    } catch (e: Exception) {
                        return@PagingList PagingList.LoadResult.Error(
                            Exception("Error during download page")
                        )
                    }
                }
            }

            privmxEndpointContainer.endpoint?.let { privmxEndpoint ->
                privmxEndpoint.registerCallback(
                    this@MainActivity,
                    EventType.ThreadStatsChangedEvent
                ) { threadStatsData ->
                    val firstItemIndex = threadsPager.value?.snapshot?.indexOfFirst {
                        it.thread.threadId == threadStatsData.threadId
                    } ?: -1
                    if (firstItemIndex >= 0) {
                        val item = threadsPager.value!!.snapshot[firstItemIndex]
                        threadsPager.value?.replace(
                            firstItemIndex,
                            item.copy(
                                thread = item.thread.fromNewStats(threadStatsData)
                            )
                        )
                    }
                }

                privmxEndpoint.registerCallback(
                    this@MainActivity,
                    EventType.ThreadDeletedEvent
                ) { deletedThread ->
                    threadsPager.value?.run {
                        removeIf { current ->
                            current.thread.threadId == deletedThread.threadId
                        }
                    }
                }

                privmxEndpoint.registerCallback(
                    this@MainActivity,
                    EventType.ThreadCreatedEvent
                ) { newThread ->
                    val threadItem = try {
                        ThreadItem(newThread)
                    } catch (e: Exception) {
                        null
                    }
                    if (threadItem == null) {
                        println("Incorrect thread")
                        return@registerCallback
                    }
                    threadsPager.value?.run {
                        prepend(
                            listOf(threadItem),
                            increase = true
                        )
                    }
                }
            }
        }
    }

    private fun removeChat(thread: ThreadItem): Promise<*> {
        return AsyncCall {
            privmxEndpointContainer?.endpoint?.let { privmxEndpoint ->
                val storeId = thread.decodedPrivateMeta.storeId
                storeId?.let {
                    privmxEndpoint.storeApi?.deleteStore(it)
                    Log.d(TAG, "Store deleted")
                }
                privmxEndpoint.threadApi.deleteThread(thread.thread.threadId)
                Log.d(TAG, "thread deleted")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        var searchingValue by remember {
            mutableStateOf("")
        }
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .contentPaddingHorizontal()
                    .fillMaxWidth()
                    .padding(
                        vertical = 10.dp,
                    )
            ) {
                val avatarInteractionSource = remember {
                    MutableInteractionSource()
                }
                Text(
                    text = stringResource(R.string.main_activity_title_chats),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Text(
                    ChateeSession.currentUser?.username ?: "Adam",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.size(10.dp))
                ChateeAvatar(
                    username = ChateeSession.currentUser?.username ?: "Adam",
                    size = { 40f },
                    modifier = Modifier.clickable(
                        avatarInteractionSource,
                        null
                    ) {
                        startActivity(
                            Intent(
                                this@MainActivity,
                                AccountActivity::class.java
                            )
                        )
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.contentPaddingHorizontal()
            ) {
                TextField(
                    value = searchingValue,
                    hint = stringResource(R.string.main_activity_search_bar_hint_search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { newValue ->
                    searchingValue = newValue
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        startActivity(
                            Intent(
                                this@MainActivity,
                                CreateThreadActivity::class.java
                            )
                        )
                        null
                    },
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(5.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.plus),
                        contentDescription = "add_chat",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(15.dp))
            val threadsListState = rememberLazyListState()
            var threadToDelete by remember {
                mutableStateOf<ThreadItem?>(null)
            }
            val bottomSheetState = rememberModalBottomSheetState()
            if (this@MainActivity.threadsPager.value != null) {
                if (searchingValue.isBlank()) {
                    LazyPagingColumn(
                        pagingList = this@MainActivity.threadsPager.value!!,
                        listState = threadsListState,
                        itemKey = { index, item ->
                            item?.thread?.threadId ?: UUID.randomUUID().toString()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .contentPadding()
                    ) { index, item ->
                        val title = item.rememberTitle(index)
                        ThreadItemRow(
                            title = title,
                            item.thread.users,
                            Modifier
                                .padding(vertical = 5.dp)
                                .combinedClickable(
                                    onLongClick = {
                                        threadToDelete = item
                                        coroutineScope.launch {
                                            bottomSheetState.show()
                                        }
                                    }
                                ) {
                                    val activityIntent = Intent(
                                        this@MainActivity,
                                        ChatActivity::class.java
                                    )
                                    activityIntent.putExtra(
                                        ChatActivity.THREAD_ID_EXTRA,
                                        item.thread.threadId
                                    )
                                    this@MainActivity.startActivity(
                                        activityIntent
                                    )
                                }
                                .fillMaxWidth()
                        )
                    }
                } else {
                    val filteredList = remember(searchingValue) {
                        mutableStateListOf(
                            *threadsPager
                                .value!!
                                .snapshot
                                .filter {
                                    try {
                                        it.decodedPrivateMeta.name.lowercase()
                                            .contains(searchingValue.lowercase())
                                    } catch (e: Exception) {
                                        false
                                    }
                                }.toTypedArray()
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .contentPadding()
                    ) {
                        itemsIndexed(
                            filteredList,
                            key = { _, item -> item.thread.threadId }
                        ) { index, item ->
                            val title = item.rememberTitle(index)
                            ThreadItemRow(
                                title = title,
                                item.thread.users,
                                Modifier
                                    .padding(vertical = 5.dp)
                                    .combinedClickable(
                                        onLongClick = {
                                            threadToDelete = item
                                            coroutineScope.launch {
                                                bottomSheetState.show()
                                            }
                                        }
                                    ) {
                                        val activityIntent = Intent(
                                            this@MainActivity,
                                            ChatActivity::class.java
                                        )
                                        activityIntent.putExtra(
                                            ChatActivity.THREAD_ID_EXTRA,
                                            item.thread.threadId
                                        )
                                        this@MainActivity.startActivity(
                                            activityIntent
                                        )
                                    }
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                if (bottomSheetState.isVisible) {
                    var currentRequest: Promise<*>? = null
                    BottomSheet(
                        bottomSheetState,
                        canDelete = ChateeSession.currentUser?.isStaff ?: false || threadToDelete?.thread?.managers?.any { it == ChateeSession.currentUser?.username } ?: false,
                        onDismiss = {
                            threadToDelete = null
                        }
                    ) {
                        threadToDelete?.let {
                            if (currentRequest == null) {
                                currentRequest = removeChat(it)
                                    .then {
                                        coroutineScope.launch {
                                            threadToDelete = null
                                            currentRequest = null
                                            bottomSheetState.hide()
                                        }
                                    }
                                    .fail {
                                        coroutineScope.launch {
                                            threadToDelete = null
                                            currentRequest = null
                                            bottomSheetState.hide()
                                            showError("Cannot remove thread")
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    @Preview(
        showBackground = true,
        showSystemUi = true
    )
    @Composable
    private fun ContentPreview() {
        Content()
    }
}

private fun Thread.fromNewStats(threadStatsData: ThreadStatsEventData) = Thread(
    contextId,
    threadId,
    createDate,
    creator,
    lastModificationDate,
    lastModifier,
    users,
    managers,
    version,
    threadStatsData.lastMsgDate,
    publicMeta,
    privateMeta,
    this.policy,
    threadStatsData.messagesCount,
    0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    bottomSheetState: SheetState,
    canDelete: Boolean = true,
    onDismiss: () -> Unit,
    onDeleteThreadClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .clickable {
                    onDeleteThreadClick.invoke()
                }
        ) {
            if (canDelete) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.trash),
                    contentDescription = "remove_chat",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(text = stringResource(R.string.main_activity_modal_bottom_sheet_label_remove_chat))
            } else {
                Text(
                    text = stringResource(R.string.chat_activity_bottom_sheet_no_available_actions),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}