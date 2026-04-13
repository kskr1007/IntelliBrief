package com.example.intellibrief

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): Unit =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun register(email: String, password: String): Unit =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    fun getHumanReadableError(context: Context, e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> context.getString(R.string.error_no_account)
            is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.error_invalid_credentials)
            is FirebaseAuthUserCollisionException -> context.getString(R.string.error_email_collision)
            is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_weak_password)
            is FirebaseNetworkException -> context.getString(R.string.error_network)
            else -> e.localizedMessage ?: context.getString(R.string.error_unexpected)
        }
    }
}