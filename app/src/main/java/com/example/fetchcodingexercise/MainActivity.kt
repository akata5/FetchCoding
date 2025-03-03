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
// Import layout components for structuring UI
import androidx.compose.foundation.layout.*
// LazyColumn is Android's equivalent to RecyclerView in Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Helper function for displaying lists in LazyColumn
import androidx.compose.foundation.lazy.items
// Material Design 3 UI components
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
// State management tools for Compose
import androidx.compose.runtime.*
// Text styling utilities
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Coroutine dispatchers for background work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Libraries for network requests and JSON parsing
import org.json.JSONArray
import java.net.URL

/**
 * Data class representing an item from the API response.
 * @param id Unique identifier for the item
 * @param listId Group identifier that items can be categorized by
 * @param name The display name of the item
 */
data class Item(val id: Int, val listId: Int, val name: String)

/**
 * Main entry point for the application.
 * Sets up the theme and initial UI container.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Apply the application theme
            FetchCodingExerciseTheme {
                // Surface container provides background color from theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start with the main screen composable
                    ItemListScreen()
                }
            }
        }
    }
}

/**
 * Fetches items from the remote API and processes them.
 * This is a suspending function that should be called from a coroutine.
 *
 * @return A map of items grouped by their listId
 */
suspend fun fetchItemsFromApi(): Map<Int, List<Item>> = withContext(Dispatchers.IO) {
    // Use IO dispatcher to perform network operations on a background thread
    try {
        // Fetch JSON data from the remote URL
        val jsonString = URL("https://fetch-hiring.s3.amazonaws.com/hiring.json").readText()

        // Parse the JSON string into a JSONArray
        val jsonArray = JSONArray(jsonString)

        // Create a list to store the parsed items
        val items = mutableListOf<Item>()

        // Iterate through each JSON object in the array
        for(i in 0 until jsonArray.length()) {
            // Extract and parse each JSON object
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getInt("id")
            val listId = obj.getInt("listId")
            // Handle null values in the name field
            val name = if (obj.isNull("name")) null else obj.getString("name")

            // Only add items with non-null, non-blank names
            if(!name.isNullOrBlank()) {
                items.add(Item(id, listId, name))
            }
        }

        // Sort the items first by listId and then by the numeric portion of the name
        val itemsSorted = items.sortedWith(
            // Primary sort by listId in ascending order
            compareBy<Item> { it.listId }
                .thenBy {
                    // Secondary sort by extracting the number from "Item X" format
                    // If extraction fails, fall back to string comparison
                    it.name.replace("Item ", "").toIntOrNull() ?: it.name
                }
        )

        // Group the sorted items by listId and return the map
        return@withContext itemsSorted.groupBy { it.listId }
    } catch (e: Exception) {
        // Propagate any exceptions to the caller
        throw e
    }
}

/**
 * Composable function that displays items grouped by their listId.
 *
 * @param groupedItems Map of items grouped by listId
 */
@Composable
fun ItemGroups(groupedItems: Map<Int, List<Item>>) {
    // LazyColumn is similar to RecyclerView, only rendering visible items
    LazyColumn {
        // For each listId (sorted in ascending order)
        groupedItems.keys.sorted().forEach { listId ->
            // Add a header for each group
            item {
                Text(
                    // Display the list ID as the group title
                    text = "List ID: $listId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Render all items for the current listId
            items(groupedItems[listId] ?: emptyList()) { item ->
                // Display each item in a Card with elevation for depth
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    // Column layout for the card content
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Item name with medium emphasis
                        Text(text = item.name, fontWeight = FontWeight.Medium)
                        // Item ID with less emphasis (smaller, dimmed text)
                        Text(
                            text = "ID: ${item.id}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Add vertical spacing between groups for better readability
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Main screen composable that manages state and displays the UI.
 * Handles loading, error states, and successful data display.
 */
@Composable
fun ItemListScreen() {
    // State management for the screen
    // Store the grouped items (empty map initially)
    var groupedItems by remember { mutableStateOf<Map<Int, List<Item>>>(emptyMap()) }
    // Track loading state (start with loading)
    var isLoading by remember { mutableStateOf(true) }
    // Store any error messages (null initially)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect runs once when the composable is first displayed
    // The key1 = true parameter means it only runs once
    LaunchedEffect(key1 = true) {
        try {
            // Attempt to fetch and process the data
            val items = fetchItemsFromApi()
            // Update state with the fetched items
            groupedItems = items
            // Set loading state to false
            isLoading = false
        } catch (e: Exception) {
            // Handle any errors by updating the error message
            errorMessage = "Error: ${e.message}"
            // Set loading state to false even on error
            isLoading = false
        }
    }

    // Main layout column for the screen
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // App title/header
        Text(
            text = "Fetch Rewards Items",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Conditional display based on the current state
        when {
            // Display loading indicator when loading
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
            }
            // Display error message when there's an error
            errorMessage != null -> {
                Text(text = errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
            // Display the grouped items when data is loaded successfully
            else -> {
                ItemGroups(groupedItems = groupedItems)
            }
        }
    }
}