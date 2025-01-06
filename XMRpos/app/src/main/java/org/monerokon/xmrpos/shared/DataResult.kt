package org.monerokon.xmrpos.shared

sealed class DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Failure(val message: String) : DataResult<Nothing>()
}