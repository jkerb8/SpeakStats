package com.kerbysoft.qwikcut.speakstats;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Game extends AppCompatActivity implements View.OnClickListener{

    protected static final int RESULT_SPEECH = 1;

    //private ImageButton btnSpeak;
    //private Button exportButton;
    //private TextView txtText;
    TextView qtrTextView;
    TextView homeScoreTextView;
    TextView awayScoreTextView;
    public ArrayList<String> playList;
    public ArrayList<String> csvList;
    public ArrayList<String> statsList;

    String csvplaylist = "play_list.csv";
    String csvstatslist = "stats_list.csv";
    FileOutputStream outputStream;
    String hometeamname="", awayteamname="", gameName = "";
    Integer playerNumber = 00, recNumber=00, ydLn = 0, gnLs=0,  fieldPos = 0, playCounter = 0,
            downNum = 0, dist = 10, qtr = 1, fgDistance = 0;
    Integer recFlag, lossFlag, returnFlag, fgMadeFlag, oppTerFlag, incompleteFlag, touchdownFlag;
    String prevWord = "", playType = "", twowordsago = "", curWord, nextWord = "", result = "";
    boolean invalidPlay = false;
    static final String logtag = "MyLogTag";
    public Team awayTeam  = new Team();
    public Team homeTeam  = new Team();
    public ArrayList<Play> gamePlays = new ArrayList<Play>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ImageButton btnSpeak;
        Button exportButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_game);

        Intent intent = getIntent(); // gets the previously created intent

        //pull in the the team names here and make new team instances and the game name
        hometeamname = intent.getStringExtra("homeName");
        awayteamname = intent.getStringExtra("awayName");

        gameName = hometeamname + " vs. " + awayteamname;

        Log.d(logtag, hometeamname + ", " + awayteamname + ", " + gameName);

        homeTeam.setTeamName(hometeamname);
        awayTeam.setTeamName(awayteamname);

        playList = new ArrayList<String>();
        csvList = new ArrayList<String>();
        statsList = new ArrayList<String>();

        awayScoreTextView = (TextView) findViewById(R.id.awayScoreNumberText);
        homeScoreTextView = (TextView) findViewById(R.id.homeScoreNumberText);
        qtrTextView = (TextView) findViewById(R.id.qtrNumberText);

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
            default : {
                Integer id = v.getId();
                Play play = gamePlays.get(id - 1);

                Log.d(logtag, String.valueOf(id) + ", " + play.getResult());

                /*
                Toast t = Toast.makeText(getApplicationContext(), "Functionality to be added soon...\n" + play.getResult(), Toast.LENGTH_SHORT);
                t.show();*/
                //here is where a pop-up dialog containing the play information where it can be edited
                //will be displayed

                // custom dialog
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.playlayout);
                dialog.setTitle("Play " + String.valueOf(id));

                // set the custom dialog components - text, image and button
                TextView result = (TextView) dialog.findViewById(R.id.text);
                TextView playType = (TextView) dialog.findViewById(R.id.playTypeView);
                TextView mainPlayer = (TextView) dialog.findViewById(R.id.mainPlayerView);
                TextView receiver = (TextView) dialog.findViewById(R.id.receiverView);
                TextView down = (TextView) dialog.findViewById(R.id.downView);
                TextView distance = (TextView) dialog.findViewById(R.id.distanceView);
                TextView quarter = (TextView) dialog.findViewById(R.id.qtrView);

                TextView playTypeEdit = (TextView) dialog.findViewById(R.id.playTypeEditText);
                TextView mainPlayerEdit = (TextView) dialog.findViewById(R.id.mainPlayerEditText);
                TextView receiverEdit = (TextView) dialog.findViewById(R.id.receiverEditText);
                TextView downEdit = (TextView) dialog.findViewById(R.id.downEditText);
                TextView distanceEdit = (TextView) dialog.findViewById(R.id.distanceEditText);
                TextView quarterEdit = (TextView) dialog.findViewById(R.id.qtrEditText);

                result.setText(play.getResult());
                playTypeEdit.setText(play.getPlayType());
                mainPlayerEdit.setText(String.valueOf(play.getPlayerNumber()));
                receiverEdit.setText(String.valueOf(play.getRecNumber()));
                downEdit.setText(String.valueOf(play.getDownNum()));
                distanceEdit.setText(String.valueOf(play.getDist()));
                quarterEdit.setText(String.valueOf(play.getQtr()));

                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonClose);
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();


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
                    resetValues();
                    analyzedPlay = analyzePlay(spokenText);
                    result = getResult();

                    if (!invalidPlay) {

                        if (!(Objects.equals(playType, "End"))) {

                            playCounter++;

                            //creating a new play and adding its attributes, then adding it to the ArrayList of plays
                            Play currentPlay = new Play();

                            currentPlay.setDist(dist);
                            currentPlay.setDownNum(downNum);
                            currentPlay.setFgDistance(fgDistance);
                            currentPlay.setFgMadeFlag(fgMadeFlag);
                            currentPlay.setFieldPos(fieldPos);
                            currentPlay.setYdLn(ydLn);
                            currentPlay.setGnLs(gnLs);
                            currentPlay.setIncompleteFlag(incompleteFlag);
                            currentPlay.setPlayCount(playCounter);
                            currentPlay.setPlayerNumber(playerNumber);
                            currentPlay.setPlayType(playType);
                            currentPlay.setQtr(qtr);
                            currentPlay.setRecNumber(recNumber);
                            currentPlay.setReturnFlag(returnFlag);
                            currentPlay.setResult(result);

                            gamePlays.add(currentPlay);
                            addButton(result, playCounter);
                            playList.add(result);
                            csvList.add(text.get(0) + " , " + String.valueOf(playCounter) + "\n");
                            statsList.add(analyzedPlay);

                            awayScoreTextView.setText(String.valueOf(awayTeam.getTeamScore()));
                            homeScoreTextView.setText(String.valueOf(homeTeam.getTeamScore()));
                        }
                        else
                            qtrTextView.setText(String.valueOf(qtr));

                    }
                    else {

                        revertToLastPlay(gamePlays.get(playCounter - 1));

                        Toast t = Toast.makeText(getApplicationContext(), "Play not recognized as a valid play. Please try again.", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
                break;
            }

        }
    }

    private void resetValues() {
        playerNumber = 00;
        recNumber = 00;
        gnLs=0;
        returnFlag=0;
        incompleteFlag=0;
        touchdownFlag=0;
        invalidPlay = false;
        playType = "";
    }

    private void revertToLastPlay(Play lastPlay) {
        dist = lastPlay.getDist();
        downNum = lastPlay.getDownNum();
        fgDistance = lastPlay.getFgDistance();
        fgMadeFlag = lastPlay.getFgMadeFlag();
        fieldPos = lastPlay.getFieldPos();
        ydLn= lastPlay.getYdLn();
        gnLs = lastPlay.getGnLs();
        incompleteFlag = lastPlay.getIncompleteFlag();
        playerNumber = lastPlay.getPlayerNumber();
        playType = lastPlay.getPlayType();
        qtr = lastPlay.getQtr();
        recNumber = lastPlay.getRecNumber();
        returnFlag = lastPlay.getReturnFlag();
        result = lastPlay.getResult();
    }

    Integer intParse(String word) {
        Integer number = 0;

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
                if ((word.length() >= 1) || (word.length() <= 3)) {
                    try {
                        number = Integer.parseInt(word);
                    }
                    catch (Exception e) {
                        invalidPlay = true;
                    }
                }
                else {
                    invalidPlay = true;
                }
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
        playBtn.setOnClickListener(this);

    }

    private String analyzePlay(String spokenText) {
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
            if (Objects.equals(curWord, "defense")) {
                if (Objects.equals(playType, "Penalty"))
                    gnLs *= -1;
            }

            if (Objects.equals(curWord, "penalty") || Objects.equals(curWord, "penalties")) {
                playType = "Penalty";
                playCounter--;
                gnLs *= -1;
                continue;
            }

            if (Objects.equals(curWord, "passed") || Objects.equals(curWord, "past") || Objects.equals(curWord, "pass") || Objects.equals(curWord, "pastor")) {
                playType = "Pass";
                prevWord = curWord;
                recFlag = 1;
                continue;
            }

            if (Objects.equals(curWord, "incomplete") || Objects.equals(curWord, "incompletes") || Objects.equals(curWord, "incompleted") || Objects.equals(curWord, "incompletion")
                    || (Objects.equals(curWord, "incompletion") && Objects.equals(prevWord, "in"))) {
                prevWord = curWord;
                incompleteFlag = 1;
                gnLs = 0;
                continue;
            }

            if (Objects.equals(curWord, "ran") || Objects.equals(curWord, "ranch") || Objects.equals(curWord, "run") || Objects.equals(curWord, "ram") || Objects.equals(curWord, "grand")
                    || Objects.equals(curWord, "rand") || Objects.equals(curWord, "rent") || Objects.equals(curWord, "ranger")) {
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

            if (Objects.equals(curWord, "quarter") || Objects.equals(curWord, "water") || Objects.equals(nextWord, "court")) {
                playType = "End";
                continue;
            }

                if (Objects.equals(curWord, "touchdown") || (Objects.equals(curWord, "touch") && Objects.equals(nextWord, "down"))) {
                gnLs = 100 - fieldPos;
                fieldPos = 100;
                touchdownFlag = 1;
                if (homeTeam.getOnOffense()) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 6);
                }
                else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 6);
                }
                //add 6 pts to current team's score - need to make score variables for each team and shiz
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
                    || Objects.equals(curWord, "cake") || Objects.equals(curWord, "kickoff") || Objects.equals(curWord, "kids")) {

                playType = "Kickoff";
                twowordsago = prevWord;
                prevWord = curWord;
                continue;
            }

            if (Objects.equals(curWord, "fieldgoal") || (Objects.equals(curWord, "field") && Objects.equals(nextWord, "goal"))) {
                playType = "Field Goal";
                fgDistance = (100 - fieldPos) + 17;
                if (homeTeam.getOnOffense()) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 3);
                }
                else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 3);
                }
            }

            if (Objects.equals(curWord, "conversion") || ((Objects.equals(prevWord, "point"))
                    && ((Objects.equals(twowordsago, "two")) || (Objects.equals(twowordsago, "2"))))) {
                playType = "2 Pt. Conversion";
                if (homeTeam.getOnOffense()) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 2);
                }
                else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 2);
                }
            }

            if (Objects.equals(curWord, "pat") || (Objects.equals(curWord, "point") && Objects.equals(nextWord, "after"))) {
                playType = "PAT";
                fgDistance = 20;
                if (homeTeam.getOnOffense()) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 1);
                }
                else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 1);
                }
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
        if ((fieldPos < 50) && (fieldPos > 0) && (oppTerFlag == 1)) {
            ydLn = fieldPos;
            fieldPos = 50 - (ydLn - 50);
        }
        else if ((fieldPos < 50) && (fieldPos > 0))
            ydLn = fieldPos * -1;
        else if (fieldPos > 49)
            ydLn = 50 - (fieldPos - 50);
        else ;
        //there's a touchdown or safety

        //putting it all together...
        String returnedPlay = String.valueOf(playCounter) + " , " + playType + " , " + String.valueOf(playerNumber) + " , " + String.valueOf(recNumber) + " , "
                + String.valueOf(downNum) + " , " + String.valueOf(dist) + " , " + String.valueOf(ydLn) + " , " + String.valueOf(gnLs) + " , " +
                String.valueOf(qtr) + "\n";

        Log.d(logtag, returnedPlay);

        return returnedPlay;
    }

    private void changePossesion() {
        homeTeam.setOnOffense(!homeTeam.getOnOffense());
        awayTeam.setOnOffense(!awayTeam.getOnOffense());
    }

    private String getResult() {
        String playResult = null;

        switch (playType) {
            case "Pass":
                if (incompleteFlag == 0) {
                    playResult = "Number " + String.valueOf(playerNumber) + " pass completed to number " + String.valueOf(recNumber)
                            + " for " + String.valueOf(gnLs) + " yards";
                }
                else {
                    if (recNumber == null)
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete";
                    else
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete to number " + String.valueOf(recNumber);
                }
                break;
            case "Run":
                playResult = "Number " + String.valueOf(playerNumber) + " ran for " + String.valueOf(gnLs) + " yards";
                break;
            case "Kickoff":
                if ((returnFlag == 1) && (touchdownFlag == 0))
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff to the " + String.valueOf(ydLn) + " yardline";
                else if ((returnFlag == 1) && (touchdownFlag == 1)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff";
                }
                else ;
                    //i guess there was a touchback or he kicked it out of bounds
                break;
            case "Punt":
                if ((returnFlag == 1) && (touchdownFlag == 0))
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the punt " + String.valueOf(gnLs) +
                            " yards to the " + String.valueOf(ydLn) + " yardline";
                else if ((returnFlag == 1) && (touchdownFlag == 1)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the punt";
                }
                else ;
                    //fair catch or touchback or some bullshit
                break;
            case "Field Goal":
                if (fgMadeFlag == 1)
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was good";
                else
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was no good";
                break;
            case "PAT":
                if (fgMadeFlag == 1)
                    playResult = "The PAT was good";
                else
                    playResult = "The PAT was no good";
                break;
            case "2 Pt. Conversion":
                playResult = "2 Pt. Conversion - fix this";
                break;
            case "Penalty":
                playResult = String.valueOf(gnLs) + " yard penalty";
                break;
            case "End":
                switch (qtr) {
                    case 1:
                        playResult = "End of 1st Quarter";
                        break;
                    case 2:
                        playResult = "End of 2nd Quarter";
                        break;
                    case 3:
                        playResult = "End of 3rd Quarter";
                        break;
                    case 4:
                        playResult = "End of Game";
                        break;
                    default:
                        playResult = "Something is wrong :-(";
                        break;
                }
                qtr++;
                break;
            default:
                invalidPlay = true;
                break;
        }

        if ((touchdownFlag == 1) && (playResult != null)) {
            playResult = playResult.concat(" for a TOUCHDOWN!");
        }

        return playResult;
    }

    /**
     * PLAY CLASS - NESTED INSIDE GAME
     */
    public class Play {

        Integer playerNumber, recNumber, ydLn, gnLs,  fieldPos, downNum, dist, qtr, fgDistance, playCount;
        Integer recFlag, lossFlag, returnFlag, fgMadeFlag, oppTerFlag, incompleteFlag;
        String playType;

        public Integer getPlayerNumber() {
            return playerNumber;
        }

        public void setPlayerNumber(Integer playerNumber) {
            this.playerNumber = playerNumber;
        }

        public Integer getRecNumber() {
            return recNumber;
        }

        public void setRecNumber(Integer recNumber) {
            this.recNumber = recNumber;
        }

        public Integer getYdLn() {
            return ydLn;
        }

        public void setYdLn(Integer ydLn) {
            this.ydLn = ydLn;
        }

        public Integer getGnLs() {
            return gnLs;
        }

        public void setGnLs(Integer gnLs) {
            this.gnLs = gnLs;
        }

        public Integer getFieldPos() {
            return fieldPos;
        }

        public void setFieldPos(Integer fieldPos) {
            this.fieldPos = fieldPos;
        }

        public Integer getDownNum() {
            return downNum;
        }

        public void setDownNum(Integer downNum) {
            this.downNum = downNum;
        }

        public Integer getDist() {
            return dist;
        }

        public void setDist(Integer dist) {
            this.dist = dist;
        }

        public Integer getQtr() {
            return qtr;
        }

        public void setQtr(Integer qtr) {
            this.qtr = qtr;
        }

        public Integer getFgDistance() {
            return fgDistance;
        }

        public void setFgDistance(Integer fgDistance) {
            this.fgDistance = fgDistance;
        }

        public Integer getPlayCount() {
            return playCount;
        }

        public void setPlayCount(Integer playCount) {
            this.playCount = playCount;
        }

        public Integer getFgMadeFlag() {
            return fgMadeFlag;
        }

        public void setFgMadeFlag(Integer fgMadeFlag) {
            this.fgMadeFlag = fgMadeFlag;
        }

        public Integer getReturnFlag() {
            return returnFlag;
        }

        public void setReturnFlag(Integer returnFlag) {
            this.returnFlag = returnFlag;
        }

        public Integer getIncompleteFlag() {
            return incompleteFlag;
        }

        public void setIncompleteFlag(Integer incompleteFlag) {
            this.incompleteFlag = incompleteFlag;
        }

        public String getPlayType() {
            return playType;
        }

        public void setPlayType(String playType) {
            this.playType = playType;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        String result = "";


    }

    /**
     * TEAM CLASS - NESTED INSIDE GAME
     */
    public static class Team {

        private int teamScore = 0;
        private String teamName;
        private boolean onOffense;
        private ArrayList<Player> players = new ArrayList<Player>();

        public int getTeamScore(){return teamScore; }

        public void setTeamScore(int teamScore){ this.teamScore = teamScore; }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public Boolean getOnOffense() {
            return onOffense;
        }

        public void setOnOffense(Boolean onOffense) {
            this.onOffense = onOffense;
        }

        public ArrayList<Player> getPlayers() {
            return players;
        }

        public void addPlayer(Player player) {
            this.players.add(player);
        }

    }

    /**
     * PLAYER CLASS - NESTED INSIDE GAME
     */
    public static class Player {
        private static Integer number;
        private static Integer passcomps;
        private static Integer passatmpts;
        private static Integer passyds;
        private static Integer runatmpts;
        private static Integer runyds;
        private static Boolean offensive;

        public void Player(Integer num, Boolean offense ) {
            this.number = num;
            this.offensive = offense;
        }

        public static Integer getRunyds() {
            return runyds;
        }

        public static void setRunyds(Integer runyds) {
            Player.runyds = runyds;
        }

        public static Integer getPasscomps() {
            return passcomps;
        }

        public static void setPasscomps(Integer passcomps) {
            Player.passcomps = passcomps;
        }

        public static Integer getPassatmpts() {
            return passatmpts;
        }

        public static void setPassatmpts(Integer passatmpts) {
            Player.passatmpts = passatmpts;
        }

        public static Integer getPassyds() {
            return passyds;
        }

        public static void setPassyds(Integer passyds) {
            Player.passyds = passyds;
        }

        public static Integer getRunatmpts() {
            return runatmpts;
        }

        public static void setRunatmpts(Integer runatmpts) {
            Player.runatmpts = runatmpts;
        }

    }
}
