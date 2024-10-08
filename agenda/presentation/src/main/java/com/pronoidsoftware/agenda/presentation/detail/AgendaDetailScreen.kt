@file:OptIn(ExperimentalMaterial3Api::class)

package com.pronoidsoftware.agenda.presentation.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.pronoidsoftware.agenda.presentation.R
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailActionText
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailDescription
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailNotification
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailTime
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailTitle
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailToolbar
import com.pronoidsoftware.agenda.presentation.detail.components.AgendaDetailType
import com.pronoidsoftware.agenda.presentation.detail.components.edittext.AgendaDetailEditTextScreen
import com.pronoidsoftware.agenda.presentation.detail.components.edittext.EditTextType
import com.pronoidsoftware.agenda.presentation.detail.components.event.photo.components.EventDetailPhotoDetail
import com.pronoidsoftware.agenda.presentation.detail.components.event.photo.components.EventDetailPhotos
import com.pronoidsoftware.agenda.presentation.detail.components.event.visitor.components.AddVisitorDialog
import com.pronoidsoftware.agenda.presentation.detail.components.event.visitor.components.EventDetailVisitorList
import com.pronoidsoftware.agenda.presentation.detail.components.event.visitor.model.toVisitorUi
import com.pronoidsoftware.core.domain.agendaitem.AgendaItemType
import com.pronoidsoftware.core.domain.agendaitem.Photo
import com.pronoidsoftware.core.presentation.designsystem.LocalClock
import com.pronoidsoftware.core.presentation.designsystem.LocalSpacing
import com.pronoidsoftware.core.presentation.designsystem.TaskyTheme
import com.pronoidsoftware.core.presentation.designsystem.TaskyWhite2
import com.pronoidsoftware.core.presentation.designsystem.components.TaskyDialog
import com.pronoidsoftware.core.presentation.designsystem.components.TaskyScaffold
import com.pronoidsoftware.core.presentation.designsystem.util.ignoreColumnPadding
import com.pronoidsoftware.core.presentation.ui.ObserveAsEvents
import com.pronoidsoftware.core.presentation.ui.formatFullDate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Composable
fun AgendaDetailScreenRoot(
    onCloseClick: () -> Unit,
    viewModel: AgendaDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is AgendaDetailEvent.OnError -> {
                Toast.makeText(
                    context,
                    event.error.asString(context),
                    Toast.LENGTH_LONG,
                ).show()
            }

            AgendaDetailEvent.OnDeleted -> {
                Toast.makeText(
                    context,
                    R.string.deleted,
                    Toast.LENGTH_LONG,
                ).show()
                onCloseClick()
            }

            AgendaDetailEvent.OnSaved -> {
                Toast.makeText(
                    context,
                    R.string.saved,
                    Toast.LENGTH_LONG,
                ).show()
            }

            AgendaDetailEvent.OnClosed -> {
                onCloseClick()
            }
        }
    }

    AgendaDetailScreen(
        state = viewModel.state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun AgendaDetailScreen(state: AgendaDetailState, onAction: (AgendaDetailAction) -> Unit) {
    val spacing = LocalSpacing.current
    val clock = LocalClock.current
    val dividerColor = TaskyWhite2
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                onAction(AgendaDetailAction.OnAddPhotoClick(Photo.Local(uri.toString())))
            }
        },
    )

    if (state.isShowingCloseConfirmationDialog) {
        val onCancelAction = when {
            state.isEditingTitle -> AgendaDetailAction.OnCancelCloseTitle
            state.isEditingDescription -> AgendaDetailAction.OnCancelCloseDescription
            else -> AgendaDetailAction.OnCancelClose
        }
        val onConfirmAction = when {
            state.isEditingTitle -> AgendaDetailAction.OnConfirmCloseTitle
            state.isEditingDescription -> AgendaDetailAction.OnConfirmCloseDescription
            else -> AgendaDetailAction.OnConfirmClose
        }
        TaskyDialog(
            title = stringResource(id = R.string.close_dialog_title),
            description = stringResource(id = R.string.confirm_close),
            onCancel = { onAction(onCancelAction) },
            onConfirm = { onAction(onConfirmAction) },
        )
    }

    if (state.isEditingTitle) {
        AgendaDetailEditTextScreen(
            type = EditTextType.Title,
            value = state.title,
            onBackClick = {
                onAction(AgendaDetailAction.OnCloseTitle)
            },
            onSaveClick = { newTitle ->
                onAction(AgendaDetailAction.OnSaveTitle(newTitle))
            },
        )
    } else if (state.isEditingDescription) {
        AgendaDetailEditTextScreen(
            type = EditTextType.Description,
            value = state.description ?: "",
            onBackClick = {
                onAction(AgendaDetailAction.OnCloseDescription)
            },
            onSaveClick = { newDescription ->
                onAction(AgendaDetailAction.OnSaveDescription(newDescription))
            },
        )
    } else if (getDetailAsEvent(state)?.selectedPhotoToView != null) {
        getDetailAsEvent(state)?.selectedPhotoToView?.let { photo ->
            EventDetailPhotoDetail(
                photo = photo,
                editEnabled = state.isEditing,
                onCloseClick = {
                    onAction(AgendaDetailAction.OnClosePhotoClick)
                },
                onDeleteClick = {
                    onAction(AgendaDetailAction.OnDeletePhotoClick(photo))
                },
            )
        }
    } else {
        BackHandler(enabled = state.isEditing && !state.isShowingCloseConfirmationDialog) {
            onAction(AgendaDetailAction.OnClose)
        }

        if (state.isShowingDeleteConfirmationDialog) {
            TaskyDialog(
                title = stringResource(id = R.string.delete_dialog_title),
                description = stringResource(id = R.string.confirm_deletion),
                onCancel = { onAction(AgendaDetailAction.OnCancelDelete) },
                onConfirm = { onAction(AgendaDetailAction.OnConfirmDelete) },
            )
        }
        if (getDetailAsEvent(state)?.isShowingAddVisitorDialog == true) {
            getDetailAsEvent(state)?.let { eventDetails ->
                AddVisitorDialog(
                    title = stringResource(id = R.string.add_visitor),
                    buttonText = stringResource(id = R.string.add),
                    onAddClick = { email ->
                        onAction(AgendaDetailAction.OnAddVisitorClick(email))
                    },
                    onCancel = {
                        onAction(AgendaDetailAction.OnToggleAddVisitorDialog)
                    },
                    isAddingAttendee = eventDetails.isAddingVisitor,
                    emailTextFieldState = eventDetails.visitorToAddEmail,
                    isEmailValid = eventDetails.isVisitorToAddEmailValid,
                    errorMessage = eventDetails.addVisitorErrorMessage,
                )
            }
        }
        TaskyScaffold(
            topAppBar = {
                AgendaDetailToolbar(
                    title = if (state.isEditing) {
                        stringResource(
                            id = R.string.edit_agenda_item,
                            getTypeString(type = state.agendaItemType, isUppercase = true),
                        )
                    } else {
                        state.selectedDate.formatFullDate(clock)
                    },
                    onCloseClick = {
                        if (state.isEditing) {
                            onAction(AgendaDetailAction.OnClose)
                        } else {
                            onAction(AgendaDetailAction.OnConfirmClose)
                        }
                    },
                    isEditing = state.isEditing,
                    onEditClick = { onAction(AgendaDetailAction.OnEnableEdit) },
                    onSaveClick = { onAction(AgendaDetailAction.OnSave) },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .clip(
                        RoundedCornerShape(
                            topStart = spacing.scaffoldContainerRadius,
                            topEnd = spacing.scaffoldContainerRadius,
                        ),
                    )
                    .background(
                        MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(
                            topStart = spacing.scaffoldContainerRadius,
                            topEnd = spacing.scaffoldContainerRadius,
                        ),
                    )
                    .padding(top = spacing.scaffoldPaddingTop)
                    .padding(horizontal = spacing.spaceMedium),
            ) {
                state.agendaItemType?.let { type ->
                    AgendaDetailType(
                        type = type,
                    )
                }
                Spacer(modifier = Modifier.height(spacing.agendaDetailSpaceMedium))
                AgendaDetailTitle(
                    title = state.title.ifEmpty {
                        stringResource(
                            id = R.string.new_agenda_item,
                            getTypeString(type = state.agendaItemType),
                        )
                    },
                    editEnabled = state.isEditing,
                    onEdit = {
                        onAction(AgendaDetailAction.OnEditTitle)
                    },
                    isCompleted = getDetailAsTask(state)?.completed == true,
                )
                Spacer(modifier = Modifier.height(spacing.spaceSmallMedium))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(spacing.spaceSmall))
                AgendaDetailDescription(
                    description = state.description,
                    editEnabled = state.isEditing,
                    onEdit = {
                        onAction(AgendaDetailAction.OnEditDescription)
                    },
                )
                Spacer(modifier = Modifier.height(spacing.spaceSmall))
                getDetailAsEvent(state)?.let { eventDetails ->
                    EventDetailPhotos(
                        photos = eventDetails.photos,
                        arePhotosFull = eventDetails.arePhotosFull,
                        editEnabled = state.isEditing,
                        onAddClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        onOpenClick = { photo ->
                            onAction(AgendaDetailAction.OnOpenPhotoClick(photo))
                        },
                        modifier = Modifier
                            .ignoreColumnPadding(spacing.spaceMedium),
                    )
                    Spacer(modifier = Modifier.height(spacing.scaffoldPaddingTop))
                }
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(spacing.agendaDetailSpaceMediumSmall))
                AgendaDetailTime(
                    timeDescription = if (state.agendaItemType == AgendaItemType.EVENT) {
                        stringResource(id = R.string.from)
                    } else {
                        stringResource(id = R.string.at)
                    },
                    localDateTime = state.startDateTime,
                    editEnabled = state.isEditing,
                    onSelectTime = { time ->
                        onAction(AgendaDetailAction.OnSelectStartTime(time))
                    },
                    timePickerExpanded = state.isEditingStartTime,
                    toggleTimePickerExpanded = {
                        onAction(AgendaDetailAction.OnToggleStartTimePickerExpanded)
                    },
                    onSelectDate = { date ->
                        onAction(AgendaDetailAction.OnSelectStartDate(date))
                    },
                    datePickerExpanded = state.isEditingStartDate,
                    toggleDatePickerExpanded = {
                        onAction(AgendaDetailAction.OnToggleStartDatePickerExpanded)
                    },
                    clock = clock,
                )
                Spacer(modifier = Modifier.height(spacing.spaceMedium))
                HorizontalDivider(color = dividerColor)
                getDetailAsEvent(state)?.let { eventDetails ->
                    AgendaDetailTime(
                        timeDescription = stringResource(id = R.string.to),
                        localDateTime = eventDetails.endDateTime,
                        editEnabled = state.isEditing,
                        onSelectTime = { time ->
                            onAction(AgendaDetailAction.OnSelectEndTime(time))
                        },
                        timePickerExpanded = eventDetails.isEditingEndTime,
                        toggleTimePickerExpanded = {
                            onAction(AgendaDetailAction.OnToggleEndTimePickerExpanded)
                        },
                        onSelectDate = { date ->
                            onAction(AgendaDetailAction.OnSelectEndDate(date))
                        },
                        datePickerExpanded = eventDetails.isEditingEndDate,
                        toggleDatePickerExpanded = {
                            onAction(AgendaDetailAction.OnToggleEndDatePickerExpanded)
                        },
                        clock = clock,
                    )
                    Spacer(modifier = Modifier.height(spacing.spaceMedium))
                    HorizontalDivider(color = dividerColor)
                }
                Spacer(modifier = Modifier.height(spacing.agendaDetailNotificationPaddingTop))
                AgendaDetailNotification(
                    notificationDescription = state.notificationDuration.text.asString(),
                    editEnabled = state.isEditing,
                    expanded = state.isEditingNotificationDuration,
                    toggleExpanded = {
                        onAction(AgendaDetailAction.OnToggleNotificationDurationExpanded)
                    },
                    onSelectNotificationDuration = { notificationDuration ->
                        onAction(
                            AgendaDetailAction.OnSelectNotificationDuration(notificationDuration),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(spacing.spaceSmall))
                HorizontalDivider(color = dividerColor)
                getDetailAsEvent(state)?.let { eventDetails ->
                    Spacer(modifier = Modifier.height(spacing.scaffoldPaddingTop))
                    EventDetailVisitorList(
                        onAllClick = {
                            onAction(AgendaDetailAction.OnAllVisitorsClick)
                        },
                        onGoingClick = {
                            onAction(AgendaDetailAction.OnGoingVisitorsClick)
                        },
                        onNotGoingClick = {
                            onAction(AgendaDetailAction.OnNotGoingVisitorsClick)
                        },
                        onAddVisitorClick = {
                            onAction(AgendaDetailAction.OnToggleAddVisitorDialog)
                        },
                        onDeleteVisitorClick = { visitor ->
                            onAction(AgendaDetailAction.OnDeleteVisitorClick(visitor))
                        },
                        selectedFilterType = eventDetails.selectedVisitorFilter,
                        goingVisitors = eventDetails.attendees
                            .filter { it.isGoing }
                            .map {
                                it.toVisitorUi(
                                    eventDetails.isUserEventCreator &&
                                        it.userId == eventDetails.host,
                                )
                            },
                        notGoingVisitors = eventDetails.attendees
                            .filterNot { it.isGoing }
                            .map {
                                it.toVisitorUi(
                                    eventDetails.isUserEventCreator &&
                                        it.userId == eventDetails.host,
                                )
                            },
                        editEnabled = state.isEditing,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (state.agendaItemType == AgendaItemType.EVENT) {
                    Spacer(modifier = Modifier.height(spacing.agendaDetailSpaceBottom))
                } else {
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(spacing.agendaDetailSpaceMediumSmall))
                }
                AgendaDetailActionText(
                    enabled = true,
                    text = stringResource(
                        id = R.string.delete_agenda_item,
                        getTypeString(type = state.agendaItemType, isUppercase = true),
                    ),
                    onClick = {
                        onAction(AgendaDetailAction.OnDelete)
                    },
                )
                Spacer(modifier = Modifier.height(spacing.agendaDetailSpaceBottom))
            }
        }
    }
}

@Composable
private fun getTypeString(type: AgendaItemType?, isUppercase: Boolean = false): String {
    val result = when (type) {
        AgendaItemType.EVENT -> stringResource(id = R.string.event)
        AgendaItemType.TASK -> stringResource(id = R.string.task)
        AgendaItemType.REMINDER -> stringResource(id = R.string.reminder)
        null -> ""
    }
    return if (isUppercase) {
        result.uppercase()
    } else {
        result
    }
}

private fun getDetailAsEvent(state: AgendaDetailState): AgendaItemDetails.Event? {
    return (state.typeSpecificDetails as? AgendaItemDetails.Event)
}

private fun getDetailAsTask(state: AgendaDetailState): AgendaItemDetails.Task? {
    return (state.typeSpecificDetails as? AgendaItemDetails.Task)
}

private fun getDetailAsReminder(state: AgendaDetailState): AgendaItemDetails.Reminder? {
    return (state.typeSpecificDetails as? AgendaItemDetails.Reminder)
}

@Preview
@Composable
private fun ReminderDetailScreenPreview() {
    TaskyTheme {
        CompositionLocalProvider(LocalClock provides Clock.System) {
            AgendaDetailScreen(
                state = AgendaDetailState(
                    agendaItemType = AgendaItemType.REMINDER,
                    title = "Project X",
                    description = "Weekly plan\nRole distribution",
                    selectedDate = LocalDate(2022, 3, 1),
                    startDateTime = LocalDateTime(2022, 7, 21, 8, 0),
                    isEditing = false,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun ReminderDetailScreenPreview_EditTitle() {
    TaskyTheme {
        CompositionLocalProvider(LocalClock provides Clock.System) {
            AgendaDetailScreen(
                state = AgendaDetailState(
                    agendaItemType = AgendaItemType.REMINDER,
                    title = "Project X",
                    description = "Weekly plan\nRole distribution",
                    selectedDate = LocalDate(2022, 3, 1),
                    startDateTime = LocalDateTime(2022, 7, 21, 8, 0),
                    isEditing = true,
                    isEditingTitle = true,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun ReminderDetailScreenPreview_EditDescription() {
    TaskyTheme {
        CompositionLocalProvider(LocalClock provides Clock.System) {
            AgendaDetailScreen(
                state = AgendaDetailState(
                    agendaItemType = AgendaItemType.REMINDER,
                    title = "Project X",
                    description = "Amet minim mollit non deserunt ullamco " +
                        "est sit aliqua dolor do amet sint. ",
                    selectedDate = LocalDate(2022, 3, 1),
                    startDateTime = LocalDateTime(2022, 7, 21, 8, 0),
                    isEditing = true,
                    isEditingDescription = true,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun ReminderDetailScreenPreview_DeleteDialog() {
    TaskyTheme {
        CompositionLocalProvider(LocalClock provides Clock.System) {
            AgendaDetailScreen(
                state = AgendaDetailState(
                    agendaItemType = AgendaItemType.REMINDER,
                    title = "Project X",
                    description = "Amet minim mollit non deserunt ullamco " +
                        "est sit aliqua dolor do amet sint. ",
                    selectedDate = LocalDate(2022, 3, 1),
                    startDateTime = LocalDateTime(2022, 7, 21, 8, 0),
                    isEditing = true,
                    isShowingDeleteConfirmationDialog = true,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun ReminderDetailScreenPreview_CloseDialog() {
    TaskyTheme {
        CompositionLocalProvider(LocalClock provides Clock.System) {
            AgendaDetailScreen(
                state = AgendaDetailState(
                    agendaItemType = AgendaItemType.REMINDER,
                    title = "Project X",
                    description = "Amet minim mollit non deserunt ullamco " +
                        "est sit aliqua dolor do amet sint. ",
                    selectedDate = LocalDate(2022, 3, 1),
                    startDateTime = LocalDateTime(2022, 7, 21, 8, 0),
                    isEditing = true,
                    isShowingCloseConfirmationDialog = true,
                ),
                onAction = {},
            )
        }
    }
}
