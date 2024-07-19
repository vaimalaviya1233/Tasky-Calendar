package com.pronoidsoftware.agenda.presentation.detail.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import timber.log.Timber

@HiltViewModel
class ReminderDetailViewModel @Inject constructor(
    clock: Clock,
) : ViewModel() {

    var state by mutableStateOf(
        ReminderDetailState(
            clock = clock,
            title = "Project X",
            description = "Weekly plan\nRole distribution",
        ),
    )
        private set

    private val eventChannel = Channel<ReminderDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: ReminderDetailAction) {
        when (action) {
            ReminderDetailAction.OnEnableEdit -> {
                state = state.copy(
                    isEditing = true,
                )
            }

            ReminderDetailAction.OnSave -> {
                viewModelScope.launch {
                    state = state.copy(
                        isEditing = false,
                    )
                    eventChannel.send(ReminderDetailEvent.OnSaved)
                }
            }

            ReminderDetailAction.OnEditTitle -> {
                state = state.copy(
                    isEditingTitle = true,
                )
            }

            ReminderDetailAction.OnCloseTitle -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = true,
                )
            }

            ReminderDetailAction.OnConfirmCloseTitle -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                    isEditingTitle = false,
                )
            }

            ReminderDetailAction.OnCancelCloseTitle -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                )
            }

            is ReminderDetailAction.OnSaveTitle -> {
                state = state.copy(
                    isEditingTitle = false,
                    title = action.newTitle,
                )
            }

            ReminderDetailAction.OnEditDescription -> {
                state = state.copy(
                    isEditingDescription = true,
                )
            }

            ReminderDetailAction.OnCloseDescription -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = true,
                )
            }

            ReminderDetailAction.OnConfirmCloseDescription -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                    isEditingDescription = false,
                )
            }

            ReminderDetailAction.OnCancelCloseDescription -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                )
            }

            is ReminderDetailAction.OnSaveDescription -> {
                state = state.copy(
                    isEditingDescription = false,
                    description = action.newDescription,
                )
            }

            is ReminderDetailAction.OnSelectDate -> {
                val date = action.date
                state = state.copy(
                    atTime = LocalDateTime(
                        date.year,
                        date.month,
                        date.dayOfMonth,
                        state.atTime.hour,
                        state.atTime.minute,
                    ),
                )
            }

            is ReminderDetailAction.OnSelectTime -> {
                val time = action.time
                state = state.copy(
                    atTime = LocalDateTime(
                        state.atTime.year,
                        state.atTime.month,
                        state.atTime.dayOfMonth,
                        time.hour,
                        time.minute,
                    ),
                )
            }

            ReminderDetailAction.OnToggleTimePickerExpanded -> {
                state = state.copy(
                    isEditingTime = !state.isEditingTime,
                )
            }

            ReminderDetailAction.OnToggleDatePickerExpanded -> {
                state = state.copy(
                    isEditingDate = !state.isEditingDate,
                )
            }

            ReminderDetailAction.OnToggleNotificationDurationExpanded -> {
                state = state.copy(
                    isEditingNotificationDuration = !state.isEditingNotificationDuration,
                )
            }

            is ReminderDetailAction.OnSelectNotificationDuration -> {
                state = state.copy(
                    notificationDuration = action.notificationDuration,
                )
            }

            ReminderDetailAction.OnDelete -> {
                state = state.copy(
                    isShowingDeleteConfirmationDialog = true,
                )
            }

            ReminderDetailAction.OnConfirmDelete -> {
                state = state.copy(
                    isShowingDeleteConfirmationDialog = false,
                )
                viewModelScope.launch {
                    eventChannel.send(ReminderDetailEvent.OnDeleted)
                }
            }

            ReminderDetailAction.OnCancelDelete -> {
                state = state.copy(
                    isShowingDeleteConfirmationDialog = false,
                )
            }

            ReminderDetailAction.OnClose -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = true,
                )
            }

            ReminderDetailAction.OnConfirmClose -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                )
                viewModelScope.launch {
                    eventChannel.send(ReminderDetailEvent.OnClosed)
                }
            }

            ReminderDetailAction.OnCancelClose -> {
                state = state.copy(
                    isShowingCloseConfirmationDialog = false,
                )
            }

            else -> {
                Timber.wtf("Unknown ReminderDetailAction in VM")
            }
        }
    }
}
