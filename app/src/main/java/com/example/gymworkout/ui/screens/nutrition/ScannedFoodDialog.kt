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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Product Not Found") },
                text = {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
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
