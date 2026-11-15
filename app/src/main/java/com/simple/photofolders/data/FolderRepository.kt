package com.simple.photofolders.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Stores folders as plain directories inside the app's private storage:
 *
 *   filesDir/folders/<id>/meta.json     -> { "name": ..., "createdAt": ... }
 *   filesDir/folders/<id>/photos/<file> -> copied images
 *
 * Photos are copied, so they stay in the app even if the originals are
 * removed from the phone's gallery.
 */
class FolderRepository(private val context: Context) {

    private val root: File = File(context.filesDir, "folders").apply { mkdirs() }

    private fun folderDir(id: String) = File(root, id)
    private fun photosDir(id: String) = File(folderDir(id), "photos")
    private fun metaFile(id: String) = File(folderDir(id), META)

    suspend fun listFolders(): List<Folder> = withContext(Dispatchers.IO) {
        root.listFiles { f -> f.isDirectory }
            .orEmpty()
            .mapNotNull { dir -> readFolder(dir.name) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun getFolder(id: String): Folder? = withContext(Dispatchers.IO) { readFolder(id) }

    suspend fun createFolder(name: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        photosDir(id).mkdirs()
        writeMeta(id, name.trim(), System.currentTimeMillis())
        id
    }

    suspend fun renameFolder(id: String, newName: String) = withContext(Dispatchers.IO) {
        val meta = readMeta(id) ?: return@withContext
        writeMeta(id, newName.trim(), meta.optLong("createdAt"))
    }

    suspend fun deleteFolder(id: String) = withContext(Dispatchers.IO) {
        folderDir(id).deleteRecursively()
    }

    /** Copies picked images into the folder. Returns number of photos copied. */
    suspend fun addPhotos(id: String, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val dir = photosDir(id).apply { mkdirs() }
        var copied = 0
        uris.forEachIndexed { index, uri ->
            val ext = extensionFor(uri)
            // Time prefix keeps pick order when sorting by name.
            val target = File(dir, "${System.currentTimeMillis()}_${index}_${UUID.randomUUID()}.$ext")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                    copied++
                }
            } catch (e: Exception) {
                target.delete()
            }
        }
        copied
    }

    suspend fun listPhotos(id: String): List<File> = withContext(Dispatchers.IO) {
        photosDir(id).listFiles { f -> f.isFile }.orEmpty().sortedBy { it.name }
    }

    suspend fun deletePhotos(files: Collection<File>) = withContext(Dispatchers.IO) {
        files.forEach { it.delete() }
    }

    private fun readFolder(id: String): Folder? {
        val meta = readMeta(id) ?: return null
        val photos = photosDir(id).listFiles { f -> f.isFile }.orEmpty().sortedBy { it.name }
        return Folder(
            id = id,
            name = meta.optString("name"),
            createdAt = meta.optLong("createdAt"),
            photoCount = photos.size,
            cover = photos.firstOrNull(),
        )
    }

    private fun readMeta(id: String): JSONObject? {
        val file = metaFile(id)
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    private fun writeMeta(id: String, name: String, createdAt: Long) {
        val json = JSONObject().put("name", name).put("createdAt", createdAt)
        metaFile(id).writeText(json.toString())
    }

    private fun extensionFor(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        return mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
    }

    private companion object {
        const val META = "meta.json"
    }
}
