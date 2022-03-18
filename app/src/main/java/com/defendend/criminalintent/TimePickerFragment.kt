package com.defendend.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE_TO_TIME = "time"

class TimePickerFragment : DialogFragment() {

    interface Callbacks{
        fun onTimeSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_DATE_TO_TIME) as Date

        val timeListener =
            TimePickerDialog.OnTimeSetListener { _ : TimePicker, hour : Int, minutes: Int ->
                val resultDate = Date(date.time)
                resultDate.hours = hour
                resultDate.minutes = minutes
                targetFragment?.let { fragment ->
                    (fragment as Callbacks).onTimeSelected(resultDate)
                }
            }

        val calendar = Calendar.getInstance()
        calendar.time = date

        val initialHour = calendar.get(Calendar.HOUR)
        val initialMinutes = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinutes,
            true

        )
    }

    companion object {
        fun newInstance(date: Date) : TimePickerFragment{
            val args = Bundle().apply {
                putSerializable(ARG_DATE_TO_TIME,date)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }
}