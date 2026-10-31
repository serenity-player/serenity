package io.github.serenity_player.serenity.library

import io.github.serenity_player.serenity.data.repository.PAGE_SIZE
import io.github.serenity_player.serenity.data.repository.PageResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
    val initialized: Boolean = false,
)

/** Loads a long list one page at a time, by offset or by cursor ([fetch] gets both). */
class Pager<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = PAGE_SIZE,
    private val fetch: suspend (offset: Int, after: String?) -> PageResult<T>,
) {
    private val _state = MutableStateFlow(ListState<T>())
    val state: StateFlow<ListState<T>> = _state.asStateFlow()

    private var offset = 0
    private var after: String? = null
    private var job: Job? = null

    /** Safe to call repeatedly: does nothing while loading or at the end. Also serves as retry. */
    fun loadNext() {
        val s = _state.value
        if (s.loading || s.endReached) return
        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch {
            when (val r = fetch(offset, after)) {
                is PageResult.Success -> {
                    offset += pageSize
                    after = r.after
                    _state.update {
                        it.copy(items = it.items + r.items, loading = false, endReached = !r.hasNext, initialized = true)
                    }
                }
                is PageResult.Failure -> _state.update {
                    it.copy(loading = false, error = r.message, initialized = true)
                }
            }
        }
    }

    /** Starts the list from an already-fetched first page (used by search's combined request). */
    fun seed(page: PageResult.Success<T>) {
        job?.cancel()
        offset = pageSize
        after = page.after
        _state.value = ListState(items = page.items, endReached = !page.hasNext, initialized = true)
    }

    fun reset() {
        job?.cancel()
        offset = 0
        after = null
        _state.value = ListState()
    }

    fun refresh() {
        reset()
        loadNext()
    }
}
