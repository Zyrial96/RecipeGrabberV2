package com.recipegrabber

import com.google.gson.JsonParser
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.domain.backup.RecipeBackupCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RecipeBackupCodecTest {
    private fun recipe() = RecipeWithDetails(
        Recipe(id = 31, title = "QA Suppe", servings = 2, sourceUrl = "https://example.org/recipe.webm", isFavorite = true, isSynced = true),
        listOf(Ingredient(id = 41, recipeId = 31, name = "Wasser", amount = 250.5, unit = "ml")),
        listOf(Step(id = 51, recipeId = 31, order = 1, instruction = "Kochen", duration = 60))
    )

    @Test fun `backup preserves recipe content and identity but resets database IDs and sync flags`() {
        val original = recipe()
        val bytes = RecipeBackupCodec.encode(listOf(original))
        val restored = RecipeBackupCodec.decode(bytes).single()
        assertEquals(original.recipe.backupId, restored.recipe.backupId)
        assertEquals(original.recipe.title, restored.recipe.title)
        assertEquals(original.recipe.sourceUrl, restored.recipe.sourceUrl)
        assertTrue(restored.recipe.isFavorite)
        assertFalse(restored.recipe.isSynced)
        assertEquals(0L, restored.recipe.id)
        assertEquals(0L, restored.ingredients.single().id)
        assertEquals(0L, restored.ingredients.single().recipeId)
        assertEquals(250.5, restored.ingredients.single().amount)
        assertEquals(60, restored.steps.single().duration)
        val json = bytes.toString(Charsets.UTF_8)
        for (forbidden in listOf("api_key", "access_token", "googleAccount", "isSynced", "\"id\":", "recipeId")) assertFalse(json.contains(forbidden))
    }

    @Test fun `repeated identities and unknown versions are rejected`() {
        val entry = recipe()
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.encode(listOf(entry, entry)) }
        val root = JsonParser.parseString(RecipeBackupCodec.encode(listOf(entry)).toString(Charsets.UTF_8)).asJsonObject
        root.addProperty("version", 2)
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.decode(root.toString().toByteArray()) }
    }

    @Test fun `invalid rows unknown fields and credential URLs reject the entire archive`() {
        val valid = RecipeBackupCodec.encode(listOf(recipe())).toString(Charsets.UTF_8)
        for (change in listOf<(com.google.gson.JsonObject) -> Unit>(
            { it.addProperty("api_key", "synthetic secret") },
            { it.getAsJsonArray("recipes")[0].asJsonObject.addProperty("servings", 0) },
            { it.getAsJsonArray("recipes")[0].asJsonObject.addProperty("sourceUrl", "https://user:password@example.org/video") },
            { it.getAsJsonArray("recipes")[0].asJsonObject.getAsJsonArray("ingredients")[0].asJsonObject.addProperty("amount", -1) }
        )) {
            val root = JsonParser.parseString(valid).asJsonObject
            change(root)
            assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.decode(root.toString().toByteArray()) }
        }
    }

    @Test fun `duplicate JSON fields deep structures trailing content and malformed UTF8 fail closed`() {
        for (value in listOf("{\"version\":1,\"version\":1}", "[".repeat(20) + "0" + "]".repeat(20), "{}{}", "{unquoted:true}")) {
            assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.decode(value.toByteArray()) }
        }
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
    }

    @Test fun `limits bound both encoding and untrusted downloads`() {
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.decode(ByteArray(RecipeBackupCodec.MAX_BYTES + 1)) }
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.encode(List(RecipeBackupCodec.MAX_RECIPES + 1) { recipe() }) }
        assertThrows(IllegalArgumentException::class.java) { RecipeBackupCodec.encode(listOf(recipe().copy(recipe = recipe().recipe.copy(title = "x".repeat(513))))) }
    }

    @Test fun `empty archives are valid and cannot imply deletion`() {
        assertTrue(RecipeBackupCodec.decode(RecipeBackupCodec.encode(emptyList())).isEmpty())
    }
}
