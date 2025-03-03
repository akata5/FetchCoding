package com.example.fetchcodingexercise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fetchcodingexercise.ui.theme.FetchCodingExerciseTheme
import androidx.compose.foundation.layout.* // for layout components (Column, Row, Box, Spacer) to structure UI
import androidx.compose.foundation.lazy.LazyColumn // used to render only visible items
import androidx.compose.foundation.lazy.items // display list of items in a lazycolumn
// material design 3 theming elements to create UI
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.* // state management tools
// used for styling text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// for work on background threads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// for network requests and JSON parsing
import org.json.JSONArray
import java.net.URL

// data class used to represent each item we parse from the JSON
data class Item(val id: Int, val listId: Int, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FetchCodingExerciseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ItemListScreen()
                }
            }
        }
    }
}
// function to get items from api, must be called within a coroutine since it's suspend
suspend fun fetchItemsFromApi(): Map<Int, List<Item>> = withContext(Dispatchers.IO) {
    // runs on IO dispatcher to run calls on background thread
    // read the response from the URL as a string
    val jsonString = URL("https://fetch-hiring.s3.amazonaws.com/hiring.json").readText()
    // parse the string into a JSON array
    val jsonArray = JSONArray(jsonString)

    // make a list of individual items that we get out of the json
    val items = mutableListOf<Item>()
    // iterate through all json objefts
    for(i in 0 until jsonArray.length()) {
        // extract first object, parse it into id, listID, name
        val obj = jsonArray.getJSONObject(i)
        val id = obj.getInt("id")
        val listId = obj.getInt("listId")
        val name = if (obj.isNull("name")) null else obj.getString("name")

        if(!name.isNullOrBlank()) {
            items.add(Item(id, listId, name))
        }
    }

    //sort items by listID and then name
    val itemsSorted = items.sortedWith(
        // first sort by the listID in ascending order
        compareBy<Item> {it.listId}
            .thenBy {
                // secondly, sort by the number next to the item name, converting it to int and comparing
                it.name.replace("Item ", "").toIntOrNull() ?:it.name
            }
    )
    //groups into map with listID as the key, then the item as the value
    return@withContext itemsSorted.groupBy { it.listId }
}

//groups items by listId, displaying using a LazyColumn
@Composable
fun ItemGroups(groupedItems: Map<Int, List<Item>>) {
    LazyColumn {
        // For each listId (sorted)
        groupedItems.keys.sorted().forEach { listId ->
            // Header for the group
            item {
                Text(
                    // display the list id as the title, in bold
                    text = "List ID: $listId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Items in this group
            items(groupedItems[listId] ?: emptyList()) { item ->
                // display each item in a Card with padding
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // name with medium font weight
                        Text(text = item.name, fontWeight = FontWeight.Medium)
                        Text(
                            // id with smaller font, dimmed color
                            text = "ID: ${item.id}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Add some vertical space between groups
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ItemListScreen() {
    var groupedItems by remember { mutableStateOf<Map<Int, List<Item>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch data when the composable is first composed
    LaunchedEffect(key1 = true) {
        try {
            val items = fetchItemsFromApi()
            groupedItems = items
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Fetch Rewards Items",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
            }
            errorMessage != null -> {
                Text(text = errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                ItemGroups(groupedItems = groupedItems)
            }
        }
    }
}

