package com.kerbysoft.qwikcut.speakstats;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class Game extends AppCompatActivity implements View.OnClickListener{

    protected static final int RESULT_SPEECH = 1;

    //private ImageButton btnSpeak;
    //private Button exportButton;
    //private TextView txtText;
    public ArrayList<String> playList;
    public ArrayList<String> csvList;
    public ArrayList<String> statsList;
    Integer playCounter = 0;
    String csvplaylist = "play_list.csv";
    String csvstatslist = "stats_list.csv";
    FileOutputStream outputStream;
    String gameName = "";
    Integer playerNumber = null, recNumber, ydLn = 0, gnLs,  fieldPos = 0,
            downNum = 0, dist = 10, qtr = 1, fgDistance = null;
    Integer recFlag, lossFlag, returnFlag, fgMadeFlag, oppTerFlag, incompleteFlag;
    String prevWord = "", playType = "", addition, twowordsago = "", curWord, nextWord = "", result = "";
    static final String logtag = "MyLogTag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ImageButton btnSpeak;
        Button exportButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_game);

        Intent intent= getIntent(); // gets the previously created intent
        String name = intent.getStringExtra("gameName");
        gameName = name;

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

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;

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

                String gamePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;
                File csvFile = new File(gamePath, csvplaylist);
                File statsFile = new File(gamePath, csvstatslist);

                statsList = AnalyzeText(playList);

                for (int i=0; i<2; i++) {
                    File file = null;
                    ArrayList<String> tempList = new ArrayList<String>();

                    switch (i) {
                        case 0:
                            file = csvFile;
                            tempList = csvList;
                            break;
                        case 1:
                            file = statsFile;
                            tempList = statsList;
                            break;
                    }

                    FileOutputStream fileStream = null;
                    try {
                        fileStream = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }


                    for(String output: tempList) {
                        try {
                            fileStream.write(output.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
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
        String spokenText, analyzedPlay;

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtText.setText(text.get(0));
                    spokenText = text.get(0);
                    playCounter++;

                    // THIS IS WHERE THE PLAY IS ANALYZED
                    //
                    //
                    //
                    String temp = spokenText.toLowerCase();
                    Log.d(logtag, temp);

                    if (downNum > 4)
                        downNum = 1;

                    String[] words = temp.split("\\s+");
                    recFlag = 0; //flag that marks if there was a reception
                    lossFlag = 0; //flag that marks if there was a loss on the play
                    returnFlag = 0; //flag that marks if there is a return on the play
                    oppTerFlag = 0; //flag to mark the field position in opponent's territory
                    incompleteFlag = 0; //flag to mark if there was an incomplete pass or not
                    recNumber = null;
                    gnLs = 0;

                    for(int m=0; m<words.length; m++){
                        curWord = words[m];
                        if (m != (words.length - 1))
                            nextWord = words[m+1];


                        if (Objects.equals(prevWord, "and")) {
                            if(curWord.charAt(0)=='4'){
                                playType = "Run";
                                curWord = curWord.substring(1);
                            }
                        }

                        if (Objects.equals(curWord, "passed") || Objects.equals(curWord, "past") || Objects.equals(curWord, "pass")) {
                            playType = "Pass";
                            prevWord = curWord;
                            recFlag = 1;
                            continue;
                        }

                        if (Objects.equals(curWord, "incomplete") || Objects.equals(curWord, "incompletes") || Objects.equals(curWord, "incompleted")) {
                            prevWord = curWord;
                            incompleteFlag = 1;
                            gnLs = 0;
                            continue;
                        }

                        if (Objects.equals(curWord, "ran") || Objects.equals(curWord, "ranch") || Objects.equals(curWord, "run") || Objects.equals(curWord, "ram") || Objects.equals(curWord, "grand")
                                || Objects.equals(curWord, "Rand")) {
                            playType = "Run";
                            prevWord = curWord;
                            m++;
                            continue;
                        }

                        if (Objects.equals(curWord, "loss")) {
                            lossFlag = 1;
                            continue;
                        }
                        if (Objects.equals(curWord, "yardline") || (Objects.equals(curWord, "yard") && Objects.equals(nextWord, "line"))) {

                            fieldPos = intParse(prevWord);
                            twowordsago = prevWord;
                            prevWord = curWord;
                            continue;
                        }

                        if (Objects.equals(curWord, "yards") || Objects.equals(curWord, "yard") || Objects.equals(curWord, "lard")) {
                            gnLs = intParse(prevWord);
                            if (lossFlag == 1) {
                                gnLs = gnLs * -1;
                            }
                        }

                        if (Objects.equals(prevWord, "number") || Objects.equals(prevWord, "numbers") || Objects.equals(prevWord, "player") || Objects.equals(prevWord, "players")
                                || Objects.equals(prevWord, "never")) {
                            if (Objects.equals(curWord, "number")) {
                                twowordsago = prevWord;
                                prevWord = curWord;
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
                            twowordsago = prevWord;
                            prevWord = curWord;
                            continue;
                        }

                        if (Objects.equals(curWord, "punt") || Objects.equals(curWord, "punts") || Objects.equals(curWord, "punted")) {

                            playType = "Punt";
                            twowordsago = prevWord;
                            prevWord = curWord;
                            continue;
                        }

                        if (Objects.equals(curWord, "kick") || Objects.equals(curWord, "kicks") || Objects.equals(curWord, "kicked") || Objects.equals(curWord, "kickoff")
                                || Objects.equals(curWord, "cake") || Objects.equals(curWord, "kickoff")) {

                            playType = "Kickoff";
                            twowordsago = prevWord;
                            prevWord = curWord;
                            continue;
                        }

                        if (Objects.equals(curWord, "fieldgoal") || (Objects.equals(curWord, "field") && Objects.equals(nextWord, "goal"))) {
                            playType = "Field Goal";
                            fgDistance = (100 - fieldPos) + 17;
                        }

                        if (Objects.equals(curWord, "pat") || (Objects.equals(curWord, "point") && Objects.equals(nextWord, "after"))) {
                            playType = "PAT";
                            fgDistance = 20;
                        }

                        //checks if FG/PAT was good, might need more cases
                        if (Objects.equals(curWord, "good") || Objects.equals(curWord, "could")) {
                            if (Objects.equals(prevWord, "no") || Objects.equals(prevWord, "not") || Objects.equals(prevWord, "knot")) {
                                fgMadeFlag = 0;
                            }
                            else
                                fgMadeFlag = 1;
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
                    //fieldPos is 1 to 100 number representing field position
                    fieldPos = fieldPos + gnLs;

                    //sets ydLn to neg if in own territory, pos if in opponents territory
                    if ((fieldPos < 50) && (fieldPos > 0) && (oppTerFlag == 1))
                        ydLn = fieldPos;
                    else if ((fieldPos < 50) && (fieldPos > 0))
                        ydLn = fieldPos * -1;
                    else if (fieldPos > 49)
                        ydLn = 50 - (fieldPos - 50);
                    else ;
                    //there's a touchdown or safety

                    //putting it all together...
                    analyzedPlay = String.valueOf(playCounter) + " , " + playType + " , " + String.valueOf(playerNumber) + " , " + String.valueOf(recNumber) + " , "
                            + String.valueOf(downNum) + " , " + String.valueOf(dist) + " , " + String.valueOf(ydLn) + " , " + String.valueOf(gnLs) + " , " +
                            String.valueOf(qtr) + "\n";

                    Log.d(logtag, analyzedPlay);

                    // THIS IS WHERE THE PLAY IS FINISHED BEING ANALYZED
                    //
                    //
                    //

                    result = getResult();
                    addButton(result, playCounter);
                    playList.add(result);
                    csvList.add(text.get(0) + " , " + String.valueOf(playCounter) + "\n");

                }
                break;
            }

        }
    }

    ArrayList<String> AnalyzeText(ArrayList<String> playList) {
        ArrayList<String> returnList = new ArrayList<String>();

        String listAddition = "";

        for (String temp : playList) {
            returnList.add(listAddition);
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
            case "none":
                number = 9;
                break;
            case "non":
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

    private void addButton(String play, Integer playNum) {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.scrollRelLayout);
        int prevId = (playNum - 1);


        //set the properties for button
        Button playBtn = new Button(this);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //int resID = getResources().getIdentifier(String.valueOf(prevId), "id", getPackageName());
        if (playNum != 1)
            rl.addRule(RelativeLayout.ABOVE, prevId);

        playBtn.setLayoutParams(rl);
        playBtn.setText("Play " + String.valueOf(playNum) + " -  " + play);
        playBtn.setId(playNum);
        ((RelativeLayout) findViewById(R.id.scrollRelLayout)).addView(playBtn, rl);

    }

    private String getResult() {
        String playResult = null;

        switch (playType) {
            case "Pass":
                if (incompleteFlag == 0) {
                    playResult = "Number " + String.valueOf(playerNumber) + " pass completed to number " + String.valueOf(recNumber)
                            + " for " + String.valueOf(gnLs) + " yards.";
                }
                else {
                    if (recNumber == null)
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete.";
                    else
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete to number " + String.valueOf(recNumber)
                                + ".";
                }
                break;
            case "Run":
                playResult = "Number " + String.valueOf(playerNumber) + " ran for " + String.valueOf(gnLs) + " yards.";
                break;
            case "Kickoff":
                if (returnFlag == 1)
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff to the " + String.valueOf(ydLn) + " yardline.";
                else ;
                    //i guess there was a touchback or he kicked it out of bounds
                break;
            case "Punt":
                if (returnFlag == 1)
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the punt " + String.valueOf(gnLs) +
                            " yards to the " + String.valueOf(ydLn) + " yardline.";
                else ;
                    //fair catch or touchback or some bullshit
                break;
            case "Field Goal":
                if (fgMadeFlag == 1)
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was good.";
                else
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was no good.";
                break;
            case "PAT":
                if (fgMadeFlag == 1)
                    playResult = "The PAT was good.";
                else
                    playResult = "The PAT was no good.";
                break;
            case "2 Pt. Conversion":

                break;
            default:

                break;

        }


        return playResult;
    }
}
