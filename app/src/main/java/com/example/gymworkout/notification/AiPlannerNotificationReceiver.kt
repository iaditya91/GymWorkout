package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.AiPlannerPreference

class AiPlannerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!AiPlannerPreference.getEnabled(context)) return

        val tip = PLANNER_TIPS.random()

        NotificationHelper.createAiPlannerNotificationChannel(context)
        NotificationHelper.showNotification(
            context = context,
            notificationId = NOTIFICATION_ID,
            title = "AI Daily Planner",
            text = tip,
            channelId = CHANNEL_ID
        )

        // Reschedule for tomorrow at a new random time
        AiPlannerNotificationScheduler.schedule(context)
    }

    companion object {
        const val NOTIFICATION_ID = 9902
        const val CHANNEL_ID = "ai_planner_reminders"

        private val PLANNER_TIPS = listOf(
            "Drink a glass of water now — staying hydrated boosts energy and focus throughout the day.",
            "Have you hit your protein target? A handful of nuts or a boiled egg can help close the gap.",
            "Time for a 5-minute stretch break — your muscles will thank you after long periods of sitting.",
            "Check your nutrition log. Small, consistent tracking beats end-of-day guessing.",
            "Aim to eat your last meal 2–3 hours before bed for better sleep and digestion.",
            "30 minutes of walking counts as exercise — no gym required today if you're short on time.",
            "Pair carbs with protein to keep blood sugar stable and avoid afternoon energy crashes.",
            "Have you had your greens today? A handful of spinach added to any meal is almost tasteless.",
            "Sleep is when your muscles grow. Aim for 7–9 hours tonight for best recovery.",
            "Pre-plan your next meal now — decisions made when hungry are rarely the healthiest.",
            "A 10-minute walk after lunch aids digestion and prevents the post-meal energy slump.",
            "Your water goal for today: make sure you've had at least half your daily target by now.",
            "Fibre matters — add lentils, beans, or whole grains to your next meal for digestive health.",
            "Rest days are productive days. Muscles repair and strengthen when you're not training.",
            "Healthy fat is essential. Avocado, olive oil, or fatty fish keep hormones balanced.",
            "Check in: are you ahead or behind on your habits today? A quick review now prevents a bad night.",
            "Eating slowly gives your brain time to register fullness — try putting your fork down between bites.",
            "A protein-rich breakfast reduces cravings throughout the day. Did you fuel up this morning?",
            "If you missed a workout, a 15-minute bodyweight circuit at home still counts.",
            "Magnesium-rich foods like dark chocolate, almonds, and spinach can help with muscle recovery.",
            "Batch-cook one ingredient today — pre-cooked rice or grilled chicken saves future meal decisions.",
            "Craving something sweet? Frozen banana, Greek yogurt, or dates are great natural alternatives.",
            "Your body is 60% water — every organ depends on it. Sip consistently, not just when thirsty.",
            "A short meditation or breathing exercise now can reset stress and improve afternoon focus.",
            "Don't forget your micronutrients — a colourful plate means a wider range of vitamins.",
            "Consistent bed and wake times regulate hunger hormones and improve daily energy levels.",
            "Strength training builds muscle that burns more calories at rest — even two sessions a week helps.",
            "Post-workout nutrition matters. Aim for protein + carbs within 45 minutes of exercise.",
            "Stress eating is real. Next time you reach for food, pause and ask if you're truly hungry.",
            "Set a reminder for your evening workout now — scheduling removes the decision later.",
        )
    }
}
