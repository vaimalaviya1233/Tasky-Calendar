package com.pronoidsoftware.agenda.network

import com.pronoidsoftware.agenda.network.dto.AgendaDto
import com.pronoidsoftware.agenda.network.dto.EventDto
import com.pronoidsoftware.agenda.network.dto.ReminderDto
import com.pronoidsoftware.agenda.network.dto.TaskDto
import com.pronoidsoftware.agenda.network.mappers.toCreateEventRequest
import com.pronoidsoftware.agenda.network.mappers.toEvent
import com.pronoidsoftware.agenda.network.mappers.toReminder
import com.pronoidsoftware.agenda.network.mappers.toTask
import com.pronoidsoftware.agenda.network.mappers.toUpdateEventRequest
import com.pronoidsoftware.agenda.network.mappers.toUpsertReminderRequest
import com.pronoidsoftware.agenda.network.mappers.toUpsertTaskRequest
import com.pronoidsoftware.core.data.networking.AgendaRoutes
import com.pronoidsoftware.core.data.networking.delete
import com.pronoidsoftware.core.data.networking.get
import com.pronoidsoftware.core.data.networking.post
import com.pronoidsoftware.core.data.networking.postMultipart
import com.pronoidsoftware.core.data.networking.put
import com.pronoidsoftware.core.data.networking.putMultipart
import com.pronoidsoftware.core.domain.SessionStorage
import com.pronoidsoftware.core.domain.agendaitem.AgendaItem
import com.pronoidsoftware.core.domain.agendaitem.Photo
import com.pronoidsoftware.core.domain.agendaitem.RemoteAgendaDataSource
import com.pronoidsoftware.core.domain.util.DataError
import com.pronoidsoftware.core.domain.util.EmptyResult
import com.pronoidsoftware.core.domain.util.Result
import com.pronoidsoftware.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.net.URL
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KtorRemoteAgendaDataSource @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
) : RemoteAgendaDataSource {

    // Reminders
    override suspend fun createReminder(
        reminder: AgendaItem.Reminder,
    ): EmptyResult<DataError.Network> {
        return httpClient.post(
            route = AgendaRoutes.REMINDER,
            body = reminder.toUpsertReminderRequest(),
        )
    }

    override suspend fun getReminder(id: String): Result<AgendaItem.Reminder, DataError.Network> {
        return httpClient.get<ReminderDto>(
            route = AgendaRoutes.REMINDER,
            queryParameters = mapOf(
                REMINDER_ID_QUERY_PARAM to id,
            ),
        ).map { it.toReminder() }
    }

    override suspend fun getAllReminders(): Result<List<AgendaItem.Reminder>, DataError.Network> {
        return httpClient.get<AgendaDto>(
            route = AgendaRoutes.FULL_AGENDA,
        )
            .map { agendaDto ->
                agendaDto.reminders.map { it.toReminder() }
            }
    }

    override suspend fun updateReminder(
        reminder: AgendaItem.Reminder,
    ): EmptyResult<DataError.Network> {
        return httpClient.put(
            route = AgendaRoutes.REMINDER,
            body = reminder.toUpsertReminderRequest(),
        )
    }

    override suspend fun deleteReminder(id: String): EmptyResult<DataError.Network> {
        return httpClient.delete(
            route = AgendaRoutes.REMINDER,
            queryParameters = mapOf(
                REMINDER_ID_QUERY_PARAM to id,
            ),
        )
    }

    // Tasks
    override suspend fun createTask(task: AgendaItem.Task): EmptyResult<DataError.Network> {
        return httpClient.post(
            route = AgendaRoutes.TASK,
            body = task.toUpsertTaskRequest(),
        )
    }

    override suspend fun getTask(id: String): Result<AgendaItem.Task, DataError.Network> {
        return httpClient.get<TaskDto>(
            route = AgendaRoutes.TASK,
            queryParameters = mapOf(
                TASK_ID_QUERY_PARAM to id,
            ),
        ).map { it.toTask() }
    }

    override suspend fun getAllTasks(): Result<List<AgendaItem.Task>, DataError.Network> {
        return httpClient.get<AgendaDto>(
            route = AgendaRoutes.FULL_AGENDA,
        )
            .map { agendaDto ->
                agendaDto.tasks.map { it.toTask() }
            }
    }

    override suspend fun updateTask(task: AgendaItem.Task): EmptyResult<DataError.Network> {
        return httpClient.put(
            route = AgendaRoutes.TASK,
            body = task.toUpsertTaskRequest(),
        )
    }

    override suspend fun deleteTask(id: String): EmptyResult<DataError.Network> {
        return httpClient.delete(
            route = AgendaRoutes.TASK,
            queryParameters = mapOf(
                TASK_ID_QUERY_PARAM to id,
            ),
        )
    }

    // Events
    override suspend fun createEvent(
        event: AgendaItem.Event,
    ): Result<AgendaItem.Event, DataError.Network> {
        return httpClient.postMultipart<EventDto>(
            route = AgendaRoutes.EVENT,
            body = MultiPartFormDataContent(
                formData {
                    append(EVENT_CREATE_REQUEST, Json.encodeToString(event.toCreateEventRequest()))
                    event.photos
                        .filterIsInstance<Photo.Local>()
                        .map { it.localPhotoUri }
                        .forEachIndexed { index, localPhotoUri ->
                            val photoName = "photo$index"
                            append(
                                photoName,
                                URL(localPhotoUri).readBytes(),
                                // TODO: get bytes of created compressed file
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/png")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=$photoName.png",
                                    )
                                },
                            )
                        }
                },
            ),
        ).map { it.toEvent(sessionStorage.get()?.userId) }
    }

    override suspend fun getEvent(id: String): Result<AgendaItem.Event, DataError.Network> {
        return httpClient.get<EventDto>(
            route = AgendaRoutes.EVENT,
            queryParameters = mapOf(
                EVENT_ID_QUERY_PARAM to id,
            ),
        ).map { it.toEvent(sessionStorage.get()?.userId) }
    }

    override suspend fun getAllEvents(): Result<List<AgendaItem.Event>, DataError.Network> {
        return httpClient.get<AgendaDto>(
            route = AgendaRoutes.FULL_AGENDA,
        )
            .map { agendaDto ->
                agendaDto.events.map { it.toEvent(sessionStorage.get()?.userId) }
            }
    }

    override suspend fun updateEvent(
        event: AgendaItem.Event,
    ): Result<AgendaItem.Event, DataError.Network> {
        return httpClient.putMultipart<EventDto>(
            route = AgendaRoutes.EVENT,
            body = MultiPartFormDataContent(
                formData {
                    append(EVENT_CREATE_REQUEST, Json.encodeToString(event.toUpdateEventRequest()))
                    event.photos
                        .filterIsInstance<Photo.Local>()
                        .map { it.localPhotoUri }
                        .forEachIndexed { index, compressedPhotoUri ->
                            val photoName = "photo$index"
                            append(
                                photoName,
                                URL(compressedPhotoUri).readBytes(),
                                // TODO: get bytes of created compressed file
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/png")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=$photoName.png",
                                    )
                                },
                            )
                        }
                },
            ),
        ).map { it.toEvent(sessionStorage.get()?.userId) }
    }

    override suspend fun deleteEvent(id: String): EmptyResult<DataError.Network> {
        return httpClient.delete(
            route = AgendaRoutes.EVENT,
            queryParameters = mapOf(
                EVENT_ID_QUERY_PARAM to id,
            ),
        )
    }

    // All
    override suspend fun getAllAgendaItems(): Result<List<AgendaItem>, DataError.Network> {
        return httpClient.get<AgendaDto>(
            route = AgendaRoutes.FULL_AGENDA,
        ).map { agendaDto ->
            (
                agendaDto.reminders.map { it.toReminder() } +
                    agendaDto.tasks.map { it.toTask() } +
                    agendaDto.events.map { it.toEvent(sessionStorage.get()?.userId) }
                )
                .sortedBy { it.startDateTime }
        }
    }

    companion object {
        const val REMINDER_ID_QUERY_PARAM = "reminderId"
        const val TASK_ID_QUERY_PARAM = "taskId"
        const val EVENT_ID_QUERY_PARAM = "eventId"
        const val EVENT_CREATE_REQUEST = "create_event_request"
    }
}
