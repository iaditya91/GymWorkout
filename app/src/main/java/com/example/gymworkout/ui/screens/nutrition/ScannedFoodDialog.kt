package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.CustomFoodItem
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.ServingUnit
import com.example.gymworkout.viewmodel.NutritionViewModel

@Composable
fun ScannedFoodDialog(
    state: NutritionViewModel.BarcodeLookupState,
    onDismiss: () -> Unit,
    onLog: (FoodItem, Float) -> Unit,
    onSave: (NutritionViewModel.ScannedProduct) -> Unit,
    onSaveAndLog: (NutritionViewModel.ScannedProduct, Float) -> Unit
) {
    when (state) {
        is NutritionViewModel.BarcodeLookupState.Loading -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Looking up product...") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching nutrition info from Open Food Facts")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is NutritionViewModel.BarcodeLookupState.Success -> {
            val product = state.product
            val food = product.foodItem
            var quantity by remember { mutableStateOf("100") }
            val qty = quantity.toFloatOrNull() ?: 0f
            val multiplier = if (qty > 0) qty / food.baseAmount else 0f
            var saved by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Column {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        if (product.brand.isNotBlank()) {
                            Text(
                                product.brand,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity (grams)") },
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
                                    Text(
                                        "Nutrition for ${qty.toInt()}g",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NutrientRowScanned("Calories", food.caloriesPerBase * multiplier, "cal")
                                    NutrientRowScanned("Protein", food.proteinPerBase * multiplier, "g")
                                    NutrientRowScanned("Carbs", food.carbsPerBase * multiplier, "g")
                                    NutrientRowScanned("Fat", food.fatPerBase * multiplier, "g")
                                    NutrientRowScanned("Fiber", food.fiberPerBase * multiplier, "g")

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
                                        Triple("Calcium", food.calciumPerBase, "mg"),
                                        Triple("Magnesium", food.magnesiumPerBase, "mg"),
                                        Triple("Potassium", food.potassiumPerBase, "mg"),
                                        Triple("Zinc", food.zincPerBase, "mg"),
                                        Triple("Copper", food.copperPerBase, "mg"),
                                        Triple("Selenium", food.seleniumPerBase, "mcg")
                                    ).filter { it.second > 0 }

                                    if (vitaminRows.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        vitaminRows.forEach { (label, value, unit) ->
                                            NutrientRowScanned(label, value * multiplier, unit)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save to custom foods button
                        FilledTonalButton(
                            onClick = {
                                onSave(product)
                                saved = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !saved
                        ) {
                            Icon(
                                if (saved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (saved) "Saved to Custom Foods" else "Save to Custom Foods")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (qty > 0) {
                                if (!saved) onSaveAndLog(product, qty) else onLog(food, qty)
                            }
                        },
                        enabled = qty > 0
                    ) { Text(if (!saved) "Save & Log" else "Log") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is NutritionViewModel.BarcodeLookupState.Error -> {
            ManualEntryDialog(
                errorMessage = state.message,
                onDismiss = onDismiss,
                onLog = onLog,
                onSave = onSave,
                onSaveAndLog = onSaveAndLog
            )
        }
        else -> {}
    }
}

@Composable
private fun NutrientRowScanned(label: String, value: Float, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            "${fmtScanned(value)} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun fmtScanned(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)

@Composable
private fun ManualEntryDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onLog: (FoodItem, Float) -> Unit,
    onSave: (NutritionViewModel.ScannedProduct) -> Unit,
    onSaveAndLog: (NutritionViewModel.ScannedProduct, Float) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }

    // Macros
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }

    // Vitamins & minerals
    var vitA by remember { mutableStateOf("") }
    var vitB1 by remember { mutableStateOf("") }
    var vitB2 by remember { mutableStateOf("") }
    var vitB3 by remember { mutableStateOf("") }
    var vitB6 by remember { mutableStateOf("") }
    var vitB12 by remember { mutableStateOf("") }
    var vitC by remember { mutableStateOf("") }
    var vitD by remember { mutableStateOf("") }
    var vitE by remember { mutableStateOf("") }
    var vitK by remember { mutableStateOf("") }
    var folate by remember { mutableStateOf("") }
    var iron by remember { mutableStateOf("") }
    var calcium by remember { mutableStateOf("") }
    var magnesium by remember { mutableStateOf("") }
    var potassium by remember { mutableStateOf("") }
    var zinc by remember { mutableStateOf("") }
    var copper by remember { mutableStateOf("") }
    var selenium by remember { mutableStateOf("") }

    var saved by remember { mutableStateOf(false) }

    val cal = calories.toFloatOrNull() ?: 0f
    val qty = quantity.toFloatOrNull() ?: 0f

    fun buildFoodItem(): FoodItem = FoodItem(
        name = productName.trim(),
        category = "Scanned",
        servingUnit = ServingUnit.GRAMS,
        defaultServing = 100f,
        caloriesPerBase = cal,
        proteinPerBase = protein.toFloatOrNull() ?: 0f,
        carbsPerBase = carbs.toFloatOrNull() ?: 0f,
        fatPerBase = fat.toFloatOrNull() ?: 0f,
        fiberPerBase = fiber.toFloatOrNull() ?: 0f,
        vitAPerBase = vitA.toFloatOrNull() ?: 0f,
        vitB1PerBase = vitB1.toFloatOrNull() ?: 0f,
        vitB2PerBase = vitB2.toFloatOrNull() ?: 0f,
        vitB3PerBase = vitB3.toFloatOrNull() ?: 0f,
        vitB6PerBase = vitB6.toFloatOrNull() ?: 0f,
        vitB12PerBase = vitB12.toFloatOrNull() ?: 0f,
        vitCPerBase = vitC.toFloatOrNull() ?: 0f,
        vitDPerBase = vitD.toFloatOrNull() ?: 0f,
        vitEPerBase = vitE.toFloatOrNull() ?: 0f,
        vitKPerBase = vitK.toFloatOrNull() ?: 0f,
        folatePerBase = folate.toFloatOrNull() ?: 0f,
        ironPerBase = iron.toFloatOrNull() ?: 0f,
        calciumPerBase = calcium.toFloatOrNull() ?: 0f,
        magnesiumPerBase = magnesium.toFloatOrNull() ?: 0f,
        potassiumPerBase = potassium.toFloatOrNull() ?: 0f,
        zincPerBase = zinc.toFloatOrNull() ?: 0f,
        copperPerBase = copper.toFloatOrNull() ?: 0f,
        seleniumPerBase = selenium.toFloatOrNull() ?: 0f
    )

    fun buildProduct(): NutritionViewModel.ScannedProduct {
        val food = buildFoodItem()
        return NutritionViewModel.ScannedProduct(
            barcode = "",
            name = productName.trim(),
            brand = "",
            foodItem = food
        )
    }

    val isValid = productName.isNotBlank() && cal > 0 && qty > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product Manually") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Error info
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. Granola Bar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Nutrition per 100g",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Macros
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calories, onValueChange = { calories = it },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = protein, onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs, onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat, onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fiber, onValueChange = { fiber = it },
                    label = { Text("Fiber (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                // Vitamins & Minerals
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Vitamins & Minerals (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vitA, onValueChange = { vitA = it }, label = { Text("Vit A (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vitC, onValueChange = { vitC = it }, label = { Text("Vit C (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vitD, onValueChange = { vitD = it }, label = { Text("Vit D (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vitE, onValueChange = { vitE = it }, label = { Text("Vit E (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vitK, onValueChange = { vitK = it }, label = { Text("Vit K (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vitB1, onValueChange = { vitB1 = it }, label = { Text("B1 (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vitB2, onValueChange = { vitB2 = it }, label = { Text("B2 (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vitB3, onValueChange = { vitB3 = it }, label = { Text("B3 (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vitB6, onValueChange = { vitB6 = it }, label = { Text("B6 (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vitB12, onValueChange = { vitB12 = it }, label = { Text("B12 (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = folate, onValueChange = { folate = it }, label = { Text("Folate (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = iron, onValueChange = { iron = it }, label = { Text("Iron (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = calcium, onValueChange = { calcium = it }, label = { Text("Calcium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = magnesium, onValueChange = { magnesium = it }, label = { Text("Magnesium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = potassium, onValueChange = { potassium = it }, label = { Text("Potassium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = zinc, onValueChange = { zinc = it }, label = { Text("Zinc (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = copper, onValueChange = { copper = it }, label = { Text("Copper (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = selenium, onValueChange = { selenium = it }, label = { Text("Selenium (mcg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save to custom foods button
                FilledTonalButton(
                    onClick = {
                        if (isValid) {
                            onSave(buildProduct())
                            saved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isValid && !saved
                ) {
                    Icon(
                        if (saved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (saved) "Saved to Custom Foods" else "Save to Custom Foods")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val product = buildProduct()
                        if (!saved) onSaveAndLog(product, qty) else onLog(buildFoodItem(), qty)
                    }
                },
                enabled = isValid
            ) { Text(if (!saved) "Save & Log" else "Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
