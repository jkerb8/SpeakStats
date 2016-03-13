package com.kerbysoft.qwikcut.speakstats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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

public class Game extends AppCompatActivity implements View.OnClickListener {

    protected static final int RESULT_SPEECH = 1;

    //private ImageButton btnSpeak;
    //private Button exportButton;
    //private TextView txtText;
    TextView qtrTextView;
    TextView homeScoreTextView;
    TextView awayScoreTextView;
    TextView downAndDistTextView;
    TextView homeTeamNameView;
    TextView awayTeamNameView;
    TextView ydLnTextView;
    ImageView awayPossImageView;
    ImageView homePossImageView;
    ArrayList<Button> buttonArrayList = new ArrayList<>();
    public ArrayList<String> playList;
    public ArrayList<String> csvList;
    public ArrayList<String> statsList;

    String csvplaylist = "play_list.csv";
    String csvOffhomestatslist = "home_offensive_stats_list.csv";
    String csvOffawaystatslist = "away_offensive_stats_list.csv";
    String csvDefhomestatslist = "home_defensive_stats_list.csv";
    String csvDeffawaystatslist = "away_defensive_stats_list.csv";
    FileOutputStream outputStream;
    String hometeamname = "", awayteamname = "", gameName = "", division = "";
    Integer fieldSize, playerNumber = 0, recNumber = 0, defNumber = 0, tacklerNumber = 0, ydLn = 0, gnLs = 0, fieldPos = 0, playCounter = 0,
            downNum = 0, dist = 0, qtr = 1, fgDistance = 0, prevDown = 0, prevDist = 0, returnYds = 0;
    Integer returnFlag,oppTerFlag;
    boolean interceptionFlag = false, fumbleFlag = false, incompleteFlag = false, touchdownFlag = false, defensivePenalty = false,
            recFlag = false, touchbackFlag = false, faircatchFlag = false, fumbleRecFlag=false, tackleflag=false, sackflag=false,
            lossFlag = false, fgMadeFlag = false, safetyFlag = false;
    String prevWord = "", playType = "", twowordsago = "", curWord, nextWord = "", result = "";
    boolean invalidPlay = false;
    static final String logtag = "MyLogTag";
    public Team awayTeam = new Team();
    public Team homeTeam = new Team();
    public ArrayList<Play> gamePlays = new ArrayList<Play>();

    //this is set to false if the awayTeam has the ball, and true if the homeTeam has the ball
    boolean possFlag = false;

    final float scale = this.getResources().getDisplayMetrics().density;
    int buttonSize = (int) (45 * scale + 0.5f);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Button btnSpeak, exportButton, undoButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_game);

        Intent intent = getIntent(); // gets the previously created intent

        //pull in the the team names here and make new team instances and the game name
        hometeamname = intent.getStringExtra("homeName");
        awayteamname = intent.getStringExtra("awayName");
        fieldSize = intParse(intent.getStringExtra("fieldSize"));
        division = intent.getStringExtra("division");


        gameName = division + "_" + hometeamname + "_vs_" + awayteamname;

        Log.d(logtag, hometeamname + ", " + awayteamname + ", " + gameName);

        homeTeam.setTeamName(hometeamname);
        awayTeam.setTeamName(awayteamname);

        playList = new ArrayList<String>();
        csvList = new ArrayList<String>();
        statsList = new ArrayList<String>();

        awayTeamNameView = (TextView) findViewById(R.id.awayTeamNameText);
        homeTeamNameView = (TextView) findViewById(R.id.homeTeamNameText);
        awayTeamNameView.setText(awayteamname);
        homeTeamNameView.setText(hometeamname);

        awayScoreTextView = (TextView) findViewById(R.id.awayScoreNumberText);
        homeScoreTextView = (TextView) findViewById(R.id.homeScoreNumberText);
        qtrTextView = (TextView) findViewById(R.id.qtrNumberText);
        ydLnTextView = (TextView) findViewById(R.id.ydLnText);
        downAndDistTextView = (TextView) findViewById(R.id.downAndDistText);

        awayPossImageView = (ImageView) findViewById(R.id.awayPossImageView);
        homePossImageView = (ImageView) findViewById(R.id.homePossImageView);

        //if away team starts with ball
        homePossImageView.setVisibility(ImageView.INVISIBLE);

        exportButton = (Button) findViewById(R.id.exportButton);
        undoButton = (Button) findViewById(R.id.undoButton);

        //txtText.setText("This is where this text will be.");

        btnSpeak = (Button) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(this);
        exportButton.setOnClickListener(this);
        undoButton.setOnClickListener(this);

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;

        File projDir = new File(dirPath);
        if (!projDir.exists())
            projDir.mkdirs();
    }

    public void onClick(View v) {
        switch (v.getId()) {
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
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder
                        .setMessage("Are you sure you want to export the game stats?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(getApplicationContext(), "Stats Exported", Toast.LENGTH_SHORT).show();
                                export();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                break;
            }

            case R.id.undoButton: {

                if (playCounter > 0) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder
                            .setMessage("Are you sure you want to undo the last play?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Toast.makeText(getApplicationContext(), "Play Deleted", Toast.LENGTH_SHORT).show();

                                    if (playCounter > 1) {
                                        revertToLastPlay(gamePlays.get(playCounter - 1));
                                    } else {
                                        completeReset();
                                        resetValues();
                                    }
                                    undoStats();
                                    removeButton(playCounter);
                                    gamePlays.remove(--playCounter);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "No plays to undo...", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            default: {
                Integer id = v.getId();
                Play play = gamePlays.get(id);

                Log.d(logtag, String.valueOf(id) + ", " + play.result);

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

                result.setText(play.result);
                playTypeEdit.setText(play.playType);
                mainPlayerEdit.setText(String.valueOf(play.playerNumber));
                receiverEdit.setText(String.valueOf(play.recNumber));
                downEdit.setText(String.valueOf(play.downNum));
                distanceEdit.setText(String.valueOf(play.dist));
                quarterEdit.setText(String.valueOf(play.qtr));
                gnLsEdit.setText(String.valueOf(play.gnLs));

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
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Activity")
                .setMessage("Unsaved data will be lost. Are you sure you want to exit the Game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
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

                            currentPlay.dist = prevDist;
                            currentPlay.downNum = prevDown;
                            currentPlay.fgDistance = fgDistance;
                            currentPlay.fgMadeFlag = fgMadeFlag;
                            currentPlay.fieldPos = fieldPos;
                            currentPlay.ydLn = ydLn;
                            currentPlay.gnLs = gnLs;
                            currentPlay.incompleteFlag = incompleteFlag;
                            currentPlay.playCount = playCounter;
                            currentPlay.playerNumber = playerNumber;
                            currentPlay.playType = playType;
                            currentPlay.qtr = qtr;
                            currentPlay.recNumber = recNumber;
                            currentPlay.returnFlag = returnFlag;
                            currentPlay.result = result;
                            currentPlay.touchdownFlag = touchdownFlag;
                            currentPlay.defNumber = defNumber;
                            currentPlay.fumbleFlag = fumbleFlag;
                            currentPlay.interceptionFlag = interceptionFlag;
                            currentPlay.touchbackFlag = touchbackFlag;
                            currentPlay.faircatchFlag = faircatchFlag;
                            currentPlay.returnYds = returnYds;
                            currentPlay.fumbleRecFlag = fumbleRecFlag;
                            currentPlay.tackleflag = tackleflag;
                            currentPlay.sackflag = sackflag;
                            currentPlay.tacklerNumber = tacklerNumber;
                            currentPlay.possFlag = possFlag;
                            currentPlay.safetyFlag = safetyFlag;
                            currentPlay.defensivePenalty = defensivePenalty;

                            gamePlays.add(currentPlay);
                            addButton(result, playCounter);
                            playList.add(result);
                            csvList.add(text.get(0) + " , " + String.valueOf(playCounter) + "\n");
                            statsList.add(analyzedPlay);

                            updateStats();
                            updateVisuals();
                        } else
                            qtrTextView.setText(String.valueOf(qtr));

                    } else {
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
        Player currentPlayer, recPlayer, defPlayer, tacklerPlayer;
        Team tempOffTeam, tempDefTeam;

        tempOffTeam = getOffensiveTeam();
        tempDefTeam = getDefensiveTeam();

        //creating the players if they do not previously exist
        if (playerNumber != 0) {
            currentPlayer = tempOffTeam.getPlayer(playerNumber);

            if (currentPlayer == null) {
                currentPlayer = new Player(true, playerNumber);
                tempOffTeam.addPlayer(currentPlayer);
            }

            if (recNumber != 0) {
                recPlayer = tempOffTeam.getPlayer(recNumber);

                if (recPlayer == null) {
                    recPlayer = new Player(true, recNumber);
                    tempOffTeam.addPlayer(recPlayer);
                }
            }

            if (tacklerNumber != 0) {
                tacklerPlayer = tempDefTeam.getPlayer(tacklerNumber);

                if (tacklerPlayer == null) {
                    tacklerPlayer = new Player(false, tacklerNumber);
                    tempDefTeam.addPlayer(tacklerPlayer);
                }
            }

            if (defNumber != 0) {
                defPlayer = tempDefTeam.getPlayer(defNumber);

                if (defPlayer == null) {
                    defPlayer = new Player(false, defNumber);
                    tempDefTeam.addPlayer(defPlayer);
                }
            }

            switch (playType) {
                case "Pass":
                    tempOffTeam.getPlayer(playerNumber).updatePassStats(gnLs, interceptionFlag, incompleteFlag, touchdownFlag, fumbleFlag);
                    if (tackleflag)
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                    if (!interceptionFlag && !incompleteFlag)
                        tempOffTeam.getPlayer(recNumber).updateRecStats(gnLs, fumbleFlag, touchdownFlag);
                    if (interceptionFlag || fumbleRecFlag)
                        tempDefTeam.getPlayer(defNumber).updateDefStats(interceptionFlag, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Run":
                    tempOffTeam.getPlayer(playerNumber).updateRunStats(gnLs, fumbleFlag, touchdownFlag);
                    if (tackleflag)
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                    if (fumbleRecFlag)
                        tempDefTeam.getPlayer(defNumber).updateDefStats(false, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Field Goal":
                    break;
                case "Kickoff":
                    tempOffTeam.getPlayer(playerNumber).updateKickRetStats(returnYds, fumbleFlag, touchdownFlag);
                    if (tackleflag)
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                    if (fumbleRecFlag)
                        tempDefTeam.getPlayer(defNumber).updateDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Punt":
                    tempOffTeam.getPlayer(playerNumber).updatePuntRetStats(returnYds, fumbleFlag, touchdownFlag);
                    if (tackleflag)
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                    if (fumbleRecFlag)
                        tempDefTeam.getPlayer(defNumber).updateDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "PAT":
                    break;
                case "2 Pt. Conversion":
                    break;
                case "Penalty":
                    break;
            }

        }

        if (homeTeam.getOnOffense()) {
            homeTeam = tempOffTeam;
            awayTeam = tempDefTeam;
        } else {
            awayTeam = tempOffTeam;
            homeTeam = tempDefTeam;
        }
    }

    private void undoStats() {
        Team tempOffTeam, tempDefTeam;

        tempOffTeam = getOffensiveTeam();
        tempDefTeam = getDefensiveTeam();

        switch (playType) {
            case "Pass":
                tempOffTeam.getPlayer(playerNumber).undoPassStats(gnLs, interceptionFlag, incompleteFlag, touchdownFlag, fumbleFlag);
                if (tackleflag)
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                if (!interceptionFlag && !incompleteFlag)
                    tempOffTeam.getPlayer(recNumber).undoRecStats(gnLs, fumbleFlag, touchdownFlag);
                if (interceptionFlag || fumbleRecFlag)
                    tempDefTeam.getPlayer(defNumber).undoDefStats(interceptionFlag, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Run":
                tempOffTeam.getPlayer(playerNumber).undoRunStats(gnLs, fumbleFlag, touchdownFlag);
                if (tackleflag)
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                if (fumbleRecFlag)
                    tempDefTeam.getPlayer(defNumber).undoDefStats(false, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Field Goal":
                break;
            case "Kickoff":
                tempOffTeam.getPlayer(playerNumber).undoKickRetStats(returnYds, fumbleFlag, touchdownFlag);
                if (tackleflag)
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                if (fumbleRecFlag)
                    tempDefTeam.getPlayer(defNumber).undoDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Punt":
                tempOffTeam.getPlayer(playerNumber).undoPuntRetStats(returnYds, fumbleFlag, touchdownFlag);
                if (tackleflag)
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                if (fumbleRecFlag)
                    tempDefTeam.getPlayer(defNumber).undoDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "PAT":
                break;
            case "2 Pt. Conversion":
                break;
            case "Penalty":
                break;
        }

        if (homeTeam.getOnOffense()) {
            homeTeam = tempOffTeam;
            awayTeam = tempDefTeam;
        } else {
            awayTeam = tempOffTeam;
            homeTeam = tempDefTeam;
        }
    }


    public Team getOffensiveTeam() {
        if (homeTeam.getOnOffense())
            return homeTeam;
            //else
            return awayTeam;
    }

    public Team getDefensiveTeam() {
        if (homeTeam.getOnOffense())
            return awayTeam;
        //else
        return homeTeam;
    }

    public void updateVisuals() {
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
        ydLnTextView.setText("YdLn: " + ydLn);
        awayScoreTextView.setText(String.valueOf(awayTeam.getTeamScore()));
        homeScoreTextView.setText(String.valueOf(homeTeam.getTeamScore()));
        qtrTextView.setText(String.valueOf(qtr));
    }

    private void resetValues() {
        playerNumber = 0;
        recNumber = 0;
        defNumber = 0;
        tacklerNumber = 0;
        gnLs=0;
        returnFlag=0;
        incompleteFlag=false;
        interceptionFlag=false;
        fumbleFlag=false;
        fgMadeFlag=false;
        touchdownFlag=false;
        invalidPlay = false;
        playType = "";
        touchbackFlag=false;
        faircatchFlag=false;
        returnYds=0;
        fumbleRecFlag=false;
        tackleflag=false;
        sackflag=false;
        safetyFlag = false;
        defensivePenalty = false;
    }

    private void revertToLastPlay(Play lastPlay) {
        dist = lastPlay.dist;
        downNum = lastPlay.downNum;
        fgDistance = lastPlay.fgDistance;
        fieldPos = lastPlay.fieldPos;
        ydLn= lastPlay.ydLn;
        gnLs = lastPlay.gnLs;
        incompleteFlag = lastPlay.incompleteFlag;
        playerNumber = lastPlay.playerNumber;
        defNumber = lastPlay.defNumber;
        qtr = lastPlay.qtr;
        recNumber = lastPlay.recNumber;
        returnFlag = lastPlay.returnFlag;
        result = lastPlay.result;
        returnYds = lastPlay.returnYds;
        fumbleRecFlag = lastPlay.fumbleRecFlag;
        tackleflag = lastPlay.tackleflag;
        sackflag = lastPlay.sackflag;
        tacklerNumber = lastPlay.tacklerNumber;
        defensivePenalty = lastPlay.defensivePenalty;

        if (possFlag != lastPlay.possFlag) {
            changePossession();
            possFlag = lastPlay.possFlag;
        }

        Team offTeam, defTeam;
        int scoreDecrease = 0;

        if (possFlag) {
            offTeam = homeTeam;
            defTeam = awayTeam;
        }
        else {
            offTeam = awayTeam;
            defTeam = homeTeam;
        }

        if (touchdownFlag) {
            scoreDecrease = 6;
        }
        else if (safetyFlag) {
            if (defTeam.getTeamScore() >= 2)
                defTeam.setTeamScore(getDefensiveTeam().getTeamScore() - 2);
        }
        else if (fgMadeFlag) {
            switch (playType) {
                case "2 Pt. Conversion":
                    scoreDecrease = 2;
                    break;
                case "Field Goal":
                    scoreDecrease = 3;
                    break;
                case "PAT":
                    scoreDecrease = 1;
                    break;
            }
        }

        if ((offTeam.getTeamScore() >= scoreDecrease) && !safetyFlag)
            offTeam.setTeamScore(getOffensiveTeam().getTeamScore() - scoreDecrease);

        if (possFlag) {
            homeTeam = offTeam;
            awayTeam = defTeam;
        }
        else {
            awayTeam = offTeam;
            homeTeam = defTeam;
        }

        touchdownFlag = lastPlay.touchdownFlag;
        safetyFlag = lastPlay.safetyFlag;
        playType = lastPlay.playType;
        fgMadeFlag = lastPlay.fgMadeFlag;

        updateVisuals();
    }

    private void completeReset() {
        dist = 0;
        downNum = 1;
        fgDistance = 0;
        fgMadeFlag = false;
        fieldPos = 0;
        ydLn= 0;
        gnLs = 0;
        incompleteFlag = false;
        interceptionFlag=false;
        touchdownFlag=false;
        fumbleFlag=false;
        recFlag=false;
        possFlag=false;
        playerNumber = 0;
        playType = "";
        qtr = 1;
        recNumber = 0;
        returnFlag = 0;
        result = "";
        fumbleRecFlag=false;
        tackleflag=false;
        sackflag=false;
        tacklerNumber = 0;
        lossFlag=false;
        defensivePenalty = false;
        ydLnTextView.setText("YdLn: " + 0);
        awayScoreTextView.setText(String.valueOf(0));
        homeScoreTextView.setText(String.valueOf(0));
        qtrTextView.setText(String.valueOf(0));
        downAndDistTextView.setText("");
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
                if (word.length() > 2) {
                    if (word.substring(1, 3).equals("14")) {
                        playType = "Run";
                        if (word.length() == 4)
                            gnLs = intParse(word.substring(3));
                        number = intParse(word.substring(0, 1));
                    }
                }

                else {
                    try {
                        number = Integer.parseInt(word);
                    }
                    catch (Exception e) {
                        invalidPlay = true;
                    }
                }
        }

        return number;
    }

    private void export() {
        String gamePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;
        File csvFile = new File(gamePath, csvplaylist);
        File homeOffStatsFile = new File(gamePath, csvOffhomestatslist);
        File awayOffStatsFile = new File(gamePath, csvOffawaystatslist);
        File homeDefStatsFile = new File(gamePath, csvDefhomestatslist);
        File awayDefStatsFile = new File(gamePath, csvDeffawaystatslist);

        if (csvFile.exists())
            csvFile.delete();
        if (homeOffStatsFile.exists())
            homeOffStatsFile.delete();
        if (awayOffStatsFile.exists())
            awayOffStatsFile.delete();
        if (homeDefStatsFile.exists())
            homeDefStatsFile.delete();
        if (awayDefStatsFile.exists())
            awayDefStatsFile.delete();

        for (int i = 0; i < 5; i++) {
            File file = null;
            ArrayList<String> tempList = new ArrayList<>();
            ArrayList<Player> playerList = new ArrayList<>();
            String offLabels = "Number, Pass Atmpts, Pass Comps, Pass Yds, Pass TDs, INTs, Run Atmpts, Run Yds, Run TDs, Receptions, Rec Yds, Rec TDs\n";
            String defLabels = "Number, Tackles, TFL, Sacks, Forced Fumbles, Fumble Recs, Interceptions, Def TDs\n";
            String temp = "";

            switch (i) {
                case 0:
                    file = csvFile;
                    tempList = csvList;
                    break;
                case 1:
                    file = homeOffStatsFile;
                    playerList = homeTeam.getPlayers();
                    break;
                case 2:
                    file = awayOffStatsFile;
                    playerList = awayTeam.getPlayers();
                    break;
                case 3:
                    file = homeDefStatsFile;
                    playerList = homeTeam.getPlayers();
                    break;
                case 4:
                    file = awayDefStatsFile;
                    playerList = awayTeam.getPlayers();
                    break;
            }

            FileOutputStream fileStream = null;
            try {
                fileStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (i == 0) {
                for (String output : tempList) {
                    try {
                        fileStream.write(output.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (i==1 || i==2){
                try {
                    fileStream.write(offLabels.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (Player player : playerList) {
                    if (player.offensive) {
                        temp = String.valueOf(player.number) + ", " + String.valueOf(player.passatmpts) + ", " + String.valueOf(player.passcomps) + ", " + String.valueOf(player.passyds)
                                + ", " + String.valueOf(player.passtds) + ", " + String.valueOf(player.ints) + ", " + String.valueOf(player.runatmpts) + ", " + String.valueOf(player.runyds) + ", " + String.valueOf(player.runtds)
                                + ", " + String.valueOf(player.catches) + ", " + String.valueOf(player.recyds) + ", " + String.valueOf(player.rectds) + "\n";
                        try {
                            fileStream.write(temp.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else {
                try {
                    fileStream.write(defLabels.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (Player player : playerList) {
                    if (!player.offensive) {
                        temp = String.valueOf(player.number) + ", " + String.valueOf(player.tackles) + ", " + String.valueOf(player.tfls) + ", " + String.valueOf(player.sacks)
                                + ", " + String.valueOf(player.forcedfums) + ", " + String.valueOf(player.fumblerecs) + ", " + String.valueOf(player.pics) + ", " + String.valueOf(player.deftds) + "\n";
                        try {
                            fileStream.write(temp.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            try {
                fileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addButton(String play, Integer playNum) {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.scrollRelLayout);
        int prevId = (playNum - 1);

        layout.getLayoutParams().height += buttonSize;


        //set the properties for button
        Button playBtn = new Button(this);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //int resID = getResources().getIdentifier(String.valueOf(prevId), "id", getPackageName());
        if (playNum != 1) {
            rl.addRule(RelativeLayout.ABOVE, prevId);
        }

        playBtn.setLayoutParams(rl);
        playBtn.setText("Play " + String.valueOf(playNum) + " -  " + play);
        playBtn.setId(playNum);
        playBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        ((RelativeLayout) findViewById(R.id.scrollRelLayout)).addView(playBtn, rl);
        playBtn.setOnClickListener(this);
        playBtn.setHeight(buttonSize);
        buttonArrayList.add(playBtn);
    }

    private void removeButton(Integer playNum) {

        int id = playNum - 1;
        Button btn = buttonArrayList.get(id);
        btn.setVisibility(View.GONE);
        buttonArrayList.remove(id);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.scrollRelLayout);
        layout.getLayoutParams().height -= buttonSize;
    }

    private String analyzePlay(String spokenText) {
        String temp = spokenText.toLowerCase();
        Log.d(logtag, temp);

        prevDown = downNum;
        prevDist = dist;

        String[] words = temp.split("\\s+");
        recFlag = false; //flag that marks if there was a reception
        lossFlag = false; //flag that marks if there was a loss on the play
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
                continue;
            }

            if (Objects.equals(curWord, "passed") || Objects.equals(curWord, "past") || Objects.equals(curWord, "pass") || Objects.equals(curWord, "pastor") || curWord.equals("path")
                    || curWord.equals("passes") || curWord.equals("paskem")) {
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

            if (curWord.equals("fumble") || curWord.equals("fumbles") || curWord.equals("fumbled") || curWord.equals("fully")) {
                fumbleFlag=true;
                continue;
            }

            if (curWord.equals("sack") || curWord.equals("sacks") || curWord.equals("sacked") || curWord.equals("sac")) {
                tackleflag=true;
                sackflag=true;
                continue;
            }

            if (curWord.equals("recovered") || curWord.equals("recovery") || curWord.equals("recover") || curWord.equals("recovers")) {
                if (fumbleFlag)
                    fumbleRecFlag=true;
                continue;
            }

            if (Objects.equals(curWord, "incomplete") || Objects.equals(curWord, "incompletes") || Objects.equals(curWord, "incompleted") || Objects.equals(curWord, "incompletion")
                    || (Objects.equals(curWord, "completion") && Objects.equals(prevWord, "in"))) {
                prevWord = curWord;
                incompleteFlag=true;
                gnLs = 0;
                continue;
            }

            if (Objects.equals(curWord, "ran") || Objects.equals(curWord, "ranch") || Objects.equals(curWord, "run") || curWord.equals("runs") || curWord.equals("ram") || Objects.equals(curWord, "grand")
                    || Objects.equals(curWord, "rand") || Objects.equals(curWord, "rent") || Objects.equals(curWord, "ranger") || curWord.equals("read")
                    || curWord.equals("rush") || curWord.equals("rushes") || curWord.equals("rushed") || curWord.equals("rain") || curWord.equals("room")
                    || curWord.equals("brand")) {
                playType = "Run";
                prevWord = curWord;
                m++;
                continue;
            }

            if (Objects.equals(curWord, "loss") || curWord.equals("lot")) {
                lossFlag = true;
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
                gnLs = intParse(prevWord);
                if (lossFlag) {
                    gnLs *= -1;
                }
            }

            if (curWord.equals("tackle") || curWord.equals("tackled") || curWord.equals("tackles")) {
                tackleflag=true;
                continue;
            }

            if (Objects.equals(prevWord, "number") || Objects.equals(prevWord, "numbers") || Objects.equals(prevWord, "player") || Objects.equals(prevWord, "players")
                    || Objects.equals(prevWord, "never")) {
                if (Objects.equals(curWord, "number")) {
                    twowordsago = prevWord;
                    prevWord = curWord;
                    continue;
                }

                if (fumbleRecFlag || interceptionFlag) {
                    if (twowordsago.equals("tackle") || twowordsago.equals("tackled") || twowordsago.equals("tackles"))
                        tacklerNumber = intParse(curWord);
                    else
                        defNumber = intParse(curWord);
                }
                else if (tackleflag || (recNumber != 0)) {
                    tacklerNumber = intParse(curWord);
                }
                else if (recFlag) {
                    recNumber = intParse(curWord);
                }
                else {
                    playerNumber = intParse(curWord);
                }
            }

            if (Objects.equals(curWord, "quarter") || Objects.equals(curWord, "water") || Objects.equals(nextWord, "court")) {
                playType = "End";
                continue;
            }

            if (Objects.equals(curWord, "touchdown") || (Objects.equals(curWord, "touch") && Objects.equals(nextWord, "down"))) {
                touchdownFlag = true;
            }

            if (curWord.equals("safety") || curWord.equals("safe") || curWord.equals("safeties")) {
                safetyFlag = true;
            }

            if (Objects.equals(curWord, "return") || Objects.equals(curWord, "returned") || Objects.equals(curWord, "returns")) {
                returnFlag = 1;
                twowordsago = prevWord;
                prevWord = curWord;
                continue;
            }

            if (Objects.equals(curWord, "punt") || Objects.equals(curWord, "punts") || Objects.equals(curWord, "punted") || Objects.equals(curWord, "punch") || curWord.equals("put")
                    || curWord.equals("points")) {

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
                fgDistance = (fieldSize - fieldPos) + 17;
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
                    fgMadeFlag = false;
                }
                else
                    fgMadeFlag = true;
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

        if ((fieldPos + gnLs) >= fieldSize) {
            touchdownFlag = true;
        }
        else if ((fieldPos + gnLs) <= 0) {
            safetyFlag = true;
        }
        else {
            //fieldPos is 1 to fieldSize number representing field position
            fieldPos = fieldPos + gnLs;

            //sets ydLn to neg if in own territory, pos if in opponents territory
            calcYdLn(fieldPos);

        }

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
        possFlag= homeTeam.getOnOffense();
    }

    private void calcYdLn(int fieldPos) {
        if ((fieldPos < (fieldSize/2)) && (fieldPos > 0) && (oppTerFlag == 1)) {
            ydLn = fieldPos;
            fieldPos = (fieldSize/2) - (ydLn - (fieldSize/2));
        } else if ((fieldPos < (fieldSize/2)) && (fieldPos > 0))
            ydLn = fieldPos * -1;
        else if (fieldPos >= (fieldSize/2))
            ydLn = (fieldSize/2) - (fieldPos - (fieldSize/2));
    }

    private String getResult() {
        String playResult = "";

        switch (playType) {
            case "Pass":
                if (!incompleteFlag && !interceptionFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " pass completed to number " + String.valueOf(recNumber);
                    if (!touchdownFlag && !safetyFlag)
                            playResult += " for " + String.valueOf(gnLs) + " yards";
                }
                else if (interceptionFlag) {
                    playResult =  "Number " + String.valueOf(playerNumber) + " pass intercepted by number " + String.valueOf(defNumber);
                    changePossession();
                    downNum = 1;
                    dist = 10;
                }
                else {
                    if (recNumber == 0)
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete";
                    else
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete to number " + String.valueOf(recNumber);
                }
                break;
            case "Run":
                playResult = "Number " + String.valueOf(playerNumber) + " ran";
                if (!touchdownFlag && !safetyFlag) {
                    playResult += " for " + String.valueOf(gnLs) + " yards";
                }
                break;
            case "Kickoff":
                downNum=0;
                if ((returnFlag == 1) && !touchdownFlag && !safetyFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff " + String.valueOf(returnYds) + " yards to the " +
                            String.valueOf(ydLn) + " yardline";
                    downNum = 1;
                    dist = 10;
                }
                else if ((returnFlag == 1) && (touchdownFlag)) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff " + String.valueOf(returnYds) + " yards";
                }
                else if (safetyFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff";
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
                else if (safetyFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff";
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
                if (fgMadeFlag) {
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was good";
                    if (homeTeam.getOnOffense()) {
                        homeTeam.setTeamScore(homeTeam.getTeamScore() + 3);
                    }
                    else {
                        awayTeam.setTeamScore(awayTeam.getTeamScore() + 3);
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
                if (fgMadeFlag) {
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
                playResult = "2 Pt. Conversion is ";
                if (fgMadeFlag) {
                    if (homeTeam.getOnOffense()) {
                        homeTeam.setTeamScore(homeTeam.getTeamScore() + 2);
                    } else {
                        awayTeam.setTeamScore(awayTeam.getTeamScore() + 2);
                    }
                    playResult += "good";
                }
                else
                    playResult += "no good";
                downNum = 0;
                dist = 0;
                break;
            case "Penalty":
                if (safetyFlag) {
                    playResult = "Penalty in endzone";
                    break;
                }
                playResult = String.valueOf(gnLs) + " yard penalty";
                if (!defensivePenalty) {
                    gnLs *= -1;
                    playResult = playResult + " on the offense";
                }
                else
                    playResult = playResult + " on the defense";
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
                        playResult = "The Game is already over";
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
            fieldPos = fieldSize - fieldPos;
            calcYdLn(fieldPos);
            downNum = 1;
            dist = 10;
        }

        if (sackflag) {
            playResult += " sacked by number " + String.valueOf(tacklerNumber);
        }

        else if (tackleflag) {
            playResult += " tackled by number " + String.valueOf(tacklerNumber);
        }

        if (fumbleFlag && !fumbleRecFlag) {
            playResult = playResult + ", fumble recovered by the offense";
        }

        else if (fumbleRecFlag) {
            playResult = playResult + ", fumble recovered by number " + String.valueOf(defNumber);
            changePossession();
            downNum = 1;
            dist = 10;
        }

        if (returnFlag==1 && !(playType.equals("Kickoff") || playType.equals("Punt"))) {
            playResult = playResult + ", returned for " + String.valueOf(returnYds) + " yards";
            if (!touchdownFlag) {
                playResult = playResult + " to the " + String.valueOf(ydLn) + " yardline";
            }
        }

        if (touchdownFlag) {
            gnLs = fieldSize - fieldPos;
            fieldPos = fieldSize;
            if (playType.equals("Pass") || playType.equals("Run"))
                playResult += " for " + gnLs + " yards";
            ydLn = 0;
            downNum = 0;
            dist = 3;
            playResult = playResult + " for a TOUCHDOWN!";
            if (homeTeam.getOnOffense()) {
                homeTeam.setTeamScore(homeTeam.getTeamScore() + 6);
            }
            else {
                awayTeam.setTeamScore(awayTeam.getTeamScore() + 6);
            }
        }

        if (safetyFlag) {
            playResult += ", result is a SAFETY!";
            gnLs = 0 - fieldPos;
            fieldPos = 0;
            ydLn = 0;
            downNum = 0;
            dist = 0;
            if (homeTeam.getOnOffense()) {
                awayTeam.setTeamScore(homeTeam.getTeamScore() + 2);
            }
            else {
                homeTeam.setTeamScore(awayTeam.getTeamScore() + 2);
            }
        }

        return playResult;
    }

    /**
     * PLAY CLASS - NESTED INSIDE GAME
     */
    public class Play {

        Integer playerNumber, recNumber, defNumber, tacklerNumber, ydLn, gnLs,  fieldPos, downNum, dist, qtr, fgDistance, playCount, returnYds;
        Integer lossFlag, returnFlag, oppTerFlag;
        boolean incompleteFlag, touchdownFlag, recFlag, touchbackFlag, faircatchFlag, interceptionFlag, fumbleFlag, fumbleRecFlag, tackleflag, sackflag, fgMadeFlag,
                possFlag, safetyFlag, defensivePenalty;
        String playType, result = "";

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
        private Integer pics, tackles, tfls, sacks, fumblerecs, forcedfums, deftds;

        public Player(boolean offensive, Integer number) {
            this.offensive = offensive;
            this.number = number;
            passcomps=0; passatmpts=0; passyds=0; runatmpts=0; runyds=0; ints=0;
            fumbles=0; catches=0; recyds=0; passtds=0; runtds=0; rectds=0; puntrettds=0;
            kickrettds=0; puntreturns=0; kickreturns=0; pics=0; tackles=0; tfls=0; sacks=0; fumblerecs=0; forcedfums=0; deftds=0;
        }

        private void updatePassStats(int yds, boolean pic, boolean incompletion, boolean td, boolean fum) {
            passyds +=  yds;
            if (pic)
                ints++;
            if(!incompletion)
                passcomps++;
            if (td && !fum && !pic)
                passtds++;
            passatmpts++;
        }

        private void undoPassStats(int yds, boolean pic, boolean incompletion, boolean td, boolean fum) {
            passyds -=  yds;
            if (pic)
                ints--;
            if(!incompletion)
                passcomps--;
            if (td && !fum && !pic)
                passtds--;
            passatmpts--;
        }

        private void updateRunStats(int yds, boolean fumb, boolean td) {
            runyds +=  yds;
            if (fumb)
                fumbles++;
            if (td && !fumb)
                runtds++;
            runatmpts++;
        }

        private void undoRunStats(int yds, boolean fumb, boolean td) {
            runyds -=  yds;
            if (fumb)
                fumbles--;
            if (td && !fumb)
                runtds--;
            runatmpts--;
        }

        private void updateRecStats(int yds, boolean fumb, boolean td) {
            recyds +=  yds;
            if (fumb)
                fumbles++;
            if (td && !fumb)
                rectds++;
            catches++;
        }

        private void undoRecStats(int yds, boolean fumb, boolean td) {
            recyds -=  yds;
            if (fumb)
                fumbles--;
            if (td && !fumb)
                rectds--;
            catches--;
        }

        private void updatePuntRetStats(int yds, boolean fumb, boolean td) {
            puntrettds +=  yds;
            if (fumb)
                fumbles++;
            if (td && !fumb)
                puntrettds++;
            puntreturns++;
        }

        private void undoPuntRetStats(int yds, boolean fumb, boolean td) {
            puntrettds -=  yds;
            if (fumb)
                fumbles--;
            if (td && !fumb)
                puntrettds--;
            puntreturns--;
        }

        private void updateKickRetStats(int yds, boolean fumb, boolean td) {
            kickrettds +=  yds;
            if (fumb)
                fumbles++;
            if (td && !fumb)
                kickrettds++;
            kickreturns++;
        }

        private void undoKickRetStats(int yds, boolean fumb, boolean td) {
            kickrettds -=  yds;
            if (fumb)
                fumbles--;
            if (td && !fumb)
                kickrettds--;
            kickreturns--;
        }

        private void updateDefStats(boolean pic, boolean tackle, boolean loss, boolean fumblerec, boolean forcedfum, boolean sack, boolean td) {
            if (pic)
                pics++;
            if (tackle)
                tackles++;
            if (tackle && loss)
                tfls++;
            if (fumblerec)
                fumblerecs++;
            if (sack)
                sacks++;
            if (td)
                deftds++;
            if (forcedfum)
                forcedfums++;
        }

        private void undoDefStats(boolean pic, boolean tackle, boolean loss, boolean fumblerec, boolean forcedfum, boolean sack, boolean td) {
            if (pic)
                pics--;
            if (tackle)
                tackles--;
            if (tackle && loss)
                tfls--;
            if (fumblerec)
                fumblerecs--;
            if (sack)
                sacks--;
            if (td)
                deftds--;
            if (forcedfum)
                forcedfums--;
        }

    }
}
