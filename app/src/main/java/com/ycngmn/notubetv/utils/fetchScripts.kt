package com.ycngmn.notubetv.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

const val SCRIPTS_URL = "https://raw.githubusercontent.com/vinh2201/NoTubeTV-custom/refs/heads/0.0.2-old/assets/userscripts.js"
suspend fun fetchScripts(): String {
    val httpClient = HttpClient(CIO)
    val response: HttpResponse = httpClient.get(SCRIPTS_URL)
    return response.body()
}
