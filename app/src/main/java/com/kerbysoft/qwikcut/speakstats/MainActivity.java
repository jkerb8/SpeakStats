package com.kerbysoft.qwikcut.speakstats;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.RecognitionService;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.Objects;

public class MainActivity extends Activity implements View.OnClickListener {

    static final String logtag = "MyLogTag";
    private String homeName = "", awayName = "", fieldsize = "", division = "";

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
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newGameBtn: {
                showNewGameDialog();
            }
            case R.id.openGameBtn: {

            }

        }

    }

    private void showNewGameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Enter the Game Information");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set up the input
        final EditText homeTeam = new EditText(MainActivity.this);
        final EditText awayTeam = new EditText(MainActivity.this);
        final Spinner divisionDropdown = new Spinner(MainActivity.this);
        final Spinner fieldSizeDropdown = new Spinner(MainActivity.this);
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



        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                homeName = homeTeam.getText().toString();
                awayName = awayTeam.getText().toString();
                division = divisionDropdown.getSelectedItem().toString();
                fieldsize = fieldSizeDropdown.getSelectedItem().toString();

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


                if (!failflag) {
                    Intent intent = new Intent(MainActivity.this, Game.class);
                    intent.putExtra("homeName", homeName);
                    intent.putExtra("awayName", awayName);
                    intent.putExtra("division", division);
                    intent.putExtra("fieldSize", fieldsize);
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
}
