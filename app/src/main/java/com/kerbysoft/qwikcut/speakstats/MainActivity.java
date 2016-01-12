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
import java.util.Objects;

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
    static final String logtag = "MyLogTag";

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

        String dirPath = getFilesDir().getAbsolutePath() + File.separator + gamename;

        File projDir = new File(dirPath);
        if (!projDir.exists())
            projDir.mkdirs();
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

                String dirPath = getFilesDir().getAbsolutePath() + File.separator + gamename;
                File csvFile = new File(dirPath, csvplaylist);
                File statsFile = new File(dirPath, csvstatslist);
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                String downloadsDir = downloads.getAbsolutePath();
                String currentDirectory = csvFile.getParent() + "\\";

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
                        copyFile(dirPath, csvplaylist, downloadsDir);
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

        Integer playnum = 1, playerNumber = 0, recNumber, downNum = 1, dist = 10, ydLn = 20, gnLs = 0, qtr = 1,
                recFlag, lossFlag, returnFlag, fieldPos = 0, oppTerFlag = 0;
        String prevWord = "", playType = "", addition = "", twowordsago = "";


        for (String temp : playList) {
            temp = temp.toLowerCase();
            Log.d(logtag, temp);

            if (downNum > 4)
                downNum = 1;

            String[] words = temp.split("\\s+");
            recFlag = 0; //flag that marks if there was a reception
            lossFlag = 0; //flag that marks if there was a loss on the play
            returnFlag = 0; //flag that marks if there is a return on the play
            oppTerFlag = 0; //flag the mark the field position in opponent's territory
            recNumber = null;

            for(int m=0; m<words.length; m++){
                String curWord = words[m];

                if (Objects.equals(prevWord, "and")) {
                    if(curWord.charAt(0)=='4'){
                        playType = "Run";
                        curWord = curWord.substring(1);
                    }
                }

                if (Objects.equals(curWord, "passed") || Objects.equals(curWord, "past") || Objects.equals(curWord, "pass")) {
                    playType = "Pass";
                    prevWord = curWord;
                    m++;
                    recFlag = 1;
                    continue;
                }

                if (Objects.equals(curWord, "ran") || Objects.equals(curWord, "ranch") || Objects.equals(curWord, "run") || Objects.equals(curWord, "ram") || Objects.equals(curWord, "grand")) {
                    playType = "Run";
                    prevWord = curWord;
                    m++;
                    continue;
                }

                if (Objects.equals(curWord, "loss")) {
                    lossFlag = 1;
                    continue;
                }

                if (Objects.equals(curWord, "yards") || Objects.equals(curWord, "yard") || Objects.equals(curWord, "lard")) {
                    gnLs = intParse(prevWord);
                    if (lossFlag == 1) {
                        gnLs = gnLs * -1;
                    }
                    continue;
                }

                if (Objects.equals(prevWord, "number") || Objects.equals(prevWord, "player")) {
                    if (Objects.equals(curWord, "number")) {
                        continue;
                    }
                    if (recFlag == 1) {
                        recNumber = intParse(curWord);
                    } else {
                        playerNumber = intParse(curWord);
                    }
                }

                if (Objects.equals(curWord, "return") || Objects.equals(curWord, "returned") || Objects.equals(curWord, "returns")) {
                    returnFlag = 1;
                    prevWord = curWord;
                    continue;
                }

                if (Objects.equals(curWord, "punt") || Objects.equals(curWord, "punts") || Objects.equals(curWord, "punted")) {
                    if (returnFlag == 1)
                        playType = "Punt Return";
                    else
                        playType = "Punt";
                    prevWord = curWord;
                    continue;
                }

                if (Objects.equals(curWord, "kick") || Objects.equals(curWord, "kicks") || Objects.equals(curWord, "kicked")) {
                    if (returnFlag == 1)
                        playType = "Kick Return";
                    else
                        playType = "Kick";
                    prevWord = curWord;
                    continue;
                }

                if (Objects.equals(curWord, "yardline") || Objects.equals(curWord, "line")) {
                    if (Objects.equals(curWord, "line") && Objects.equals(prevWord, "yard")) {
                        fieldPos = intParse(twowordsago);
                    }
                    else
                        fieldPos = intParse(prevWord);
                    continue;
                }

                if (Objects.equals(curWord, "opponent") || Objects.equals(curWord, "opponents") || Objects.equals(curWord, "opponent's")) {
                    oppTerFlag = 1;
                }

                if ((Objects.equals(curWord, "down") && (Objects.equals(prevWord, "first") || Objects.equals(curWord, "1st")))) {
                    downNum = 1;
                }

                /*for(int n=0; n<words[m].length(); n++){
                    }*/

                twowordsago = prevWord;
                prevWord = curWord;
            }

            fieldPos = fieldPos + gnLs;

            if ((fieldPos < 50) && (fieldPos > 0) && (oppTerFlag == 1))
                ydLn = fieldPos;
            else if ((fieldPos < 50) && (fieldPos > 0))
                ydLn = fieldPos * -1;
            else if (fieldPos > 49)
                ydLn = 50 - (fieldPos - 50);
            else ;
                //there's a touchdown or safety


            addition = String.valueOf(playnum) + " , " + playType + " , " + String.valueOf(playerNumber) + " , " + String.valueOf(recNumber) + " , "
                    + String.valueOf(downNum) + " , " + String.valueOf(dist) + " , " + String.valueOf(ydLn) + " , " + String.valueOf(gnLs) + " , " +
                    String.valueOf(qtr) + "\n";
            returnList.add(addition);
            Log.d(logtag, addition);

            playnum++;
            downNum++;
        }


        return returnList;
    }

    Integer intParse(String word) {
        Integer number = null;

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
            case "five":
                number = 5;
                break;
            case "six":
                number = 6;
                break;
            case "seven":
                number = 7;
                break;
            case "ate":
                number = 8;
                break;
            case "eight":
                number = 8;
                break;
            case "nine":
                number = 9;
                break;
            case "ten":
                number = 10;
                break;
            default:
                if ((word.length() == 1) || (word.length() == 2)) {
                    number = Integer.parseInt(word);
                }
                break;
        }

        return number;
    }

}
