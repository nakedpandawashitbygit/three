package com.example.three

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var deadlineButtons: List<TextView>
    private lateinit var projectNameButtons: List<TextView>
    private lateinit var timerButtons: List<TextView>
    private val isRunning = BooleanArray(8) { false }
    private val startTimes = LongArray(8) { 0L }
    private val elapsedTimes = LongArray(8) { 0L }
    private val deadlines = Array(8) { "" }
    private val projectNames = Array(8) { "" }
    private val handler = Handler()
    private var activeTimerIndex = -1

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        loadData()

        deadlineButtons = listOf(
            findViewById(R.id.deadlineButton1),
            findViewById(R.id.deadlineButton2),
            findViewById(R.id.deadlineButton3),
            findViewById(R.id.deadlineButton4),
            findViewById(R.id.deadlineButton5),
            findViewById(R.id.deadlineButton6),
            findViewById(R.id.deadlineButton7),
            findViewById(R.id.deadlineButton8)
        )

        projectNameButtons = listOf(
            findViewById(R.id.projectNameButton1),
            findViewById(R.id.projectNameButton2),
            findViewById(R.id.projectNameButton3),
            findViewById(R.id.projectNameButton4),
            findViewById(R.id.projectNameButton5),
            findViewById(R.id.projectNameButton6),
            findViewById(R.id.projectNameButton7),
            findViewById(R.id.projectNameButton8)
        )

        timerButtons = listOf(
            findViewById(R.id.timerButton1),
            findViewById(R.id.timerButton2),
            findViewById(R.id.timerButton3),
            findViewById(R.id.timerButton4),
            findViewById(R.id.timerButton5),
            findViewById(R.id.timerButton6),
            findViewById(R.id.timerButton7),
            findViewById(R.id.timerButton8)
        )

        deadlineButtons.forEachIndexed { index, button ->
            button.setOnLongClickListener {
                showDeadlineInput(index)
                true
            }
            button.text = deadlines[index]
            button.textSize = 12f // Уменьшаем размер шрифта дедлайна
            button.layoutParams.width = 100 // Устанавливаем ширину дедлайна на 100 пикселей
        }

        projectNameButtons.forEachIndexed { index, button ->
            button.setOnLongClickListener {
                showProjectNameInput(index)
                true
            }
            button.text = projectNames[index]
            button.textSize = 12f // Уменьшаем размер шрифта названия проекта
        }

        timerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { toggleTimer(index) }
            button.setOnLongClickListener {
                resetTimer(index)
                true
            }
            button.layoutParams.width = 100 // Уменьшаем ширину кнопки таймера до 100 пикселей
            button.textSize = 12f // Уменьшаем размер шрифта таймера
            button.text = getTimerText(index)
        }

        updateTimerTexts()
    }

    override fun onPause() {
        super.onPause()
        saveData()
    }

    private fun loadData() {
        for (i in 0 until 8) {
            deadlines[i] = sharedPreferences.getString("deadline_$i", "")!!
            projectNames[i] = sharedPreferences.getString("project_name_$i", "")!!
        }
    }

    private fun saveData() {
        val editor = sharedPreferences.edit()
        for (i in 0 until 8) {
            editor.putString("deadline_$i", deadlines[i])
            editor.putString("project_name_$i", projectNames[i])
        }
        editor.apply()
    }

    private fun toggleTimer(index: Int) {
        if (isRunning[index]) {
            pauseTimer(index)
        } else {
            startTimer(index)
        }
    }

    private fun resetTimer(index: Int) {
        elapsedTimes[index] = 0L
        updateTimerText(index)
    }

    private fun startTimer(index: Int) {
        if (activeTimerIndex != -1 && activeTimerIndex != index) {
            pauseTimer(activeTimerIndex)
        }

        startTimes[index] = SystemClock.elapsedRealtime() - elapsedTimes[index]
        handler.post(updateTimerTask(index))
        isRunning[index] = true
        activeTimerIndex = index
    }

    private fun pauseTimer(index: Int) {
        handler.removeCallbacks(updateTimerTask(index))
        elapsedTimes[index] = SystemClock.elapsedRealtime() - startTimes[index]
        isRunning[index] = false
        if (activeTimerIndex == index) {
            activeTimerIndex = -1
        }
    }

    private fun updateTimerTask(index: Int) = object : Runnable {
        override fun run() {
            elapsedTimes[index] = SystemClock.elapsedRealtime() - startTimes[index]
            updateTimerText(index)
            if (isRunning[index]) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun showDeadlineInput(index: Int) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val dateTimePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                TimePickerDialog(
                    this,
                    { _, selectedHour, selectedMinute ->
                        val deadline = String.format("%04d-%02d-%02d %02d:%02d", selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
                        updateDeadline(index, deadline)
                    },
                    hour, minute, true
                ).show()
            },
            year, month, day
        )

        dateTimePickerDialog.show()
    }

    private fun showProjectNameInput(index: Int) {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.setText(projectNames[index])
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val projectName = input.text.toString().trim()
            updateProjectName(index, projectName)
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateProjectName(index: Int, projectName: String) {
        projectNames[index] = projectName
        projectNameButtons[index].text = projectName
    }

    private fun updateDeadline(index: Int, deadline: String) {
        deadlines[index] = deadline
        deadlineButtons[index].text = deadline
    }

    private fun updateTimerTexts() {
        for (i in timerButtons.indices) {
            updateTimerText(i)
        }
    }

    private fun updateTimerText(index: Int) {
        val timeString = getTimeString(elapsedTimes[index])
        timerButtons[index].text = timeString
    }

    private fun getTimeString(elapsedTime: Long): String {
        val hours = (elapsedTime / 3600000).toInt()
        val minutes = ((elapsedTime - hours * 3600000) / 60000).toInt()
        val seconds = ((elapsedTime - hours * 3600000 - minutes * 60000) / 1000).toInt()
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getTimerText(index: Int): String {
        val elapsedTime = if (isRunning[index]) {
            SystemClock.elapsedRealtime() - startTimes[index] + elapsedTimes[index]
        } else {
            elapsedTimes[index]
        }
        return getTimeString(elapsedTime)
    }
}