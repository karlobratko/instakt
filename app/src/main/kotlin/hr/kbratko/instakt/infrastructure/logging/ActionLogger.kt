package hr.kbratko.instakt.infrastructure.logging

import hr.kbratko.instakt.domain.model.AuditLog
import hr.kbratko.instakt.domain.model.AuditLog.Action.Create
import hr.kbratko.instakt.domain.model.AuditLog.Action.Delete
import hr.kbratko.instakt.domain.model.AuditLog.Action.Login
import hr.kbratko.instakt.domain.model.AuditLog.Action.Register
import hr.kbratko.instakt.domain.model.AuditLog.Action.Reset
import hr.kbratko.instakt.domain.model.AuditLog.Action.Update
import hr.kbratko.instakt.domain.model.AuditLog.Resource.Content
import hr.kbratko.instakt.domain.model.AuditLog.Resource.Password
import hr.kbratko.instakt.domain.model.AuditLog.Resource.Plan
import hr.kbratko.instakt.domain.model.AuditLog.Resource.ProfilePicture
import hr.kbratko.instakt.domain.model.AuditLog.Resource.SocialMediaLink
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.infrastructure.flows.AuditLogEmitter
import hr.kbratko.instakt.domain.model.AuditLog.Resource.User as UserResource

class ActionLogger(private val auditLogEmitter: AuditLogEmitter) {
    suspend fun logContentCreation(userId: User.Id) = log(userId, Create, Content)

    suspend fun logContentUpdate(userId: User.Id) = log(userId, Update, Content)

    suspend fun logContentDeletion(userId: User.Id) = log(userId, Delete, Content)

    suspend fun logUserRegistration(userId: User.Id) = log(userId, Register, UserResource)

    suspend fun logUserLogin(userId: User.Id) = log(userId, Login, UserResource)

    suspend fun logPasswordReset(userId: User.Id) = log(userId, Reset, Password)

    suspend fun logProfilePictureUpdate(userId: User.Id) = log(userId, Update, ProfilePicture)

    suspend fun logProfilePictureDeletion(userId: User.Id) = log(userId, Delete, ProfilePicture)

    suspend fun logPlanUpdate(userId: User.Id) = log(userId, Update, Plan)

    suspend fun logUserUpdate(userId: User.Id) = log(userId, Update, UserResource)

    suspend fun logSocialMediaLinkCreation(userId: User.Id) = log(userId, Create, SocialMediaLink)

    suspend fun logSocialMediaLinkUpdate(userId: User.Id) = log(userId, Update, SocialMediaLink)

    suspend fun logSocialMediaLinkDeletion(userId: User.Id) = log(userId, Delete, SocialMediaLink)

    private suspend fun log(userId: User.Id, action: AuditLog.Action, resource: AuditLog.Resource) {
        auditLogEmitter.emit(AuditLog.New(userId, action, resource))
    }
}
