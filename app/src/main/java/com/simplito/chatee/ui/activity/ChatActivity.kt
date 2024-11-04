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

import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.Utils
import com.simplito.chatee.Utils.ChateeJson
import com.simplito.chatee.classes.FileUploadMessageJson
import com.simplito.chatee.classes.MessageJson
import com.simplito.chatee.classes.SaveDocumentInput
import com.simplito.chatee.classes.SaveDocumentStreamResultContract
import com.simplito.chatee.classes.TextMessageJson
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import com.simplito.chatee.classes.pagingList.LazyPagingColumn
import com.simplito.chatee.classes.pagingList.PagingList
import com.simplito.chatee.extension.endpoint
import com.simplito.chatee.extension.mExecutor
import com.simplito.chatee.model.FilePublicMeta
import com.simplito.chatee.model.ThreadMessage
import com.simplito.chatee.model.ThreadMessagePublicMeta
import com.simplito.chatee.model.ThreadPrivateMeta
import com.simplito.chatee.ui.component.basic.Button
import com.simplito.chatee.ui.component.basic.TextField
import com.simplito.chatee.ui.component.chat.AdditionalFileMessageContentInfo
import com.simplito.chatee.ui.component.chat.AuthorTag
import com.simplito.chatee.ui.component.chat.FileMessageContent
import com.simplito.chatee.ui.component.chat.TextMessageContent
import com.simplito.chatee.ui.theme.LightGrey
import com.simplito.java.privmx_endpoint.model.File
import com.simplito.java.privmx_endpoint.model.Store
import com.simplito.java.privmx_endpoint.model.Thread
import com.simplito.java.privmx_endpoint_extra.events.EventType
import com.simplito.java.privmx_endpoint_extra.model.SortOrder
import com.simplito.java.privmx_endpoint_extra.storeFileStream.StoreFileStreamReader
import com.simplito.java.privmx_endpoint_extra.storeFileStream.StoreFileStreamWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import java.util.UUID
import kotlin.time.Duration.Companion.minutes


class ChatActivity : BasicActivity() {
    companion object {
        const val TAG = "[ChatActivity]"
        const val THREAD_ID_EXTRA = "com.simplito.chatee.ChatActivity.threadId"

        private enum class ViewSection {
            CHAT, FILES
        }
    }

    private val threadInfo: MutableState<Thread?> = mutableStateOf(null)
    private val storeInfo: MutableState<Store?> = mutableStateOf(null)
    private val currentViewSection = mutableStateOf(ViewSection.CHAT)
    private val messagesPager = mutableStateOf<PagingList<MessageItem>?>(null)
    private val filesPager = mutableStateOf<PagingList<FileItem>?>(null)
    private val showNewMessageBar = mutableStateOf(false)
    private var messagingListState: LazyListState? = LazyListState()
    private var filesListState: LazyListState? = LazyListState()
    private var coroutineScope: CoroutineScope? = null
    private val uploadingState: MutableState<Boolean> = mutableStateOf(false)

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let {
                val mimeType = Utils.getMimeType(contentResolver, it) ?: run {
                    uploadingState.value = false
                    Log.e(TAG, "openDocumentLauncher: cannot read mimetype")
                    showError(getString(R.string.chat_activity_snackbar_error_cannot_read_file))
                    return@let
                }
                val cursor = contentResolver.query(it, null, null, null, null)
                cursor?.moveToFirst()
                val fileNameIndex: Int? = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (fileNameIndex == null) {
                    uploadingState.value = false
                    Log.e(TAG, "openDocumentLauncher: cannot init cursor column indexes")
                    showError(getString(R.string.chat_activity_snackbar_error_cannot_read_file))
                    return@let
                }
                val fileName = cursor.getString(fileNameIndex) ?: run {
                    uploadingState.value = false
                    Log.e(TAG, "openDocumentLauncher: cannot read file name")
                    showError(getString(R.string.chat_activity_snackbar_error_cannot_read_file))
                    return@let
                }
                cursor.close()
                try {
                    AsyncCall {
                        storeInfo.value?.let { storeInfo ->
                            val fileId = this.contentResolver.openInputStream(it)?.use { iS ->
                                StoreFileStreamWriter.createFile(
                                    privmxEndpointContainer?.endpoint?.storeApi,
                                    storeInfo.storeId,
                                    ChateeJson.encodeToString(
                                        FilePublicMeta(
                                            mimeType,
                                            fileName,
                                            threadInfo.value?.threadId ?: ""
                                        )
                                    ).toByteArray(),
                                    ByteArray(0),
                                    iS.available().toLong(),
                                    iS
                                )
                            }
                            if (fileId != null && threadInfo.value != null) {
                                sendMessage(
                                    FileUploadMessageJson(
                                        fileId,
                                        fileName
                                    )
                                )
                            }
                            return@AsyncCall fileId
                        }
                    }.then { v ->
                        //called on background thread, because looper not passed
                        uploadingState.value = false
                    }.fail(mExecutor) {
                        uploadingState.value = false
                        Log.e(
                            TAG,
                            "Error with create or write file [${it.javaClass.name}: ${it.message}]"
                        )
                        showError(getString(R.string.chat_activity_snackbar_error_cannot_create_file))
                    }
                } catch (e: Exception) {
                    uploadingState.value = false
                    showError(getString(R.string.chat_activity_snackbar_error_cannot_read_file))
                    Log.e(TAG, "Cannot read file [${e.javaClass.name}: ${e.message}]")
                }

            } ?: run {
                uploadingState.value = false
            }
        }


    private val saveDocumentLauncher = registerForActivityResult(
        SaveDocumentStreamResultContract("*/*")
    ) { (uri, fileId, updateDownloadingState) ->
        if (uri == null) {
            Log.e(
                TAG,
                "Cannot save message without uri"
            )
            return@registerForActivityResult
        }
        updateDownloadingState(true)
        AsyncCall {
            contentResolver.openOutputStream(uri)?.use { oS ->
                privmxEndpointContainer?.endpoint?.storeApi?.let { storeApi ->
                    StoreFileStreamReader.openFile(
                        storeApi,
                        fileId,
                        oS,
                    )
                }
            }
        }.then(mExecutor) {
            updateDownloadingState(false)
            Log.d(TAG, "File downloaded")
        }.fail(mExecutor) {
            Log.e(
                TAG,
                "Cannot download file [REASON]: ${it.message}"
            )
            showError(getString(R.string.chat_activity_snackbar_error_cannot_download_file))
            updateDownloadingState(false)
        }
    }

    override fun onPrivmxEndpointStart() {
        super.onPrivmxEndpointStart()
        initThreadModule()?.then(mExecutor) { threadInfo ->
            Log.d(TAG, "Set Thread info value (call store init)")
            initStoreModule(threadInfo)
        }
    }

    private fun initThreadModule(): Promise<Thread>? {
        if (privmxEndpointContainer?.endpoint?.threadApi == null) {
            showError(getString(R.string.chat_activity_snackbar_error_thread_api_not_initialized))
            return null
        }
        return intent?.extras?.getString(THREAD_ID_EXTRA, null)?.let { threadId ->
            val threadApi = privmxEndpointContainer.endpoint!!.threadApi
            //Load threadInfo
            val threadResult = AsyncCall {
                threadApi.getThread(threadId)
            }.then(mExecutor) {
                Log.d(TAG, "Set Thread info value")
                this.threadInfo.value = it
            }.fail {
                Log.e(TAG, "Cannot get thread info. [error]: ${it.message}")
                it.printStackTrace()
            }

            //Init messages list
            if (messagesPager.value == null) {
                messagesPager.value = PagingList(
                    this@ChatActivity,
                    id = { item -> item.message.getId() },
                ) { skip, size ->
                    try {
                        val messages = threadApi.listMessages(
                            threadId,
                            skip,
                            size,
                            SortOrder.DESC
                        )
                        if (messages.totalAvailable == null || messages.readItems == null) {
                            return@PagingList PagingList.LoadResult.Error(
                                message = Exception("Cannot load page")
                            )
                        }
                        return@PagingList PagingList.LoadResult.Success(
                            messages.totalAvailable,
                            messages.readItems.mapNotNull {
                                try {
                                    MessageItem(ThreadMessage.fromCoreMessage(it))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                            }
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@PagingList PagingList.LoadResult.Error(
                            message = Exception("Cannot load page")
                        )
                    }
                }
            } else {
                messagingListState = LazyListState()
                messagesPager.value?.refresh()
            }

            //register thread events
            privmxEndpointContainer.endpoint?.registerCallback(
                this@ChatActivity,
                EventType.ThreadNewMessageEvent(threadId)
            ) { newMsg ->
                if (newMsg?.info?.threadId == threadId) {
                    val decodedMessage = try {
                        ThreadMessage.fromCoreMessage(newMsg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Cannot decode message")
                        return@registerCallback
                    }
                    try {
                        messagesPager.value?.let { pager ->
                            val existingMsgIndex = pager.indexOfFirst { current ->
                                current.message.publicMeta.pendingId == decodedMessage.publicMeta.pendingId
                            }
                            if (existingMsgIndex > -1) {
                                pager.replace(existingMsgIndex, MessageItem(decodedMessage))
                            } else {
                                messagesPager.value?.prepend(
                                    listOf(MessageItem(decodedMessage)),
                                    increase = true
                                )
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(
                            TAG,
                            "Message append result: false [${e.javaClass.name}: ${e.message}]"
                        )
                    }
                    coroutineScope?.launch {
                        onNewMessage()
                    }
                }
            }

            privmxEndpointContainer?.endpoint?.registerCallback(
                this@ChatActivity,
                EventType.ThreadMessageDeletedEvent(threadId)
            ) { deletedMessage ->
                messagesPager.value?.run {
                    removeIf { current ->
                        current.message.info?.messageId == deletedMessage.messageId
                    }.also {
                        Log.d(TAG, "Remove message successfull: $it")
                    }
                }
            }
            threadResult
        }
    }

    private fun initStoreModule(threadInfo: Thread): Promise<Store?>? {
        if (privmxEndpointContainer?.endpoint?.storeApi == null) {
            showError(getString(R.string.chat_activity_snackbar_error_store_api_not_initialized))
            return null
        }
        val storeId = try {
            ChateeJson.decodeFromString<ThreadPrivateMeta>(threadInfo.privateMeta.decodeToString()).storeId
        } catch (_: Exception) {
            null
        }
        return storeId?.let {
            AsyncCall {
                privmxEndpointContainer?.endpoint?.storeApi?.getStore(storeId)
            }.fail(mExecutor) { e ->
                Log.e(TAG, "Cannot get store info. [error]: ${e.message}")
                e.printStackTrace()
                showError(getString(R.string.chat_activity_snackbar_error_cannot_get_store_info))
            }.then(mExecutor) { storeInfo ->
                this.storeInfo.value = storeInfo
                if (storeInfo != null) {
                    if (filesPager.value == null) {
                        filesPager.value = PagingList(
                            this@ChatActivity,
                            id = { item -> item.file.info.fileId }
                        ) { skip, size ->
                            val storeApi = privmxEndpointContainer?.endpoint?.storeApi
                            if (storeApi == null) {
                                showError(getString(R.string.chat_activity_snackbar_error_store_api_not_initialized))
                                return@PagingList PagingList.LoadResult.Error(
                                    message = Exception(
                                        "Cannot load page, REASON: Store api is not initialized"
                                    )
                                )
                            }

                            try {
                                val files = storeApi.listFiles(
                                    storeInfo.storeId,
                                    skip,
                                    size,
                                    SortOrder.DESC
                                )
                                if (files.totalAvailable == null || files.readItems == null) {
                                    return@PagingList PagingList.LoadResult.Error(
                                        message = Exception("Cannot load page")
                                    )
                                }
                                return@PagingList PagingList.LoadResult.Success(
                                    files.totalAvailable,
                                    files.readItems
                                        .map {
                                            FileItem(it)
                                        }
                                        .filter {
                                            !(it.decodedPrivateMeta?.name?.startsWith(".") ?: false)
                                        }
                                )
                            } catch (e: Exception) {
                                return@PagingList PagingList.LoadResult.Error(
                                    message = Exception("Cannot load page")
                                )
                            }
                        }
                    } else {
                        filesListState = LazyListState()
                        runOnUiThread {
                            filesPager.value?.refresh()
                        }
                    }

                    privmxEndpointContainer?.endpoint?.registerCallback(
                        this@ChatActivity,
                        EventType.StoreFileCreatedEvent(storeInfo.storeId)
                    ) { newFile ->
                        filesPager.value?.run {
                            if (snapshot.none { item -> item.file.info.fileId == newFile.info.fileId }) {
                                runOnUiThread {
                                    try {
                                        filesPager.value?.prepend(
                                            listOf(
                                                FileItem(
                                                    newFile,
                                                    mutableStateOf(false)
                                                )
                                            ),
                                            increase = true
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "File append error")
                                        //Ignore
                                    }
                                }
                            }
                        }
                    }

                    privmxEndpointContainer?.endpoint?.registerCallback(
                        this@ChatActivity,
                        EventType.StoreFileUpdatedEvent(storeId)
                    ) { updatedFile ->
                        filesPager.value?.run {
                            replaceAll { current ->
                                if (current.file.info.fileId == updatedFile.info.fileId) {
                                    current.file = updatedFile
                                }
                                current
                            }
                        }
                    }

                    privmxEndpointContainer?.endpoint?.registerCallback(
                        this@ChatActivity,
                        EventType.StoreFileDeletedEvent(storeId)
                    ) { deletedFile ->
                        filesPager.value?.run {
                            removeIf { current ->
                                current.file.info.fileId == deletedFile.fileId
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendMessage(message: MessageJson): Promise<String>? {
        privmxEndpointContainer.endpoint?.let { privmxEndpoint ->
            threadInfo.value?.let {
                val messagePlaceholder = ThreadMessagePlaceholder(
                    false,
                    UUID.randomUUID().toString(),
                    message,
                    if (message is TextMessageJson) "text" else "file"
                )
                try {
                    messagesPager.value?.prepend(
                        listOf(
                            MessageItem(messagePlaceholder)
                        )
                    )
                    if (messagingListState != null && coroutineScope != null) {
                        coroutineScope?.launch {
                            scrollToMessage(0, messagingListState!!)
                        }
                    }
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "Message append result: false")
                }
                return@sendMessage AsyncCall {
                    privmxEndpoint.threadApi.sendMessage(
                        it.threadId,
                        ChateeJson.encodeToString(messagePlaceholder.publicMeta).toByteArray(),
                        messagePlaceholder.privateMeta,
                        ChateeJson.encodeToString(messagePlaceholder.data).toByteArray(),
                    )
                }.fail { exception ->
                    Log.d(TAG, "Message send error: ${it.javaClass.name} ${exception.message}")
                    exception.printStackTrace()
                }
            }
        }
        return null
    }

    private fun removeMessage(message: ThreadMessage): Promise<*> {
        return AsyncCall {
            privmxEndpointContainer?.endpoint?.let { privmxEndpoint ->
                when (message.data) {
                    is TextMessageJson -> {
                        privmxEndpoint.threadApi?.deleteMessage(message.info?.messageId)
                    }

                    is FileUploadMessageJson -> {
                        try {
                            privmxEndpoint.storeApi?.deleteFile(message.data.fileId)
                        } catch (e: Exception) {
                        }
                        privmxEndpoint.threadApi?.deleteMessage(message.info?.messageId)
                    }

                    else -> throw Exception("Unknown message type")
                }
            }
        }
    }

    private suspend fun onNewMessage() {
        messagingListState?.let { listState ->
            messagesPager.value?.let {
                if (it.hasKey(0) && listState.firstVisibleItemIndex < 3) {
                    listState.animateScrollToItem(0)
                } else {
                    showNewMessageBar.value = true
                }
            }
        }

    }

    private suspend fun scrollToMessage(key: Int, listState: LazyListState) {
        messagesPager.value?.let {

            val result = it.goTo(key) {
                it.getItemIndex(key)?.let {
                    runBlocking {
                        listState.scrollToItem(it)
                    }
                }
            }
            if (!result) {
                it.getItemIndex(key)?.let { index ->
                    if (listState.firstVisibleItemIndex < 3) {
                        listState.animateScrollToItem(index)
                    } else {
                        listState.scrollToItem(index)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            coroutineScope = rememberCoroutineScope()
            var messageToDelete by remember {
                mutableStateOf<MessageItem?>(null)
            }
            val bottomSheetState = rememberModalBottomSheetState()
            if (threadInfo.value != null) {
                val threadTitle = remember(threadInfo) {
                    threadInfo.value?.privateMeta?.let {
                        ChateeJson.decodeFromString<ThreadPrivateMeta>(it.decodeToString()).name
                    } ?: ""
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.message),
                        contentDescription = "chat_icon",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = threadTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(LightGrey.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.messages),
                            contentDescription = "chat_icon",
                            tint = if (currentViewSection.value == ViewSection.CHAT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    currentViewSection.value = ViewSection.CHAT
                                }
                                .background(if (currentViewSection.value == ViewSection.CHAT) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(9.dp)
                                .size(24.dp)
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.files),
                            contentDescription = "file_icon",
                            tint = if (currentViewSection.value == ViewSection.FILES) MaterialTheme.colorScheme.onPrimary else com.simplito.chatee.ui.theme.Text,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    currentViewSection.value = ViewSection.FILES
                                }
                                .background(if (currentViewSection.value == ViewSection.FILES) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(9.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                            null
                        },
                        isAsync = true,
                        processingState = uploadingState,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(9.dp),
                        processingContent = {
                            CircularProgressIndicator(color = LightGrey, strokeWidth = 2.dp)
                        },
                        modifier = Modifier
                            .size(42.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.paperclip),
                            contentDescription = "add_chat",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                var downloadingFiles by remember {
                    mutableStateOf(setOf<String>())
                }
                when (currentViewSection.value) {
                    ViewSection.CHAT -> {
                        if (this@ChatActivity.messagesPager.value != null) {
                            LaunchedEffect(messagingListState) {
                                snapshotFlow {
                                    val index = messagingListState?.firstVisibleItemIndex
                                    showNewMessageBar.value && index == 0 && messagesPager.value!!.getItemKey(
                                        index
                                    ) == 0
                                }.distinctUntilChanged()
                                    .filter { it }
                                    .collectLatest { showNewMessageBar.value = false }
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                LazyPagingColumn(
                                    pagingList = this@ChatActivity.messagesPager.value!!,
                                    listState = messagingListState!!,
                                    verticalArrangement = Arrangement.Bottom,
                                    reverseLayout = true,
                                    itemKey = { index, item ->
                                        item?.message?.getId() ?: UUID.randomUUID().toString()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) { index, item ->
                                    val message = remember(index, item) {
                                        item.message.data
                                    }

                                    val showAuthorTag by remember(index, item.message.getId()) {
                                        derivedStateOf {
                                            val list = messagesPager.value!!.snapshot
                                            if (list.size <= index + 1) {
                                                true
                                            } else {
                                                val previous = list[index + 1]
                                                val timeDifference = Instant
                                                    .fromEpochMilliseconds(
                                                        item.message.info?.createDate ?: 0
                                                    ).minus(
                                                        Instant.fromEpochMilliseconds(
                                                            previous.message.info?.createDate ?: 0
                                                        )
                                                    )
                                                (previous.message.info?.author != item.message.info?.author && item.message !is ThreadMessagePlaceholder) ||
                                                        timeDifference >= 3.minutes

                                            }

                                        }
                                    }
                                    val content: @Composable () -> Unit = {
                                        when (message) {
                                            is TextMessageJson -> {
                                                TextMessageContent(
                                                    text = message.text,
                                                    isMy = item.message.info?.author == ChateeSession.currentUser?.username || item.message is ThreadMessagePlaceholder,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onLongClick = {
                                                                messageToDelete = item
                                                                coroutineScope?.launch {
                                                                    bottomSheetState.show()
                                                                }
                                                            },
                                                            onClick = {}
                                                        )
                                                )
                                            }

                                            is FileUploadMessageJson -> {
                                                val itemIsDownloading by remember(
                                                    message.fileId,
                                                    downloadingFiles
                                                ) {
                                                    derivedStateOf {
                                                        downloadingFiles.contains(message.fileId)
                                                    }
                                                }
                                                FileMessageContent(
                                                    fileName = message.fileName,
                                                    downloading = itemIsDownloading,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onLongClick = {
                                                                messageToDelete = item
                                                                coroutineScope?.launch {
                                                                    bottomSheetState.show()
                                                                }
                                                            },
                                                            onClick = {}
                                                        )
                                                ) {
                                                    saveDocumentLauncher.launch(
                                                        SaveDocumentInput(
                                                            message.fileName,
                                                            message.fileId
                                                        ) {
                                                            downloadingFiles = if (it) {
                                                                setOf(
                                                                    *downloadingFiles.toTypedArray(),
                                                                    message.fileId
                                                                )
                                                            } else {
                                                                setOf(
                                                                    *downloadingFiles.filter { it != message.fileId }
                                                                        .toTypedArray()
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            else -> TextMessageContent(
                                                text = (item.message.data as? TextMessageJson)?.text
                                                    ?: "",
                                                isMy = item.message.info?.author == ChateeSession.currentUser?.username,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onLongClick = {
                                                            messageToDelete = item
                                                            coroutineScope?.launch {
                                                                bottomSheetState.show()
                                                            }
                                                        },
                                                        onClick = {}
                                                    )
                                            )
                                        }
                                    }
                                    if (showAuthorTag) {
                                        AuthorTag(
                                            author = item.message.info?.author ?: "",
                                            date = item.message.info?.createDate ?: 0,
                                            isSending = item.message is ThreadMessagePlaceholder,
                                            hasError = item.message is ThreadMessagePlaceholder && item.message.error,
                                            modifier = Modifier
//                                                .animateItemPlacement()
                                                .padding(
                                                    end = 15.dp,
                                                    start = 10.dp,
                                                )
                                                .padding(
                                                    vertical = 5.dp
                                                )
                                        ) {
                                            content()
                                        }
                                    } else {
                                        Box(
                                            Modifier
//                                                .animateItemPlacement()
                                                .padding(start = 60.dp, end = 15.dp)
                                                .padding(vertical = 5.dp)
                                        ) {
                                            content()
                                        }
                                    }
                                    if (index < messagesPager.value!!.size - 1) {
                                        Spacer(Modifier.height(5.dp))
                                    }
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(10.dp)
                                )
                                {
                                    NewMessageBar(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                coroutineScope?.launch {
                                                    scrollToMessage(0, messagingListState!!)
                                                }
                                                showNewMessageBar.value = false
                                            }
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(15.dp),
                                    )
                                }
                            }
                        }

                        var message by remember {
                            mutableStateOf("")
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            TextField(
                                value = message,
                                fontSize = 18.sp,
                                singleLine = false,
                                contentPadding = PaddingValues(
                                    vertical = 6.dp,
                                    horizontal = 6.dp
                                ),
                                hint = LocalContext.current.getString(
                                    R.string.chat_activity_input_field_hint_new_message_to,
                                    threadTitle
                                ),
                                hintMaxLines = 3,
                                suffix = {
                                    Button(
                                        onClick = {
                                            if (message.isNotBlank()) {
                                                sendMessage(TextMessageJson(message))
                                                message = ""
                                            }
                                            null
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(8.dp),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .aspectRatio(1f),
                                    ) {
                                        Icon(
                                            ImageVector.vectorResource(id = R.drawable.send),
                                            contentDescription = "Send message",
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .heightIn(max = 150.dp)
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) { newValue ->
                                message = newValue
                            }
                        }
                    }

                    ViewSection.FILES -> {
                        if (this@ChatActivity.filesPager.value != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                LazyPagingColumn(
                                    pagingList = this@ChatActivity.filesPager.value!!,
                                    listState = filesListState!!,
                                    itemKey = { index, item ->
                                        item?.file?.info?.fileId ?: UUID.randomUUID().toString()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) { index, item ->
                                    val itemIsDownloading by remember(
                                        item.file.info.fileId,
                                        downloadingFiles
                                    ) {
                                        derivedStateOf {
                                            downloadingFiles.contains(item.file.info.fileId)
                                        }
                                    }
                                    FileMessageContent(
                                        fileName = item.decodedPrivateMeta?.name ?: "Unknown name",
                                        downloading = itemIsDownloading,
                                        additionalInfo = AdditionalFileMessageContentInfo(
                                            item.file.info.createDate,
                                            item.file.info.author,
                                            item.file.size
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        saveDocumentLauncher.launch(
                                            SaveDocumentInput(
                                                item.decodedPrivateMeta?.name ?: "Unnamed",
                                                item.file.info.fileId
                                            ) {
                                                downloadingFiles = if (it) {
                                                    setOf(
                                                        *downloadingFiles.toTypedArray(),
                                                        item.file.info.fileId
                                                    )
                                                } else {
                                                    setOf(
                                                        *downloadingFiles.filter { it != item.file.info.fileId }
                                                            .toTypedArray()
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    if (index < filesPager.value!!.size - 1) {
                                        Spacer(Modifier.height(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                if (bottomSheetState.isVisible) {
                    var currentRequest: Promise<*>? = null
                    BottomSheet(
                        bottomSheetState,
                        canDelete = ChateeSession.currentUser?.isStaff ?: false || messageToDelete?.message?.info?.author == ChateeSession.currentUser?.username,
                        onDismiss = {
                            messageToDelete = null
                        }
                    ) {
                        messageToDelete?.let {
                            if (currentRequest == null) {
                                currentRequest = removeMessage(it.message)
                                    .then {
                                        coroutineScope?.launch {
                                            messageToDelete = null
                                            currentRequest = null
                                            bottomSheetState.hide()
                                        }
                                    }
                                    .fail {
                                        coroutineScope?.launch {
                                            messageToDelete = null
                                            currentRequest = null
                                            bottomSheetState.hide()
                                            showError("Cannot remove message")
                                        }
                                        Log.e(
                                            TAG,
                                            "Cannot remove message [REASON: ${it.message}] "
                                        )
                                    }
                            }
                        } ?: run {
                            coroutineScope?.launch {
                                messageToDelete = null
                                currentRequest = null
                                bottomSheetState.hide()
                            }
                        }
                    }
                }

            } else {
                Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.LightGray)
                }
            }

        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun NewMessageBar(
        modifier: Modifier,
    ) {
        AnimatedVisibility(
            visible = showNewMessageBar.value,
            enter = slideInVertically(
                animationSpec = tween(
                    300,
                    easing = EaseOutBack
                ),
                initialOffsetY = { it }),
            exit = slideOutVertically(
                animationSpec = tween(
                    300,
                    200,
                    easing = EaseInBack
                ),
                targetOffsetY = { it }),
            label = "NewMessageBar_animation"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        R.drawable.baseline_arrow_downward_24
                    ),
                    contentDescription = "new message",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Row(
                    modifier = Modifier.animateEnterExit(
                        enter = expandHorizontally(
                            animationSpec = tween(
                                200,
                                300,
                                easing = EaseOutQuad
                            )
                        ),
                        exit = shrinkHorizontally(
                            animationSpec = tween(
                                200,
                                easing = EaseOutQuad
                            )
                        ),
                        label = "NewMessageBar_text_animation"
                    )
                ) {
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "New messages",
                        color = Color.White,
                    )
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

private class ThreadMessagePlaceholder(
    var error: Boolean = false,
    messageId: String,
    message: MessageJson,
    mimeType: String,
) : ThreadMessage(
    ThreadMessagePublicMeta(
        messageId,
        mimeType
    ),
    message
)

private fun ThreadMessage.getId(): String {
    return info?.author?.plus(info.messageId) ?: ""
}

data class MessageItem(
    val message: ThreadMessage,
    val downloading: MutableState<Boolean> = mutableStateOf(false),
)

data class FileItem(
    var file: File,
    val downloading: MutableState<Boolean> = mutableStateOf(false)
) {
    val decodedPrivateMeta: FilePublicMeta? = try {
        ChateeJson.decodeFromString<FilePublicMeta?>(file.publicMeta.decodeToString())
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    bottomSheetState: SheetState,
    canDelete: Boolean = true,
    onDismiss: () -> Unit,
    onDeleteMessageClick: () -> Unit
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
                    onDeleteMessageClick.invoke()
                }
        ) {
            if (canDelete) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.trash),
                    contentDescription = "remove_message",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(text = stringResource(R.string.chat_activity_modal_bottom_sheet_label_remove_message))
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