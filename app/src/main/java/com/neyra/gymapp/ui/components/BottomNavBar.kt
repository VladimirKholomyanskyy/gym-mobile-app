package com.neyra.gymapp.ui.components


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, "home"),
        BottomNavItem("Training", Icons.AutoMirrored.Filled.List, "trainingPrograms"),
        BottomNavItem("Exercises", Icons.Filled.FitnessCenter, "exercises"),
        BottomNavItem("Profile", Icons.Filled.Person, "profile")
    )

    NavigationBar {
        val currentDestination =
            navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination == item.route,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)
