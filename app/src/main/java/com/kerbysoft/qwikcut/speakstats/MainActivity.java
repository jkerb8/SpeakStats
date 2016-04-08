package com.kerbysoft.qwikcut.speakstats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.RecognitionService;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final String logtag = "MyLogTag";
    private String homeName = "", awayName = "", fieldsize = "", division = "", dayString = "", monthString = "", yearString = "";
    String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Button btnNewGame;
        Button btnOpenGame;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnNewGame = (Button) findViewById(R.id.newGameBtn);

        btnOpenGame = (Button) findViewById(R.id.openGameBtn);

        btnNewGame.setOnClickListener(this);

        btnOpenGame.setOnClickListener(this);

        Toolbar actionBar = (Toolbar) findViewById(R.id.actionBar);
        actionBar.setTitle("SpeakStats");
        setSupportActionBar(actionBar);

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newGameBtn: {
                showNewGameDialog();
                break;
            }
            case R.id.openGameBtn: {
                openGame();
                break;
            }

        }

    }

    private String intToMonth(int m) {
        switch (m) {
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";
            default:
                return Integer.toString(m);
        }
    }

    private int monthToInt(String m) {
        switch (m) {
            case "January":
                return 1;
            case "February":
                return 2;
            case "March":
                return 3;
            case "April":
                return 4;
            case "May":
                return 5;
            case "June":
                return 6;
            case "July":
                return 7;
            case "August":
                return 8;
            case "September":
                return 9;
            case "October":
                return 10;
            case "November":
                return 11;
            case "December":
                return 12;
            default:
                return 0;
        }
    }

    private void openGame() {

        List<File> files = getListFolders(new File(dirPath));

        final String[] pastGames = new String[files.size()];
        final String[] gameInfo = new String[files.size()];
        final ArrayList<fbGame> games = new ArrayList<>();
        String words[][] = new String[files.size()][5];

        for (int i=0; i<files.size(); i++){
            pastGames[i] = files.get(i).getName();
            words[i] = pastGames[i].split("_");
            fbGame current = new fbGame(words[i][6], words[i][4], words[i][3],
                    words[i][1], words[i][0], words[i][2]);
            games.add(current);
            if (words[i].length > 6) {
                gameInfo[i] = current.homeTeam + " vs. " + current.awayTeam + "\n\t\t" +
                        current.division + "\n\t\t" +
                        intToMonth(Integer.parseInt(current.month)) + " " + current.day + ", " + current.year;
            }
            else
                gameInfo[i] = pastGames[i];
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select a Game to Open");

        builder.setItems(gameInfo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position
                // of the selected item
                String gameDir = dirPath + pastGames[which];
                fbGame selected = games.get(which);

                Intent intent = new Intent(MainActivity.this, Game.class);
                intent.putExtra("gameDir", gameDir);
                intent.putExtra("openingPastGame", "true");
                intent.putExtra("gameName", pastGames[which]);
                intent.putExtra("homeName", selected.homeTeam);
                intent.putExtra("awayName", selected.awayTeam);
                intent.putExtra("division", selected.division);
                intent.putExtra("day", selected.day);
                intent.putExtra("month", selected.month);
                intent.putExtra("year", selected.year);
                startActivity(intent);
            }
        });
        AlertDialog alertDialog = builder.create();
        ListView listView = alertDialog.getListView();
        listView.setDivider(new ColorDrawable(Color.parseColor("#006400"))); // set color
        listView.setDividerHeight(4);

        alertDialog.show();
    }

    private List<File> getListFolders(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.add(file);
            }
            else {
                /*if(file.getName().endsWith(".csv")){
                    inFiles.add(file);
                }*/
            }
        }
        return inFiles;
    }

    private void showNewGameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Enter the Game Information");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set up the input
        final EditText homeTeam = new EditText(MainActivity.this);
        final EditText awayTeam = new EditText(MainActivity.this);

        //Drop down spinners for division and field size
        final Spinner divisionDropdown = new Spinner(MainActivity.this);
        final Spinner fieldSizeDropdown = new Spinner(MainActivity.this);

        final Spinner dayDropdown = new Spinner(MainActivity.this);
        final Spinner monthDropdown = new Spinner(MainActivity.this);
        final Spinner yearDropdown = new Spinner(MainActivity.this);

        // Specify the type of input expected;
        homeTeam.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        homeTeam.setHint("Home Team");
        layout.addView(homeTeam);

        awayTeam.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        awayTeam.setHint("Away Team");
        layout.addView(awayTeam);

        String[] divisions = new String[]{"Select Division", "TINY-MITE", "MITEY-MITE", "JR. PEE WEE", "PEE WEE",
                        "JR. MIDGET", "MIDGET", "FRESHMAN", "JR. VARSITY", "VARSITY"};
        ArrayAdapter<String> divadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, divisions);
        divisionDropdown.setAdapter(divadapter);
        divisionDropdown.setSelection(0);
        layout.addView(divisionDropdown);

        String[] fields = new String[]{"Select Field Size", "100", "80"};
        ArrayAdapter<String> fsadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, fields);
        fieldSizeDropdown.setAdapter(fsadapter);
        fieldSizeDropdown.setSelection(0);
        layout.addView(fieldSizeDropdown);

        //array for the spinner MONTH
        String[] months = new String[]{"Select Month","January","February","March","April","May","June","July","August","September","October","November","December"};
        ArrayAdapter<String> month_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, months);
        monthDropdown.setAdapter(month_adapter);
        monthDropdown.setSelection(0);
        layout.addView(monthDropdown);

        //array for the spinner DAY
        String[] days = new String[]{"Select Day","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15",
                "16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31"};
        ArrayAdapter<String> day_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, days);
        dayDropdown.setAdapter(day_adapter);
        dayDropdown.setSelection(0);
        layout.addView(dayDropdown);

        //array for the spinner YEAR
        String[] years = new String[]{"Select Year", "2016","2017","2018","2019","2020","2021","2022","2023","2024","2025","2026"};
        ArrayAdapter<String> year_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, years);
        yearDropdown.setAdapter(year_adapter);
        yearDropdown.setSelection(0);
        layout.addView(yearDropdown);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                homeName = homeTeam.getText().toString();
                awayName = awayTeam.getText().toString();

                division = divisionDropdown.getSelectedItem().toString();
                fieldsize = fieldSizeDropdown.getSelectedItem().toString();

                dayString = dayDropdown.getSelectedItem().toString();
                monthString = monthDropdown.getSelectedItem().toString();
                yearString = yearDropdown.getSelectedItem().toString();

                boolean failflag = false;

                if (homeName.equals("")) {
                    homeTeam.setError("Home Team is required!");
                    Toast t = Toast.makeText(getApplicationContext(), "Please Enter a Home Team", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }
                if (awayName.equals("")) {
                    awayTeam.setError("Away Team is required!");
                    Toast t = Toast.makeText(getApplicationContext(), "Please Enter an Away Team", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }
                if (division.equals("Select Division")) {
                    Toast t = Toast.makeText(getApplicationContext(), "Please Select a Division", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }
                if (fieldsize.equals("Select Field Size")) {
                    Toast t = Toast.makeText(getApplicationContext(), "Please Select a Field Size", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }

                if (dayString.equals("Select Day")) {
                    Toast t = Toast.makeText(getApplicationContext(), "Please Select a Day", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }
                if (monthString.equals("Select Month")) {
                    Toast t = Toast.makeText(getApplicationContext(), "Please Select a Month", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }
                if (dayString.equals("Select Year")) {
                    Toast t = Toast.makeText(getApplicationContext(), "Please Select a Year", Toast.LENGTH_SHORT);
                    t.show();
                    failflag = true;
                }

                Log.d(logtag, "MONTH: " + monthToInt(monthString));

                if (!failflag) {
                    Intent intent = new Intent(MainActivity.this, Game.class);
                    intent.putExtra("homeName", homeName);
                    intent.putExtra("awayName", awayName);
                    intent.putExtra("division", division);
                    intent.putExtra("fieldSize", fieldsize);
                    intent.putExtra("day", dayString);
                    intent.putExtra("month", Integer.toString(monthToInt(monthString)));
                    intent.putExtra("year", yearString);
                    intent.putExtra("openingPastGame", "false");
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    //Game class nested inside MainActivity
    private class fbGame {
        String awayTeam, homeTeam, division, year, month, day;

        fbGame(String awayName, String homeName, String div, String day_date, String month_date, String year_date) {
            awayTeam = awayName; homeTeam = homeName; division = div;
            year = year_date; month = month_date; day = day_date;
        }
    }
}
