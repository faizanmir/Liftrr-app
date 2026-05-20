package org.liftrr.data.models.dto

import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UnitSystem

data class UserProfileDto(
    val firstName: String,
    val lastName: String,
    val gender: Gender,
    val height: Float,
    val fitnessLevel: FitnessLevel,
    val dob: Long,
    // Profile data
    val dateOfBirth: Long? = null,           // Timestamp
    val weight: Float? = null,               // In kg (always store in metric)

    // Goals and preferences
    val goalsJson: String? = null,           // JSON array of fitness goals
    val preferredExercises: String? = null,  // JSON array of exercise types
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String? = null,        // e.g., "09:00" for 9 AM reminders
)
