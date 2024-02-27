package com.specknet.pdiotapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class DisplayDataActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "com.specknet.pdiotapp.EXTRA_DATE";
    public static final String EXTRA_RANGE = "com.specknet.pdiotapp.EXTRA_RANGE";

    private TextView textViewDateInfo;
    private TextView textViewData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_data);

        textViewDateInfo = findViewById(R.id.textViewDateInfo);
        textViewData = findViewById(R.id.textViewData);

        String date = getIntent().getStringExtra(EXTRA_DATE);
        String range = getIntent().getStringExtra(EXTRA_RANGE);

        String dateRangeText = getDateRangeText(date, range);
        textViewDateInfo.setText("Selected Date Range: " + dateRangeText);

        displayFilteredData(date, range);
    }

    private String getDateRangeText(String date, String range) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault());

        try {
            Date selectedDate = inputFormat.parse(date);
            if (selectedDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(selectedDate);
                String startDate = outputFormat.format(cal.getTime());

                switch (range) {
                    case "Week":
                        cal.add(Calendar.DAY_OF_YEAR, 6);
                        break;
                    case "Month":
                        cal.add(Calendar.MONTH, 1);
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        break;
                }

                String endDate = outputFormat.format(cal.getTime());
                return startDate + " to " + endDate;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void displayFilteredData(String date, String range) {
        SharedPreferences sharedPreferences = getSharedPreferences("ActivityPredictions", MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        StringBuilder filteredData = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date selectedDate = inputFormat.parse(date);
            if (selectedDate != null) {
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    try {
                        Date entryDate = sdf.parse(entry.getKey());
                        if (entryDate != null && isDateInRange(entryDate, selectedDate, range)) {
                            filteredData.append(entry.getKey()).append(": ")
                                    .append(entry.getValue().toString()).append("\n");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        textViewData.setText(filteredData.toString());
    }

    private boolean isDateInRange(Date entryDate, Date selectedDate, String range) {
        Calendar calEntry = Calendar.getInstance();
        calEntry.setTime(entryDate);

        Calendar calSelected = Calendar.getInstance();
        calSelected.setTime(selectedDate);

        switch (range) {
            case "Day":
                return isSameDay(calEntry, calSelected);
            case "Week":
                return isSameWeek(calEntry, calSelected);
            case "Month":
                return isSameMonth(calEntry, calSelected);
            default:
                return false;
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(Calendar calEntry, Calendar calSelected) {
        Calendar weekEnd = (Calendar) calSelected.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 7); // Adding 6 days to cover the entire week

        return (calEntry.after(calSelected) || calEntry.equals(calSelected)) && calEntry.before(weekEnd);
    }


    private boolean isSameMonth(Calendar calEntry, Calendar calSelected) {
        Calendar monthEnd = (Calendar) calSelected.clone();
        monthEnd.add(Calendar.MONTH, 1); // Move to the next month
        monthEnd.add(Calendar.DAY_OF_MONTH, -0); //
        // Subtract one day to get the last day of the current month

        return (calEntry.after(calSelected) || calEntry.equals(calSelected)) && calEntry.before(monthEnd);
    }
}
