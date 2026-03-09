package com.example.smslogger.data.exception

/**
 * Domain exception hierarchy for authentication errors (#51).
 *
 * HTTP error codes are mapped to these typed exceptions inside [AuthRepository]
 * so that upper layers (ViewModel, UI) never need to inspect raw HTTP codes.
 *
 * Mapping:
 * - 401                  → [InvalidCredentialsException]
 * - 403 + "locked"       → [AccountLockedException]
 * - 403 + "inactive"     → [AccountInactiveException]
 * - 5xx                  → [ServerErrorException]
 * - Network / IO failure → [NetworkException]
 */
sealed class AuthException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Thrown when the server returns HTTP 401 (wrong username / password / TOTP). */
class InvalidCredentialsException(
    message: String = "Invalid username or password"
) : AuthException(message)

/**
 * Thrown when the server returns HTTP 403 and indicates the account is locked
 * (e.g. too many failed attempts).
 *
 * @param retryAfterSeconds Optional hint from the server about when to retry.
 */
class AccountLockedException(
    message: String = "Account locked. Try again in 30 minutes",
    val retryAfterSeconds: Long? = null
) : AuthException(message)

/** Thrown when the server returns HTTP 403 and the account has been deactivated. */
class AccountInactiveException(
    message: String = "Your account has been deactivated"
) : AuthException(message)

/** Thrown when the server returns HTTP 5xx. */
class ServerErrorException(
    message: String = "Server error. Please try again later",
    val httpCode: Int = 500
) : AuthException(message)

/** Thrown on network-level failures (no connectivity, timeout, DNS failure, etc.). */
class NetworkException(
    message: String = "Network error. Please check your connection",
    cause: Throwable? = null
) : AuthException(message, cause)

/**
 * Thrown when OkHttp's [CertificatePinner] rejects the server's certificate (#56).
 *
 * This indicates either:
 * - A genuine MITM attack
 * - The server certificate was rotated and the app pins are stale
 *
 * The user should be warned and sync halted until the app is updated.
 */
class CertificatePinningException(
    message: String = "Server certificate does not match the expected pin. Connection refused.",
    cause: Throwable? = null
) : AuthException(message, cause)

