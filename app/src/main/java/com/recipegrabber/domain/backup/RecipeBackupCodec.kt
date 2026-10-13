package com.recipegrabber.domain.backup

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

internal object RecipeBackupCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_RECIPES = 500
    private const val FORMAT = "recipegrabber-recipes"

    fun encode(recipes: List<RecipeWithDetails>, createdAt: Long = System.currentTimeMillis()): ByteArray {
        require(recipes.size <= MAX_RECIPES) { "Backup exceeds the recipe limit" }
        val output = ByteArrayOutputStream()
        val bounded = object : OutputStream() {
            override fun write(value: Int) { require(output.size() < MAX_BYTES) { "Backup exceeds the size limit" }; output.write(value) }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                require(output.size() + length <= MAX_BYTES) { "Backup exceeds the size limit" }
                output.write(bytes, offset, length)
            }
        }
        JsonWriter(OutputStreamWriter(bounded, Charsets.UTF_8)).use { writer ->
            writer.serializeNulls = true
            writer.beginObject().name("format").value(FORMAT).name("version").value(1)
                .name("createdAt").value(createdAt).name("recipes").beginArray()
            recipes.forEach { entry ->
                val recipe = entry.recipe
                writer.beginObject().name("backupId").value(recipe.backupId).name("title").value(recipe.title)
                    .name("description").value(recipe.description).name("servings").value(recipe.servings)
                    .name("prepTimeMinutes").value(recipe.prepTimeMinutes).name("cookTimeMinutes").value(recipe.cookTimeMinutes)
                    .name("sourceUrl").value(recipe.sourceUrl).name("sourceType").value(recipe.sourceType)
                    .name("thumbnailUrl").value(recipe.thumbnailUrl).name("createdAt").value(recipe.createdAt)
                    .name("updatedAt").value(recipe.updatedAt).name("isFavorite").value(recipe.isFavorite)
                    .name("ingredients").beginArray()
                entry.ingredients.forEach { ingredient ->
                    writer.beginObject().name("name").value(ingredient.name).name("amount").value(ingredient.amount)
                        .name("unit").value(ingredient.unit).name("notes").value(ingredient.notes)
                        .name("orderIndex").value(ingredient.orderIndex).endObject()
                }
                writer.endArray().name("steps").beginArray()
                entry.steps.forEach { step ->
                    writer.beginObject().name("order").value(step.order).name("instruction").value(step.instruction)
                        .name("duration").value(step.duration).name("imageUrl").value(step.imageUrl).endObject()
                }
                writer.endArray().endObject()
            }
            writer.endArray().endObject()
        }
        return output.toByteArray().also { decode(it) }
    }

    fun decode(bytes: ByteArray): List<RecipeWithDetails> = try {
        require(bytes.size in 1..MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        preflight(text)
        val root = JsonParser.parseString(text).asJsonObject
        exact(root, "format", "version", "createdAt", "recipes")
        require(string(root, "format", 64) == FORMAT && integer(root, "version", 1, 1) == 1)
        timestamp(root, "createdAt")
        val recipes = root.getAsJsonArray("recipes")
        require(recipes.size() <= MAX_RECIPES)
        val ids = mutableSetOf<String>()
        recipes.map { element ->
            val entry = element.asJsonObject
            exact(entry, "backupId", "title", "description", "servings", "prepTimeMinutes", "cookTimeMinutes", "sourceUrl", "sourceType", "thumbnailUrl", "createdAt", "updatedAt", "isFavorite", "ingredients", "steps")
            val id = string(entry, "backupId", 36)
            require(UUID.fromString(id).toString() == id && ids.add(id))
            val recipe = Recipe(
                title = string(entry, "title", 512).also { require(it.isNotBlank()) },
                description = string(entry, "description", 16_384),
                servings = integer(entry, "servings", 1, 10_000),
                prepTimeMinutes = integer(entry, "prepTimeMinutes", 0, 100_000),
                cookTimeMinutes = integer(entry, "cookTimeMinutes", 0, 100_000),
                sourceUrl = string(entry, "sourceUrl", 8192).also(::safeUrl),
                sourceType = string(entry, "sourceType", 64),
                thumbnailUrl = nullableString(entry, "thumbnailUrl", 8192)?.also(::safeUrl),
                createdAt = timestamp(entry, "createdAt"), updatedAt = timestamp(entry, "updatedAt"),
                isFavorite = entry["isFavorite"].asJsonPrimitive.let { require(it.isBoolean); it.asBoolean },
                backupId = id
            )
            val ingredients = entry.getAsJsonArray("ingredients")
            val steps = entry.getAsJsonArray("steps")
            require(ingredients.size() in 1..200 && steps.size() in 1..200)
            val decodedIngredients = ingredients.map { value ->
                val item = value.asJsonObject
                exact(item, "name", "amount", "unit", "notes", "orderIndex")
                val amount = item["amount"].takeUnless(JsonElement::isJsonNull)?.asJsonPrimitive?.let {
                    require(it.isNumber); it.asDouble.also { number -> require(number.isFinite() && number in 0.0..1e12) }
                }
                Ingredient(recipeId = 0, name = string(item, "name", 512).also { require(it.isNotBlank()) },
                    amount = amount, unit = string(item, "unit", 128), notes = string(item, "notes", 4096),
                    orderIndex = integer(item, "orderIndex", 0, 10_000))
            }
            val orders = mutableSetOf<Int>()
            val decodedSteps = steps.map { value ->
                val item = value.asJsonObject
                exact(item, "order", "instruction", "duration", "imageUrl")
                val order = integer(item, "order", 1, 10_000).also { require(orders.add(it)) }
                Step(recipeId = 0, order = order, instruction = string(item, "instruction", 16_384).also { require(it.isNotBlank()) },
                    duration = if (item["duration"].isJsonNull) null else integer(item, "duration", 0, 1_000_000),
                    imageUrl = nullableString(item, "imageUrl", 8192)?.also(::safeUrl))
            }
            RecipeWithDetails(recipe, decodedIngredients, decodedSteps.sortedBy { it.order })
        }
    } catch (_: Exception) { throw IllegalArgumentException("Invalid or unsupported recipe backup") }

    private fun exact(value: JsonObject, vararg names: String) { require(value.keySet() == names.toSet()) }
    private fun string(value: JsonObject, name: String, limit: Int): String = value[name].asJsonPrimitive.let {
        require(it.isString); it.asString.also { text -> require(text.length <= limit) }
    }
    private fun nullableString(value: JsonObject, name: String, limit: Int) = if (value[name].isJsonNull) null else string(value, name, limit)
    private fun integer(value: JsonObject, name: String, min: Int, max: Int): Int = value[name].asJsonPrimitive.let {
        require(it.isNumber && it.asString.matches(Regex("[0-9]{1,10}")))
        it.asString.toLong().also { number -> require(number in min.toLong()..max.toLong()) }.toInt()
    }
    private fun timestamp(value: JsonObject, name: String): Long = value[name].asJsonPrimitive.let {
        require(it.isNumber && it.asString.matches(Regex("[0-9]{1,15}")))
        it.asString.toLong().also { number -> require(number in 0..253402300799999L) }
    }
    private fun safeUrl(value: String) {
        if (value.isEmpty()) return
        val uri = URI(value)
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.userInfo == null)
    }

    private fun preflight(text: String) {
        var nodes = 0
        JsonReader(StringReader(text)).use { reader ->
            fun visit(depth: Int) {
                require(depth <= 12 && ++nodes <= 100_000)
                when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> {
                        reader.beginObject()
                        val names = mutableSetOf<String>()
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            require(name.length <= 64 && names.add(name))
                            visit(depth + 1)
                        }
                        reader.endObject()
                    }
                    JsonToken.BEGIN_ARRAY -> { reader.beginArray(); while (reader.hasNext()) visit(depth + 1); reader.endArray() }
                    JsonToken.STRING -> require(reader.nextString().length <= 16_384)
                    JsonToken.NUMBER -> require(reader.nextString().length <= 128)
                    JsonToken.BOOLEAN -> reader.nextBoolean()
                    JsonToken.NULL -> reader.nextNull()
                    else -> throw IllegalArgumentException("Invalid backup structure")
                }
            }
            visit(0)
            require(reader.peek() == JsonToken.END_DOCUMENT)
        }
    }
}
