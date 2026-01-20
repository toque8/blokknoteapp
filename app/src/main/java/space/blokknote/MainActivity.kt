package space.blokknote

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.webkit.WebSettings
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.launch
import space.blokknote.data.AppDatabase
import space.blokknote.data.Note
import space.blokknote.utils.SoundManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var soundManager: SoundManager

    private lateinit var editor: RichEditor
    private lateinit var btnBold: TextView
    private lateinit var btnItalic: TextView
    private lateinit var btnUnderline: TextView
    private lateinit var btnHeading: TextView
    private lateinit var btnList: TextView
    private lateinit var langRu: TextView
    private lateinit var langEn: TextView
    private lateinit var btnClear: View
    private lateinit var btnDownload: View
    private lateinit var btnShare: View
    private lateinit var btnCancel: View
    private lateinit var pencilIcon: View

    private var currentNoteId: String? = null
    private val history = mutableListOf<String>()
    private var currentLanguage = "ru"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToDoc()
        } else {
            Toast.makeText(this, "Разрешение необходимо для сохранения файлов", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        loadLastNote()
        updateLanguageUI()
        applyThemeColors()
        setupEditor()
    }

    private fun applyThemeColors() {
        val color = ContextCompat.getColor(this, R.color.primary)
        applyTintToImageView(findViewById(R.id.pencilIcon), color)
        applyTintToImageView(findViewById(R.id.btnCancel), color)
        applyTintToImageView(findViewById(R.id.btnClear), color)
        applyTintToImageView(findViewById(R.id.btnDownload), color)
        applyTintToImageView(findViewById(R.id.btnShare), color)
    }

    private fun applyTintToImageView(view: View, color: Int) {
        if (view is ImageView) {
            val drawable = DrawableCompat.wrap(view.drawable.mutate())
            DrawableCompat.setTint(drawable, color)
            view.setImageDrawable(drawable)
        }
    }

    private fun initViews() {
        editor = findViewById(R.id.editor)
        btnBold = findViewById(R.id.btnBold)
        btnItalic = findViewById(R.id.btnItalic)
        btnUnderline = findViewById(R.id.btnUnderline)
        btnHeading = findViewById(R.id.btnHeading)
        btnList = findViewById(R.id.btnList)
        langRu = findViewById(R.id.langRu)
        langEn = findViewById(R.id.langEn)
        btnClear = findViewById(R.id.btnClear)
        btnDownload = findViewById(R.id.btnDownload)
        btnShare = findViewById(R.id.btnShare)
        btnCancel = findViewById(R.id.btnCancel)
        pencilIcon = findViewById(R.id.pencilIcon)
    }

    private fun setupEditor() {
        val backgroundColor = ContextCompat.getColor(this, R.color.background)
        val fontColor = ContextCompat.getColor(this, R.color.primary)

        editor.setEditorBackgroundColor(backgroundColor)
        editor.setEditorFontColor(fontColor)
        editor.setEditorFontSize(17)
        editor.setPlaceholder(getPlaceholderText())
        
        editor.isEnabled = true
        editor.isFocusable = true
        editor.isFocusableInTouchMode = true
        editor.requestFocus()

        editor.setOnTextChangeListener { html ->
            soundManager.playTyping()
            saveToHistory(html)
            saveNoteToDatabase(html)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val settings = editor.webView.settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.setForceDark(WebSettings.FORCE_DARK_OFF)
                } else {
                    settings.javaClass.getMethod("setForceDark", Int::class.javaPrimitiveType)
                        .invoke(settings, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupListeners() {
        btnBold.setOnClickListener { 
            editor.setBold()
            soundManager.playTyping()
        }
        btnItalic.setOnClickListener { 
            editor.setItalic()
            soundManager.playTyping()
        }
        btnUnderline.setOnClickListener { 
            editor.setUnderline()
            soundManager.playTyping()
        }
        btnHeading.setOnClickListener { 
            editor.setHeading(3)
            soundManager.playTyping()
        }
        btnList.setOnClickListener { 
            editor.setBullets()
            soundManager.playTyping()
        }
        
        langRu.setOnClickListener { setLanguage("ru") }
        langEn.setOnClickListener { setLanguage("en") }
        
        btnClear.setOnClickListener { 
            soundManager.playErase()
            clearNote() 
        }
        btnDownload.setOnClickListener { 
            soundManager.playDownload()
            checkPermissionAndExport()
        }
        btnShare.setOnClickListener { 
            soundManager.playDownload()
            shareNote()
        }
        btnCancel.setOnClickListener { 
            soundManager.playErase()
            undo()
        }
        pencilIcon.setOnClickListener { soundManager.playTyping() }
    }

    private fun setLanguage(lang: String) {
        currentLanguage = lang
        updateLanguageUI()
        editor.setPlaceholder(getPlaceholderText())
        soundManager.playLang()
    }

    private fun updateLanguageUI() {
        langRu.alpha = if (currentLanguage == "ru") 1.0f else 0.5f
        langEn.alpha = if (currentLanguage == "en") 1.0f else 0.5f
        btnCancel.visibility = View.VISIBLE
    }

    private fun getPlaceholderText(): String {
        return if (currentLanguage == "ru") "Пиши…" else "Typing…"
    }

    private fun saveToHistory(html: String) {
        history.add(html)
        if (history.size > 50) history.removeAt(0)
    }

    private fun undo() {
        if (history.size > 1) {
            history.removeAt(history.size - 1)
            val previous = history.last()
            editor.html = previous
        }
    }

    private fun saveNoteToDatabase(html: String) {
        lifecycleScope.launch {
            val id = currentNoteId ?: System.currentTimeMillis().toString()
            val note = Note(
                id = id,
                content = html,
                htmlContent = html,
                fontSize = 17,
                language = currentLanguage,
                updatedAt = System.currentTimeMillis()
            )
            val database = AppDatabase.getDatabase(this@MainActivity)
            database.noteDao().insertNote(note)
            if (currentNoteId == null) {
                currentNoteId = id
            }
        }
    }

    private fun saveCurrentNote() {
        saveNoteToDatabase(editor.html)
    }

    private fun loadLastNote() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            val notes = database.noteDao().getAllNotesSorted()
            if (notes.isNotEmpty() && currentNoteId == null) {
                val note = notes.first()
                currentNoteId = note.id
                currentLanguage = note.language ?: "ru"
                editor.html = note.htmlContent ?: ""
                history.add(editor.html)
                updateLanguageUI()
                btnCancel.visibility = View.VISIBLE
            } else {
                btnCancel.visibility = View.VISIBLE
            }
        }
    }

    private fun clearNote() {
        editor.html = ""
        history.clear()
        history.add("")
        currentNoteId = null
    }

    private fun checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToDoc()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                exportToDoc()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            exportToDoc()
        }
    }

    private fun exportToDoc() {
        val htmlContent = editor.html
        if (TextUtils.isEmpty(htmlContent)) {
            Toast.makeText(this, "Добавьте текст", Toast.LENGTH_SHORT).show()
            return
        }

        val content = android.text.Html.fromHtml(htmlContent, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "Добавьте текст", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "blokknote.txt"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charset.forName("UTF-8")))
                    }
                    showDownloadResult(uri, fileName)
                } else {
                    throw IOException("Не удалось создать URI")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { output ->
                    output.write(content.toByteArray(Charset.forName("UTF-8")))
                }
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                showDownloadResult(uri, fileName)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showDownloadResult(uri: Uri, fileName: String) {
        Toast.makeText(this, "Файл сохранён: $fileName", Toast.LENGTH_LONG).show()

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Открыть"))
        } catch (e: Exception) {
            Toast.makeText(this, "Файл сохранён в папке Downloads", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareNote() {
        val htmlContent = editor.html
        if (TextUtils.isEmpty(htmlContent)) {
            Toast.makeText(this, "Добавьте текст", Toast.LENGTH_SHORT).show()
            return
        }

        val textContent = android.text.Html.fromHtml(
            htmlContent,
            android.text.Html.FROM_HTML_MODE_COMPACT
        ).toString().trim()

        if (textContent.isEmpty()) {
            Toast.makeText(this, "Добавьте текст", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textContent)
        }

        startActivity(Intent.createChooser(shareIntent, "Поделиться"))
    }

    override fun onPause() {
        super.onPause()
        saveCurrentNote()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
