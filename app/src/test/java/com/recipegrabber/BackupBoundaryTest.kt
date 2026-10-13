package com.recipegrabber

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupBoundaryTest {
    private fun document(name: String) = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(File("src/main/res/xml/$name.xml"))

    @Test fun `both backup formats allow only the recipe database and WAL`() {
        for (name in listOf("backup_rules", "data_extraction_rules")) {
            val nodes = document(name).getElementsByTagName("include")
            assertTrue(nodes.length > 0)
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as Element
                assertEquals("database", node.getAttribute("domain"))
                assertTrue(node.getAttribute("path") in setOf("recipe_grabber_db", "recipe_grabber_db-wal"))
            }
        }
        val cloud = document("data_extraction_rules").getElementsByTagName("cloud-backup").item(0) as Element
        assertEquals("true", cloud.getAttribute("disableIfNoEncryptionCapabilities"))
    }

    @Test fun `file sharing exposes only the dedicated diagnostic export directory`() {
        val nodes = document("file_paths").documentElement.childNodes
        var rules = 0
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            rules++
            assertEquals("cache-path", node.tagName)
            assertEquals("diagnostics/", node.getAttribute("path"))
        }
        assertEquals(1, rules)
    }
}
