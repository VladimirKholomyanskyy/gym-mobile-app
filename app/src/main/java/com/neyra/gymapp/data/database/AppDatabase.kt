package com.neyra.gymapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.ExerciseLogDao
import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.dao.ScheduledWorkoutDao
import com.neyra.gymapp.data.dao.SettingsDao
import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.dao.WorkoutSessionDao
import com.neyra.gymapp.data.entities.ExerciseEntity
import com.neyra.gymapp.data.entities.ExerciseLogEntity
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.entities.ScheduledWorkoutEntity
import com.neyra.gymapp.data.entities.SettingsEntity
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.data.entities.WorkoutEntity
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
import com.neyra.gymapp.data.entities.WorkoutSessionEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        ProfileEntity::class,
        SettingsEntity::class,
        TrainingProgramEntity::class,
        WorkoutEntity::class,
        ExerciseEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        ScheduledWorkoutEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun trainingProgramDao(): TrainingProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao

}