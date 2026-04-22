package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.AccountabilityCheckPreference
import com.example.gymworkout.data.social.DailyProgress
import com.example.gymworkout.data.social.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AccountabilityCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCheck(context)
            } finally {
                // Always reschedule for tomorrow so a misfire or failure doesn't kill the alarm.
                if (AccountabilityCheckPreference.getEnabled(context)) {
                    AccountabilityCheckScheduler.schedule(context)
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun runCheck(context: Context) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repo = SocialRepository()
        val partnerships = repo.getPartnershipsList(myUid)
            .filter { it.status == "active" && (it.notifyWorkout || it.notifyHabits) }
        if (partnerships.isEmpty()) return

        NotificationHelper.createAccountabilityNotificationChannel(context)

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        partnerships.forEachIndexed { index, p ->
            val partnerUid = if (p.user1Id == myUid) p.user2Id else p.user1Id
            val partnerName = if (p.user1Id == myUid) p.user2Name else p.user1Name
            val partner = repo.getUser(partnerUid) ?: return@forEachIndexed

            val dp = partner.dailyProgress
            val isToday = dp.date == today
            val missedWorkout = p.notifyWorkout && !(isToday && dp.workoutDone)
            val missedHabits = p.notifyHabits && !(isToday && hasAnyHabitProgress(dp))
            if (!missedWorkout && !missedHabits) return@forEachIndexed

            val title = "Check in on $partnerName"
            val body = buildMessage(partnerName, missedWorkout, missedHabits)
            NotificationHelper.showNotification(
                context = context,
                notificationId = NOTIFICATION_ID_BASE + index,
                title = title,
                text = body,
                channelId = NotificationHelper.ACCOUNTABILITY_CHANNEL_ID
            )
        }
    }

    private fun hasAnyHabitProgress(dp: DailyProgress): Boolean {
        return dp.proteinProgress > 0f ||
                dp.caloriesProgress > 0f ||
                dp.waterProgress > 0f ||
                dp.sleepProgress > 0f
    }

    private fun buildMessage(name: String, missedWorkout: Boolean, missedHabits: Boolean): String {
        return when {
            missedWorkout && missedHabits ->
                "$name hasn't logged a workout or habits today. A quick nudge could help."
            missedWorkout -> "$name hasn't logged a workout today."
            missedHabits -> "$name hasn't logged any habits today."
            else -> "$name may need a nudge today."
        }
    }

    companion object {
        const val NOTIFICATION_ID_BASE = 9210
    }
}
