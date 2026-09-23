package com.kumpello.whereiseveryone.common.presentation

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `populated view state maps on worker while side effects and reducers stay on Main`() = runTest {
        Executors.newSingleThreadExecutor { Thread(it, "view-state-worker") }.asCoroutineDispatcher().use { worker ->
            val mainThread = Thread.currentThread()
            val viewModel = TestViewModel(worker) {
                assertSame(mainThread, Thread.currentThread())
                1
            }
            val store = ViewModelStore().apply { put("test", viewModel) }
            try {
                // Trigger before first observation to cover lazy StateFlow initialization too.
                viewModel.trigger(Event.Load)
                viewModel.state.test {
                    assertEquals(0, awaitItem())
                    assertEquals(1, awaitItem())
                }
                assertEquals(listOf(mainThread, mainThread), viewModel.reducerThreads)
                assertEquals(listOf("view-state-worker"), viewModel.mappingThreads.map { it.name.substringBefore(" @") })
            } finally {
                store.clear()
            }
        }
    }

    @Test
    fun `cancelled async work is not converted to a global error or result event`() = runTest {
        val viewModel = TestViewModel(mainDispatcherRule.testDispatcher) {
            throw CancellationException("ViewModel cleared")
        }
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            viewModel.trigger(Event.Load)
            runCurrent()
            assertTrue(viewModel.errors.isEmpty())
            assertEquals(1, viewModel.reducerThreads.size)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `background events are reduced on Main without losing state updates`() = runTest {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { worker ->
            val mainThread = Thread.currentThread()
            val viewModel = TestViewModel(mainDispatcherRule.testDispatcher) { 1 }
            val store = ViewModelStore().apply { put("test", viewModel) }
            try {
                viewModel.state.test {
                    assertEquals(0, awaitItem())
                    withContext(worker) {
                        repeat(100) { viewModel.trigger(Event.Increment) }
                    }
                    runCurrent()
                    assertEquals(100, expectMostRecentItem())
                }
                assertEquals(100, viewModel.reducerThreads.size)
                assertTrue(viewModel.reducerThreads.all { it === mainThread })
            } finally {
                store.clear()
            }
        }
    }

    private sealed interface Event {
        data object Load : Event
        data object Increment : Event
        data class Loaded(val value: Int) : Event
    }

    private class TestViewModel(dispatcher: CoroutineDispatcher, val work: suspend () -> Int) :
        BaseViewModel<Int, Int, Event, Unit>(0, dispatcher) {
        val reducerThreads = mutableListOf<Thread>()
        val mappingThreads = mutableListOf<Thread>()
        val errors = mutableListOf<Exception>()

        override fun reduce(state: Int, event: Event): ReducerResult<Int, Event, Unit> {
            reducerThreads += Thread.currentThread()
            return when (event) {
                Event.Load -> state.toResult(SideEffect.AsyncWork { Event.Loaded(work()) })
                Event.Increment -> (state + 1).toResult()
                is Event.Loaded -> event.value.toResult()
            }
        }

        override fun Int.toViewState(): Int {
            if (this != 0) mappingThreads += Thread.currentThread()
            return this
        }

        override fun handleGlobalError(e: Exception) {
            errors += e
        }
    }
}
