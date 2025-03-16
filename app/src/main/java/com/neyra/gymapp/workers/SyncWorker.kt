package com.neyra.gymapp.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neyra.gymapp.data.repository.TrainingProgramRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trainingProgramRepository: TrainingProgramRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // First sync any pending local changes to the server
            val syncResult = trainingProgramRepository.syncAllPendingPrograms()

            if (syncResult.isFailure) {
                // If we couldn't sync outgoing changes, retry later
                return@withContext Result.retry()
            }

            // Then refresh data from server
            val refreshResult = trainingProgramRepository.refreshTrainingPrograms()

            return@withContext if (refreshResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            // Log.e("SyncWorker", "Error during sync: ${e.message}", e)
            Result.retry()
        }
    }
}