package com.kerbysoft.qwikcut.speakstats;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.RecognitionService;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class MainActivity extends Activity implements View.OnClickListener {

    protected static final int RESULT_SPEECH = 1;

    //private ImageButton btnSpeak;
    //private Button exportButton;
    //private TextView txtText;
    public ArrayList<String> playList;
    public ArrayList<String> csvList;
    public ArrayList<String> statsList;
    int counter = 0;
    String csvplaylist = "play_list.csv";
    String csvstatslist = "stats_list.csv";
    FileOutputStream outputStream;
    String gamename = "Game1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ImageButton btnSpeak;
        Button exportButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playList = new ArrayList<String>();
        csvList = new ArrayList<String>();
        statsList = new ArrayList<String>();

        //File directory = getFilesDir();
        // Create a new output file stream



        exportButton = (Button) findViewById(R.id.exportButton);

        //txtText.setText("This is where this text will be.");

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(this);

        exportButton.setOnClickListener(this);

        File folder = new File(this.getFilesDir() + "/" + gamename);

        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.btnSpeak: {

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    //txtText.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(), "Oops! Your device doesn't support Speech to Text", Toast.LENGTH_SHORT);
                    t.show();
                }
                break;
            }
            case R.id.exportButton: {

                File csvFile = new File(this.getFilesDir() + "/" + gamename + "/", csvplaylist);
                File statsFile = new File(this.getFilesDir() + "/" + gamename + "/", csvstatslist);
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                String downloadsDir = downloads.getAbsolutePath();
                String currentDirectory = csvFile.getParent() + "/";

                statsList = AnalyzeText(playList);

                for (int i=0; i<2; i++) {
                    String filename = "";
                    ArrayList<String> tempList = new ArrayList<String>();

                    switch (i) {
                        case 0:
                            filename = csvplaylist;
                            tempList = csvList;
                            break;
                        case 1:
                            filename = csvstatslist;
                            tempList = statsList;
                            break;
                    }


                    try {
                        outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    for(String output: tempList) {
                        try {
                            outputStream.write(output.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        copyFile(currentDirectory, csvplaylist, downloadsDir);
                    } catch (Exception a) {
                        Toast t = Toast.makeText(getApplicationContext(), "Uh oh. Export Failed!", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }


                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TextView txtText = (TextView) findViewById(R.id.txtText);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtText.setText(text.get(0));
                    playList.add(text.get(0));
                    csvList.add(text.get(0) + " , " + String.valueOf(++counter) + "\n");

                }
                break;
            }

        }
    }

    private void copyFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                //dir.mkdirs();
                Toast t = Toast.makeText(getApplicationContext(), "Downloads directory could not be found!", Toast.LENGTH_SHORT);
                t.show();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;

        }  catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    ArrayList<String> AnalyzeText(ArrayList<String> playList) {
        ArrayList<String> returnList = new ArrayList<String>();
        //ArrayList<String> type = new ArrayList<String>();
        /*for (int i=0; i<9; i++) {
            switch (i) {
                case 0:
                    type.add("Run");
                    break;
                case 1:
                    type.add("Pass");
                    break;
                case 2:
                    type.add("Kick");
                    break;
                case 3:
                    type.add("Punt");
                    break;
                case 4:
                    type.add("Kick Return");
                    break;
                case 5:
                    type.add("Punt Return");
                    break;
                case 6:
                    type.add("Field Goal");
                    break;
                case 7:
                    type.add("PAT");
                    break;
                case 8:
                    type.add("2 Point Conv.");
                    break;
                default:
                    break;
            }
        }*/

        Integer playnum = 1, playerNumber = 0, recNumber, downNum = 1, dist = 10, ydLn = 20, gnLs = 0, qtr = 1, recFlag, lossFlag;
        String prevWord = "", playType = "", addition = "";


        for (String temp : playList) {
            temp = temp.toLowerCase();
            System.out.println(temp);

            String[] words = temp.split("\\s+");
            recFlag = 0;
            lossFlag = 0;
            recNumber = 0;

            for(int m=0; m<words.length; m++){
                String curWord = words[m];
                System.out.println(curWord + ", " + prevWord);

                if (prevWord == "and") {
                    if(curWord.charAt(0)=='4'){
                        playType = "Run";
                        curWord = '0' + curWord.substring(1);
                    }
                }

                if ((curWord == "passed") || (curWord == "past") || (curWord == "pass")) {
                    playType = "Pass";
                    prevWord = curWord;
                    m++;
                    recFlag = 1;
                    continue;
                }

                if ((curWord == "ran") || (curWord == "ranch") || (curWord == "run") || (curWord == "ram") || (curWord == "grand")) {
                    playType = "Run";
                    prevWord = curWord;
                    m++;
                    continue;
                }

                if (curWord == "loss") {
                    lossFlag = 1;
                    continue;
                }

                if ((curWord == "yards") || (curWord == "yard") || (curWord == "lard")) {
                    gnLs = intParse(prevWord);
                    if (lossFlag == 1) {
                        gnLs = gnLs * -1;
                    }
                    else
                        continue;
                }

                if ((prevWord == "number") || (prevWord == "player")) {
                    if (curWord == "number") {
                        continue;
                    }
                    if (recFlag == 1) {
                        recNumber = intParse(curWord);
                    } else {
                        playerNumber = intParse(curWord);
                    }
                }


                /*for(int n=0; n<words[m].length(); n++){
                    }*/


                prevWord = curWord;

            }
            addition = String.valueOf(playnum) + " , " + playType + " , " + String.valueOf(playerNumber) + " , " + String.valueOf(recNumber) + " , "
                    + String.valueOf(downNum) + " , " + String.valueOf(dist) + " , " + String.valueOf(ydLn) + " , " + String.valueOf(gnLs) + " , " +
                    String.valueOf(qtr) + "\n";
            returnList.add(addition);

            playnum++;
        }


        return returnList;
    }

    Integer intParse(String word) {
        Integer number;

        switch (word) {
            case "won":
                number = 1;
                break;
            case "one":
                number = 1;
                break;
            case "two":
                number = 2;
                break;
            case "to":
                number = 2;
                break;
            case "too":
                number = 2;
                break;
            case "for":
                number = 4;
                break;
            case "fore":
                number = 4;
                break;
            case "four":
                number = 4;
                break;
            case "ate":
                number = 8;
                break;
            default:
                number = Integer.parseInt(word);
                break;
        }

        return number;
    }

}
