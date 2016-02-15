package com.kerbysoft.qwikcut.speakstats;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
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
    TextView downAndDistTextView;
    ImageView awayPossImageView;
    ImageView homePossImageView;
    public ArrayList<String> playList;
    public ArrayList<String> csvList;
    public ArrayList<String> statsList;

    String csvplaylist = "play_list.csv";
    String csvhomestatslist = "home_stats_list.csv";
    String csvawaystatslist = "away_stats_list.csv";
    FileOutputStream outputStream;
    String hometeamname="", awayteamname="", gameName = "";
    Integer playerNumber = 0, recNumber=0, defNumber=0, ydLn = 0, gnLs=0,  fieldPos = 0, playCounter = 0,
            downNum = 0, dist = 0, qtr = 1, fgDistance = 0, prevDown=0, prevDist=0, returnYds=0;
    Integer lossFlag, returnFlag, fgMadeFlag, oppTerFlag;
    boolean interceptionFlag=false, fumbleFlag=false, incompleteFlag=false, touchdownFlag=false, defensivePenalty=false,
            recFlag=false, touchbackFlag=false, faircatchFlag=false;
    String prevWord = "", playType = "", twowordsago = "", curWord, nextWord = "", result = "";
    boolean invalidPlay = false;
    static final String logtag = "MyLogTag";
    public Team awayTeam  = new Team();
    public Team homeTeam  = new Team();
    public ArrayList<Play> gamePlays = new ArrayList<Play>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Button btnSpeak;
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
        downAndDistTextView = (TextView) findViewById(R.id.downAndDistText);

        awayPossImageView = (ImageView) findViewById(R.id.awayPossImageView);
        homePossImageView = (ImageView) findViewById(R.id.homePossImageView);

        //if away team starts with ball
        homePossImageView.setVisibility(ImageView.INVISIBLE);

        exportButton = (Button) findViewById(R.id.exportButton);

        //txtText.setText("This is where this text will be.");

        btnSpeak = (Button) findViewById(R.id.btnSpeak);

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
                File homeStatsFile = new File(gamePath, csvhomestatslist);
                File awayStatsFile = new File(gamePath, csvawaystatslist);

                if (csvFile.exists())
                    csvFile.delete();
                if (homeStatsFile.exists())
                    homeStatsFile.delete();
                if (awayStatsFile.exists())
                    awayStatsFile.delete();

                for (int i=0; i<3; i++) {
                    File file = null;
                    ArrayList<String> tempList = new ArrayList<>();
                    ArrayList<Player> playerList = new ArrayList<>();
                    String labels = "Number, Pass Atmpts, Pass Comps, Pass Yds, Pass TDs, Run Atmpts, Run Yds, Run TDs, Receptions, Rec Yds, Rec TDs\n";
                    String temp = "";

                    switch (i) {
                        case 0:
                            file = csvFile;
                            tempList = csvList;
                            break;
                        case 1:
                            file = homeStatsFile;
                            playerList = homeTeam.getPlayers();
                            break;
                        case 2:
                            file = awayStatsFile;
                            playerList = awayTeam.getPlayers();
                            break;
                    }

                    FileOutputStream fileStream = null;
                    try {
                        fileStream = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    if (i==0) {
                        for (String output : tempList) {
                            try {
                                fileStream.write(output.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        try {
                            fileStream.write(labels.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        for (Player player : playerList) {
                            temp = String.valueOf(player.number) + ", " + String.valueOf(player.passatmpts) + ", " + String.valueOf(player.passcomps) + ", " + String.valueOf(player.passyds)
                                    + ", " + String.valueOf(player.passtds) + ", " + String.valueOf(player.runatmpts) + ", " + String.valueOf(player.runyds) + ", " + String.valueOf(player.runtds)
                                    + ", " + String.valueOf(player.catches) + ", " + String.valueOf(player.recyds) + ", " + String.valueOf(player.rectds) + "\n";
                            try {
                                fileStream.write(temp.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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

            case R.id.undoButton: {
                Toast t = Toast.makeText(getApplicationContext(), "Functionality will be added soon", Toast.LENGTH_SHORT);
                t.show();
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
                TextView gnLs = (TextView) dialog.findViewById(R.id.gnLsView);

                TextView playTypeEdit = (TextView) dialog.findViewById(R.id.playTypeEditText);
                TextView mainPlayerEdit = (TextView) dialog.findViewById(R.id.mainPlayerEditText);
                TextView receiverEdit = (TextView) dialog.findViewById(R.id.receiverEditText);
                TextView downEdit = (TextView) dialog.findViewById(R.id.downEditText);
                TextView distanceEdit = (TextView) dialog.findViewById(R.id.distanceEditText);
                TextView quarterEdit = (TextView) dialog.findViewById(R.id.qtrEditText);
                TextView gnLsEdit = (TextView) dialog.findViewById(R.id.gnLsEditText);

                result.setText(play.getResult());
                playTypeEdit.setText(play.getPlayType());
                mainPlayerEdit.setText(String.valueOf(play.getPlayerNumber()));
                receiverEdit.setText(String.valueOf(play.getRecNumber()));
                downEdit.setText(String.valueOf(play.getDownNum()));
                distanceEdit.setText(String.valueOf(play.getDist()));
                quarterEdit.setText(String.valueOf(play.getQtr()));
                gnLsEdit.setText(String.valueOf(play.getGnLs()));

                Button dialogCloseButton = (Button) dialog.findViewById(R.id.dialogButtonClose);
                Button dialogSaveButton = (Button) dialog.findViewById(R.id.dialogButtonSave);

                // if button is clicked, close the custom dialog
                dialogCloseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialogSaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast t = Toast.makeText(getApplicationContext(), "Functionality will be added soon", Toast.LENGTH_SHORT);
                        t.show();
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

                            currentPlay.setDist(prevDist);
                            currentPlay.setDownNum(prevDown);
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
                            currentPlay.touchdownFlag = touchdownFlag;
                            currentPlay.defNumber = defNumber;
                            currentPlay.fumbleFlag = fumbleFlag;
                            currentPlay.interceptionFlag = interceptionFlag;
                            currentPlay.touchbackFlag = touchbackFlag;
                            currentPlay.faircatchFlag = faircatchFlag;
                            currentPlay.returnYds = returnYds;

                            gamePlays.add(currentPlay);
                            addButton(result, playCounter);
                            playList.add(result);
                            csvList.add(text.get(0) + " , " + String.valueOf(playCounter) + "\n");
                            statsList.add(analyzedPlay);

                            updateStats();
                            awayScoreTextView.setText(String.valueOf(awayTeam.getTeamScore()));
                            homeScoreTextView.setText(String.valueOf(homeTeam.getTeamScore()));

                            switch (downNum) {
                                case 0:
                                    downAndDistTextView.setText("");
                                    break;
                                case 1:
                                    downAndDistTextView.setText("1st and " + dist);
                                    break;
                                case 2:
                                    downAndDistTextView.setText("2nd and " + dist);
                                    break;
                                case 3:
                                    downAndDistTextView.setText("3rd and " + dist);
                                    break;
                                case 4:
                                    downAndDistTextView.setText("4th and " + dist);
                                    break;
                            }
                        }
                        else
                            qtrTextView.setText(String.valueOf(qtr));

                    }
                    else {
                        if (playCounter > 0)
                            revertToLastPlay(gamePlays.get(playCounter - 1));
                        else {
                            completeReset();
                            resetValues();
                        }

                        Toast t = Toast.makeText(getApplicationContext(), "Play not recognized as a valid play. Please try again.", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
                break;
            }

        }
    }

    private void updateStats() {
        Player currentPlayer, recPlayer;
        Team tempTeam;

        tempTeam = getOffensiveTeam();

        if (playerNumber != 0) {
            currentPlayer = homeTeam.getPlayer(playerNumber);

            if (currentPlayer == null) {
                currentPlayer = new Player(true, playerNumber);
                homeTeam.addPlayer(currentPlayer);
            }

            if (recNumber != 0) {
                recPlayer = homeTeam.getPlayer(recNumber);

                if (recPlayer == null) {
                    recPlayer = new Player(true, recNumber);
                    homeTeam.addPlayer(recPlayer);
                }
            }

            switch (playType) {
                case "Pass":
                    homeTeam.getPlayer(playerNumber).updatePassStats(gnLs, interceptionFlag, incompleteFlag, touchdownFlag);
                    if (!interceptionFlag && !incompleteFlag)
                        homeTeam.getPlayer(recNumber).updateRecStats(gnLs, fumbleFlag, touchdownFlag);
                    break;
                case "Run":
                    homeTeam.getPlayer(playerNumber).updateRunStats(gnLs, fumbleFlag, touchdownFlag);
                    break;
                case "Field Goal" :
                    break;
                case "Kickoff":
                    homeTeam.getPlayer(playerNumber).updateKickRetStats(gnLs, fumbleFlag, touchdownFlag);
                    break;
                case "Punt" :
                    homeTeam.getPlayer(playerNumber).updatePuntRetStats(gnLs, fumbleFlag, touchdownFlag);
                    break;
                case "PAT" :
                    break;
                case "2 Pt. Conversion" :
                    break;
                case "Penalty" :
                    break;
            }
        }

        if (homeTeam.getOnOffense()) {
            homeTeam = tempTeam;
        }
        else {
            awayTeam = tempTeam;
        }
    }

    public Team getOffensiveTeam() {
        if (homeTeam.getOnOffense())
            return homeTeam;
            //else
            return awayTeam;
    }

    private void resetValues() {
        playerNumber = 0;
        recNumber = 0;
        defNumber = 0;
        gnLs=0;
        returnFlag=0;
        incompleteFlag=false;
        interceptionFlag=false;
        fumbleFlag=false;
        fgMadeFlag=0;
        touchdownFlag=false;
        invalidPlay = false;
        playType = "";
        touchbackFlag=false;
        faircatchFlag=false;
        returnYds=0;
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
        defNumber = lastPlay.defNumber;
        playType = lastPlay.getPlayType();
        qtr = lastPlay.getQtr();
        recNumber = lastPlay.getRecNumber();
        returnFlag = lastPlay.getReturnFlag();
        result = lastPlay.getResult();
        returnYds = lastPlay.returnYds;
    }

    private void completeReset() {
        dist = 0;
        downNum = 1;
        fgDistance = 0;
        fgMadeFlag = 0;
        fieldPos = 0;
        ydLn= 0;
        gnLs = 0;
        incompleteFlag = false;
        interceptionFlag=false;
        touchdownFlag=false;
        fumbleFlag=false;
        recFlag=false;
        playerNumber = 0;
        playType = "";
        qtr = 1;
        recNumber = 0;
        returnFlag = 0;
        result = "";
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
            case "three":
                number = 3;
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
        playBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        ((RelativeLayout) findViewById(R.id.scrollRelLayout)).addView(playBtn, rl);
        playBtn.setOnClickListener(this);

    }

    private String analyzePlay(String spokenText) {
        String temp = spokenText.toLowerCase();
        Log.d(logtag, temp);

        prevDown = downNum;
        prevDist = dist;

        String[] words = temp.split("\\s+");
        recFlag = false; //flag that marks if there was a reception
        lossFlag = 0; //flag that marks if there was a loss on the play
        returnFlag = 0; //flag that marks if there is a return on the play
        oppTerFlag = 0; //flag to mark the field position in opponent's territory
        recNumber =0;
        gnLs = 0;

        for(int m=0; m<words.length; m++){
            curWord = words[m];
            if (m != (words.length - 1))
                nextWord = words[m+1];


            if (Objects.equals(prevWord, "and")) {
                if(curWord.substring(0, 1).equals("4")){
                    playType = "Run";
                    curWord = curWord.substring(1);
                }
            }
            if (Objects.equals(curWord, "defense")) {
                if (Objects.equals(playType, "Penalty"))
                    defensivePenalty=true;
            }

            if (Objects.equals(curWord, "penalty") || Objects.equals(curWord, "penalties")) {
                playType = "Penalty";
                playCounter--;
                continue;
            }

            if (Objects.equals(curWord, "passed") || Objects.equals(curWord, "past") || Objects.equals(curWord, "pass") || Objects.equals(curWord, "pastor")) {
                playType = "Pass";
                prevWord = curWord;
                recFlag = true;
                continue;
            }

            if (curWord.equals("touchback") || (curWord.equals("back") && prevWord.equals("touch"))) {
                fieldPos = 20;
                returnFlag = 0;
                touchbackFlag=true;
                continue;
            }

            if (curWord.equals("ferrets") || (curWord.equals("catch") && prevWord.equals("fair"))) {
                returnFlag = 0;
                faircatchFlag=true;
                continue;
            }

            if (Objects.equals(curWord, "interception") || Objects.equals(curWord, "intercepts") || Objects.equals(curWord, "intercepted") || Objects.equals(curWord, "intercept")
                    || curWord.equals("picked") || curWord.equals("pick") || curWord.equals("picks") || curWord.equals("pic")) {
                playType = "Pass";
                interceptionFlag = true;
                recFlag = false;
                continue;
            }

            if (curWord.equals("fumble") || curWord.equals("fumbles") || curWord.equals("fumbled")) {
                fumbleFlag=true;
                continue;
            }

            if (Objects.equals(curWord, "incomplete") || Objects.equals(curWord, "incompletes") || Objects.equals(curWord, "incompleted") || Objects.equals(curWord, "incompletion")
                    || (Objects.equals(curWord, "completion") && Objects.equals(prevWord, "in"))) {
                prevWord = curWord;
                incompleteFlag=true;
                gnLs = 0;
                continue;
            }

            if (Objects.equals(curWord, "ran") || Objects.equals(curWord, "ranch") || Objects.equals(curWord, "run") || Objects.equals(curWord, "ram") || Objects.equals(curWord, "grand")
                    || Objects.equals(curWord, "rand") || Objects.equals(curWord, "rent") || Objects.equals(curWord, "ranger") || curWord.equals("read")
                    || curWord.equals("rush") || curWord.equals("rushes") || curWord.equals("rushed")) {
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
                if (returnFlag==1) {
                    returnYds = intParse(prevWord);
                }
                else {
                    gnLs = intParse(prevWord);
                    if (lossFlag == 1) {
                        gnLs = gnLs * -1;
                    }
                }
            }

            if (Objects.equals(prevWord, "number") || Objects.equals(prevWord, "numbers") || Objects.equals(prevWord, "player") || Objects.equals(prevWord, "players")
                    || Objects.equals(prevWord, "never")) {
                if (Objects.equals(curWord, "number")) {
                    twowordsago = prevWord;
                    prevWord = curWord;
                    continue;
                }
                if (recFlag) {
                    recNumber = intParse(curWord);
                }
                else {
                    int len = curWord.length();
                    if (len > 2) {
                        if (curWord.substring(1, 3).equals("14")) {
                            playType = "Run";
                            if (len == 4)
                                gnLs = intParse(curWord.substring(3));
                            curWord = curWord.substring(0, 1);
                        }
                    }
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
                touchdownFlag = true;
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

            if (Objects.equals(curWord, "punt") || Objects.equals(curWord, "punts") || Objects.equals(curWord, "punted") || Objects.equals(curWord, "punch")) {

                playType = "Punt";
                twowordsago = prevWord;
                prevWord = curWord;
                continue;
            }

            if (Objects.equals(curWord, "kick") || Objects.equals(curWord, "kicks") || Objects.equals(curWord, "kicked") || Objects.equals(curWord, "kickoff")
                    || Objects.equals(curWord, "cake") || Objects.equals(curWord, "kickoff") || Objects.equals(curWord, "kids") || curWord.equals("king")) {

                playType = "Kickoff";
                twowordsago = prevWord;
                prevWord = curWord;
                continue;
            }

            if (Objects.equals(curWord, "fieldgoal") || (Objects.equals(curWord, "field") && Objects.equals(nextWord, "goal"))) {
                playType = "Field Goal";
                fgDistance = (100 - fieldPos) + 17;
            }

            if (Objects.equals(curWord, "conversion") || ((Objects.equals(prevWord, "point"))
                    && ((Objects.equals(twowordsago, "two")) || (Objects.equals(twowordsago, "2"))))) {
                playType = "2 Pt. Conversion";
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
                if (playType.equals("Penalty")) {
                    downNum = 1;
                    dist = 10;
                }
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

    private void changePossession() {
        if (awayTeam.getOnOffense()) {
            awayPossImageView.setVisibility(ImageView.INVISIBLE);
            homePossImageView.setVisibility(ImageView.VISIBLE);
            homeTeam.setOnOffense(true);
            awayTeam.setOnOffense(false);
        }
        else {
            awayPossImageView.setVisibility(ImageView.VISIBLE);
            homePossImageView.setVisibility(ImageView.INVISIBLE);
            homeTeam.setOnOffense(false);
            awayTeam.setOnOffense(true);
        }
    }

    private String getResult() {
        String playResult = "";

        switch (playType) {
            case "Pass":
                if (!incompleteFlag && !interceptionFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " pass completed to number " + String.valueOf(recNumber)
                            + " for " + String.valueOf(gnLs) + " yards";
                }
                else if (interceptionFlag) {
                    playResult =  "Number " + String.valueOf(playerNumber) + " pass intercepted by number " + String.valueOf(recNumber);
                    changePossession();
                }
                else {
                    if (recNumber == 0)
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete";
                    else
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete to number " + String.valueOf(recNumber);
                }
                break;
            case "Run":
                playResult = "Number " + String.valueOf(playerNumber) + " ran for " + String.valueOf(gnLs) + " yards";
                break;
            case "Kickoff":
                downNum=0;
                if ((returnFlag == 1) && (!touchdownFlag)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff " + String.valueOf(returnYds) + " yards to the " +
                            String.valueOf(ydLn) + " yardline";
                    downNum = 1;
                    dist = 10;
                }
                else if ((returnFlag == 1) && (touchdownFlag)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff " + String.valueOf(returnYds) + " yards";
                }
                else {
                    //i guess there was a touchback or he kicked it out of bounds
                    downNum = 1;
                    dist = 10;
                    if (touchbackFlag) {
                        playResult = "Kickoff goes for a touchback";
                    }
                    else if (faircatchFlag) {
                        playResult = "Kickoff caught for a fair catch at the " + String.valueOf(ydLn) + " yardline.";
                    }
                }
                changePossession();
                break;
            case "Punt":
                if ((returnFlag == 1) && (!touchdownFlag)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the punt " + String.valueOf(returnYds) +
                            " yards to the " + String.valueOf(ydLn) + " yardline";
                    downNum = 1;
                    dist = 10;
                }
                else if ((returnFlag == 1) && (touchdownFlag)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the punt " + String.valueOf(returnYds) + " yards";
                    downNum = 0;
                    dist = 0;
                }
                else {
                    //fair catch or touchback or some bullshit
                    if (touchbackFlag) {
                        playResult = "Punt goes for a touchback";
                    }
                    else if (faircatchFlag) {
                        playResult = "Punt caught for a fair catch at the " + String.valueOf(ydLn) + " yardline.";
                    }
                    downNum = 1;
                    dist = 10;
                }
                changePossession();
                break;
            case "Field Goal":
                if (fgMadeFlag == 1) {
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was good";
                    if (homeTeam.getOnOffense()) {
                        homeTeam.setTeamScore(homeTeam.getTeamScore() + 1);
                    }
                    else {
                        awayTeam.setTeamScore(awayTeam.getTeamScore() + 1);
                    }
                    changePossession();
                    downNum = 0;
                    dist = 0;
                }
                else {
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was no good";
                    changePossession();
                    downNum = 1;
                    dist = 10;
                }
                break;
            case "PAT":
                if (fgMadeFlag == 1) {
                    playResult = "The PAT was good";
                    if (homeTeam.getOnOffense()) {
                        homeTeam.setTeamScore(homeTeam.getTeamScore() + 1);
                    }
                    else {
                        awayTeam.setTeamScore(awayTeam.getTeamScore() + 1);
                    }
                }
                else
                    playResult = "The PAT was no good";
                downNum = 0;
                dist = 0;
                break;
            case "2 Pt. Conversion":
                playResult = "2 Pt. Conversion - fix this";
                if (homeTeam.getOnOffense()) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 1);
                }
                else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 1);
                }
                downNum = 0;
                dist = 0;
                break;
            case "Penalty":
                playResult = String.valueOf(gnLs) + " yard penalty";
                if (!defensivePenalty) {
                    gnLs *= -1;
                    playResult = playResult.concat(" on the offense");
                }
                else
                    playResult = playResult.concat(" on the defense");
                dist -= gnLs;
                break;
            case "End":
                switch (qtr) {
                    case 1:
                        playResult = "End of 1st Quarter";
                        break;
                    case 2:
                        playResult = "End of 2nd Quarter";
                        downNum = 1;
                        dist = 10;
                        //ask at the beginning who got the ball first
                        //and here, set the possession to opposite team.
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
        if (playType.equals("Pass") || playType.equals("Run")) {
            downNum++;
            dist -= gnLs;
        }

        if (dist <= 0) {
            downNum = 1;
            dist = 10;
        }
        if (downNum > 4) {
            changePossession();
            downNum = 1;
            dist = 10;
        }

        if (fumbleFlag) {
            playResult = playResult.concat(", fumble recovered by number " + String.valueOf(defNumber));
            changePossession();
        }

        if (returnFlag==1 && !(playType.equals("Kickoff") || playType.equals("Punt"))) {
            playResult = playResult.concat(", returned for " + String.valueOf(returnYds) + " yards");
            if (!touchdownFlag) {
                playResult = playResult.concat(" to the " + String.valueOf(ydLn) + " yardline");
            }
        }

        if (touchdownFlag) {
            playResult = playResult.concat(" for a TOUCHDOWN!");
            downNum = 0;
            dist = 3;
        }

        return playResult;
    }

    /**
     * PLAY CLASS - NESTED INSIDE GAME
     */
    public class Play {

        Integer playerNumber, recNumber, defNumber, ydLn, gnLs,  fieldPos, downNum, dist, qtr, fgDistance, playCount, returnYds;
        Integer lossFlag, returnFlag, fgMadeFlag, oppTerFlag;
        boolean incompleteFlag, touchdownFlag, recFlag, touchbackFlag, faircatchFlag, interceptionFlag, fumbleFlag;
        String playType, result = "";

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

        public boolean getIncompleteFlag() {
            return incompleteFlag;
        }

        public void setIncompleteFlag(boolean incompleteFlag) {
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

        public Player getPlayer(Integer number) {
            Player currentPlayer;
            for (int i=0; i<players.size(); i++) {

                currentPlayer = players.get(i);

                if (currentPlayer.number == number)
                    return currentPlayer;
            }
            return null;
        }

        public void addPlayer(Player player) {
            Player currentPlayer;
            for (int i=0; i<players.size(); i++) {

                currentPlayer = this.players.get(i);

                if (currentPlayer.number == player.number)
                   players.add(i, player);
            }
            players.add(player);
        }

    }

    /**
     * PLAYER CLASS - NESTED INSIDE GAME
     */
    public static class Player {

        private boolean offensive;
        private Integer number, passcomps, passatmpts, passyds, runatmpts, runyds, ints, fumbles, catches, recyds,
        passtds, runtds, rectds, puntrettds, kickrettds, puntreturns, kickreturns;

        public Player(boolean offensive, Integer number) {
            this.offensive = offensive;
            this.number = number;
            passcomps=0; passatmpts=0; passyds=0; runatmpts=0; runyds=0; ints=0;
            fumbles=0; catches=0; recyds=0; passtds=0; runtds=0; rectds=0; puntrettds=0;
            kickrettds=0; puntreturns=0; kickreturns=0;
        }

        private void updatePassStats(int yds, boolean pic, boolean incompletion, boolean td) {
            passyds +=  yds;
            if (pic)
                ints++;
            if(!incompletion)
                passcomps++;
            if (td)
                passtds++;
            passatmpts++;
        }

        private void updateRunStats(int yds, boolean fumb, boolean td) {
            runyds +=  yds;
            if (fumb)
                fumbles++;
            if (td)
                runtds++;
            runatmpts++;
        }

        private void updateRecStats(int yds, boolean fumb, boolean td) {
            recyds +=  yds;
            if (fumb)
                fumbles++;
            if (td)
                rectds++;
            catches++;
        }
        private void updatePuntRetStats(int yds, boolean fumb, boolean td) {
            puntrettds +=  yds;
            if (fumb)
                fumbles++;
            if (td)
                puntrettds++;
            puntreturns++;
        }
        private void updateKickRetStats(int yds, boolean fumb, boolean td) {
            kickrettds +=  yds;
            if (fumb)
                fumbles++;
            if (td)
                kickrettds++;
            kickreturns++;
        }

    }
}
