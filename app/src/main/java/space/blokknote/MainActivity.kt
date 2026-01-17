package space.blokknote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.launch
import space.blokknote.data.AppDatabase
import space.blokknote.data.Note
import space.blokknote.utils.SoundManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
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
    private lateinit var btnClear: View
    private lateinit var btnDownload: View
    private lateinit var btnShare: View
    private lateinit var btnCancel: View
    private lateinit var btnFonts: View
    private lateinit var btnSize12: TextView
    private lateinit var btnSize14: TextView
    private lateinit var btnSizePlus: TextView
    private lateinit var btnSizeMinus: TextView
    private lateinit var langRu: TextView
    private lateinit var langEn: TextView

    private var currentNoteId: String? = null
    private var currentLanguage = "ru"
    private val history = mutableListOf<String>()
    private var currentFontSize = 17

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
        setupEditor()
        setupListeners()
        loadLastNote()
    }

    private fun initViews() {
        editor = findViewById(R.id.editor)
        btnBold = findViewById(R.id.btnBold)
        btnItalic = findViewById(R.id.btnItalic)
        btnUnderline = findViewById(R.id.btnUnderline)
        btnHeading = findViewById(R.id.btnHeading)
        btnList = findViewById(R.id.btnList)
        btnClear = findViewById(R.id.btnClear)
        btnDownload = findViewById(R.id.btnDownload)
        btnShare = findViewById(R.id.btnShare)
        btnCancel = findViewById(R.id.btnCancel)
        btnFonts = findViewById(R.id.btnFonts)
        btnSize12 = findViewById(R.id.btnSize12)
        btnSize14 = findViewById(R.id.btnSize14)
        btnSizePlus = findViewById(R.id.btnSizePlus)
        btnSizeMinus = findViewById(R.id.btnSizeMinus)
        langRu = findViewById(R.id.langRu)
        langEn = findViewById(R.id.langEn)
    }

    private fun setupEditor() {
        editor.setPlaceholder(getString(R.string.hint_text))
        editor.setEditorFontSize(currentFontSize)
        editor.setEditorFontColor(ContextCompat.getColor(this, R.color.primary))

        editor.setOnTextChangeListener { text ->
            soundManager.playTyping()
            saveToHistory(text)
            saveNoteToDatabase(text)
        }

        editor.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveCurrentNote()
            }
        }
    }

    private fun setupListeners() {
        // Форматирование
        btnBold.setOnClickListener {
            editor.setBold()
            updateFormatButtons()
        }
        
        btnItalic.setOnClickListener {
            editor.setItalic()
            updateFormatButtons()
        }
        
        btnUnderline.setOnClickListener {
            editor.setUnderline()
            updateFormatButtons()
        }
        
        btnHeading.setOnClickListener {
            val currentFormat = editor.heading
            editor.setHeading(if (currentFormat == 3) 0 else 3)
        }
        
        btnList.setOnClickListener {
            editor.setBullets()
        }

        // Язык
        langRu.setOnClickListener { setLanguage("ru") }
        langEn.setOnClickListener { setLanguage("en") }

        // Размер шрифта
        btnSize12.setOnClickListener { setFontSize(12) }
        btnSize14.setOnClickListener { setFontSize(14) }
        btnSizePlus.setOnClickListener { adjustFontSize(1) }
        btnSizeMinus.setOnClickListener { adjustFontSize(-1) }

        // Кнопки действий
        btnClear.setOnClickListener { clearNote() }
        btnDownload.setOnClickListener { checkPermissionAndExport() }
        btnShare.setOnClickListener { shareNote() }
        btnCancel.setOnClickListener { undo() }
        btnFonts.setOnClickListener { showFontsDialog() }
    }

    private fun setLanguage(lang: String) {
        currentLanguage = lang
        langRu.setTextColor(ContextCompat.getColor(this, 
            if (lang == "ru") R.color.primary else R.color.secondary))
        langEn.setTextColor(ContextCompat.getColor(this,
            if (lang == "en") R.color.primary else R.color.secondary))
        
        editor.setPlaceholder(if (lang == "ru") 
            getString(R.string.hint_text) else getString(R.string.hint_text_en))
        
        soundManager.playLang()
        saveCurrentNote()
    }

    private fun setFontSize(size: Int) {
        currentFontSize = size
        editor.setEditorFontSize(size)
        soundManager.playTyping()
        saveCurrentNote()
    }

    private fun adjustFontSize(delta: Int) {
        currentFontSize = (currentFontSize + delta).coerceIn(8, 30)
        editor.setEditorFontSize(currentFontSize)
        soundManager.playTyping()
        saveCurrentNote()
    }

    private fun updateFormatButtons() {
        // Можно добавить визуальную обратную связь для активного форматирования
    }

    private fun saveToHistory(text: String) {
        history.add(text)
        if (history.size > 50) history.removeAt(0)
        btnCancel.visibility = if (history.size > 1) View.VISIBLE else View.INVISIBLE
    }

    private fun undo() {
        if (history.size > 1) {
            history.removeAt(history.size - 1)
            val previous = history.last()
            editor.html = previous
            soundManager.playErase()
        }
    }

    private fun saveNoteToDatabase(html: String) {
        lifecycleScope.launch {
            val note = Note(
                id = currentNoteId ?: "",
                content = editor.text,
                htmlContent = html,
                fontSize = currentFontSize,
                language = currentLanguage,
                updatedAt = System.currentTimeMillis()
            )
            
            val database = AppDatabase.getDatabase(this@MainActivity)
            database.noteDao().insertNote(note)
            
            if (currentNoteId == null) {
                currentNoteId = note.id
            }
        }
    }

    private fun saveCurrentNote() {
        saveNoteToDatabase(editor.html)
    }

    private fun loadLastNote() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            val notes = database.noteDao().getAllNotes()
            notes.collect { noteList ->
                if (noteList.isNotEmpty() && currentNoteId == null) {
                    val note = noteList.first()
                    currentNoteId = note.id
                    editor.html = note.htmlContent
                    currentFontSize = note.fontSize
                    currentLanguage = note.language
                    editor.setEditorFontSize(currentFontSize)
                    setLanguage(currentLanguage)
                    history.add(editor.html)
                    btnCancel.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun clearNote() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear))
            .setMessage(getString(R.string.confirm_clear))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                editor.html = ""
                history.clear()
                currentNoteId = null
                soundManager.playErase()
                btnCancel.visibility = View.INVISIBLE
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                exportToDoc()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            exportToDoc()
        }
    }

    private fun exportToDoc() {
        val content = editor.text
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "blokknote_$date.html"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Blokknote</title>
                    <style>
                        body { 
                            font-family: sans-serif; 
                            line-height: 1.6; 
                            padding: 24px;
                            margin: 0;
                        }
                    </style>
                </head>
                <body>
                    ${editor.html}
                </body>
                </html>
            """.trimIndent()

            FileOutputStream(file).use { output ->
                output.write(htmlContent.toByteArray())
            }

            Toast.makeText(this, getString(R.string.file_saved, file.name), Toast.LENGTH_LONG).show()
            soundManager.playDownload()

            // Открываем файл
            val uri = FileProvider.getUriForFile(this, "space.blokknote.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Открыть с помощью"))

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.file_save_error), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun shareNote() {
        val content = editor.text
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
            putExtra(Intent.EXTRA_TEXT, content)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser)))
        soundManager.playDownload()
    }

    private fun showFontsDialog() {
        val fonts = resources.getStringArray(R.array.font_display_names)
        val fontFamilies = resources.getStringArray(R.array.font_families)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.font))
            .setItems(fonts) { _, which ->
                applyFont(fontFamilies[which])
                soundManager.playTyping()
            }
            .show()
    }

    private fun applyFont(fontFamily: String) {
        editor.evaluateJavascript("document.execCommand('fontName', false, '$fontFamily');", null)
        saveCurrentNote()
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