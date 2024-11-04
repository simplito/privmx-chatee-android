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

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import java.util.UUID
import java.util.function.Predicate
import java.util.function.UnaryOperator

class PagingList<Value : Any>(
    context: Context,
    private val config: PagingListConfig = PagingListConfig(),
    private val id: ((Value) -> Any)? = null,
    private val loadData: (start: Long, size: Long) -> LoadResult<Value>
) : List<Value> {
    companion object {
        private const val TAG = "[PrivmxPagingList]"

        enum class Action {
            APPEND, PREPEND, REFRESH
        }
    }

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    var totalItems: Long? = null
        private set
    private val list = mutableStateListOf<Value>()
    private var nextPageKey = PagingKey(config.initialIndex, false)
    private var previousPageKey = PagingKey(config.initialIndex, false)
    private val loading = mutableStateOf<LoadingState>(LoadingState.NotLoading)
    private val prependLoading = mutableStateOf<LoadingState>(LoadingState.NotLoading)
    private val appendLoading = mutableStateOf<LoadingState>(LoadingState.NotLoading)
    private val currentKey = mutableIntStateOf(config.initialIndex)
    private var loadListener: PagingLoadListener? = null
    private var version: UInt = 0U
    val loadState get() = loading.value
    val prependLoadState get() = prependLoading.value
    val appendLoadState get() = appendLoading.value

    init {
        refresh(config.initialIndex)
    }

    val snapshot get() = list.toList()
    fun getId(index: Int, getKey: (item: Value?) -> Any) =
        getKey(if (index in 0..<size) snapshot[index] else null)

    fun getItemKey(index: Int) = firstKey + index
    fun getItemIndex(key: Int): Int? =
        if (key >= firstKey && key < nextPageKey.key) key - firstKey else null

    fun setLoadListener(loadListener: PagingLoadListener) {
        this.loadListener = loadListener
    }

    private val firstKey: Int get() = if (previousPageKey.pagingEndReached) 0 else previousPageKey.key + config.pageSize
    private fun loadNextPage() {
        if (!nextPageKey.pagingEndReached && appendLoading.value != LoadingState.Loading) {
            appendLoading.value = LoadingState.Loading
            doLoad(nextPageKey.key, config.pageSize, Action.APPEND)
        }
    }

    private fun loadPreviousPage() {
        if (!previousPageKey.pagingEndReached && prependLoading.value != LoadingState.Loading) {
            prependLoading.value = LoadingState.Loading
            var size = config.pageSize
            //To load top items
            var key = previousPageKey.key
            if (key < 0) {
                size += key
                key = 0
            }
            doLoad(key, size, Action.PREPEND)
        }
    }

    private fun doLoad(
        pageKey: Int?, size: Int, action: Action
    ): Promise<LoadResult<Value>> {
        if ((pageKey ?: 0) < 0) throw IndexOutOfBoundsException()
        val start = pageKey ?: config.initialIndex
        val nextPageKeyOnStart = nextPageKey
        val previousPageKeyOnStart = previousPageKey
        val callVersion = version
        return AsyncCall {
            loadData(start.toLong(), size.toLong())
        }.then(mainExecutor) {
            if (it is LoadResult.Success) {
                totalItems = it.total
                when (action) {
                    Action.PREPEND -> {
                        synchronized(previousPageKey) {
                            val offset =
                                (previousPageKey.key - previousPageKeyOnStart.key).coerceAtLeast(0)
                            if (callVersion != version || offset != 0) {
                                prependLoading.value = LoadingState.NotLoading
                                return@then
                            }
                            val filteredList = it.items.let {
                                if (id != null) {
                                    val first = list.firstOrNull()
                                    if (first != null) {
                                        val indexOfFirstDuplicate = it.indexOfFirst { item ->
                                            id.invoke(item) == id.invoke(first)
                                        }
                                        if (indexOfFirstDuplicate >= 0) {
                                            return@let it.take(indexOfFirstDuplicate)
                                        }
                                    }
                                }
                                return@let it
                            }
                            list.addAll(filteredList)
                            if (it.items.isEmpty() || start == 0) {
                                previousPageKey.key = -config.pageSize
                                previousPageKey.pagingEndReached = true
                            } else {
                                previousPageKey.key = start.minus(it.items.size)
                            }
                            prependLoading.value = LoadingState.NotLoading
                        }
                    }

                    Action.APPEND -> {
                        if (callVersion != version) {
                            appendLoading.value = LoadingState.NotLoading
                            return@then
                        }
                        synchronized(nextPageKey) {
                            val offset = (nextPageKey.key - start).coerceAtLeast(0)
                            if (offset < it.items.size) {
                                val filteredList = it.items.takeLast(it.items.size - offset).let {
                                    if (id != null) {
                                        val last = list.lastOrNull()
                                        if (last != null) {
                                            val indexOfLastDuplicate = it.indexOfLast { item ->
                                                id.invoke(item) == id.invoke(last)
                                            }
                                            if (indexOfLastDuplicate >= 0) {
                                                return@let it.takeLast(it.size - indexOfLastDuplicate - 1)
                                            }
                                        }
                                    }
                                    return@let it
                                }
                                list.addAll(filteredList)
                                nextPageKey.key += filteredList.size
                                if (it.items.isEmpty() && offset == 0) {
                                    nextPageKey.pagingEndReached = true
                                }
                            }
                        }
                        appendLoading.value = LoadingState.NotLoading
                    }

                    Action.REFRESH -> {
                        if (callVersion != version) {
                            loading.value = LoadingState.NotLoading
                            return@then
                        }
                        val offset = nextPageKey.key - nextPageKeyOnStart.key
                        list.addAll(it.items)
                        nextPageKey.key = start + it.items.size + offset
                        if (it.items.isEmpty() && offset == 0) {
                            nextPageKey.pagingEndReached = true
                        }
                        previousPageKey.key = start - config.pageSize + offset
                        if ((it.items.isEmpty() || start == 0) && offset == 0) {
                            previousPageKey.pagingEndReached = true
                        }
                        loading.value = LoadingState.NotLoading
                    }
                }
                onSuccesLoad(action)
            } else if (it is LoadResult.Error) {
                onErrorLoad(action, it.message)
            }
        }.fail {
            Log.e(TAG, "Cannot load page [${it.javaClass.name}: ${it.message}]")
            it.printStackTrace()
            onErrorLoad(action, it)
        }
    }

    override operator fun get(index: Int): Value {
        return list[index].also {
            currentKey.intValue = firstKey.plus(index)
            if (index >= list.lastIndex - (config.prefetchDistance * config.pageSize)) {
                loadNextPage()
            }
            if (index <= (config.prefetchDistance * config.pageSize)) {
                loadPreviousPage()
            }
        }
    }

    override fun indexOf(element: Value): Int = list.indexOf(element)

    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<Value> = list.iterator()

    override fun listIterator(): ListIterator<Value> = list.listIterator()

    override fun listIterator(index: Int): ListIterator<Value> = list.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Value> =
        list.subList(fromIndex, toIndex)

    override fun lastIndexOf(element: Value): Int = list.lastIndexOf(element)
    override val size: Int get() = list.size
    override fun containsAll(elements: Collection<Value>): Boolean = list.containsAll(elements)

    override fun contains(element: Value): Boolean = list.contains(element)

    @Throws(IllegalStateException::class)
    fun append(values: List<Value>) {
        if (!nextPageKey.pagingEndReached) throw IllegalStateException("Cannot insert element to not ended list")
        list.addAll(values)
        totalItems = totalItems?.plus(values.size)
        nextPageKey.key += values.size
        collector(Action.APPEND)
    }

    fun prepend(values: List<Value>, increase: Boolean = false) {
        if (loading.value == LoadingState.Loading || prependLoading.value == LoadingState.Loading || appendLoading.value == LoadingState.Loading) {
            if (increase) {
                synchronized(previousPageKey) {
                    previousPageKey.key += values.size
                }
                synchronized(nextPageKey) {
                    nextPageKey.key += values.size
                }
            }
            throw IllegalStateException("Cannot insert element to loading list")
        }
        if (!previousPageKey.pagingEndReached) throw IllegalStateException("Cannot insert element to not ended list")
        list.addAll(0, values)
        nextPageKey.key += values.size
        totalItems = totalItems?.plus(values.size)
        collector(Action.PREPEND)
    }

    fun replaceAll(operator: UnaryOperator<Value>) = list.replaceAll(operator)

    fun replace(index: Int, element: Value) = list.set(index, element)

    fun removeIf(filter: Predicate<Value>) = list.removeIf(filter)

    fun refresh(): Boolean {
        return refresh(0/*(currentKey.intValue - config.initialPageSize / 2).coerceAtLeast(0)*/) != null
    }

    private fun refresh(key: Int): Promise<LoadResult<Value>>? {
        if (loading.value == LoadingState.Loading) return null
        version += 1U
        loading.value = LoadingState.Loading
        list.clear()
        previousPageKey = PagingKey(key, false)
        nextPageKey = PagingKey(key, false)
        return doLoad(
            key, config.initialPageSize, Action.REFRESH
        ).then {
            currentKey.intValue = key
        }
    }

    fun hasKey(key: Int): Boolean {
        return key >= firstKey && key <= firstKey + list.size
    }

    fun currentPosition(): Int {
        return currentKey.intValue - firstKey
    }

    fun goTo(key: Int, onSuccess: (() -> Unit)? = null): Boolean {
        if (hasKey(key)) return false
        refresh(key)?.then(mainExecutor) {
            onSuccess?.invoke()
        }
        return true
    }

    private fun onSuccesLoad(action: Action) {
        collector(action)
        loadListener?.onSuccess()
    }

    private fun onErrorLoad(action: Action, error: Throwable) {
        when (action) {
            Action.PREPEND -> {
                prependLoading.value = LoadingState.Error(error)
            }

            Action.APPEND -> {
                appendLoading.value = LoadingState.Error(error)
            }

            Action.REFRESH -> {
                loading.value = LoadingState.Error(error)
            }
        }
        loadListener?.onError(error)
    }

    private fun collector(action: Action) {
        if (config.maxSize > 0 && config.maxSize > config.pageSize && list.size > config.maxSize && currentKey.intValue > 0) {
            val itemsToDelete = list.size - config.maxSize
            val currentIndex = currentKey.intValue - firstKey
            when (action) {
                Action.PREPEND -> {
                    val min =
                        (list.size - itemsToDelete).coerceAtLeast(currentIndex + config.pageSize)
                            .coerceAtMost(list.size - 1)
                    val size = list.size - min
                    list.removeRange(min, list.size)
                    nextPageKey.key = nextPageKey.key.minus(size).coerceAtLeast(firstKey)
                    nextPageKey.pagingEndReached = false
                }

                Action.APPEND -> {
                    val max =
                        itemsToDelete.coerceAtMost(currentIndex - config.pageSize).coerceAtLeast(0)
                    list.removeRange(0, max)
                    previousPageKey.key =
                        previousPageKey.key.plus(max).coerceAtMost(nextPageKey.key)
                    previousPageKey.pagingEndReached = false
                }

                Action.REFRESH -> {

                }
            }
        }
    }

    private inner class PagingKey(
        var key: Int, var pagingEndReached: Boolean
    )

    sealed class LoadResult<Value> {
        class Success<Value>(
            val total: Long,
            val items: List<Value>
        ) : LoadResult<Value>()

        class Error<Value>(
            val message: Throwable
        ) : LoadResult<Value>()
    }

    data class Item<Value>(
        val id: UUID = UUID.randomUUID(),
        val value: Value
    )

    data class PagingListConfig(
        val initialIndex: Int = 0,
        val initialPageSize: Int = 20,
        val pageSize: Int = 10,
        val prefetchDistance: Int = 3,
        val defaultRefreshOnStart: Boolean = true,
        val maxSize: Int = Int.MAX_VALUE
    )

    sealed class LoadingState(
        val error: Throwable?
    ) {
        data object NotLoading : LoadingState(null)
        data object Loading : LoadingState(null)
        class Error(error: Throwable) : LoadingState(error) {
            override fun equals(other: Any?): Boolean {
                return other is Error && other.error == this.error
            }

            override fun hashCode(): Int {
                return error.hashCode()
            }
        }
    }

    interface PagingLoadListener {
        fun onSuccess()
        fun onError(error: Throwable)
    }
}