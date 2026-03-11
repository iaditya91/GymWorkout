package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ServingUnit(val label: String) {
    GRAMS("g"),
    ML("ml"),
    PIECE("pc")
}

data class FoodItem(
    val name: String,
    val category: String,
    val servingUnit: ServingUnit = ServingUnit.GRAMS,
    val defaultServing: Float = 100f,
    // Macros per base (per 100g/100ml or per 1 piece)
    val caloriesPerBase: Float,
    val proteinPerBase: Float,
    val carbsPerBase: Float,
    val fatPerBase: Float,
    val fiberPerBase: Float = 0f,
    // Vitamins per base
    val vitAPerBase: Float = 0f,    // mcg RAE
    val vitB1PerBase: Float = 0f,   // mg (Thiamine)
    val vitB2PerBase: Float = 0f,   // mg (Riboflavin)
    val vitB3PerBase: Float = 0f,   // mg (Niacin)
    val vitB6PerBase: Float = 0f,   // mg
    val vitB12PerBase: Float = 0f,  // mcg
    val vitCPerBase: Float = 0f,    // mg
    val vitDPerBase: Float = 0f,    // mcg
    val vitEPerBase: Float = 0f,    // mg
    val vitKPerBase: Float = 0f,    // mcg
    val folatePerBase: Float = 0f,  // mcg
    // Minerals per base
    val ironPerBase: Float = 0f,       // mg
    val calciumPerBase: Float = 0f,    // mg
    val magnesiumPerBase: Float = 0f,  // mg
    val potassiumPerBase: Float = 0f,  // mg
    val zincPerBase: Float = 0f,       // mg
    val copperPerBase: Float = 0f,     // mg
    val seleniumPerBase: Float = 0f    // mcg
) {
    val baseAmount: Float get() = if (servingUnit == ServingUnit.PIECE) 1f else 100f
}

@Entity(tableName = "custom_foods")
data class CustomFoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val servingUnit: String = "g",
    val defaultServing: Float = 100f,
    val caloriesPerBase: Float = 0f,
    val proteinPerBase: Float = 0f,
    val carbsPerBase: Float = 0f,
    val fatPerBase: Float = 0f,
    val fiberPerBase: Float = 0f,
    val vitAPerBase: Float = 0f,
    val vitB1PerBase: Float = 0f,
    val vitB2PerBase: Float = 0f,
    val vitB3PerBase: Float = 0f,
    val vitB6PerBase: Float = 0f,
    val vitB12PerBase: Float = 0f,
    val vitCPerBase: Float = 0f,
    val vitDPerBase: Float = 0f,
    val vitEPerBase: Float = 0f,
    val vitKPerBase: Float = 0f,
    val folatePerBase: Float = 0f,
    val ironPerBase: Float = 0f,
    val calciumPerBase: Float = 0f,
    val magnesiumPerBase: Float = 0f,
    val potassiumPerBase: Float = 0f,
    val zincPerBase: Float = 0f,
    val copperPerBase: Float = 0f,
    val seleniumPerBase: Float = 0f
) {
    fun toFoodItem(): FoodItem = FoodItem(
        name = name,
        category = "Custom",
        servingUnit = ServingUnit.entries.first { it.label == servingUnit },
        defaultServing = defaultServing,
        caloriesPerBase = caloriesPerBase,
        proteinPerBase = proteinPerBase,
        carbsPerBase = carbsPerBase,
        fatPerBase = fatPerBase,
        fiberPerBase = fiberPerBase,
        vitAPerBase = vitAPerBase,
        vitB1PerBase = vitB1PerBase,
        vitB2PerBase = vitB2PerBase,
        vitB3PerBase = vitB3PerBase,
        vitB6PerBase = vitB6PerBase,
        vitB12PerBase = vitB12PerBase,
        vitCPerBase = vitCPerBase,
        vitDPerBase = vitDPerBase,
        vitEPerBase = vitEPerBase,
        vitKPerBase = vitKPerBase,
        folatePerBase = folatePerBase,
        ironPerBase = ironPerBase,
        calciumPerBase = calciumPerBase,
        magnesiumPerBase = magnesiumPerBase,
        potassiumPerBase = potassiumPerBase,
        zincPerBase = zincPerBase,
        copperPerBase = copperPerBase,
        seleniumPerBase = seleniumPerBase
    )
}

@Entity(tableName = "food_log")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String = "",
    val foodName: String = "",
    val quantity: Float = 0f,
    val unit: String = "g",
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val vitaminA: Float = 0f,
    val vitaminB1: Float = 0f,
    val vitaminB2: Float = 0f,
    val vitaminB3: Float = 0f,
    val vitaminB6: Float = 0f,
    val vitaminB12: Float = 0f,
    val vitaminC: Float = 0f,
    val vitaminD: Float = 0f,
    val vitaminE: Float = 0f,
    val vitaminK: Float = 0f,
    val folate: Float = 0f,
    val iron: Float = 0f,
    val calcium: Float = 0f,
    val magnesium: Float = 0f,
    val potassium: Float = 0f,
    val zinc: Float = 0f,
    val copper: Float = 0f,
    val selenium: Float = 0f
)

// Helper to build FoodItem concisely: macros + vitamins + minerals
private fun food(
    name: String, cat: String, unit: ServingUnit = ServingUnit.GRAMS, defServing: Float = 100f,
    cal: Float, pro: Float, carb: Float, fat: Float, fiber: Float = 0f,
    vA: Float = 0f, vB1: Float = 0f, vB2: Float = 0f, vB3: Float = 0f,
    vB6: Float = 0f, vB12: Float = 0f, vC: Float = 0f, vD: Float = 0f,
    vE: Float = 0f, vK: Float = 0f, folate: Float = 0f,
    iron: Float = 0f, calcium: Float = 0f,
    mg: Float = 0f, k: Float = 0f, zn: Float = 0f, cu: Float = 0f, se: Float = 0f
) = FoodItem(name, cat, unit, defServing, cal, pro, carb, fat, fiber,
    vA, vB1, vB2, vB3, vB6, vB12, vC, vD, vE, vK, folate, iron, calcium,
    mg, k, zn, cu, se)

object FoodDatabase {
    val foods = listOf(
        // === PROTEIN (per 100g unless piece) ===
        // mg=magnesium(mg), k=potassium(mg), zn=zinc(mg), cu=copper(mg), se=selenium(mcg)
        food("Egg (whole)", "Protein", ServingUnit.PIECE, 1f, 78f, 6.3f, 0.6f, 5.3f, 0f,
            vA=80f, vB1=0.04f, vB2=0.23f, vB3=0.04f, vB6=0.07f, vB12=0.56f, vD=1.1f, vE=0.5f, vK=0.2f, folate=24f, iron=0.9f, calcium=28f, mg=6f, k=69f, zn=0.6f, cu=0.01f, se=15.4f),
        food("Egg white", "Protein", ServingUnit.PIECE, 1f, 17f, 3.6f, 0.2f, 0.1f, 0f,
            vB2=0.15f, vB3=0.04f, folate=1f, iron=0f, calcium=2f, mg=4f, k=54f, zn=0f, cu=0.01f, se=6.6f),
        food("Chicken breast", "Protein", ServingUnit.GRAMS, 150f, 165f, 31f, 0f, 3.6f, 0f,
            vA=6f, vB1=0.06f, vB2=0.11f, vB3=13.7f, vB6=0.6f, vB12=0.3f, vE=0.3f, iron=0.7f, calcium=11f, mg=29f, k=256f, zn=0.9f, cu=0.04f, se=27.6f),
        food("Chicken thigh", "Protein", ServingUnit.GRAMS, 150f, 209f, 26f, 0f, 10.9f, 0f,
            vA=18f, vB1=0.07f, vB2=0.16f, vB3=6.5f, vB6=0.35f, vB12=0.3f, iron=0.9f, calcium=9f, mg=23f, k=222f, zn=1.8f, cu=0.06f, se=22f),
        food("Turkey breast", "Protein", ServingUnit.GRAMS, 150f, 135f, 30f, 0f, 0.7f, 0f,
            vB1=0.05f, vB2=0.14f, vB3=11.8f, vB6=0.8f, vB12=0.4f, vD=0.4f, iron=0.7f, calcium=8f, mg=27f, k=293f, zn=1.2f, cu=0.04f, se=30.2f),
        food("Salmon", "Protein", ServingUnit.GRAMS, 150f, 208f, 20f, 0f, 13f, 0f,
            vA=40f, vB1=0.23f, vB2=0.38f, vB3=8f, vB6=0.8f, vB12=3.2f, vD=11f, vE=3.6f, iron=0.3f, calcium=12f, mg=29f, k=363f, zn=0.6f, cu=0.05f, se=36.5f),
        food("Tuna", "Protein", ServingUnit.GRAMS, 150f, 130f, 28f, 0f, 1.3f, 0f,
            vA=18f, vB1=0.05f, vB2=0.12f, vB3=10.5f, vB6=0.5f, vB12=2.2f, vD=1.7f, iron=1f, calcium=8f, mg=35f, k=252f, zn=0.6f, cu=0.04f, se=90.6f),
        food("Shrimp", "Protein", ServingUnit.GRAMS, 100f, 99f, 24f, 0.2f, 0.3f, 0f,
            vA=54f, vB1=0.03f, vB2=0.03f, vB3=2.6f, vB6=0.1f, vB12=1.1f, vC=2f, vE=1.3f, iron=1.8f, calcium=52f, mg=37f, k=259f, zn=1.6f, cu=0.26f, se=38f),
        food("Paneer", "Protein", ServingUnit.GRAMS, 100f, 265f, 18f, 1.2f, 21f, 0f,
            vA=120f, vB2=0.4f, vB12=0.8f, vD=0.5f, calcium=480f, iron=0.2f, mg=20f, k=100f, zn=2.7f, cu=0.03f, se=14f),
        food("Tofu", "Protein", ServingUnit.GRAMS, 100f, 76f, 8f, 1.9f, 4.8f, 0.3f,
            vB1=0.06f, vB2=0.05f, vB6=0.05f, folate=15f, iron=2.7f, calcium=350f, mg=30f, k=121f, zn=0.8f, cu=0.19f, se=8.9f),
        food("Mutton", "Protein", ServingUnit.GRAMS, 150f, 258f, 25f, 0f, 17f, 0f,
            vB1=0.1f, vB2=0.23f, vB3=5.7f, vB6=0.3f, vB12=2.6f, iron=1.9f, calcium=12f, mg=23f, k=310f, zn=4.4f, cu=0.12f, se=26f),
        food("Fish (tilapia)", "Protein", ServingUnit.GRAMS, 150f, 96f, 20f, 0f, 1.7f, 0f,
            vB1=0.04f, vB2=0.06f, vB3=3.9f, vB6=0.2f, vB12=1.6f, vD=3.1f, iron=0.6f, calcium=10f, mg=27f, k=302f, zn=0.3f, cu=0.03f, se=41.8f),
        food("Beef (lean)", "Protein", ServingUnit.GRAMS, 150f, 250f, 26f, 0f, 15f, 0f,
            vB1=0.05f, vB2=0.18f, vB3=5.4f, vB6=0.4f, vB12=2.6f, vE=0.4f, vK=1.5f, iron=2.6f, calcium=12f, mg=21f, k=318f, zn=6.3f, cu=0.08f, se=28.5f),
        food("Pork (lean)", "Protein", ServingUnit.GRAMS, 150f, 143f, 26f, 0f, 3.5f, 0f,
            vA=2f, vB1=0.88f, vB2=0.23f, vB3=5.3f, vB6=0.5f, vB12=0.7f, vC=0.6f, vD=0.5f, iron=0.5f, calcium=6f, mg=25f, k=362f, zn=2.4f, cu=0.06f, se=33.2f),

        // === DAIRY ===
        food("Milk (whole)", "Dairy", ServingUnit.ML, 200f, 61f, 3.2f, 4.8f, 3.3f, 0f,
            vA=46f, vB1=0.04f, vB2=0.18f, vB12=0.45f, vD=1.3f, vK=0.3f, calcium=113f, mg=10f, k=132f, zn=0.4f, cu=0.01f, se=3.7f),
        food("Milk (skimmed)", "Dairy", ServingUnit.ML, 200f, 34f, 3.4f, 5f, 0.1f, 0f,
            vA=1f, vB1=0.04f, vB2=0.18f, vB12=0.5f, vD=1.2f, calcium=122f, mg=11f, k=156f, zn=0.4f, cu=0.01f, se=3.3f),
        food("Yogurt (plain)", "Dairy", ServingUnit.GRAMS, 150f, 59f, 10f, 3.6f, 0.4f, 0f,
            vA=2f, vB2=0.28f, vB12=0.75f, vC=0.5f, calcium=110f, mg=11f, k=141f, zn=0.5f, cu=0.01f, se=2.2f),
        food("Greek yogurt", "Dairy", ServingUnit.GRAMS, 150f, 97f, 9f, 3.6f, 5f, 0f,
            vA=26f, vB2=0.27f, vB12=0.75f, calcium=100f, mg=11f, k=141f, zn=0.5f, cu=0.02f, se=9.7f),
        food("Cheese (cheddar)", "Dairy", ServingUnit.GRAMS, 30f, 403f, 25f, 1.3f, 33f, 0f,
            vA=265f, vB2=0.37f, vB12=0.83f, vD=0.3f, vK=2.8f, calcium=721f, iron=0.7f, mg=28f, k=98f, zn=3.1f, cu=0.03f, se=13.9f),
        food("Cottage cheese", "Dairy", ServingUnit.GRAMS, 100f, 98f, 11f, 3.4f, 4.3f, 0f,
            vA=37f, vB2=0.16f, vB12=0.43f, calcium=83f, mg=8f, k=104f, zn=0.4f, cu=0.03f, se=9.7f),
        food("Butter", "Dairy", ServingUnit.GRAMS, 10f, 717f, 0.9f, 0.1f, 81f, 0f,
            vA=684f, vD=0.6f, vE=2.3f, vK=7f, calcium=24f, mg=2f, k=24f, zn=0.1f, cu=0f, se=1f),
        food("Whey protein", "Dairy", ServingUnit.GRAMS, 30f, 400f, 80f, 10f, 5f, 0f,
            vB6=0.5f, vB12=1f, iron=2f, calcium=100f, mg=80f, k=400f, zn=3f, cu=0.1f, se=20f),
        food("Lassi", "Dairy", ServingUnit.ML, 200f, 75f, 3f, 11f, 2f, 0f,
            vA=20f, vB2=0.15f, vB12=0.3f, vC=1f, calcium=90f, mg=10f, k=130f, zn=0.4f, cu=0.01f, se=2f),

        // === FRUITS ===
        food("Apple", "Fruit", ServingUnit.PIECE, 1f, 95f, 0.5f, 25f, 0.3f, 4.4f,
            vA=3f, vB6=0.06f, vC=8.4f, vE=0.3f, vK=4f, folate=5f, iron=0.2f, calcium=11f, mg=9f, k=195f, zn=0.1f, cu=0.05f, se=0f),
        food("Banana", "Fruit", ServingUnit.PIECE, 1f, 105f, 1.3f, 27f, 0.4f, 3.1f,
            vA=4f, vB1=0.04f, vB6=0.43f, vC=10.3f, vE=0.1f, vK=0.6f, folate=24f, iron=0.3f, calcium=6f, mg=32f, k=422f, zn=0.2f, cu=0.09f, se=1.5f),
        food("Orange", "Fruit", ServingUnit.PIECE, 1f, 62f, 1.2f, 15f, 0.2f, 3.1f,
            vA=14f, vB1=0.11f, vB6=0.08f, vC=70f, vE=0.2f, folate=39f, calcium=52f, mg=13f, k=237f, zn=0.1f, cu=0.06f, se=0.7f),
        food("Mango", "Fruit", ServingUnit.GRAMS, 150f, 60f, 0.8f, 15f, 0.4f, 1.6f,
            vA=54f, vB6=0.12f, vC=36f, vE=0.9f, vK=4.2f, folate=43f, iron=0.2f, calcium=11f, mg=10f, k=168f, zn=0.1f, cu=0.11f, se=0.6f),
        food("Grapes", "Fruit", ServingUnit.GRAMS, 100f, 69f, 0.7f, 18f, 0.2f, 0.9f,
            vA=3f, vB6=0.09f, vC=4f, vE=0.2f, vK=14.6f, folate=2f, iron=0.4f, calcium=10f, mg=7f, k=191f, zn=0.1f, cu=0.13f, se=0.1f),
        food("Watermelon", "Fruit", ServingUnit.GRAMS, 200f, 30f, 0.6f, 8f, 0.2f, 0.4f,
            vA=28f, vB1=0.03f, vB6=0.05f, vC=8.1f, iron=0.2f, calcium=7f, mg=10f, k=112f, zn=0.1f, cu=0.04f, se=0.4f),
        food("Papaya", "Fruit", ServingUnit.GRAMS, 150f, 43f, 0.5f, 11f, 0.3f, 1.7f,
            vA=47f, vB6=0.04f, vC=62f, vE=0.3f, vK=2.6f, folate=37f, iron=0.3f, calcium=20f, mg=21f, k=182f, zn=0.1f, cu=0.02f, se=0.6f),
        food("Pineapple", "Fruit", ServingUnit.GRAMS, 150f, 50f, 0.5f, 13f, 0.1f, 1.4f,
            vB1=0.08f, vB6=0.11f, vC=48f, vE=0f, folate=18f, iron=0.3f, calcium=13f, mg=12f, k=109f, zn=0.1f, cu=0.11f, se=0.1f),
        food("Strawberry", "Fruit", ServingUnit.GRAMS, 100f, 32f, 0.7f, 7.7f, 0.3f, 2f,
            vB6=0.05f, vC=59f, vE=0.3f, vK=2.2f, folate=24f, iron=0.4f, calcium=16f, mg=13f, k=153f, zn=0.1f, cu=0.05f, se=0.4f),
        food("Blueberry", "Fruit", ServingUnit.GRAMS, 100f, 57f, 0.7f, 14f, 0.3f, 2.4f,
            vA=3f, vC=10f, vE=0.6f, vK=19.3f, folate=6f, iron=0.3f, calcium=6f, mg=6f, k=77f, zn=0.2f, cu=0.06f, se=0.1f),
        food("Pomegranate", "Fruit", ServingUnit.PIECE, 1f, 234f, 4.7f, 53f, 3.3f, 11.3f,
            vB6=0.2f, vC=29f, vE=1.7f, vK=46.2f, folate=108f, iron=0.8f, calcium=28f, mg=34f, k=666f, zn=1f, cu=0.45f, se=1.4f),
        food("Guava", "Fruit", ServingUnit.PIECE, 1f, 37f, 1.4f, 8f, 0.5f, 3f,
            vA=17f, vB6=0.06f, vC=126f, vE=0.4f, vK=1.4f, folate=27f, iron=0.1f, calcium=10f, mg=12f, k=229f, zn=0.1f, cu=0.13f, se=0.3f),

        // === VEGETABLES (per 100g) ===
        food("Spinach", "Vegetable", ServingUnit.GRAMS, 100f, 23f, 2.9f, 3.6f, 0.4f, 2.2f,
            vA=469f, vB1=0.08f, vB2=0.19f, vB6=0.2f, vC=28f, vE=2f, vK=483f, folate=194f, iron=2.7f, calcium=99f, mg=79f, k=558f, zn=0.5f, cu=0.13f, se=1f),
        food("Broccoli", "Vegetable", ServingUnit.GRAMS, 100f, 34f, 2.8f, 7f, 0.4f, 2.6f,
            vA=31f, vB1=0.07f, vB2=0.12f, vB6=0.17f, vC=89f, vE=0.8f, vK=102f, folate=63f, iron=0.7f, calcium=47f, mg=21f, k=316f, zn=0.4f, cu=0.05f, se=2.5f),
        food("Carrot", "Vegetable", ServingUnit.GRAMS, 100f, 41f, 0.9f, 10f, 0.2f, 2.8f,
            vA=835f, vB1=0.07f, vB6=0.14f, vC=6f, vE=0.7f, vK=13.2f, folate=19f, iron=0.3f, calcium=33f, mg=12f, k=320f, zn=0.2f, cu=0.05f, se=0.1f),
        food("Tomato", "Vegetable", ServingUnit.GRAMS, 100f, 18f, 0.9f, 3.9f, 0.2f, 1.2f,
            vA=42f, vB6=0.08f, vC=14f, vE=0.5f, vK=7.9f, folate=15f, iron=0.3f, calcium=10f, mg=11f, k=237f, zn=0.2f, cu=0.06f, se=0f),
        food("Cucumber", "Vegetable", ServingUnit.GRAMS, 100f, 15f, 0.7f, 3.6f, 0.1f, 0.5f,
            vA=5f, vC=2.8f, vK=16.4f, iron=0.3f, calcium=16f, mg=13f, k=147f, zn=0.2f, cu=0.04f, se=0.3f),
        food("Onion", "Vegetable", ServingUnit.GRAMS, 100f, 40f, 1.1f, 9.3f, 0.1f, 1.7f,
            vB6=0.12f, vC=7.4f, folate=19f, iron=0.2f, calcium=23f, mg=10f, k=146f, zn=0.2f, cu=0.04f, se=0.5f),
        food("Potato", "Vegetable", ServingUnit.GRAMS, 150f, 77f, 2f, 17f, 0.1f, 2.2f,
            vB1=0.08f, vB6=0.3f, vC=20f, vK=2f, folate=15f, iron=0.8f, calcium=12f, mg=23f, k=421f, zn=0.3f, cu=0.11f, se=0.4f),
        food("Sweet potato", "Vegetable", ServingUnit.GRAMS, 150f, 86f, 1.6f, 20f, 0.1f, 3f,
            vA=709f, vB6=0.21f, vC=2.4f, vE=0.3f, folate=11f, iron=0.6f, calcium=30f, mg=25f, k=337f, zn=0.3f, cu=0.15f, se=0.6f),
        food("Capsicum", "Vegetable", ServingUnit.GRAMS, 100f, 31f, 1f, 6f, 0.3f, 2.1f,
            vA=18f, vB6=0.29f, vC=128f, vE=1.6f, vK=4.9f, folate=46f, iron=0.4f, calcium=7f, mg=12f, k=211f, zn=0.3f, cu=0.02f, se=0.1f),
        food("Cauliflower", "Vegetable", ServingUnit.GRAMS, 100f, 25f, 1.9f, 5f, 0.3f, 2f,
            vB6=0.18f, vC=48f, vK=15.5f, folate=57f, iron=0.4f, calcium=22f, mg=15f, k=299f, zn=0.3f, cu=0.04f, se=0.6f),
        food("Mushroom", "Vegetable", ServingUnit.GRAMS, 100f, 22f, 3.1f, 3.3f, 0.3f, 1f,
            vB2=0.4f, vB3=3.6f, vD=7f, folate=17f, iron=0.5f, calcium=3f, mg=9f, k=318f, zn=0.5f, cu=0.32f, se=9.3f),
        food("Peas (green)", "Vegetable", ServingUnit.GRAMS, 100f, 81f, 5.4f, 14f, 0.4f, 5.1f,
            vA=38f, vB1=0.27f, vB6=0.17f, vC=40f, vE=0.1f, vK=25f, folate=65f, iron=1.5f, calcium=25f, mg=33f, k=244f, zn=1.2f, cu=0.18f, se=1.8f),
        food("Corn", "Vegetable", ServingUnit.GRAMS, 100f, 86f, 3.3f, 19f, 1.4f, 2.7f,
            vA=9f, vB1=0.16f, vB3=1.8f, vB6=0.06f, vC=6.8f, folate=46f, iron=0.5f, calcium=2f, mg=37f, k=270f, zn=0.5f, cu=0.05f, se=0.6f),
        food("Beetroot", "Vegetable", ServingUnit.GRAMS, 100f, 43f, 1.6f, 10f, 0.2f, 2.8f,
            vB6=0.07f, vC=4.9f, folate=109f, iron=0.8f, calcium=16f, mg=23f, k=325f, zn=0.4f, cu=0.08f, se=0.7f),

        // === GRAINS ===
        food("Rice (white, cooked)", "Grain", ServingUnit.GRAMS, 200f, 130f, 2.7f, 28f, 0.3f, 0.4f,
            vB1=0.02f, vB3=0.4f, folate=3f, iron=0.2f, calcium=10f, mg=12f, k=35f, zn=0.5f, cu=0.07f, se=7.5f),
        food("Rice (brown, cooked)", "Grain", ServingUnit.GRAMS, 200f, 112f, 2.6f, 23f, 0.9f, 1.8f,
            vB1=0.1f, vB3=1.5f, vB6=0.15f, folate=4f, iron=0.4f, calcium=10f, mg=44f, k=79f, zn=0.6f, cu=0.1f, se=9.8f),
        food("Oats", "Grain", ServingUnit.GRAMS, 50f, 389f, 17f, 66f, 7f, 11f,
            vB1=0.76f, vB2=0.14f, vB6=0.12f, vE=0.4f, folate=56f, iron=4.7f, calcium=54f, mg=177f, k=429f, zn=4f, cu=0.63f, se=34f),
        food("Wheat bread", "Grain", ServingUnit.PIECE, 1f, 79f, 2.7f, 15f, 1f, 0.8f,
            vB1=0.12f, vB2=0.07f, vB3=1.4f, folate=24f, iron=0.8f, calcium=36f, mg=7f, k=37f, zn=0.3f, cu=0.04f, se=7.8f),
        food("Roti/Chapati", "Grain", ServingUnit.PIECE, 1f, 104f, 3.4f, 20f, 1.3f, 1.4f,
            vB1=0.1f, vB3=1.2f, vB6=0.07f, folate=14f, iron=0.9f, calcium=10f, mg=18f, k=65f, zn=0.5f, cu=0.1f, se=12f),
        food("Pasta (cooked)", "Grain", ServingUnit.GRAMS, 200f, 131f, 5f, 25f, 1.1f, 1.8f,
            vB1=0.1f, vB3=0.7f, folate=7f, iron=1.3f, calcium=7f, mg=18f, k=44f, zn=0.5f, cu=0.1f, se=26.4f),
        food("Quinoa (cooked)", "Grain", ServingUnit.GRAMS, 150f, 120f, 4.4f, 21f, 1.9f, 2.8f,
            vB1=0.1f, vB2=0.11f, vB6=0.12f, vE=0.6f, folate=42f, iron=1.5f, calcium=17f, mg=64f, k=172f, zn=1.1f, cu=0.19f, se=2.8f),
        food("Cornflakes", "Grain", ServingUnit.GRAMS, 30f, 357f, 7f, 84f, 0.4f, 1.2f,
            vB1=1.2f, vB2=1.3f, vB6=1.7f, vB12=5f, vD=1.5f, folate=333f, iron=8f, calcium=1f, mg=16f, k=87f, zn=0.7f, cu=0.04f, se=4.4f),
        food("Muesli", "Grain", ServingUnit.GRAMS, 50f, 340f, 10f, 56f, 8f, 7.5f,
            vB1=0.5f, vB2=0.2f, vE=2f, iron=3.5f, calcium=40f, mg=100f, k=350f, zn=2.5f, cu=0.3f, se=15f),
        food("Poha (flattened rice)", "Grain", ServingUnit.GRAMS, 100f, 110f, 2.5f, 23f, 0.5f, 1f,
            vB1=0.05f, iron=1f, calcium=5f, mg=15f, k=50f, zn=0.4f, cu=0.06f, se=3f),
        food("Dosa", "Grain", ServingUnit.PIECE, 1f, 133f, 2.7f, 18f, 5.2f, 0.7f,
            vB1=0.04f, iron=0.5f, calcium=10f, mg=10f, k=45f, zn=0.3f, cu=0.05f, se=3f),
        food("Idli", "Grain", ServingUnit.PIECE, 1f, 39f, 1.3f, 8f, 0.1f, 0.3f,
            vB1=0.02f, iron=0.3f, calcium=5f, mg=5f, k=25f, zn=0.2f, cu=0.03f, se=2f),

        // === LEGUMES (per 100g cooked) ===
        food("Dal (cooked)", "Legume", ServingUnit.GRAMS, 150f, 116f, 9f, 20f, 0.4f, 8f,
            vB1=0.17f, vB6=0.18f, vC=1f, folate=181f, iron=2.5f, calcium=19f, mg=36f, k=369f, zn=1.3f, cu=0.22f, se=2.8f),
        food("Chickpeas (cooked)", "Legume", ServingUnit.GRAMS, 150f, 164f, 9f, 27f, 2.6f, 8f,
            vB1=0.12f, vB6=0.14f, vC=1.3f, vE=0.4f, vK=4f, folate=172f, iron=2.9f, calcium=49f, mg=48f, k=291f, zn=1.5f, cu=0.35f, se=3.7f),
        food("Kidney beans (cooked)", "Legume", ServingUnit.GRAMS, 150f, 127f, 9f, 22f, 0.5f, 6.4f,
            vB1=0.16f, vB6=0.12f, vC=1.2f, vK=8.4f, folate=130f, iron=2.2f, calcium=28f, mg=45f, k=403f, zn=1f, cu=0.24f, se=1.2f),
        food("Moong dal (cooked)", "Legume", ServingUnit.GRAMS, 150f, 105f, 7f, 19f, 0.4f, 7.6f,
            vB1=0.16f, vB6=0.07f, vC=1f, folate=159f, iron=1.4f, calcium=16f, mg=48f, k=266f, zn=0.8f, cu=0.16f, se=2.5f),
        food("Soybean", "Legume", ServingUnit.GRAMS, 100f, 446f, 36f, 30f, 20f, 9.3f,
            vB1=0.87f, vB2=0.87f, vB6=0.38f, vC=6f, vE=0.9f, vK=47f, folate=375f, iron=15.7f, calcium=277f, mg=280f, k=1797f, zn=4.9f, cu=1.66f, se=17.8f),
        food("Black beans (cooked)", "Legume", ServingUnit.GRAMS, 150f, 132f, 9f, 24f, 0.5f, 8.7f,
            vB1=0.24f, vB6=0.07f, folate=149f, iron=2.1f, calcium=27f, mg=70f, k=355f, zn=1.1f, cu=0.21f, se=1.2f),

        // === NUTS & SEEDS (per 100g) ===
        food("Almonds", "Nuts", ServingUnit.GRAMS, 25f, 579f, 21f, 22f, 50f, 12f,
            vB2=1.14f, vB3=3.6f, vB6=0.14f, vE=25.6f, folate=44f, iron=3.7f, calcium=269f, mg=270f, k=733f, zn=3.1f, cu=1.03f, se=4.1f),
        food("Peanuts", "Nuts", ServingUnit.GRAMS, 30f, 567f, 26f, 16f, 49f, 8.5f,
            vB1=0.64f, vB3=12.1f, vB6=0.35f, vE=8.3f, folate=240f, iron=2f, calcium=62f, mg=168f, k=705f, zn=3.3f, cu=1.14f, se=7.2f),
        food("Cashews", "Nuts", ServingUnit.GRAMS, 25f, 553f, 18f, 30f, 44f, 3.3f,
            vB1=0.42f, vB6=0.42f, vE=0.9f, vK=34.1f, folate=25f, iron=6.7f, calcium=37f, mg=292f, k=660f, zn=5.8f, cu=2.2f, se=19.9f),
        food("Walnuts", "Nuts", ServingUnit.GRAMS, 25f, 654f, 15f, 14f, 65f, 6.7f,
            vB6=0.54f, vE=0.7f, folate=98f, iron=2.9f, calcium=98f, mg=158f, k=441f, zn=3.1f, cu=1.59f, se=4.9f),
        food("Peanut butter", "Nuts", ServingUnit.GRAMS, 30f, 588f, 25f, 20f, 50f, 6f,
            vB3=13.1f, vB6=0.44f, vE=9f, folate=92f, iron=1.7f, calcium=43f, mg=154f, k=649f, zn=2.8f, cu=0.6f, se=4.1f),
        food("Chia seeds", "Nuts", ServingUnit.GRAMS, 15f, 486f, 17f, 42f, 31f, 34f,
            vB1=0.62f, vB3=8.8f, vC=1.6f, vE=0.5f, folate=49f, iron=7.7f, calcium=631f, mg=335f, k=407f, zn=4.6f, cu=0.92f, se=55.2f),
        food("Flax seeds", "Nuts", ServingUnit.GRAMS, 15f, 534f, 18f, 29f, 42f, 27f,
            vB1=1.64f, vB6=0.47f, vC=0.6f, vE=0.3f, folate=87f, iron=5.7f, calcium=255f, mg=392f, k=813f, zn=4.3f, cu=1.22f, se=25.4f),
        food("Sunflower seeds", "Nuts", ServingUnit.GRAMS, 25f, 584f, 21f, 20f, 51f, 8.6f,
            vB1=1.48f, vB6=1.35f, vC=1.4f, vE=35.2f, folate=227f, iron=5.3f, calcium=78f, mg=325f, k=645f, zn=5f, cu=1.8f, se=53f),

        // === OILS ===
        food("Olive oil", "Oil", ServingUnit.ML, 15f, 884f, 0f, 0f, 100f, 0f,
            vE=14.4f, vK=60.2f, iron=0.6f, mg=0f, k=1f, zn=0f, cu=0f, se=0f),
        food("Coconut oil", "Oil", ServingUnit.ML, 15f, 862f, 0f, 0f, 100f, 0f,
            vE=0.1f),
        food("Ghee", "Oil", ServingUnit.GRAMS, 10f, 900f, 0f, 0f, 100f, 0f,
            vA=340f, vD=0.6f, vE=2.8f, vK=8.6f, calcium=4f),

        // === SNACKS ===
        food("Dark chocolate", "Snack", ServingUnit.GRAMS, 25f, 546f, 5f, 60f, 31f, 7f,
            vB2=0.08f, vE=0.5f, vK=7.3f, iron=8f, calcium=56f, mg=146f, k=559f, zn=2.3f, cu=1.77f, se=6.8f),
        food("Honey", "Snack", ServingUnit.GRAMS, 20f, 304f, 0.3f, 82f, 0f, 0.2f,
            vC=0.5f, iron=0.4f, calcium=6f, mg=2f, k=52f, zn=0.2f, cu=0.04f, se=0.8f),
        food("Samosa", "Snack", ServingUnit.PIECE, 1f, 262f, 4f, 24f, 17f, 1.5f,
            vA=5f, vB1=0.06f, vC=1f, iron=0.8f, calcium=10f, mg=10f, k=80f, zn=0.3f, cu=0.05f, se=3f),
        food("Upma", "Snack", ServingUnit.GRAMS, 200f, 95f, 2.5f, 14f, 3f, 1f,
            vB1=0.05f, iron=0.5f, calcium=8f, mg=10f, k=40f, zn=0.3f, cu=0.04f, se=5f),

        // === BEVERAGES (per 100ml) ===
        food("Black coffee", "Beverage", ServingUnit.ML, 150f, 2f, 0.3f, 0f, 0f, 0f,
            vB3=0.2f, mg=3f, k=49f, zn=0f, cu=0f, se=0f),
        food("Tea (with milk)", "Beverage", ServingUnit.ML, 150f, 37f, 1f, 5f, 1.2f, 0f,
            vA=5f, vB2=0.05f, calcium=20f, mg=3f, k=37f, zn=0.1f, cu=0.01f, se=0f),
        food("Coconut water", "Beverage", ServingUnit.ML, 250f, 19f, 0.7f, 3.7f, 0.2f, 1.1f,
            vB6=0.03f, vC=2.4f, iron=0.3f, calcium=24f, mg=25f, k=250f, zn=0.1f, cu=0.04f, se=1f),
        food("Orange juice", "Beverage", ServingUnit.ML, 200f, 45f, 0.7f, 10f, 0.2f, 0.2f,
            vA=10f, vB1=0.09f, vB6=0.04f, vC=50f, folate=30f, iron=0.2f, calcium=11f, mg=11f, k=200f, zn=0.1f, cu=0.04f, se=0.1f),
        food("Protein shake", "Beverage", ServingUnit.ML, 300f, 120f, 24f, 5f, 1.5f, 0.5f,
            vB6=0.5f, vB12=1f, iron=1f, calcium=50f, mg=40f, k=200f, zn=1.5f, cu=0.1f, se=10f)
    )

    val categories = foods.map { it.category }.distinct().sorted()

    fun search(query: String): List<FoodItem> {
        if (query.isBlank()) return foods
        val lower = query.lowercase()
        return foods.filter { it.name.lowercase().contains(lower) }
    }
}
