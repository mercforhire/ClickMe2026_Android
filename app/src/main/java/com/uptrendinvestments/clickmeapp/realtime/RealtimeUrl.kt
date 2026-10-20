package com.uptrendinvestments.clickmeapp.realtime

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * The socket origin: the REST base URL with its `/api/v1` path removed. The scheme is kept so an
 * https base yields a secure (wss) socket.
 */
fun socketUrl(baseUrl: String): String {
    val url = baseUrl.toHttpUrl()
    val port = if (url.port == HttpUrl.defaultPort(url.scheme)) "" else ":${url.port}"
    return "${url.scheme}://${url.host}$port"
}
