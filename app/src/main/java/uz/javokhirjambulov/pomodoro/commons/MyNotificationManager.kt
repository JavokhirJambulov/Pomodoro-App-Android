package uz.javokhirjambulov.pomodoro.commons

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import uz.javokhirjambulov.pomodoro.ActionReceiver
import uz.javokhirjambulov.pomodoro.MainActivity
import uz.javokhirjambulov.pomodoro.R

class MyNotificationManager(private val context: Context) {
    private val channel_ID = "CHANNEL_ID"
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getNotification(
        contentText: String? = null,
        timerStatus: TimerStatus,
        timerType: TimerType
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channel_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentIntent(createActivityIntent())
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentText(contentText)

        builder.apply {
            if (timerType != TimerType.SESSION_NOT_STARTED_YET) {
                when (timerType) {
                    TimerType.POMODORO -> {
                        if (timerStatus == TimerStatus.IN_PROGRESS) {
                            addAction(buildPauseAction(context))
                            setContentTitle(context.getString(R.string.pomodoro_in_progress_notification))

                        } else {
                            addAction(buildResumeAction(context))
                            setContentTitle(context.getString(R.string.pomodoro_is_paused_notification))
                        }
                        addAction(buildStopAction(context))
                    }
                    TimerType.BREAK -> {
                        if (timerStatus == TimerStatus.IN_PROGRESS) {
                            addAction(buildPauseAction(context))
                            setContentTitle(context.getString(R.string.break_in_progress_notification))

                        } else {
                            addAction(buildResumeAction(context))
                            setContentTitle(context.getString(R.string.break_is_paused_notification))
                        }
                        addAction(buildStopAction(context))

                    }
                    TimerType.LONG_BREAK -> {
                        if (timerStatus == TimerStatus.IN_PROGRESS) {
                            addAction(buildPauseAction(context))
                            setContentTitle(context.getString(R.string.long_break_in_progress_notification))

                        } else {
                            addAction(buildResumeAction(context))
                            setContentTitle(context.getString(R.string.long_break_is_paused_notification))
                        }
                        addAction(buildStopAction(context))
                    }
                    else -> {
                        setContentTitle(context.getString(R.string.session_completed))
                        addAction(buildResumeAction(context))
                        addAction(buildStopAction(context))
                    }
                }
            }
        }
        return builder
    }

    fun clearNotification() {
        notificationManager.cancelAll()
    }

    fun showNotification(
        contentText: String? = null,
        timerStatus: TimerStatus,
        timerType: TimerType
    ) {
        notificationManager.notify(
            Constants.POMODORO_NOTIFICATION_ID,
            getNotification(contentText, timerStatus, timerType).build()
        )
    }


    companion object {

        private const val RESUME_ID = 36
        private const val PAUSE_ID = 37
        private const val STOP_ID = 38


        private fun buildStopAction(context: Context): NotificationCompat.Action {
            fun createButtonIntent(actionValue: String): Intent {
                val buttonIntent = Intent(context, ActionReceiver::class.java)
                buttonIntent.putExtra(Constants.BUTTON_ACTION, actionValue)
                return buttonIntent
            }

            fun createButtonPendingIntent(actionValue: String): PendingIntent? {
                return PendingIntent.getBroadcast(
                    context,
                    STOP_ID,
                    createButtonIntent(actionValue),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            }

            return NotificationCompat.Action.Builder(
                R.drawable.ic_baseline_stop, context.getString(R.string.stop),
                createButtonPendingIntent(Constants.BUTTON_STOP)
            ).build()
        }


        private fun buildResumeAction(context: Context): NotificationCompat.Action {
            fun createButtonIntent(actionValue: String): Intent {
                val buttonIntent = Intent(context, ActionReceiver::class.java)
                buttonIntent.putExtra(Constants.BUTTON_ACTION, actionValue)
                return buttonIntent
            }

            fun createButtonPendingIntent(actionValue: String): PendingIntent? {
                return PendingIntent.getBroadcast(
                    context,
                    RESUME_ID,
                    createButtonIntent(actionValue),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            }

            return NotificationCompat.Action.Builder(
                R.drawable.ic_baseline_play, context.getString(R.string.resume),
                createButtonPendingIntent(Constants.BUTTON_START)
            ).build()
        }


        private fun buildPauseAction(context: Context): NotificationCompat.Action {
            fun createButtonIntent(actionValue: String): Intent {
                val buttonIntent = Intent(context, ActionReceiver::class.java)
                buttonIntent.putExtra(Constants.BUTTON_ACTION, actionValue)
                return buttonIntent
            }

            fun createButtonPendingIntent(actionValue: String): PendingIntent? {

                return PendingIntent.getBroadcast(
                    context,
                    PAUSE_ID,
                    createButtonIntent(actionValue),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            }

            return NotificationCompat.Action.Builder(
                R.drawable.ic_baseline_pause, context.getString(R.string.pause),
                createButtonPendingIntent(Constants.BUTTON_PAUSE)
            ).build()
        }
    }

    private fun initChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.resources.getString(R.string.channel_name)
            val descriptionText = context.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channel_ID, name, importance).apply {
                description = descriptionText
            }
            channel.setBypassDnd(true)
            channel.setShowBadge(true)
            channel.setSound(null, null)

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannels()
        }
    }
}
