package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.CustomFoodItem
import com.example.gymworkout.data.FoodDatabase
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.ServingUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodLogDialog(
    customFoods: List<FoodItem> = emptyList(),
    onDismiss: () -> Unit,
    onLog: (FoodItem, Float) -> Unit,
    onSaveCustomFood: (CustomFoodItem) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var quantity by remember { mutableStateOf("") }
    var showCustomForm by remember { mutableStateOf(false) }

    // Custom food form state - macros
    var customName by remember { mutableStateOf("") }
    var customServingUnit by remember { mutableStateOf(ServingUnit.GRAMS) }
    var customQuantity by remember { mutableStateOf("100") }
    var customCalories by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var customFiber by remember { mutableStateOf("") }

    // Custom food form state - vitamins & minerals
    var customVitA by remember { mutableStateOf("") }
    var customVitB1 by remember { mutableStateOf("") }
    var customVitB2 by remember { mutableStateOf("") }
    var customVitB3 by remember { mutableStateOf("") }
    var customVitB6 by remember { mutableStateOf("") }
    var customVitB12 by remember { mutableStateOf("") }
    var customVitC by remember { mutableStateOf("") }
    var customVitD by remember { mutableStateOf("") }
    var customVitE by remember { mutableStateOf("") }
    var customVitK by remember { mutableStateOf("") }
    var customFolate by remember { mutableStateOf("") }
    var customIron by remember { mutableStateOf("") }
    var customCalcium by remember { mutableStateOf("") }

    // Combine built-in and custom foods
    val allFoods = remember(customFoods) { FoodDatabase.foods + customFoods }
    val allCategories = remember(customFoods) {
        (FoodDatabase.categories + if (customFoods.isNotEmpty()) listOf("Custom") else emptyList()).distinct().sorted()
    }

    val filteredFoods = remember(searchQuery, selectedCategory, customFoods) {
        val bySearch = if (searchQuery.isBlank()) allFoods
        else allFoods.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
        if (selectedCategory != null) bySearch.filter { it.category == selectedCategory } else bySearch
    }

    if (showCustomForm) {
        val qty = customQuantity.toFloatOrNull() ?: 0f
        val baseAmount = if (customServingUnit == ServingUnit.PIECE) 1f else 100f
        val cal = customCalories.toFloatOrNull() ?: 0f
        val pro = customProtein.toFloatOrNull() ?: 0f
        val carb = customCarbs.toFloatOrNull() ?: 0f
        val fat = customFat.toFloatOrNull() ?: 0f
        val fib = customFiber.toFloatOrNull() ?: 0f
        val multiplier = if (qty > 0) qty / baseAmount else 0f

        AlertDialog(
            onDismissRequest = { showCustomForm = false },
            title = { Text("Add Custom Food") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Food Name") },
                        placeholder = { Text("e.g. Protein Bar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Serving unit selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ServingUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = customServingUnit == unit,
                                onClick = {
                                    customServingUnit = unit
                                    if (unit == ServingUnit.PIECE && customQuantity == "100") customQuantity = "1"
                                    else if (unit != ServingUnit.PIECE && customQuantity == "1") customQuantity = "100"
                                },
                                label = {
                                    Text(
                                        when (unit) {
                                            ServingUnit.GRAMS -> "Grams"
                                            ServingUnit.ML -> "ml"
                                            ServingUnit.PIECE -> "Piece"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customQuantity,
                        onValueChange = { customQuantity = it },
                        label = {
                            Text(
                                when (customServingUnit) {
                                    ServingUnit.PIECE -> "Quantity (pieces)"
                                    ServingUnit.ML -> "Quantity (ml)"
                                    ServingUnit.GRAMS -> "Quantity (grams)"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Nutrition per ${if (customServingUnit == ServingUnit.PIECE) "1 pc" else "100${customServingUnit.label}"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Macros
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customCalories,
                            onValueChange = { customCalories = it },
                            label = { Text("Calories") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customProtein,
                            onValueChange = { customProtein = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customCarbs,
                            onValueChange = { customCarbs = it },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customFat,
                            onValueChange = { customFat = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customFiber,
                        onValueChange = { customFiber = it },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Vitamins & Minerals section
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Vitamins & Minerals (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customVitA,
                            onValueChange = { customVitA = it },
                            label = { Text("Vit A (mcg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customVitC,
                            onValueChange = { customVitC = it },
                            label = { Text("Vit C (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customVitD,
                            onValueChange = { customVitD = it },
                            label = { Text("Vit D (mcg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customVitE,
                            onValueChange = { customVitE = it },
                            label = { Text("Vit E (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customVitK,
                            onValueChange = { customVitK = it },
                            label = { Text("Vit K (mcg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customVitB1,
                            onValueChange = { customVitB1 = it },
                            label = { Text("B1 (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customVitB2,
                            onValueChange = { customVitB2 = it },
                            label = { Text("B2 (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customVitB3,
                            onValueChange = { customVitB3 = it },
                            label = { Text("B3 (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customVitB6,
                            onValueChange = { customVitB6 = it },
                            label = { Text("B6 (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customVitB12,
                            onValueChange = { customVitB12 = it },
                            label = { Text("B12 (mcg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customFolate,
                            onValueChange = { customFolate = it },
                            label = { Text("Folate (mcg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customIron,
                            onValueChange = { customIron = it },
                            label = { Text("Iron (mg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customCalcium,
                        onValueChange = { customCalcium = it },
                        label = { Text("Calcium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Preview
                    if (qty > 0 && cal > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val displayQty = when (customServingUnit) {
                                    ServingUnit.PIECE -> "${qty.toInt()} pc"
                                    ServingUnit.ML -> "${qty.toInt()} ml"
                                    ServingUnit.GRAMS -> "${qty.toInt()} g"
                                }
                                Text(
                                    "Total for $displayQty",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                NutrientRow("Calories", cal * multiplier, "cal")
                                NutrientRow("Protein", pro * multiplier, "g")
                                NutrientRow("Carbs", carb * multiplier, "g")
                                NutrientRow("Fat", fat * multiplier, "g")
                                if (fib > 0) NutrientRow("Fiber", fib * multiplier, "g")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customName.isNotBlank() && qty > 0 && cal > 0) {
                            val vA = customVitA.toFloatOrNull() ?: 0f
                            val vB1 = customVitB1.toFloatOrNull() ?: 0f
                            val vB2 = customVitB2.toFloatOrNull() ?: 0f
                            val vB3 = customVitB3.toFloatOrNull() ?: 0f
                            val vB6 = customVitB6.toFloatOrNull() ?: 0f
                            val vB12 = customVitB12.toFloatOrNull() ?: 0f
                            val vC = customVitC.toFloatOrNull() ?: 0f
                            val vD = customVitD.toFloatOrNull() ?: 0f
                            val vE = customVitE.toFloatOrNull() ?: 0f
                            val vK = customVitK.toFloatOrNull() ?: 0f
                            val fol = customFolate.toFloatOrNull() ?: 0f
                            val ir = customIron.toFloatOrNull() ?: 0f
                            val ca = customCalcium.toFloatOrNull() ?: 0f

                            // Save to custom foods DB
                            onSaveCustomFood(
                                CustomFoodItem(
                                    name = customName.trim(),
                                    servingUnit = customServingUnit.label,
                                    defaultServing = qty,
                                    caloriesPerBase = cal,
                                    proteinPerBase = pro,
                                    carbsPerBase = carb,
                                    fatPerBase = fat,
                                    fiberPerBase = fib,
                                    vitAPerBase = vA,
                                    vitB1PerBase = vB1,
                                    vitB2PerBase = vB2,
                                    vitB3PerBase = vB3,
                                    vitB6PerBase = vB6,
                                    vitB12PerBase = vB12,
                                    vitCPerBase = vC,
                                    vitDPerBase = vD,
                                    vitEPerBase = vE,
                                    vitKPerBase = vK,
                                    folatePerBase = fol,
                                    ironPerBase = ir,
                                    calciumPerBase = ca
                                )
                            )

                            // Also log it immediately
                            val food = FoodItem(
                                name = customName.trim(),
                                category = "Custom",
                                servingUnit = customServingUnit,
                                defaultServing = qty,
                                caloriesPerBase = cal,
                                proteinPerBase = pro,
                                carbsPerBase = carb,
                                fatPerBase = fat,
                                fiberPerBase = fib,
                                vitAPerBase = vA,
                                vitB1PerBase = vB1,
                                vitB2PerBase = vB2,
                                vitB3PerBase = vB3,
                                vitB6PerBase = vB6,
                                vitB12PerBase = vB12,
                                vitCPerBase = vC,
                                vitDPerBase = vD,
                                vitEPerBase = vE,
                                vitKPerBase = vK,
                                folatePerBase = fol,
                                ironPerBase = ir,
                                calciumPerBase = ca
                            )
                            onLog(food, qty)
                        }
                    },
                    enabled = customName.isNotBlank() && qty > 0 && cal > 0
                ) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomForm = false }) { Text("Back") }
            }
        )
    } else if (selectedFood != null) {
        val food = selectedFood!!
        val qty = quantity.toFloatOrNull() ?: 0f
        val multiplier = if (qty > 0) qty / food.baseAmount else 0f

        AlertDialog(
            onDismissRequest = { selectedFood = null },
            title = { Text(food.name) },
            text = {
                Column {
                    val unitLabel = when (food.servingUnit) {
                        ServingUnit.PIECE -> "Quantity (pieces)"
                        ServingUnit.ML -> "Quantity (ml)"
                        ServingUnit.GRAMS -> "Quantity (grams)"
                    }
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(unitLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (qty > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val displayQty = when (food.servingUnit) {
                                    ServingUnit.PIECE -> "${qty.toInt()} pc"
                                    ServingUnit.ML -> "${qty.toInt()} ml"
                                    ServingUnit.GRAMS -> "${qty.toInt()} g"
                                }
                                Text(
                                    "Nutrition for $displayQty",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NutrientRow("Calories", food.caloriesPerBase * multiplier, "cal")
                                NutrientRow("Protein", food.proteinPerBase * multiplier, "g")
                                NutrientRow("Carbs", food.carbsPerBase * multiplier, "g")
                                NutrientRow("Fat", food.fatPerBase * multiplier, "g")
                                NutrientRow("Fiber", food.fiberPerBase * multiplier, "g")

                                // Vitamins & Minerals
                                val vitaminRows = listOf(
                                    Triple("Vitamin A", food.vitAPerBase, "mcg"),
                                    Triple("Vitamin B1", food.vitB1PerBase, "mg"),
                                    Triple("Vitamin B2", food.vitB2PerBase, "mg"),
                                    Triple("Vitamin B3", food.vitB3PerBase, "mg"),
                                    Triple("Vitamin B6", food.vitB6PerBase, "mg"),
                                    Triple("Vitamin B12", food.vitB12PerBase, "mcg"),
                                    Triple("Vitamin C", food.vitCPerBase, "mg"),
                                    Triple("Vitamin D", food.vitDPerBase, "mcg"),
                                    Triple("Vitamin E", food.vitEPerBase, "mg"),
                                    Triple("Vitamin K", food.vitKPerBase, "mcg"),
                                    Triple("Folate", food.folatePerBase, "mcg"),
                                    Triple("Iron", food.ironPerBase, "mg"),
                                    Triple("Calcium", food.calciumPerBase, "mg")
                                ).filter { it.second > 0 }

                                if (vitaminRows.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    vitaminRows.forEach { (label, value, unit) ->
                                        NutrientRow(label, value * multiplier, unit)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { if (qty > 0) onLog(food, qty) },
                    enabled = qty > 0
                ) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { selectedFood = null }) { Text("Back") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log Food") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search food...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        allCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { showCustomForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Food")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredFoods, key = { "${it.category}_${it.name}" }) { food ->
                            FoodItemRow(food = food, onClick = {
                                selectedFood = food
                                quantity = food.defaultServing.let {
                                    if (food.servingUnit == ServingUnit.PIECE) it.toInt().toString() else it.toInt().toString()
                                }
                            })
                        }

                        if (filteredFoods.isEmpty()) {
                            item {
                                Text(
                                    "No foods found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
private fun FoodItemRow(food: FoodItem, onClick: () -> Unit) {
    val servingLabel = when (food.servingUnit) {
        ServingUnit.PIECE -> "per pc"
        ServingUnit.ML -> "per 100ml"
        ServingUnit.GRAMS -> "per 100g"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    food.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "P: ${fmt(food.proteinPerBase)}g  C: ${fmt(food.carbsPerBase)}g  F: ${fmt(food.fatPerBase)}g  ($servingLabel)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "${food.caloriesPerBase.toInt()} cal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NutrientRow(label: String, value: Float, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            "${fmt(value)} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun fmt(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
