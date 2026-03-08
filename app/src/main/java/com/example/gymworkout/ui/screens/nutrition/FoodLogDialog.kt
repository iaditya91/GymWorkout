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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.gymworkout.data.FoodDatabase
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.ServingUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodLogDialog(
    onDismiss: () -> Unit,
    onLog: (FoodItem, Float) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var quantity by remember { mutableStateOf("") }

    val filteredFoods = remember(searchQuery, selectedCategory) {
        val bySearch = if (searchQuery.isBlank()) FoodDatabase.foods else FoodDatabase.search(searchQuery)
        if (selectedCategory != null) bySearch.filter { it.category == selectedCategory } else bySearch
    }

    if (selectedFood != null) {
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
                        FoodDatabase.categories.forEach { cat ->
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

                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredFoods, key = { it.name }) { food ->
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
