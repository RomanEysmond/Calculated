package com.example.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var leftTable: TableLayout
    private lateinit var rightTable: TableLayout
    private lateinit var btnSave: Button
    private lateinit var btnShowArchive: Button
    private lateinit var btnClearArchive: Button

    private val channelNames = listOf("Канал", "1А", "1B", "2А", "2B", "3А", "3B", "4А", "4B")

    // Пустые ячейки для столбца ДИ: 2А (индекс 3) и 4B (индекс 7)
    private val emptyDiRows = setOf(3, 7)

    // Данные таблиц
    private var leftTableData = mutableListOf<TableRowData>()
    private var rightTableData = mutableListOf<TableRowData>()

    private val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("calibration_prefs", Context.MODE_PRIVATE)
    }

    private val archiveList = mutableListOf<ArchiveEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        leftTable = findViewById(R.id.tableLeft)
        rightTable = findViewById(R.id.tableRight)
        btnSave = findViewById(R.id.btnSave)
        btnShowArchive = findViewById(R.id.btnShowArchive)
        btnClearArchive = findViewById(R.id.btnClearArchive)

        loadSavedData()
        loadArchive()
        setupCalculations()
        setupButtons()
    }

    private fun loadSavedData() {
        val gson = Gson()

        // Загружаем левую таблицу
        val leftJson = sharedPrefs.getString("left_table_data", "")
        if (leftJson.isNullOrEmpty()) {
            initializeDefaultLeftTable()
        } else {
            try {
                val type = object : TypeToken<MutableList<TableRowData>>() {}.type
                val loaded = gson.fromJson<MutableList<TableRowData>>(leftJson, type)
                if (loaded != null && loaded.isNotEmpty()) {
                    leftTableData = loaded
                } else {
                    initializeDefaultLeftTable()
                }
            } catch (e: Exception) {
                initializeDefaultLeftTable()
            }
        }

        // Загружаем правую таблицу
        val rightJson = sharedPrefs.getString("right_table_data", "")
        if (rightJson.isNullOrEmpty()) {
            initializeDefaultRightTable()
        } else {
            try {
                val type = object : TypeToken<MutableList<TableRowData>>() {}.type
                val loaded = gson.fromJson<MutableList<TableRowData>>(rightJson, type)
                if (loaded != null && loaded.isNotEmpty()) {
                    rightTableData = loaded
                } else {
                    initializeDefaultRightTable()
                }
            } catch (e: Exception) {
                initializeDefaultRightTable()
            }
        }

        renderTable(leftTable, leftTableData, isLeftTable = true)
        renderTable(rightTable, rightTableData, isLeftTable = false)
    }

    private fun saveCurrentData() {
        val gson = Gson()
        val leftJson = gson.toJson(leftTableData)
        val rightJson = gson.toJson(rightTableData)
        sharedPrefs.edit()
            .putString("left_table_data", leftJson)
            .putString("right_table_data", rightJson)
            .apply()
    }

    private fun initializeDefaultLeftTable() {
        leftTableData.clear()
        val leftData = mapOf(
            1 to listOf("0,960", "1,405", "0,893"),
            2 to listOf("1,231", "2,236", "0,876"),
            3 to listOf("", "1,337", "0,887"),
            4 to listOf("0,949", "1,356", "0,892"),
            5 to listOf("1,175", "2,000", "0,937"),
            6 to listOf("1,030", "2,125", "0,893"),
            7 to listOf("0,802", "2,168", "0,852"),
            8 to listOf("", "1,802", "0,895")
        )

        for (i in channelNames.indices) {
            val isHeader = i == 0
            val channel = channelNames[i]

            if (isHeader) {
                leftTableData.add(TableRowData(channel, "ДИ", "ПД", "РД", isHeader))
            } else {
                val data = leftData[i] ?: listOf("0", "0", "0")
                leftTableData.add(TableRowData(channel, data[0], data[1], data[2], isHeader))
            }
        }
    }

    private fun initializeDefaultRightTable() {
        rightTableData.clear()
        for (i in channelNames.indices) {
            val isHeader = i == 0
            val channel = channelNames[i]

            if (isHeader) {
                rightTableData.add(TableRowData(channel, "ДИ", "ПД", "РД", isHeader))
            } else {
                rightTableData.add(TableRowData(channel, "", "", "1", isHeader))
            }
        }
    }

    private fun renderTable(tableLayout: TableLayout, rows: List<TableRowData>, isLeftTable: Boolean) {
        tableLayout.removeAllViews()

        for ((index, row) in rows.withIndex()) {
            val tableRow = TableRow(this)

            // Ячейка Канал
            val cellChannel = TextView(this)
            cellChannel.text = row.channel
            cellChannel.gravity = Gravity.CENTER
            cellChannel.setBackgroundResource(R.drawable.cell_border)
            cellChannel.setTextColor(android.graphics.Color.BLACK)
            cellChannel.textSize = 11f
            cellChannel.setPadding(4, 8, 4, 8)
            if (row.isHeader) {
                cellChannel.setTypeface(null, android.graphics.Typeface.BOLD)
                cellChannel.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            }
            cellChannel.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            tableRow.addView(cellChannel)

            // Ячейка ДИ
            val cellDi = EditText(this)
            cellDi.setText(row.di)
            cellDi.gravity = Gravity.CENTER
            cellDi.setBackgroundResource(R.drawable.cell_border)
            cellDi.setTextColor(android.graphics.Color.BLACK)
            cellDi.textSize = 11f
            cellDi.setPadding(4, 8, 4, 8)
            cellDi.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            if (row.isHeader) {
                cellDi.setTypeface(null, android.graphics.Typeface.BOLD)
                cellDi.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                cellDi.isEnabled = false
                cellDi.isFocusable = false
                cellDi.inputType = InputType.TYPE_NULL
            } else {
                if (isLeftTable) {
                    cellDi.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                    cellDi.keyListener = DigitsKeyListener.getInstance("0123456789,.")
                    cellDi.isEnabled = true
                    cellDi.isFocusable = true
                    cellDi.isFocusableInTouchMode = true
                    cellDi.isClickable = true

                    val position = index
                    cellDi.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            val newValue = s?.toString() ?: ""
                            if (newValue != leftTableData[position].di) {
                                leftTableData[position] = leftTableData[position].copy(di = newValue)
                                saveCurrentData()
                            }
                        }
                    })
                } else {
                    cellDi.isEnabled = false
                    cellDi.isFocusable = false
                    cellDi.inputType = InputType.TYPE_NULL
                }
            }
            tableRow.addView(cellDi)

            // Ячейка ПД
            val cellPd = EditText(this)
            cellPd.setText(row.pd)
            cellPd.gravity = Gravity.CENTER
            cellPd.setBackgroundResource(R.drawable.cell_border)
            cellPd.setTextColor(android.graphics.Color.BLACK)
            cellPd.textSize = 11f
            cellPd.setPadding(4, 8, 4, 8)
            cellPd.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            if (row.isHeader) {
                cellPd.setTypeface(null, android.graphics.Typeface.BOLD)
                cellPd.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                cellPd.isEnabled = false
                cellPd.isFocusable = false
                cellPd.inputType = InputType.TYPE_NULL
            } else {
                if (isLeftTable) {
                    cellPd.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                    cellPd.keyListener = DigitsKeyListener.getInstance("0123456789,.")
                    cellPd.isEnabled = true
                    cellPd.isFocusable = true
                    cellPd.isFocusableInTouchMode = true
                    cellPd.isClickable = true

                    val position = index
                    cellPd.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            val newValue = s?.toString() ?: ""
                            if (newValue != leftTableData[position].pd) {
                                leftTableData[position] = leftTableData[position].copy(pd = newValue)
                                saveCurrentData()
                            }
                        }
                    })
                } else {
                    cellPd.isEnabled = false
                    cellPd.isFocusable = false
                    cellPd.inputType = InputType.TYPE_NULL
                }
            }
            tableRow.addView(cellPd)

            // Ячейка РД
            val cellRd = EditText(this)
            cellRd.setText(row.rd)
            cellRd.gravity = Gravity.CENTER
            cellRd.setBackgroundResource(R.drawable.cell_border)
            cellRd.setTextColor(android.graphics.Color.BLACK)
            cellRd.textSize = 11f
            cellRd.setPadding(4, 8, 4, 8)
            cellRd.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            if (row.isHeader) {
                cellRd.setTypeface(null, android.graphics.Typeface.BOLD)
                cellRd.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                cellRd.isEnabled = false
                cellRd.isFocusable = false
                cellRd.inputType = InputType.TYPE_NULL
            } else {
                cellRd.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                cellRd.keyListener = DigitsKeyListener.getInstance("0123456789,.")
                cellRd.isEnabled = true
                cellRd.isFocusable = true
                cellRd.isFocusableInTouchMode = true
                cellRd.isClickable = true

                val position = index
                val isLeft = isLeftTable
                cellRd.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        val newValue = s?.toString() ?: "0"
                        if (newValue.isEmpty()) return

                        if (isLeft) {
                            if (newValue != leftTableData[position].rd) {
                                leftTableData[position] = leftTableData[position].copy(rd = newValue)
                                saveCurrentData()
                            }
                        } else {
                            if (newValue != rightTableData[position].rd) {
                                rightTableData[position] = rightTableData[position].copy(rd = newValue)
                                saveCurrentData()
                            }
                        }
                    }
                })
            }

            tableRow.addView(cellRd)
            tableLayout.addView(tableRow)
        }
    }

    private fun setupCalculations() {
        lifecycleScope.launch {
            while (true) {
                delay(50)
                calculateRightTable()
                updateRightTableDisplay()
                saveCurrentData()
            }
        }
    }

    private fun calculateRightTable() {
        if (rightTableData.isEmpty() || leftTableData.isEmpty()) return

        for (i in 1 until rightTableData.size) {
            if (rightTableData[i].isHeader) continue

            val leftRow = leftTableData[i]
            val leftRd = leftRow.rd.replace(",", ".").toDoubleOrNull() ?: 1.0
            val leftPd = leftRow.pd.replace(",", ".").toDoubleOrNull() ?: 1.0
            val leftDi = if (leftRow.di.isEmpty()) 0.0 else leftRow.di.replace(",", ".").toDoubleOrNull() ?: 0.0

            val userRd = rightTableData[i].rd.replace(",", ".").toDoubleOrNull() ?: 1.0

            val calculatedPd = if (leftRd != 0.0) {
                leftPd * userRd / leftRd
            } else {
                leftPd
            }

            val calculatedDi = if (leftPd != 0.0 && leftDi != 0.0) {
                leftDi * calculatedPd / leftPd
            } else {
                if (i in emptyDiRows) 0.0 else leftDi
            }

            val diString = if (i in emptyDiRows || calculatedDi == 0.0) "" else String.format("%.3f", calculatedDi).replace(".", ",")
            val pdString = String.format("%.3f", calculatedPd).replace(".", ",")

            rightTableData[i] = rightTableData[i].copy(
                di = diString,
                pd = pdString
            )
        }
    }

    private fun updateRightTableDisplay() {
        val rightTableLayout = rightTable
        for (i in 0 until rightTableLayout.childCount) {
            val tableRow = rightTableLayout.getChildAt(i) as? TableRow ?: continue
            if (tableRow.childCount >= 4 && i < rightTableData.size && i > 0) {
                val rowData = rightTableData[i]
                val diView = tableRow.getChildAt(1) as? EditText
                if (diView != null && !diView.isFocused) {
                    diView.setText(rowData.di)
                }
                val pdView = tableRow.getChildAt(2) as? EditText
                if (pdView != null && !pdView.isFocused) {
                    pdView.setText(rowData.pd)
                }
            }
        }
    }

    private fun setupButtons() {
        btnSave.setOnClickListener {
            saveCalibrationData()
        }

        btnShowArchive.setOnClickListener {
            showArchiveDialog()
        }

        btnClearArchive.setOnClickListener {
            showClearArchiveConfirmation()
        }
    }

    private fun showClearArchiveConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Очистка архива")
            .setMessage("Вы уверены, что хотите очистить весь архив? Это действие нельзя отменить.")
            .setPositiveButton("Очистить") { _, _ ->
                clearArchive()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearArchive() {
        archiveList.clear()
        saveArchive()
        showArchiveDialog() // Обновляем отображение архива
    }

    private fun saveCalibrationData() {
        for (i in 1 until leftTableData.size) {
            if (i < rightTableData.size && !leftTableData[i].isHeader) {
                val rightRow = rightTableData[i]
                leftTableData[i] = leftTableData[i].copy(
                    di = rightRow.di,
                    pd = rightRow.pd,
                    rd = rightRow.rd
                )
            }
        }

        renderTable(leftTable, leftTableData, isLeftTable = true)
        saveCurrentData()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val archiveEntry = ArchiveEntry(
            timestamp = timestamp,
            leftTableData = leftTableData.toList()
        )

        archiveList.add(0, archiveEntry)
        saveArchive()

        for (i in 1 until rightTableData.size) {
            if (!rightTableData[i].isHeader) {
                rightTableData[i] = rightTableData[i].copy(
                    di = "",
                    pd = "",
                    rd = "1"
                )
            }
        }

        calculateRightTable()
        renderTable(rightTable, rightTableData, isLeftTable = false)
        saveCurrentData()
    }

    private fun saveArchive() {
        val gson = Gson()
        val json = gson.toJson(archiveList)
        sharedPrefs.edit().putString("calibration_archive", json).apply()
    }

    private fun loadArchive() {
        val gson = Gson()
        val json = sharedPrefs.getString("calibration_archive", "[]")
        val type = object : TypeToken<MutableList<ArchiveEntry>>() {}.type
        val loaded = gson.fromJson<MutableList<ArchiveEntry>>(json, type)
        archiveList.clear()
        archiveList.addAll(loaded ?: emptyList())
    }

    private fun showArchiveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_archive, null)
        val archiveContainer = dialogView.findViewById<LinearLayout>(R.id.archiveContainer)
        val btnClearArchiveDialog = dialogView.findViewById<Button>(R.id.btnClearArchiveDialog)

        archiveContainer.removeAllViews()

        if (archiveList.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Архив пуст"
            emptyText.gravity = Gravity.CENTER
            emptyText.setPadding(0, 20, 0, 20)
            emptyText.textSize = 12f
            archiveContainer.addView(emptyText)
        } else {
            for (entry in archiveList) {
                val dateText = TextView(this)
                dateText.text = "📅 ${entry.timestamp}"
                dateText.textSize = 12f
                dateText.setTypeface(null, android.graphics.Typeface.BOLD)
                dateText.setPadding(8, 12, 8, 8)
                archiveContainer.addView(dateText)

                for (row in entry.leftTableData) {
                    if (!row.isHeader) {
                        val rowText = TextView(this)
                        rowText.text = "${row.channel}: ДИ=${row.di}, ПД=${row.pd}, РД=${row.rd}"
                        rowText.textSize = 11f
                        rowText.setPadding(16, 2, 16, 2)
                        archiveContainer.addView(rowText)
                    }
                }

                val divider = View(this)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                divider.layoutParams = layoutParams
                divider.setBackgroundColor(0xFFCCCCCC.toInt())
                divider.setPadding(0, 6, 0, 6)
                archiveContainer.addView(divider)
            }
        }

        btnClearArchiveDialog.setOnClickListener {
            val alertDialog = it.context as? android.app.AlertDialog
            AlertDialog.Builder(this)
                .setTitle("Очистка архива")
                .setMessage("Вы уверены, что хотите очистить весь архив? Это действие нельзя отменить.")
                .setPositiveButton("Очистить") { _, _ ->
                    clearArchive()
                    alertDialog?.dismiss()
                    showArchiveDialog()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle("Архив тарировок")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }
}

data class TableRowData(
    val channel: String,
    val di: String,
    val pd: String,
    val rd: String,
    val isHeader: Boolean = false
)

data class ArchiveEntry(
    val timestamp: String,
    val leftTableData: List<TableRowData>
)