package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DarktableStyle
import com.example.model.EditParams

@Entity(tableName = "styles")
data class StyleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isBuiltIn: Boolean,
    val paramsJson: String
) {
    fun toDarktableStyle(): DarktableStyle {
        return DarktableStyle(
            id = id,
            name = name,
            description = description,
            category = category,
            isBuiltIn = isBuiltIn,
            params = EditParams.fromJson(paramsJson)
        )
    }

    companion object {
        fun fromDarktableStyle(style: DarktableStyle): StyleEntity {
            return StyleEntity(
                id = style.id,
                name = style.name,
                description = style.description,
                category = style.category,
                isBuiltIn = style.isBuiltIn,
                paramsJson = style.params.toJson()
            )
        }
    }
}
