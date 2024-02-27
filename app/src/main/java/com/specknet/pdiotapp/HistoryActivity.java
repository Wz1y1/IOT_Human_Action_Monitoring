package com.specknet.pdiotapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private Spinner spinnerTimeRange;
    private Calendar selectedDate;
    private Button buttonPickDate;
    private Button buttonViewData;
    private TextView textViewDateRange;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        buttonPickDate = findViewById(R.id.buttonPickDate);
        buttonViewData = findViewById(R.id.buttonViewData);
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        textViewDateRange = findViewById(R.id.textViewDateRange);


        selectedDate = Calendar.getInstance();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.date_range_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(adapter);

        buttonPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        buttonViewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchDataForSelectedRange();
            }
        });

        updateDateInView(); // Update the initial date display
    }
//    private void updateDateInView() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        String formattedDate = sdf.format(selectedDate.getTime());
//        buttonPickDate.setText(formattedDate); // Update the button text to show selected date
//    }

//    private void updateDateInView() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        String formattedDate = sdf.format(selectedDate.getTime());
//        buttonPickDate.setText(formattedDate); // Set the button text to the new date
//    }

//    private void updateDateInView() {
//        String timeRange = spinnerTimeRange.getSelectedItem().toString();
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
//        String formattedDate = sdf.format(selectedDate.getTime());
//
//        switch (timeRange) {
//            case "Day":
//                textViewDateRange.setText(formattedDate);
//                break;
//            case "Week":
//                // Set the calendar to the start of the week and get the end of the week date
//                Calendar endOfWeek = (Calendar) selectedDate.clone();
//                endOfWeek.add(Calendar.DAY_OF_MONTH, 6); // Adding 6 days to start date for end date
//                textViewDateRange.setText(formattedDate + " to " + sdf.format(endOfWeek.getTime()));
//                break;
//            case "Month":
//                // Set the calendar to the start of the month and get the end of the month date
//                Calendar startOfMonth = (Calendar) selectedDate.clone();
//                startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
//                Calendar endOfMonth = (Calendar) startOfMonth.clone();
//                endOfMonth.add(Calendar.MONTH, 1);
//                endOfMonth.add(Calendar.DAY_OF_MONTH, -1);
//                textViewDateRange.setText(sdf.format(startOfMonth.getTime()) + " to " + sdf.format(endOfMonth.getTime()));
//                break;
//            default:
//                textViewDateRange.setText(formattedDate);
//                break;
//        }
//    }

    private void updateDateInView() {
        String timeRange = spinnerTimeRange.getSelectedItem().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault());
        String formattedDate = sdf.format(selectedDate.getTime());

        switch (timeRange) {
            case "Day":
                textViewDateRange.setText(formattedDate);
                break;
            case "Week":
                // Calculate the end of the week
                Calendar endOfWeek = (Calendar) selectedDate.clone();
                endOfWeek.add(Calendar.DAY_OF_MONTH, 6); // Adding 6 days for the end date
                textViewDateRange.setText(formattedDate + " to " + sdf.format(endOfWeek.getTime()));
                break;
            case "Month":
                // Calculate the end of the month range
                Calendar endOfMonth = (Calendar) selectedDate.clone();
                endOfMonth.add(Calendar.MONTH, 1); // Add one month
                endOfMonth.add(Calendar.DAY_OF_MONTH, -1); // Subtract one day
                textViewDateRange.setText(formattedDate + " to " + sdf.format(endOfMonth.getTime()));
                break;
            default:
                textViewDateRange.setText(formattedDate);
                break;
        }
    }





//    private void showDatePickerDialog() {
//        int year = selectedDate.get(Calendar.YEAR);
//        int month = selectedDate.get(Calendar.MONTH);
//        int day = selectedDate.get(Calendar.DAY_OF_MONTH);
//
//        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
//                new DatePickerDialog.OnDateSetListener() {
//                    @Override
//                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
//                        selectedDate.set(year, month, dayOfMonth);
//                    }
//                }, year, month, day);
//        datePickerDialog.show();
//    }

    private void showDatePickerDialog() {
        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateInView(); // Update the button text after date is selected
                    }
                }, year, month, day);
        datePickerDialog.show();
    }


    private void fetchDataForSelectedRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(selectedDate.getTime());
        String timeRange = spinnerTimeRange.getSelectedItem().toString();

        Intent intent = new Intent(HistoryActivity.this, DisplayDataActivity.class);
        intent.putExtra(DisplayDataActivity.EXTRA_DATE, formattedDate);
        intent.putExtra(DisplayDataActivity.EXTRA_RANGE, timeRange);
        startActivity(intent);
    }

}