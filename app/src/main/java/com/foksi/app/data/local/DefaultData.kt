package com.foksi.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seed categories. Names are stored as keys and resolved to localised strings at display time,
 * so switching the device language renames the built-in categories too.
 */
object DefaultData {

    val BUILT_IN = listOf(
        "work" to 0xFFE95420.toInt(),
        "study" to 0xFF7B68EE.toInt(),
        "personal" to 0xFF3BA776.toInt(),
        "meetings" to 0xFF3A86C8.toInt(),
        "important" to 0xFFD2496B.toInt(),
        "other" to 0xFF8A8175.toInt(),
    )

    fun insertDefaultCategories(db: SupportSQLiteDatabase) {
        BUILT_IN.forEachIndexed { index, (key, color) ->
            db.execSQL(
                "INSERT INTO categories (id, name, colorArgb, builtInKey) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(index + 1L, key, color, key)
            )
        }
    }
}
