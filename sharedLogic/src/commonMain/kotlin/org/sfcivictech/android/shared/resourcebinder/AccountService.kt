package org.sfcivictech.android.shared.resourcebinder

import kotlinx.coroutines.delay

interface AccountService {
    suspend fun sendSignInLink(email: String): SendSignInLinkResult

    suspend fun verifySignInLink(token: String): VerifySignInLinkResult

    suspend fun requestAccess(
        fullName: String,
        email: String,
        organization: String,
    ): RequestAccessResult

    suspend fun verifyInvitation(token: String): VerifyInvitationResult
}

enum class RequestAccessResult {
    SUCCESS,
    ACCOUNT_ALREADY_EXISTS,
    NETWORK_ERROR,
}

enum class VerifyInvitationResult {
    SUCCESS,
    EXPIRED,
    NETWORK_ERROR,
}

enum class SendSignInLinkResult {
    SUCCESS,
    ACCOUNT_NOT_FOUND,
    NETWORK_ERROR,
}

enum class VerifySignInLinkResult {
    SUCCESS,
    EXPIRED,
    ALREADY_USED,
    NETWORK_ERROR,
}

class FakeAccountService : AccountService {
    var sendSignInLinkOutput: SendSignInLinkResult = SendSignInLinkResult.SUCCESS
    var sendSignInLinkDelayInMilliseconds: Long = 0L

    var verifySignInLinkOutput: VerifySignInLinkResult = VerifySignInLinkResult.SUCCESS
    var verifySignInLinkDelayInMilliseconds: Long = 0L

    var requestAccessOutput: RequestAccessResult = RequestAccessResult.SUCCESS
    var requestAccessDelayInMilliseconds: Long = 0L

    var verifyInvitationOutput: VerifyInvitationResult = VerifyInvitationResult.SUCCESS
    var verifyInvitationDelayInMilliseconds: Long = 0L

    override suspend fun sendSignInLink(email: String): SendSignInLinkResult {

        delay(sendSignInLinkDelayInMilliseconds)

        return sendSignInLinkOutput
    }

    override suspend fun verifySignInLink(token: String): VerifySignInLinkResult {

        delay(verifySignInLinkDelayInMilliseconds)

        return verifySignInLinkOutput
    }

    override suspend fun requestAccess(
        fullName: String,
        email: String,
        organization: String,
    ): RequestAccessResult {

        delay(requestAccessDelayInMilliseconds)

        return requestAccessOutput
    }

    override suspend fun verifyInvitation(token: String): VerifyInvitationResult {

        delay(verifyInvitationDelayInMilliseconds)

        return verifyInvitationOutput
    }
}