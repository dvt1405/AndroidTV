package com.kt.apps.core.exceptions

import com.kt.apps.core.ErrorCode

class ConnectionTimeOut(override val message: String, override val cause: Throwable? = null) :
    MyException(ErrorCode.CONNECT_TIMEOUT, message) {
}