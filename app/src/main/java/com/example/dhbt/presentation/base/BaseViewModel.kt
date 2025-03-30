package com.example.dhbt.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : UiState, E : UiEvent> : ViewModel() {

    private val initialState: S by lazy { createInitialState() }

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _event = Channel<E>()
    val event = _event.receiveAsFlow()

    abstract fun createInitialState(): S

    protected fun setState(reduce: S.() -> S) {
        val newState = state.value.reduce()
        _state.value = newState
    }

    fun sendEvent(event: E) {
        viewModelScope.launch {
            _event.send(event)
        }
    }
}

interface UiState
interface UiEvent