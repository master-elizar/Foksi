package com.foksi.app.data.local

import androidx.room.TypeConverter
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.RepeatMode

class Converters {

    @TypeConverter
    fun intSetToString(value: Set<Int>?): String = value.orEmpty().joinToString(",")

    @TypeConverter
    fun stringToIntSet(value: String?): Set<Int> =
        value.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    @TypeConverter
    fun itemTypeToString(value: ItemType): String = value.name

    @TypeConverter
    fun stringToItemType(value: String): ItemType =
        runCatching { ItemType.valueOf(value) }.getOrDefault(ItemType.EVENT)

    @TypeConverter
    fun priorityToString(value: Priority): String = value.name

    @TypeConverter
    fun stringToPriority(value: String): Priority =
        runCatching { Priority.valueOf(value) }.getOrDefault(Priority.NORMAL)

    @TypeConverter
    fun reminderTypeToString(value: ReminderType): String = value.name

    @TypeConverter
    fun stringToReminderType(value: String): ReminderType =
        runCatching { ReminderType.valueOf(value) }.getOrDefault(ReminderType.NORMAL)

    @TypeConverter
    fun repeatModeToString(value: RepeatMode): String = value.name

    @TypeConverter
    fun stringToRepeatMode(value: String): RepeatMode =
        runCatching { RepeatMode.valueOf(value) }.getOrDefault(RepeatMode.NONE)

    @TypeConverter
    fun advanceModeToString(value: AdvanceMode): String = value.name

    @TypeConverter
    fun stringToAdvanceMode(value: String): AdvanceMode =
        runCatching { AdvanceMode.valueOf(value) }.getOrDefault(AdvanceMode.CHAIN)

    @TypeConverter
    fun kindToString(value: NotificationKind): String = value.name

    @TypeConverter
    fun stringToKind(value: String): NotificationKind =
        runCatching { NotificationKind.valueOf(value) }.getOrDefault(NotificationKind.MAIN)
}
