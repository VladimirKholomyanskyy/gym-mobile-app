package com.neyra.gymapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.entities.ExerciseEntity
import com.neyra.gymapp.data.entities.ExerciseLogEntity
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.data.entities.WorkoutEntity
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
import com.neyra.gymapp.data.entities.WorkoutSessionEntity

@Database(
    entities = [TrainingProgramEntity::class, WorkoutEntity::class, ExerciseEntity::class, WorkoutExerciseEntity::class, WorkoutSessionEntity::class, ExerciseLogEntity::class],
    version = 1
)
//@TypeConverters(YourTypeConverters::class) // If needed
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingProgramDao(): TrainingProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    
}