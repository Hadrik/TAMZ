package com.example.tamz

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.aay.compose.lineChart.LineChart
import com.aay.compose.lineChart.model.LineParameters
import com.aay.compose.lineChart.model.LineType
import com.example.tamz.ui.theme.TAMZTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// Data Models
data class LocationItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val city: String = "",
    val country: String = "",
    val isGps: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class GeocodingItem(
    val name: String?,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
)

data class WeatherResponse(
    val name: String?,
    val sys: SysData?,
    val main: MainData?,
    val weather: List<WeatherDescriptionData>?,
    val wind: WindData?
)

data class SysData(
    val country: String?
)

data class MainData(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double?,
    @SerializedName("temp_min") val tempMin: Double?,
    @SerializedName("temp_max") val tempMax: Double?,
    val pressure: Int?,
    val humidity: Int?
)

data class WeatherDescriptionData(
    val id: Int?,
    val main: String?,
    val description: String?,
    val icon: String?
)

data class WindData(
    val speed: Double?
)

data class ForecastResponse(
    val list: List<ForecastItem>?,
    val city: CityData?
)

data class CityData(
    val name: String?,
    val country: String?
)

data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherDescriptionData>?,
    @SerializedName("dt_txt") val dtTxt: String?
)

// SharedPreferences Storage
class LocationStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getSavedLocations(): List<LocationItem> {
        val json = prefs.getString("saved_locations", null)
        if (json.isNullOrEmpty()) return defaultLocations()
        return try {
            val type = object : TypeToken<List<LocationItem>>() {}.type
            gson.fromJson<List<LocationItem>>(json, type) ?: defaultLocations()
        } catch (_: Exception) {
            defaultLocations()
        }
    }

    fun saveLocations(locations: List<LocationItem>) {
        val json = gson.toJson(locations)
        prefs.edit { putString("saved_locations", json) }
    }

    fun getActiveLocationId(): String {
        val defaultId = getSavedLocations().firstOrNull()?.id ?: "default_prague"
        return prefs.getString("active_location_id", defaultId) ?: defaultId
    }

    fun saveActiveLocationId(id: String) {
        prefs.edit { putString("active_location_id", id) }
    }

    private fun defaultLocations(): List<LocationItem> {
        val list = listOf(
            LocationItem(id = "default_prague", name = "Prague, CZ", city = "Prague", country = "CZ"),
            LocationItem(id = "default_london", name = "London, UK", city = "London", country = "GB"),
            LocationItem(id = "default_ny", name = "New York, US", city = "New York", country = "US")
        )
        saveLocations(list)
        saveActiveLocationId("default_prague")
        return list
    }
}

// API Functions using OpenWeatherMap Geocoding API for city lookups
object WeatherApi {
    private suspend fun getCoordinates(city: String, country: String, apiKey: String): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val query = if (country.isNotBlank()) "$city,$country" else city
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.openweathermap.org/geo/1.0/direct?q=$encodedQuery&limit=1&appid=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: throw Exception("Empty response from geocoding server")
        if (!response.isSuccessful) {
            throw Exception("Geocoding Error ${response.code}: ${response.message}")
        }
        val type = object : TypeToken<List<GeocodingItem>>() {}.type
        val list: List<GeocodingItem> = Gson().fromJson(bodyString, type) ?: emptyList()
        if (list.isEmpty()) {
            throw Exception("City not found: $city,$country")
        }
        Pair(list[0].lat, list[0].lon)
    }

    suspend fun getWeatherAndForecast(location: LocationItem, apiKey: String): Pair<WeatherResponse, ForecastResponse> = withContext(Dispatchers.IO) {
        val (lat, lon) = if (location.latitude != null && location.longitude != null) {
            Pair(location.latitude, location.longitude)
        } else {
            getCoordinates(location.city, location.country, apiKey)
        }

        val client = OkHttpClient()
        val weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
        val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        val weatherReq = Request.Builder().url(weatherUrl).build()
        val weatherResp = client.newCall(weatherReq).execute()
        val weatherBody = weatherResp.body?.string() ?: throw Exception("Empty response from weather server")
        if (!weatherResp.isSuccessful) {
            throw Exception("Weather API Error ${weatherResp.code}: ${weatherResp.message}")
        }

        val forecastReq = Request.Builder().url(forecastUrl).build()
        val forecastResp = client.newCall(forecastReq).execute()
        val forecastBody = forecastResp.body?.string() ?: throw Exception("Empty response from forecast server")
        if (!forecastResp.isSuccessful) {
            throw Exception("Forecast API Error ${forecastResp.code}: ${forecastResp.message}")
        }

        val gson = Gson()
        val weatherData = gson.fromJson(weatherBody, WeatherResponse::class.java)
        val forecastData = gson.fromJson(forecastBody, ForecastResponse::class.java)

        Pair(weatherData, forecastData)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TAMZTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val storage = remember { LocationStorage(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var savedLocations by remember { mutableStateOf(storage.getSavedLocations()) }
    var activeLocationId by remember { mutableStateOf(storage.getActiveLocationId()) }

    val activeLocation = remember(savedLocations, activeLocationId) {
        savedLocations.find { it.id == activeLocationId } ?: savedLocations.firstOrNull()
    }

    fun updateLocationsList(newList: List<LocationItem>) {
        savedLocations = newList
        storage.saveLocations(newList)
        if (newList.none { it.id == activeLocationId } && newList.isNotEmpty()) {
            val newActive = newList.first().id
            activeLocationId = newActive
            storage.saveActiveLocationId(newActive)
        }
    }

    fun setActiveLocation(id: String) {
        activeLocationId = id
        storage.saveActiveLocationId(id)
        selectedTab = 0
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WbSunny, contentDescription = "Weather") },
                    label = { Text("Weather") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Locations") },
                    label = { Text("Locations") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> WeatherPage(
                    location = activeLocation,
                    onSelectLocationTab = { selectedTab = 1 }
                )
                1 -> SavedLocationsPage(
                    locations = savedLocations,
                    activeLocationId = activeLocationId,
                    onSelectLocation = { setActiveLocation(it.id) },
                    onAddLocation = { newLoc ->
                        val updated = savedLocations + newLoc
                        updateLocationsList(updated)
                        setActiveLocation(newLoc.id)
                    },
                    onDeleteLocation = { locToDelete ->
                        val updated = savedLocations.filter { it.id != locToDelete.id }
                        updateLocationsList(updated)
                    }
                )
            }
        }
    }
}

fun getWeatherIconResId(iconCode: String?): Int {
    return when (iconCode) {
        "01d" -> R.drawable.icon_01d
        "01n" -> R.drawable.icon_01n
        "02d" -> R.drawable.icon_02d
        "02n" -> R.drawable.icon_02n
        "03d" -> R.drawable.icon_03d
        "03n" -> R.drawable.icon_03n
        "04d" -> R.drawable.icon_04d
        "04n" -> R.drawable.icon_04n
        "09d" -> R.drawable.icon_09d
        "09n" -> R.drawable.icon_09n
        "10d" -> R.drawable.icon_10d
        "10n" -> R.drawable.icon_10n
        "11d" -> R.drawable.icon_11d
        "11n" -> R.drawable.icon_11n
        "13d" -> R.drawable.icon_13d
        "13n" -> R.drawable.icon_13n
        "50d" -> R.drawable.icon_50d
        "50n" -> R.drawable.icon_50n
        else -> R.drawable.icon_01d
    }
}

@Composable
fun WeatherPage(
    location: LocationItem?,
    onSelectLocationTab: () -> Unit
) {
    val apiKey = stringResource(R.string.openweather_api_key)
    val scope = rememberCoroutineScope()

    var currentWeather by remember { mutableStateOf<WeatherResponse?>(null) }
    var forecast by remember { mutableStateOf<ForecastResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadWeatherData() {
        if (location == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val (weatherRes, forecastRes) = WeatherApi.getWeatherAndForecast(location, apiKey)
                currentWeather = weatherRes
                forecast = forecastRes
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load weather data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(location?.id) {
        loadWeatherData()
    }

    if (location == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No location selected")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onSelectLocationTab) {
                    Text("Select Location")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val displayName = if (location.isGps && currentWeather?.name != null) {
                        "${currentWeather?.name}, ${currentWeather?.sys?.country ?: ""} (GPS)"
                    } else {
                        location.name
                    }
                    Text(
                        text = displayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (location.isGps) "Current Location (GPS)" else "Saved City",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { loadWeatherData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (errorMessage != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadWeatherData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else if (currentWeather != null) {
            item {
                CurrentWeatherCard(weather = currentWeather!!)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (forecast?.list != null && forecast!!.list!!.isNotEmpty()) {
                item {
                    ForecastGraphCard(forecastItems = forecast!!.list!!)
                }
            }
        }
    }
}

@Composable
fun CurrentWeatherCard(weather: WeatherResponse) {
    val tempInt = weather.main?.temp?.roundToInt() ?: 0
    val feelsLike = weather.main?.feelsLike?.roundToInt()
    val weatherDesc = weather.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
    val iconCode = weather.weather?.firstOrNull()?.icon
    val iconResId = getWeatherIconResId(iconCode)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$tempInt°C",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (feelsLike != null) {
                        Text(
                            text = "Feels like $feelsLike°C",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weatherDesc,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = weatherDesc,
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WeatherDetailItem(label = "Humidity", value = "${weather.main?.humidity ?: 0}%")
                WeatherDetailItem(label = "Pressure", value = "${weather.main?.pressure ?: 0} hPa")
                WeatherDetailItem(label = "Wind", value = "${weather.wind?.speed ?: 0.0} m/s")
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ForecastGraphCard(forecastItems: List<ForecastItem>) {
    val next24Hours = remember(forecastItems) { forecastItems.take(8) }
    if (next24Hours.isEmpty()) return

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val xLabels = remember(next24Hours) {
        next24Hours.map { item ->
            timeFormat.format(Date(item.dt * 1000L))
        }
    }
    val temperatures = remember(next24Hours) {
        next24Hours.map { it.main.temp }
    }

    val lineParameters = remember(temperatures) {
        listOf(
            LineParameters(
                label = "Temperature (°C)",
                data = temperatures,
                lineType = LineType.CURVED_LINE,
                lineColor = Color(0xFF1E88E5),
                lineShadow = true
            )
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Next 24 Hours Forecast",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                linesParameters = lineParameters,
                xAxisData = xLabels,
                yAxisRange = 8,
                animateChart = true
            )
        }
    }
}

@Composable
fun SavedLocationsPage(
    locations: List<LocationItem>,
    activeLocationId: String,
    onSelectLocation: (LocationItem) -> Unit,
    onAddLocation: (LocationItem) -> Unit,
    onDeleteLocation: (LocationItem) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun addGpsLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val gpsItem = LocationItem(
                            id = "gps_${System.currentTimeMillis()}",
                            name = "GPS (${String.format(Locale.US, "%.2f", loc.latitude)}, ${String.format(Locale.US, "%.2f", loc.longitude)})",
                            isGps = true,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                        onAddLocation(gpsItem)
                        Toast.makeText(context, "Added GPS Location", Toast.LENGTH_SHORT).show()
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                val gpsItem = LocationItem(
                                    id = "gps_${System.currentTimeMillis()}",
                                    name = "GPS (${String.format(Locale.US, "%.2f", lastLoc.latitude)}, ${String.format(Locale.US, "%.2f", lastLoc.longitude)})",
                                    isGps = true,
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude
                                )
                                onAddLocation(gpsItem)
                                Toast.makeText(context, "Added GPS Location", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unable to fetch GPS position", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "GPS Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            addGpsLocation()
        } else {
            Toast.makeText(context, "Location permission is required for GPS", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestGpsAndAdd() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            addGpsLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Locations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add City")
            }

            Button(
                onClick = { requestGpsAndAdd() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add GPS")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (locations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved locations. Add one above!")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations, key = { it.id }) { loc ->
                    val isActive = loc.id == activeLocationId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLocation(loc) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active Location",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        text = loc.name,
                                        fontSize = 18.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = if (loc.isGps) "GPS Location" else if (loc.country.isNotBlank()) "City in ${loc.country}" else "City",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteLocation(loc) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete location")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { city, country ->
                val name = if (country.isNotBlank()) "${city.trim()}, ${country.trim().uppercase()}" else city.trim()
                val newLocation = LocationItem(
                    name = name,
                    city = city.trim(),
                    country = country.trim().uppercase()
                )
                onAddLocation(newLocation)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (city: String, country: String) -> Unit
) {
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Location") },
        text = {
            Column {
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        error = null
                    },
                    label = { Text("City (e.g. Prague)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country code (e.g. CZ, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (city.isBlank()) {
                        error = "City name cannot be empty"
                    } else {
                        onConfirm(city, country)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
