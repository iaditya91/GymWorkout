package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class FoodItem(
    val name: String,
    val category: String, // e.g. "Protein", "Fruit", "Dairy", "Grain", "Vegetable", "Nuts"
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f
)

@Entity(tableName = "food_log")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String = "",         // yyyy-MM-dd
    val foodName: String = "",
    val quantityGrams: Float = 0f,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f
)

object FoodDatabase {
    val foods = listOf(
        // Protein sources
        FoodItem("Egg (whole)", "Protein", 155f, 13f, 1.1f, 11f, 0f),
        FoodItem("Egg white", "Protein", 52f, 11f, 0.7f, 0.2f, 0f),
        FoodItem("Chicken breast", "Protein", 165f, 31f, 0f, 3.6f, 0f),
        FoodItem("Chicken thigh", "Protein", 209f, 26f, 0f, 10.9f, 0f),
        FoodItem("Turkey breast", "Protein", 135f, 30f, 0f, 0.7f, 0f),
        FoodItem("Salmon", "Protein", 208f, 20f, 0f, 13f, 0f),
        FoodItem("Tuna", "Protein", 130f, 28f, 0f, 1.3f, 0f),
        FoodItem("Shrimp", "Protein", 99f, 24f, 0.2f, 0.3f, 0f),
        FoodItem("Paneer", "Protein", 265f, 18f, 1.2f, 21f, 0f),
        FoodItem("Tofu", "Protein", 76f, 8f, 1.9f, 4.8f, 0.3f),
        FoodItem("Mutton", "Protein", 258f, 25f, 0f, 17f, 0f),
        FoodItem("Fish (tilapia)", "Protein", 96f, 20f, 0f, 1.7f, 0f),
        FoodItem("Beef (lean)", "Protein", 250f, 26f, 0f, 15f, 0f),
        FoodItem("Pork (lean)", "Protein", 143f, 26f, 0f, 3.5f, 0f),

        // Dairy
        FoodItem("Milk (whole)", "Dairy", 61f, 3.2f, 4.8f, 3.3f, 0f),
        FoodItem("Milk (skimmed)", "Dairy", 34f, 3.4f, 5f, 0.1f, 0f),
        FoodItem("Yogurt (plain)", "Dairy", 59f, 10f, 3.6f, 0.4f, 0f),
        FoodItem("Greek yogurt", "Dairy", 97f, 9f, 3.6f, 5f, 0f),
        FoodItem("Cheese (cheddar)", "Dairy", 403f, 25f, 1.3f, 33f, 0f),
        FoodItem("Cottage cheese", "Dairy", 98f, 11f, 3.4f, 4.3f, 0f),
        FoodItem("Butter", "Dairy", 717f, 0.9f, 0.1f, 81f, 0f),
        FoodItem("Whey protein", "Dairy", 400f, 80f, 10f, 5f, 0f),

        // Fruits
        FoodItem("Apple", "Fruit", 52f, 0.3f, 14f, 0.2f, 2.4f),
        FoodItem("Banana", "Fruit", 89f, 1.1f, 23f, 0.3f, 2.6f),
        FoodItem("Orange", "Fruit", 47f, 0.9f, 12f, 0.1f, 2.4f),
        FoodItem("Mango", "Fruit", 60f, 0.8f, 15f, 0.4f, 1.6f),
        FoodItem("Grapes", "Fruit", 69f, 0.7f, 18f, 0.2f, 0.9f),
        FoodItem("Watermelon", "Fruit", 30f, 0.6f, 8f, 0.2f, 0.4f),
        FoodItem("Papaya", "Fruit", 43f, 0.5f, 11f, 0.3f, 1.7f),
        FoodItem("Pineapple", "Fruit", 50f, 0.5f, 13f, 0.1f, 1.4f),
        FoodItem("Strawberry", "Fruit", 32f, 0.7f, 7.7f, 0.3f, 2f),
        FoodItem("Blueberry", "Fruit", 57f, 0.7f, 14f, 0.3f, 2.4f),
        FoodItem("Pomegranate", "Fruit", 83f, 1.7f, 19f, 1.2f, 4f),
        FoodItem("Guava", "Fruit", 68f, 2.6f, 14f, 1f, 5.4f),

        // Vegetables
        FoodItem("Spinach", "Vegetable", 23f, 2.9f, 3.6f, 0.4f, 2.2f),
        FoodItem("Broccoli", "Vegetable", 34f, 2.8f, 7f, 0.4f, 2.6f),
        FoodItem("Carrot", "Vegetable", 41f, 0.9f, 10f, 0.2f, 2.8f),
        FoodItem("Tomato", "Vegetable", 18f, 0.9f, 3.9f, 0.2f, 1.2f),
        FoodItem("Cucumber", "Vegetable", 15f, 0.7f, 3.6f, 0.1f, 0.5f),
        FoodItem("Onion", "Vegetable", 40f, 1.1f, 9.3f, 0.1f, 1.7f),
        FoodItem("Potato", "Vegetable", 77f, 2f, 17f, 0.1f, 2.2f),
        FoodItem("Sweet potato", "Vegetable", 86f, 1.6f, 20f, 0.1f, 3f),
        FoodItem("Capsicum", "Vegetable", 31f, 1f, 6f, 0.3f, 2.1f),
        FoodItem("Cauliflower", "Vegetable", 25f, 1.9f, 5f, 0.3f, 2f),
        FoodItem("Mushroom", "Vegetable", 22f, 3.1f, 3.3f, 0.3f, 1f),
        FoodItem("Peas (green)", "Vegetable", 81f, 5.4f, 14f, 0.4f, 5.1f),
        FoodItem("Corn", "Vegetable", 86f, 3.3f, 19f, 1.4f, 2.7f),
        FoodItem("Beetroot", "Vegetable", 43f, 1.6f, 10f, 0.2f, 2.8f),

        // Grains & Carbs
        FoodItem("Rice (white, cooked)", "Grain", 130f, 2.7f, 28f, 0.3f, 0.4f),
        FoodItem("Rice (brown, cooked)", "Grain", 112f, 2.6f, 23f, 0.9f, 1.8f),
        FoodItem("Oats", "Grain", 389f, 17f, 66f, 7f, 11f),
        FoodItem("Wheat bread", "Grain", 265f, 9f, 49f, 3.2f, 2.7f),
        FoodItem("Roti/Chapati", "Grain", 297f, 9.8f, 56f, 3.7f, 4f),
        FoodItem("Pasta (cooked)", "Grain", 131f, 5f, 25f, 1.1f, 1.8f),
        FoodItem("Quinoa (cooked)", "Grain", 120f, 4.4f, 21f, 1.9f, 2.8f),
        FoodItem("Cornflakes", "Grain", 357f, 7f, 84f, 0.4f, 1.2f),
        FoodItem("Muesli", "Grain", 340f, 10f, 56f, 8f, 7.5f),
        FoodItem("Poha (flattened rice)", "Grain", 110f, 2.5f, 23f, 0.5f, 1f),

        // Lentils & Legumes
        FoodItem("Dal (cooked)", "Legume", 116f, 9f, 20f, 0.4f, 8f),
        FoodItem("Chickpeas (cooked)", "Legume", 164f, 9f, 27f, 2.6f, 8f),
        FoodItem("Kidney beans (cooked)", "Legume", 127f, 9f, 22f, 0.5f, 6.4f),
        FoodItem("Moong dal (cooked)", "Legume", 105f, 7f, 19f, 0.4f, 7.6f),
        FoodItem("Soybean", "Legume", 446f, 36f, 30f, 20f, 9.3f),
        FoodItem("Black beans (cooked)", "Legume", 132f, 9f, 24f, 0.5f, 8.7f),

        // Nuts & Seeds
        FoodItem("Almonds", "Nuts", 579f, 21f, 22f, 50f, 12f),
        FoodItem("Peanuts", "Nuts", 567f, 26f, 16f, 49f, 8.5f),
        FoodItem("Cashews", "Nuts", 553f, 18f, 30f, 44f, 3.3f),
        FoodItem("Walnuts", "Nuts", 654f, 15f, 14f, 65f, 6.7f),
        FoodItem("Peanut butter", "Nuts", 588f, 25f, 20f, 50f, 6f),
        FoodItem("Chia seeds", "Nuts", 486f, 17f, 42f, 31f, 34f),
        FoodItem("Flax seeds", "Nuts", 534f, 18f, 29f, 42f, 27f),
        FoodItem("Sunflower seeds", "Nuts", 584f, 21f, 20f, 51f, 8.6f),

        // Oils & Fats
        FoodItem("Olive oil", "Oil", 884f, 0f, 0f, 100f, 0f),
        FoodItem("Coconut oil", "Oil", 862f, 0f, 0f, 100f, 0f),
        FoodItem("Ghee", "Oil", 900f, 0f, 0f, 100f, 0f),

        // Snacks & Others
        FoodItem("Dark chocolate", "Snack", 546f, 5f, 60f, 31f, 7f),
        FoodItem("Honey", "Snack", 304f, 0.3f, 82f, 0f, 0.2f),
        FoodItem("Samosa", "Snack", 262f, 4f, 24f, 17f, 1.5f),
        FoodItem("Idli", "Snack", 58f, 2f, 12f, 0.1f, 0.5f),
        FoodItem("Dosa", "Snack", 133f, 2.7f, 18f, 5.2f, 0.7f),
        FoodItem("Upma", "Snack", 95f, 2.5f, 14f, 3f, 1f),

        // Beverages
        FoodItem("Black coffee", "Beverage", 2f, 0.3f, 0f, 0f, 0f),
        FoodItem("Tea (with milk)", "Beverage", 37f, 1f, 5f, 1.2f, 0f),
        FoodItem("Coconut water", "Beverage", 19f, 0.7f, 3.7f, 0.2f, 1.1f),
        FoodItem("Orange juice", "Beverage", 45f, 0.7f, 10f, 0.2f, 0.2f),
        FoodItem("Protein shake", "Beverage", 120f, 24f, 5f, 1.5f, 0.5f),
        FoodItem("Lassi", "Beverage", 75f, 3f, 11f, 2f, 0f)
    )

    val categories = foods.map { it.category }.distinct().sorted()

    fun search(query: String): List<FoodItem> {
        if (query.isBlank()) return foods
        val lower = query.lowercase()
        return foods.filter { it.name.lowercase().contains(lower) }
    }

    fun getByCategory(category: String): List<FoodItem> {
        return foods.filter { it.category == category }
    }
}
