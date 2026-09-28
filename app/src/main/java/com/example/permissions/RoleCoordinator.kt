package com.example.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build

data class RoleItem(
    val roleName: String,
    val title: String,
    val description: String,
    val isHeld: Boolean,
    val isAvailable: Boolean
)

object RoleCoordinator {

    fun getRoleManager(context: Context): RoleManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else null
    }

    fun isRoleHeld(context: Context, roleName: String): Boolean {
        val rm = getRoleManager(context) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rm.isRoleHeld(roleName)
        } else false
    }

    fun isRoleAvailable(context: Context, roleName: String): Boolean {
        val rm = getRoleManager(context) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rm.isRoleAvailable(roleName)
        } else false
    }

    fun createRequestRoleIntent(context: Context, roleName: String): Intent? {
        val rm = getRoleManager(context) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && rm.isRoleAvailable(roleName)) {
            rm.createRequestRoleIntent(roleName)
        } else null
    }

    fun getRoles(context: Context): List<RoleItem> {
        val roles = mutableListOf<RoleItem>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roles.add(
                RoleItem(
                    roleName = RoleManager.ROLE_ASSISTANT,
                    title = "Default Digital Assistant",
                    description = "Sets Aura as your phone's default voice and assist provider",
                    isHeld = isRoleHeld(context, RoleManager.ROLE_ASSISTANT),
                    isAvailable = isRoleAvailable(context, RoleManager.ROLE_ASSISTANT)
                )
            )
            roles.add(
                RoleItem(
                    roleName = RoleManager.ROLE_DIALER,
                    title = "Default Phone App",
                    description = "Allows deep in-call control and direct phone handling",
                    isHeld = isRoleHeld(context, RoleManager.ROLE_DIALER),
                    isAvailable = isRoleAvailable(context, RoleManager.ROLE_DIALER)
                )
            )
            roles.add(
                RoleItem(
                    roleName = RoleManager.ROLE_SMS,
                    title = "Default SMS App",
                    description = "Allows full background SMS dispatch without interactive dialogs",
                    isHeld = isRoleHeld(context, RoleManager.ROLE_SMS),
                    isAvailable = isRoleAvailable(context, RoleManager.ROLE_SMS)
                )
            )
            roles.add(
                RoleItem(
                    roleName = RoleManager.ROLE_CALL_SCREENING,
                    title = "Call Screening Service",
                    description = "Screen incoming spam calls before answering",
                    isHeld = isRoleHeld(context, RoleManager.ROLE_CALL_SCREENING),
                    isAvailable = isRoleAvailable(context, RoleManager.ROLE_CALL_SCREENING)
                )
            )
        }
        return roles
    }
}
