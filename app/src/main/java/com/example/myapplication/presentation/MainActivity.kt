/* While this template provides a good starting point for using Wear Compose, you can always
 */

package com.example.myapplication.presentation

import android.os.StrictMode
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import com.example.myapplication.presentation.theme.MyApplicationTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ChipDefaults
import androidx.compose.ui.text.style.TextAlign

// These values get populated by setSecrets
var clientId = ""
var clientSecret = ""
var refreshToken = ""
var listId = ""

lateinit var taskListNew: MutableList<Map<String, String>>


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set secrets only on the first load
        setSecrets()

        setContent {
            setupApp()
        }
    }

    override fun onStart(){
        super.onStart()

        setContent {
            setupApp()
        }
    }

    // Set global secrets to be re-used by all functions
    private fun setSecrets() {

        val secrets = assets.open("secrets.json").bufferedReader().use{it.readText()}
        var jsonObject = JSONObject(secrets)

        clientId = jsonObject.getString("clientId")
        clientSecret = jsonObject.getString("clientSecret")
        refreshToken = jsonObject.getString("refreshToken")
        listId = jsonObject.getString("listId")

    }

}

@Composable
fun setupApp(){
    val tasks = getTasks()
    WearApp(tasks)
}

@Composable
fun WearApp(tasks: MutableList<Map<String, String>>) {
    // State to be able to update the list of tasks and trigger a re-compose
    // Reference: https://developer.android.com/jetpack/compose/state
    val (taskList, setTaskList) = remember {mutableStateOf(tasks)}
    MyApplicationTheme {
        TaskList(taskList, setTaskList)
    }
}

fun getTasks(): MutableList<Map<String, String>> {

    val url = getURL(listId)
    val accessToken = getAccessToken()
    val tasks = fetchTasks(url, accessToken)

    return tasks
}

@Composable
fun TaskList(
    taskList: MutableList<Map<String, String>>,
    setTaskList: (MutableList<Map<String, String>>) -> Unit
) {
    val accessToken = getAccessToken()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {

        items(taskList.size) {
            Chip(
                // in the on click would be the logic to mark them as checked
                onClick = {
                    // mark task as completed in the API
                    completeTask(taskList[it]["id"], accessToken)

                    // We need a copy to signal the state that it's a new value (Kotlin uses pass-by-reference)
                    val itemList = taskList.toMutableList()
                    itemList.removeAt(it)

                    // this modifies the state
                    setTaskList(itemList)
                },
                label = { taskList[it]["title"]?.let { it1 -> Text(
                    it1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                } },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
       }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val tasks = getTasks()
    WearApp(tasks)
}

// Mark tas as completed in the API
fun completeTask(taskId: String?, accessToken: String){

    val url = "https://graph.microsoft.com/v1.0/me/todo/lists/$listId/tasks/$taskId"

    // Prepare request
    val mediaType = "application/json".toMediaType()
    val body = "{\"status\": \"completed\"}".toRequestBody(mediaType)
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .patch(body)
        .addHeader("Authorization", "Bearer $accessToken")
        .build()

    // Disable the `NetworkOnMainThreadException` and make sure it is just logged.
    StrictMode
        .setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())

    client.newCall(request).execute().use { response ->
        Log.d("TAG", "Task deleted")
    }
}

// Parse access token out of Microsoft Oauth response
fun parseAccessToken(response: Response): String {
    val responseJson = response.body!!.string()
    val jsonObject = JSONObject(responseJson)
    return jsonObject.optString("access_token", null)
}

fun fetchTasks(url: String,accessToken: String): MutableList<Map<String, String>> {

    // Prepare request
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $accessToken")
        .build()

    // Disable the `NetworkOnMainThreadException` and make sure it is just logged.
    StrictMode
        .setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())

    client.newCall(request).execute().use { response ->
        val tasks = parseTasks(response)
        return tasks
    }
}

fun parseTasks(response: Response): MutableList<Map<String, String>> {
    val responseJson = response.body!!.string()
    val jsonObject = JSONObject(responseJson)
    val itemsArray = jsonObject.getJSONArray("value")

    val itemList: MutableList<Map<String, String>> = ArrayList()

    // iterate over items to build StringArray
    for (i in 0 until itemsArray.length()) {
        // Extract relevant information
        val id = itemsArray.getJSONObject(i).getString("id")
        val title = itemsArray.getJSONObject(i).getString("title")

        // Create item and add it to list
        val item = mapOf("id" to id, "title" to title)
        itemList.add(item)
    }

    return itemList
}

// Retrieve access token from microsoft API using the oauth credentials
fun getAccessToken(): String {
    // prepare request
    val url = "https://login.microsoftonline.com/common/oauth2/v2.0/token?"
    val client = OkHttpClient()
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("client_id", clientId)
        .addFormDataPart("refresh_token", refreshToken)
        .addFormDataPart("client_secret", clientSecret)
        .addFormDataPart("grant_type","refresh_token")
        .build()
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    // Disable the `NetworkOnMainThreadException` and make sure it is just logged.
    StrictMode
        .setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
    client.newCall(request).execute().use { response ->
        return parseAccessToken(response)
    }
}

fun getURL(listId: String): String {
    return "https://graph.microsoft.com/v1.0/me/todo/lists/$listId/tasks?\$filter=status%20eq%20%27notStarted%27"
}
