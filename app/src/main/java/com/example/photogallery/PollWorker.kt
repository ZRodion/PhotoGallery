package com.example.photogallery

import android.app.PendingIntent
import android.content.Context
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class PollWorker(
    private val context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        Log.i("MyTag", "doWork")
        val photoRepository = PhotoRepository()
        val preferencesRepository = PreferencesRepository.get()

        val lastResultId = preferencesRepository.lastResultId.first()
        val query = preferencesRepository.storedQuery.first()

        if(query.isEmpty()){
            return Result.success()
        }

        return try{
            val items = photoRepository.searchPhotos(query)
            if(items.isNotEmpty()){
                val newResultId = items.first().id
                if (newResultId != lastResultId){
                    preferencesRepository.setLastResultId(newResultId)
                    notifyUser()
                }
                return Result.success()
            }

            return Result.success()
        }catch (e: Exception){
            Log.e("MyTag", "Worker failed", e)
            Result.failure()
        }
    }

    private fun notifyUser(){
        Log.i("MyTag", "notifyUser")
        val intent = MainActivity.newIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_report_image)
            .setContentTitle(context.resources.getString(R.string.new_pictures_title))
            .setContentText(context.resources.getString(R.string.new_pictures_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(0, notification)
    }
}