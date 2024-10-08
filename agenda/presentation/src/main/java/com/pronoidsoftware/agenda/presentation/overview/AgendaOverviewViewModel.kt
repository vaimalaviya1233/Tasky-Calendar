package com.pronoidsoftware.agenda.presentation.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronoidsoftware.agenda.presentation.overview.mappers.toEventUi
import com.pronoidsoftware.agenda.presentation.overview.mappers.toReminderUi
import com.pronoidsoftware.agenda.presentation.overview.mappers.toTaskUi
import com.pronoidsoftware.core.domain.SessionStorage
import com.pronoidsoftware.core.domain.agendaitem.AgendaItem
import com.pronoidsoftware.core.domain.agendaitem.AgendaItemType
import com.pronoidsoftware.core.domain.agendaitem.AgendaRepository
import com.pronoidsoftware.core.domain.util.initializeAndCapitalize
import com.pronoidsoftware.core.domain.util.today
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

@HiltViewModel
class AgendaOverviewViewModel @Inject constructor(
    sessionStorage: SessionStorage,
    clock: Clock,
    private val agendaRepository: AgendaRepository,
) : ViewModel() {

    var state by mutableStateOf(AgendaOverviewState(selectedDate = today(clock)))
        private set

    private val eventChannel = Channel<AgendaOverviewEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        agendaRepository.getAllAgendaItems().onEach { agendaItems ->
            val items = agendaItems.map { agendaItem ->
                when (agendaItem) {
                    is AgendaItem.Event -> agendaItem.toEventUi()
                    is AgendaItem.Reminder -> agendaItem.toReminderUi()
                    is AgendaItem.Task -> agendaItem.toTaskUi()
                }
            }
            state = state.copy(items = items)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            state = state.copy(
                userInitials = sessionStorage.get()?.fullName?.initializeAndCapitalize()
                    ?: error("User initials not available. Has the user logged out?"),
            )
            agendaRepository.fetchAllAgendaItems()
        }
    }

    fun onAction(action: AgendaOverviewAction) {
        when (action) {
            is AgendaOverviewAction.OnSelectDate -> {
                state = state.copy(
                    selectedDate = action.date,
                )
            }

            AgendaOverviewAction.OnToggleProfileDropdownMenu -> {
                state = state.copy(
                    profileDropdownMenuExpanded = !state.profileDropdownMenuExpanded,
                )
            }

            AgendaOverviewAction.OnToggleDatePickerExpanded -> {
                state = state.copy(
                    datePickerExpanded = !state.datePickerExpanded,
                )
            }

            AgendaOverviewAction.OnToggleFABDropdownMenuExpanded -> {
                state = state.copy(
                    fabDropdownMenuExpanded = !state.fabDropdownMenuExpanded,
                )
            }

            is AgendaOverviewAction.OnDeleteClick -> {
                viewModelScope.launch {
                    when (action.type) {
                        AgendaItemType.EVENT -> agendaRepository.deleteEvent(action.id)
                        AgendaItemType.TASK -> agendaRepository.deleteTask(action.id)
                        AgendaItemType.REMINDER -> agendaRepository.deleteReminder(action.id)
                    }
                }
            }

            else -> {
                Timber.wtf("Unknown AgendaOverviewAction in VM")
            }
        }
    }
}
