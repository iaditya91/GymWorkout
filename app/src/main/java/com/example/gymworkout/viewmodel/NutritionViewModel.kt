package com.example.gymworkout.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.CustomFoodItem
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.FoodLogEntry
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.ServingUnit
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.notification.ReminderScheduler
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).nutritionDao()
    private val reminderDao = WorkoutDatabase.getDatabase(application).reminderDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(formatter))
    val selectedDate: StateFlow<String> = _selectedDate

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun getEntriesForDate(date: String): Flow<List<NutritionEntry>> =
        dao.getEntriesForDate(date)

    fun getTotalForCategory(date: String, category: String): Flow<Float> =
        dao.getTotalForDateAndCategory(date, category)

    fun getAllTargets(): Flow<List<NutritionTarget>> = dao.getAllTargets()

    fun getTarget(category: String): Flow<NutritionTarget?> = dao.getTarget(category)

    fun addEntry(date: String, category: NutritionCategory, value: Float) {
        viewModelScope.launch {
            dao.insertEntry(
                NutritionEntry(
                    date = date,
                    category = category.name,
                    value = value
                )
            )
        }
    }

    fun deleteEntry(entry: NutritionEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    fun setTarget(category: NutritionCategory, value: Float) {
        viewModelScope.launch {
            dao.insertTarget(
                NutritionTarget(
                    category = category.name,
                    targetValue = value,
                    label = category.label,
                    unit = category.unit,
                    isCustom = false
                )
            )
        }
    }

    fun setTargetByKey(category: String, value: Float) {
        viewModelScope.launch {
            val existing = dao.getTargetSync(category)
            if (existing != null) {
                dao.insertTarget(existing.copy(targetValue = value))
            }
        }
    }

    fun addCustomObjective(
        name: String, unit: String, target: Float,
        timerSeconds: Int = 0, timerNotifyEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val key = "CUSTOM_${name.uppercase().replace(" ", "_")}_${System.currentTimeMillis()}"
            dao.insertTarget(
                NutritionTarget(
                    category = key,
                    targetValue = target,
                    label = name,
                    unit = unit,
                    isCustom = true,
                    timerSeconds = timerSeconds,
                    timerNotifyEnabled = timerNotifyEnabled
                )
            )
        }
    }

    fun updateNotes(category: String, notes: String) {
        viewModelScope.launch {
            dao.updateTargetNotes(category, notes)
        }
    }

    fun updateCustomObjective(category: String, label: String, timerSeconds: Int, notifyEnabled: Boolean) {
        viewModelScope.launch {
            dao.updateCustomObjective(category, label, timerSeconds, notifyEnabled)
        }
    }

    fun deleteObjective(category: String) {
        viewModelScope.launch {
            dao.deleteTarget(category)
            dao.deleteEntriesForCategory(category)
        }
    }

    fun addEntryByKey(date: String, category: String, value: Float) {
        viewModelScope.launch {
            dao.insertEntry(NutritionEntry(date = date, category = category, value = value))
        }
    }

    fun initDefaultTargets() {
        viewModelScope.launch {
            val defaults = mapOf<NutritionCategory, Float>(
                NutritionCategory.WATER to 3f,
                NutritionCategory.CALORIES to 2000f,
                NutritionCategory.CARBS to 200f,
                NutritionCategory.PROTEIN to 120f,
                NutritionCategory.SLEEP to 8f
            )
            defaults.forEach { (cat, value) ->
                if (dao.getTargetSync(cat.name) == null) {
                    dao.insertTarget(
                        NutritionTarget(
                            category = cat.name,
                            targetValue = value,
                            label = cat.label,
                            unit = cat.unit,
                            isCustom = false
                        )
                    )
                }
            }
        }
    }

    fun todayString(): String = LocalDate.now().format(formatter)

    // --- Reminder methods ---

    fun getRemindersForCategory(category: String): Flow<List<NutritionReminder>> =
        reminderDao.getRemindersForCategory(category)

    fun saveReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            val id = reminderDao.insertReminder(reminder)
            val saved = reminder.copy(id = id.toInt())
            ReminderScheduler.scheduleReminder(getApplication(), saved)
        }
    }

    fun updateReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder)
            if (reminder.enabled) {
                ReminderScheduler.scheduleReminder(getApplication(), reminder)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), reminder)
            }
        }
    }

    fun deleteReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication(), reminder)
            reminderDao.deleteReminder(reminder)
        }
    }

    fun toggleReminderEnabled(reminder: NutritionReminder) {
        val updated = reminder.copy(enabled = !reminder.enabled)
        updateReminder(updated)
    }

    // --- Food log methods ---

    fun getFoodLogForDate(date: String): Flow<List<FoodLogEntry>> =
        dao.getFoodLogForDate(date)

    fun logFood(date: String, food: FoodItem, quantity: Float) {
        viewModelScope.launch {
            val m = quantity / food.baseAmount

            dao.insertFoodLog(
                FoodLogEntry(
                    date = date,
                    foodName = food.name,
                    quantity = quantity,
                    unit = food.servingUnit.label,
                    calories = food.caloriesPerBase * m,
                    protein = food.proteinPerBase * m,
                    carbs = food.carbsPerBase * m,
                    fat = food.fatPerBase * m,
                    fiber = food.fiberPerBase * m,
                    vitaminA = food.vitAPerBase * m,
                    vitaminB1 = food.vitB1PerBase * m,
                    vitaminB2 = food.vitB2PerBase * m,
                    vitaminB3 = food.vitB3PerBase * m,
                    vitaminB6 = food.vitB6PerBase * m,
                    vitaminB12 = food.vitB12PerBase * m,
                    vitaminC = food.vitCPerBase * m,
                    vitaminD = food.vitDPerBase * m,
                    vitaminE = food.vitEPerBase * m,
                    vitaminK = food.vitKPerBase * m,
                    folate = food.folatePerBase * m,
                    iron = food.ironPerBase * m,
                    calcium = food.calciumPerBase * m,
                    magnesium = food.magnesiumPerBase * m,
                    potassium = food.potassiumPerBase * m,
                    zinc = food.zincPerBase * m,
                    copper = food.copperPerBase * m,
                    selenium = food.seleniumPerBase * m
                )
            )

            // Auto-add to nutrition categories
            val calories = food.caloriesPerBase * m
            val protein = food.proteinPerBase * m
            val carbs = food.carbsPerBase * m
            dao.insertEntry(NutritionEntry(date = date, category = NutritionCategory.CALORIES.name, value = calories))
            dao.insertEntry(NutritionEntry(date = date, category = NutritionCategory.PROTEIN.name, value = protein))
            dao.insertEntry(NutritionEntry(date = date, category = NutritionCategory.CARBS.name, value = carbs))
        }
    }

    fun deleteFoodLog(entry: FoodLogEntry) {
        viewModelScope.launch {
            dao.deleteFoodLog(entry)
        }
    }

    // --- Custom food methods ---

    val customFoods: Flow<List<CustomFoodItem>> = dao.getAllCustomFoods()

    fun saveCustomFood(item: CustomFoodItem) {
        viewModelScope.launch {
            dao.insertCustomFood(item)
        }
    }

    fun deleteCustomFood(item: CustomFoodItem) {
        viewModelScope.launch {
            dao.deleteCustomFood(item)
        }
    }

    // --- Barcode scanner ---

    data class ScannedProduct(
        val barcode: String,
        val name: String,
        val brand: String,
        val foodItem: FoodItem
    )

    sealed class BarcodeLookupState {
        data object Idle : BarcodeLookupState()
        data object Loading : BarcodeLookupState()
        data class Success(val product: ScannedProduct) : BarcodeLookupState()
        data class Error(val message: String) : BarcodeLookupState()
    }

    private val _barcodeLookupState = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val barcodeLookupState: StateFlow<BarcodeLookupState> = _barcodeLookupState

    fun resetBarcodeLookup() {
        _barcodeLookupState.value = BarcodeLookupState.Idle
    }

    fun lookupBarcode(barcode: String) {
        _barcodeLookupState.value = BarcodeLookupState.Loading
        viewModelScope.launch {
            try {
                val product = withContext(Dispatchers.IO) { fetchProductFromOpenFoodFacts(barcode) }
                if (product != null) {
                    _barcodeLookupState.value = BarcodeLookupState.Success(product)
                } else {
                    _barcodeLookupState.value = BarcodeLookupState.Error("Product not found for barcode: $barcode")
                }
            } catch (e: Exception) {
                Log.e("BarcodeLookup", "Failed to lookup barcode", e)
                _barcodeLookupState.value = BarcodeLookupState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun fetchProductFromOpenFoodFacts(barcode: String): ScannedProduct? {
        val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode?fields=product_name,brands,nutriments")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "GymWorkout Android App - barcode scanner")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        return try {
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = Gson().fromJson(response, JsonObject::class.java)

                val status = json.get("status")?.asInt ?: 0
                if (status != 1) return null

                val product = json.getAsJsonObject("product") ?: return null
                val name = product.get("product_name")?.asString?.takeIf { it.isNotBlank() } ?: "Unknown Product"
                val brand = product.get("brands")?.asString ?: ""
                val nutriments = product.getAsJsonObject("nutriments") ?: return null

                val calories = nutriments.get("energy-kcal_100g")?.asFloat ?: 0f
                val protein = nutriments.get("proteins_100g")?.asFloat ?: 0f
                val carbs = nutriments.get("carbohydrates_100g")?.asFloat ?: 0f
                val fat = nutriments.get("fat_100g")?.asFloat ?: 0f
                val fiber = nutriments.get("fiber_100g")?.asFloat ?: 0f
                val vitA = nutriments.get("vitamin-a_100g")?.asFloat ?: 0f
                val vitC = nutriments.get("vitamin-c_100g")?.asFloat ?: 0f
                val vitD = nutriments.get("vitamin-d_100g")?.asFloat ?: 0f
                val vitE = nutriments.get("vitamin-e_100g")?.asFloat ?: 0f
                val vitK = nutriments.get("vitamin-k_100g")?.asFloat ?: 0f
                val vitB1 = nutriments.get("vitamin-b1_100g")?.asFloat ?: 0f
                val vitB2 = nutriments.get("vitamin-b2_100g")?.asFloat ?: 0f
                val vitB3 = nutriments.get("vitamin-pp_100g")?.asFloat ?: 0f
                val vitB6 = nutriments.get("vitamin-b6_100g")?.asFloat ?: 0f
                val vitB12 = nutriments.get("vitamin-b12_100g")?.asFloat ?: 0f
                val folate = nutriments.get("folates_100g")?.asFloat ?: 0f
                val iron = nutriments.get("iron_100g")?.asFloat ?: 0f
                val calcium = nutriments.get("calcium_100g")?.asFloat ?: 0f
                val magnesium = nutriments.get("magnesium_100g")?.asFloat ?: 0f
                val potassium = nutriments.get("potassium_100g")?.asFloat ?: 0f
                val zinc = nutriments.get("zinc_100g")?.asFloat ?: 0f
                val copper = nutriments.get("copper_100g")?.asFloat ?: 0f
                val selenium = nutriments.get("selenium_100g")?.asFloat ?: 0f

                // Convert mcg values: Open Food Facts returns some vitamins in grams
                // vitamin-a, vitamin-d, vitamin-k, vitamin-b12, folates are in mcg on OFF
                // vitamin-c, vitamin-b1, vitamin-b2, vitamin-pp, vitamin-b6, vitamin-e, iron, calcium are in mg

                val displayName = if (brand.isNotBlank()) "$name ($brand)" else name

                val foodItem = FoodItem(
                    name = displayName,
                    category = "Scanned",
                    servingUnit = ServingUnit.GRAMS,
                    defaultServing = 100f,
                    caloriesPerBase = calories,
                    proteinPerBase = protein,
                    carbsPerBase = carbs,
                    fatPerBase = fat,
                    fiberPerBase = fiber,
                    vitAPerBase = vitA * 1000000f,   // g to mcg
                    vitB1PerBase = vitB1 * 1000f,    // g to mg
                    vitB2PerBase = vitB2 * 1000f,
                    vitB3PerBase = vitB3 * 1000f,
                    vitB6PerBase = vitB6 * 1000f,
                    vitB12PerBase = vitB12 * 1000000f, // g to mcg
                    vitCPerBase = vitC * 1000f,       // g to mg
                    vitDPerBase = vitD * 1000000f,    // g to mcg
                    vitEPerBase = vitE * 1000f,       // g to mg
                    vitKPerBase = vitK * 1000000f,    // g to mcg
                    folatePerBase = folate * 1000000f, // g to mcg
                    ironPerBase = iron * 1000f,        // g to mg
                    calciumPerBase = calcium * 1000f,  // g to mg
                    magnesiumPerBase = magnesium * 1000f, // g to mg
                    potassiumPerBase = potassium * 1000f, // g to mg
                    zincPerBase = zinc * 1000f,         // g to mg
                    copperPerBase = copper * 1000f,     // g to mg
                    seleniumPerBase = selenium * 1000000f // g to mcg
                )

                ScannedProduct(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    foodItem = foodItem
                )
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    fun saveScannedAsCustomFood(product: ScannedProduct) {
        val food = product.foodItem
        saveCustomFood(
            CustomFoodItem(
                name = food.name,
                servingUnit = food.servingUnit.label,
                defaultServing = food.defaultServing,
                caloriesPerBase = food.caloriesPerBase,
                proteinPerBase = food.proteinPerBase,
                carbsPerBase = food.carbsPerBase,
                fatPerBase = food.fatPerBase,
                fiberPerBase = food.fiberPerBase,
                vitAPerBase = food.vitAPerBase,
                vitB1PerBase = food.vitB1PerBase,
                vitB2PerBase = food.vitB2PerBase,
                vitB3PerBase = food.vitB3PerBase,
                vitB6PerBase = food.vitB6PerBase,
                vitB12PerBase = food.vitB12PerBase,
                vitCPerBase = food.vitCPerBase,
                vitDPerBase = food.vitDPerBase,
                vitEPerBase = food.vitEPerBase,
                vitKPerBase = food.vitKPerBase,
                folatePerBase = food.folatePerBase,
                ironPerBase = food.ironPerBase,
                calciumPerBase = food.calciumPerBase,
                magnesiumPerBase = food.magnesiumPerBase,
                potassiumPerBase = food.potassiumPerBase,
                zincPerBase = food.zincPerBase,
                copperPerBase = food.copperPerBase,
                seleniumPerBase = food.seleniumPerBase
            )
        )
    }
}
