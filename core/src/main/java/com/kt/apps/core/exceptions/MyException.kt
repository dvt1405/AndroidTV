package com.kt.apps.core.exceptions

import com.kt.apps.core.ErrorCode
import java.net.SocketTimeoutException

abstract class MyException(
    val code: Int, override val message: String?,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

    companion object {
        fun createException(
            code: Int,
            message: String? = null,
            cause: Throwable? = null
        ) = object : MyException(code, message, cause) {}
    }
}

fun Throwable.mapToMyException(code: Int? = null, msg: String? = null): MyException {
    return when (this) {
        is SocketTimeoutException -> ConnectionTimeOut(
            msg ?: "Không thể kết nối tới server, vui lòng thử link khác nhé"
        )

        else -> MyException.createException(
            code ?: ErrorCode.UN_EXPECTED_ERROR,
            msg ?: message,
            cause
        )
    }
}