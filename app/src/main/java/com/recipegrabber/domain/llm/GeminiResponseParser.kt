package com.recipegrabber.domain.llm

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step

object GeminiResponseParser {
    fun parse(response: String, sourceUrl: String): Recipe {
        val root = JsonParser.parseString(response).asJsonObject
        val candidate = root.getAsJsonArray("candidates")?.firstOrNull()?.asJsonObject
            ?: throw IllegalArgumentException("Gemini hat kein Rezept geliefert.")
        require(candidate.get("finishReason")?.asString == "STOP") {
            "Die Gemini-Antwort ist unvollstaendig oder wurde blockiert."
        }
        val parts = candidate.getAsJsonObject("content")?.getAsJsonArray("parts")
            ?: throw IllegalArgumentException("Gemini hat keinen Rezepttext geliefert.")
        val text = parts.map { it.asJsonObject }
            .filter { it.get("thought")?.asBoolean != true }
            .mapNotNull { it.get("text")?.asString }.joinToString("").trim()
        val json = if (text.startsWith("```json\n") && text.endsWith("```")) {
            text.removePrefix("```json\n").removeSuffix("```").trim()
        } else text
        val extracted = requireNotNull(Gson().fromJson(json, ExtractedRecipe::class.java)) {
            "Gemini hat kein Rezept geliefert."
        }
        require(!extracted.title.isNullOrBlank() && (extracted.servings ?: 0) > 0 &&
            !extracted.ingredients.isNullOrEmpty() && !extracted.steps.isNullOrEmpty()) {
            "Die KI hat kein vollstaendiges Rezept geliefert."
        }
        require((extracted.prepTimeMinutes ?: 0) >= 0 && (extracted.cookTimeMinutes ?: 0) >= 0)
        return Recipe(
            title = extracted.title.trim(), description = extracted.description.orEmpty(),
            servings = extracted.servings!!, prepTimeMinutes = extracted.prepTimeMinutes ?: 0,
            cookTimeMinutes = extracted.cookTimeMinutes ?: 0, sourceUrl = sourceUrl,
            sourceType = "VIDEO"
        ).also { recipe ->
            recipe.ingredients = extracted.ingredients!!.mapIndexed { index, ingredient ->
                require(!ingredient.name.isNullOrBlank() &&
                    (ingredient.amount == null || (ingredient.amount.isFinite() && ingredient.amount >= 0))) {
                    "Die KI hat ungueltige Zutaten geliefert."
                }
                Ingredient(recipeId = 0, name = ingredient.name.trim(), amount = ingredient.amount,
                    unit = ingredient.unit.orEmpty(), notes = ingredient.notes.orEmpty(), orderIndex = index)
            }
            recipe.steps = extracted.steps!!.mapIndexed { index, step ->
                require(!step.instruction.isNullOrBlank() && (step.duration ?: 0) >= 0) {
                    "Die KI hat ungueltige Zubereitungsschritte geliefert."
                }
                Step(recipeId = 0, order = index + 1, instruction = step.instruction.trim(), duration = step.duration)
            }
        }
    }
}
