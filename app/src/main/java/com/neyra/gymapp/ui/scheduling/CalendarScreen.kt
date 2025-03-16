package com.neyra.gymapp.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView() {
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val currentMonth = YearMonth.from(currentDate)
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val scheduledWorkouts = mapOf(
        currentDate.plusDays(1) to "Leg Day",
        currentDate.plusDays(3) to "Upper Body Strength",
        currentDate.plusDays(5) to "Full Body Workout"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentDate = currentDate.minusMonths(1) }) {
                Text(
                    text = "<",
                    color = Color.Magenta,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${
                    currentDate.month.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    )
                } ${currentDate.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Magenta
            )
            IconButton(onClick = { currentDate = currentDate.plusMonths(1) }) {
                Text(
                    text = ">",
                    color = Color.Magenta,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Text(text = day, color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            var dayCounter = 1
            repeat(6) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { day ->
                        if (week == 0 && day < startDayOfWeek || dayCounter > daysInMonth) {
                            Spacer(modifier = Modifier.size(48.dp))
                        } else {
                            val date = currentMonth.atDay(dayCounter)
                            val workout = scheduledWorkouts[date]
                            DayItem(dayCounter, workout)
                            dayCounter++
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DayItem(day: Int, workout: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(48.dp)
            .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Text(text = "$day", color = Color.White, fontSize = 14.sp)
        if (workout != null) {
            Text(text = "•", color = Color.Magenta, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
