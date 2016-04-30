package com.kerbysoft.qwikcut.speakstats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
    public ArrayList<String> gameDataList;
    public ArrayList<String> statsList;

    String csvplaylist = "play_list.csv";
    String csvGameData = "game_data.csv";
    String csvOffhomestatslist = "home_offensive_stats_list.csv";
    String csvOffawaystatslist = "away_offensive_stats_list.csv";
    String csvDefhomestatslist = "home_defensive_stats_list.csv";
    String csvDeffawaystatslist = "away_defensive_stats_list.csv";
    String themeColor = "#6d9e31";
    String hometeamname = "", awayteamname = "", gameName = "", division = "";
    Integer fieldSize = 100, playerNumber = -1, recNumber = -1, defNumber = -1, tacklerNumber = -1, ydLn = 0, gnLs = 0, fieldPos = 0, playCounter = 0,
            downNum = 0, dist = 0, qtr = 1, fgDistance = 0, prevDown = 0, prevDist = 0, returnYds = 0, day, month, year, firstDn = 0;
    Integer returnFlag,oppTerFlag;
    boolean interceptionFlag = false, fumbleFlag = false, incompleteFlag = false, touchdownFlag = false, defensivePenalty = false,
            recFlag = false, touchbackFlag = false, faircatchFlag = false, fumbleRecFlag=false, tackleflag=false, sackflag=false,
            lossFlag = false, fgMadeFlag = false, safetyFlag = false, possFlag = false, homeTeamOpeningKickoff = false, endOfGame = false,
            updateFlag = false;
    //possflag is set to false if the awayTeam has the ball, and true if the homeTeam has the ball
    String prevWord = "", playType = "", twowordsago = "", threewordsago = "", curWord = "", nextWord = "", result = "", notes = "", dirPath = "";
    boolean invalidPlay = false;
    static final String logtag = "MyLogTag";
    public Team awayTeam;
    public Team homeTeam;
    public ArrayList<Play> gamePlays;
    float scale;
    int buttonSize;
    File projDir;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Button btnSpeak, undoButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_game);



        scale = this.getResources().getDisplayMetrics().density;
        buttonSize = (int) (70 * scale + 0.5f);

        Intent intent = getIntent(); // gets the previously created intent

        //pull in the the team names here and make new team instances and the game name
        hometeamname = intent.getStringExtra("homeName");
        awayteamname = intent.getStringExtra("awayName");

        Toolbar actionBar = (Toolbar) findViewById(R.id.actionBar);
        actionBar.setTitle(awayteamname + " vs. " + hometeamname);
        actionBar.setBackgroundColor(Color.parseColor(themeColor));
        actionBar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(actionBar);

        //getWindow().setStatusBarColor(Color.parseColor(themeColor));

        //pull in the date for clarity reasons
        day = Integer.parseInt(intent.getStringExtra("day"));
        month = Integer.parseInt(intent.getStringExtra("month"));
        year = Integer.parseInt(intent.getStringExtra("year"));

        division = intent.getStringExtra("division");

        gameName = month + "_" + day + "_" + year + "_" + division + "_" + hometeamname + "_vs_" + awayteamname;

        Log.d(logtag, hometeamname + ", " + awayteamname + ", " + gameName);

        if (intent.getStringExtra("openingPastGame").equals("true")) {
            dirPath = intent.getStringExtra("gameDir");
            gameName = intent.getStringExtra("gameName");
        }
        else {
            fieldSize = Integer.parseInt(intent.getStringExtra("fieldSize"));
            dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;
        }

        projDir = new File(dirPath);

        homeTeam = new Team(hometeamname, false);
        awayTeam = new Team(awayteamname, true);

        playList = new ArrayList<String>();
        gameDataList = new ArrayList<String>();
        statsList = new ArrayList<String>();
        gamePlays = new ArrayList<Play>();

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

        undoButton = (Button) findViewById(R.id.undoButton);

        //txtText.setText("This is where this text will be.");

        btnSpeak = (Button) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(this);
        undoButton.setOnClickListener(this);

        if (intent.getStringExtra("openingPastGame").equals("true")) {
            openGame();
        }
        else
            openingKickoffDialog();


        //Building persistent notification
        /*
        Intent notificationIntent = intent;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 01, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationIntent.putExtra("openingPastGame", "true");

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentTitle("This is the title");
        builder.setContentText("This is the text");
        builder.setSubText("Some sub text");
        builder.setNumber(101);
        builder.setContentIntent(pendingIntent);
        builder.setTicker("Fancy Notification");
        builder.setSmallIcon(R.drawable.american_football);
        //builder.setLargeIcon();
        builder.setAutoCancel(true);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setOngoing(true);
        Notification notification = builder.build();
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.notify(01, notification);
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.exportButton:
                exportDialog();

                return true;
            case R.id.saveGameButton:
                saveGame();
                return true;
            case R.id.exitGameButton:
                exitGameDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

            case R.id.undoButton: {

                if (playCounter > 0) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder
                            .setMessage("Are you sure you want to undo the last play?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Toast.makeText(getApplicationContext(), "Play Deleted", Toast.LENGTH_SHORT).show();

                                    undoStats();
                                    if (playCounter > 1) {
                                        revertToLastPlay(gamePlays.get(gamePlays.size() - 2));
                                    } else {
                                        completeReset();
                                        resetValues();
                                    }
                                    removeButton(buttonArrayList.size());
                                    playCounter--;
                                    gamePlays.remove(gamePlays.size() - 1);
                                    playList.remove(playList.size() - 1);
                                    gameDataList.remove(gameDataList.size() - 1);
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
                    openingKickoffDialog();
                }
                break;
            }

            default: {
                final Integer id = v.getId();
                Log.d(logtag, "ID: " + id);
                final Play play = gamePlays.get(id-1);
                Log.d(logtag, "Result: " + play.result);
                final String notesString = "";


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
                final TextView resultText = (TextView) dialog.findViewById(R.id.text);
                final TextView playTypeText = (TextView) dialog.findViewById(R.id.playTypeView);
                TextView mainPlayerText = (TextView) dialog.findViewById(R.id.mainPlayerView);
                TextView receiverText = (TextView) dialog.findViewById(R.id.receiverView);
                TextView downText = (TextView) dialog.findViewById(R.id.downView);
                TextView distanceText = (TextView) dialog.findViewById(R.id.distanceView);
                TextView ydLnText = (TextView) dialog.findViewById(R.id.ydLnView);
                TextView gnLsText = (TextView) dialog.findViewById(R.id.gnLsView);
                TextView notesText = (TextView) dialog.findViewById(R.id.notesView);

                final TextView playTypeEdit = (TextView) dialog.findViewById(R.id.playTypeEditText);
                final TextView mainPlayerEdit = (TextView) dialog.findViewById(R.id.mainPlayerEditText);
                final TextView receiverEdit = (TextView) dialog.findViewById(R.id.receiverEditText);
                final TextView downEdit = (TextView) dialog.findViewById(R.id.downEditText);
                final TextView distanceEdit = (TextView) dialog.findViewById(R.id.distanceEditText);
                final TextView ydLnEdit = (TextView) dialog.findViewById(R.id.ydLnEditText);
                final TextView gnLsEdit = (TextView) dialog.findViewById(R.id.gnLsEditText);
                final TextView notesEdit = (TextView) dialog.findViewById(R.id.notesEditText);

                resultText.setText(play.result);
                playTypeEdit.setText(play.playType);
                mainPlayerEdit.setText(String.valueOf(play.playerNumber));
                receiverEdit.setText(String.valueOf(play.recNumber));
                downEdit.setText(String.valueOf(play.prevDown));
                distanceEdit.setText(String.valueOf(play.prevDist));
                ydLnEdit.setText(String.valueOf(play.ydLn));
                gnLsEdit.setText(String.valueOf(play.gnLs));
                notesEdit.setText(play.notes);

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
                        updateFlag = true;
                        Play tempPlay = gamePlays.get(gamePlays.size() - 1);

                        if (id < gamePlays.size()) {
                            setVarsToPlay(play);
                        }
                        undoStats();

                        //play.playType = playTypeEdit.getText().toString();
                        play.playerNumber = intParse(mainPlayerEdit.getText().toString());
                        play.recNumber = intParse(receiverEdit.getText().toString());
                        //playType = play.playType;
                        playerNumber = play.playerNumber;
                        recNumber = play.recNumber;

                        if (id == gamePlays.size()) {
                            play.ydLn = ydLn = intParse(ydLnEdit.getText().toString());
                            play.gnLs = gnLs = intParse(gnLsEdit.getText().toString());
                            play.notes = notes = notesEdit.getText().toString();
                        }
                        result = play.result = getResult();
                        gamePlays.set(id-1, play);

                        downNum = play.downNum; dist = play.dist;
                        String buttonText = "Play " + playCounter + " - " + result;
                        buttonArrayList.get(id-1).setText(buttonText);

                        gameDataList.set(id-1, prevDist + "," + prevDown + "," + downNum + "," + dist + "," + fgDistance + "," + fgMadeFlag + "," + fieldPos
                                + "," + ydLn + "," + gnLs + "," + incompleteFlag + "," + playCounter + "," + playerNumber + "," + playType + "," +
                                qtr + "," + recNumber + "," + returnFlag + "," + touchbackFlag + "," + defNumber + "," + fumbleFlag + "," + interceptionFlag
                                + "," + touchbackFlag + "," + faircatchFlag + "," + returnYds + "," + fumbleRecFlag + "," + tackleflag + "," + sackflag + "," + tacklerNumber
                                + "," + possFlag + "," + safetyFlag + "," + defensivePenalty + "," + homeTeam.getTeamScore() + "," + awayTeam.getTeamScore() + "," + firstDn
                                + "," + result + "\n");

                        playList.set(id-1, playCounter + "," + getOffensiveTeam().getTeamName() + "," + playType + "," + prevDown + "," + prevDist + "," + playerNumber + "," + recNumber +
                                "," + gnLs + "," + calcYdLn(fieldPos - gnLs) + "," + ydLn + "," + result + "\n");
                        updateStats();

                        updateVisuals();

                        Toast t = Toast.makeText(getApplicationContext(), "Play Updated", Toast.LENGTH_SHORT);
                        t.show();
                        if (id < gamePlays.size()) {
                            setVarsToPlay(tempPlay);
                        }
                        updateFlag = false;
                        dialog.dismiss();
                    }
                });

                dialog.show();
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        exitGameDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_game_feed, menu);
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
                    Log.d(logtag, "Home: " + homeTeam.getTeamScore() + " Away: " + awayTeam.getTeamScore());
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

                            currentPlay.prevDist = prevDist;
                            currentPlay.prevDown = prevDown;
                            currentPlay.downNum = downNum;
                            currentPlay.dist = dist;
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
                            currentPlay.homeScore = homeTeam.getTeamScore();
                            currentPlay.awayScore = awayTeam.getTeamScore();
                            currentPlay.result = result;
                            currentPlay.firstDn = firstDn;

                            try {
                                gamePlays.set(playCounter - 1, currentPlay);
                            }
                            catch (IndexOutOfBoundsException e){
                                gamePlays.add(currentPlay);
                            }

                            addButton(result, playCounter);

                            gameDataList.add(prevDist + "," + prevDown + "," + downNum + "," + dist + "," + fgDistance + "," + fgMadeFlag + "," + fieldPos
                                    + "," + ydLn + "," + gnLs + "," + incompleteFlag + "," + playCounter + "," + playerNumber + "," + playType + "," +
                                    qtr + "," + recNumber + "," + returnFlag + "," + touchdownFlag + "," + defNumber + "," + fumbleFlag + "," + interceptionFlag
                                    + "," + touchbackFlag + "," + faircatchFlag + "," + returnYds + "," + fumbleRecFlag + "," + tackleflag + "," + sackflag + "," + tacklerNumber
                                    + "," + possFlag + "," + safetyFlag + "," + defensivePenalty + "," + homeTeam.getTeamScore() + "," + awayTeam.getTeamScore() + "," + firstDn
                                    + "," + result + "\n");

                            playList.add(playCounter + "," + getOffensiveTeam().getTeamName() + "," + playType + "," + prevDown + "," + prevDist + "," + playerNumber + "," + recNumber +
                                    "," + gnLs + "," + calcYdLn(fieldPos - gnLs) + "," + ydLn + "," + result + "\n");

                            statsList.add(analyzedPlay);

                            updateStats();
                            updateVisuals();
                        } else
                            qtrTextView.setText(String.valueOf(qtr));

                    } else {
                        if (playCounter > 0)
                            revertToLastPlay(gamePlays.get(gamePlays.size() - 1));
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

    private void setVarsToPlay(Play currentPlay) {
        prevDist = currentPlay.prevDist;
        prevDown  = currentPlay.prevDown;
        downNum = currentPlay.downNum;
        dist = currentPlay.dist;
        fgDistance = currentPlay.fgDistance;
        fgMadeFlag = currentPlay.fgMadeFlag;
        fieldPos = currentPlay.fieldPos;
        ydLn = currentPlay.ydLn;
        gnLs = currentPlay.gnLs;
        incompleteFlag = currentPlay.incompleteFlag;
        playCounter = currentPlay.playCount;
        playerNumber = currentPlay.playerNumber;
        playType = currentPlay.playType;
        qtr = currentPlay.qtr;
        recNumber = currentPlay.recNumber;
        returnFlag = currentPlay.returnFlag;
        touchdownFlag = currentPlay.touchdownFlag;
        defNumber = currentPlay.defNumber;
        fumbleFlag = currentPlay.fumbleFlag;
        interceptionFlag = currentPlay.interceptionFlag;
        touchbackFlag = currentPlay.touchbackFlag;
        faircatchFlag = currentPlay.faircatchFlag;
        returnYds = currentPlay.returnYds;
        fumbleRecFlag = currentPlay.fumbleRecFlag;
        tackleflag = currentPlay.tackleflag;
        sackflag = currentPlay.sackflag;
        tacklerNumber = currentPlay.tacklerNumber;
        possFlag = currentPlay.possFlag;
        safetyFlag = currentPlay.safetyFlag;
        defensivePenalty = currentPlay.defensivePenalty;
        firstDn = currentPlay.firstDn;
        result = currentPlay.result;
        if (possFlag != homeTeam.getOnOffense())
            changePossession();
    }

    private void updateStats() {
        Player currentPlayer, recPlayer, defPlayer, tacklerPlayer;
        Team tempOffTeam, tempDefTeam;

        tempOffTeam = getOffensiveTeam();
        tempDefTeam = getDefensiveTeam();

        //creating the players if they do not previously exist
        if (playerNumber != -1) {
            currentPlayer = tempOffTeam.getPlayer(playerNumber);

            if (currentPlayer == null) {
                currentPlayer = new Player(true, playerNumber);
                tempOffTeam.addPlayer(currentPlayer);
            }

            if (recNumber != -1) {
                recPlayer = tempOffTeam.getPlayer(recNumber);

                if (recPlayer == null) {
                    recPlayer = new Player(true, recNumber);
                    tempOffTeam.addPlayer(recPlayer);
                }
            }

            if (tacklerNumber != -1) {
                tacklerPlayer = tempDefTeam.getPlayer(tacklerNumber);

                if (tacklerPlayer == null) {
                    tacklerPlayer = new Player(false, tacklerNumber);
                    tempDefTeam.addPlayer(tacklerPlayer);
                }
            }

            if (defNumber != -1) {
                defPlayer = tempDefTeam.getPlayer(defNumber);

                if (defPlayer == null) {
                    defPlayer = new Player(false, defNumber);
                    tempDefTeam.addPlayer(defPlayer);
                }
            }

            switch (playType) {
                case "Pass":
                    tempOffTeam.getPlayer(playerNumber).updatePassStats(gnLs, interceptionFlag, incompleteFlag, touchdownFlag, fumbleFlag);
                    if (tackleflag && (tacklerNumber != -1))
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                    if (!interceptionFlag && !incompleteFlag && (recNumber != -1))
                        tempOffTeam.getPlayer(recNumber).updateRecStats(gnLs, fumbleFlag, touchdownFlag);
                    if ((interceptionFlag || fumbleRecFlag) && (defNumber != -1))
                        tempDefTeam.getPlayer(defNumber).updateDefStats(interceptionFlag, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Run":
                    tempOffTeam.getPlayer(playerNumber).updateRunStats(gnLs, fumbleFlag, touchdownFlag);
                    if (tackleflag && (tacklerNumber != -1))
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                    if (fumbleRecFlag && (defNumber != -1))
                        tempDefTeam.getPlayer(defNumber).updateDefStats(false, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Field Goal":
                    break;
                case "Kickoff":
                    tempOffTeam.getPlayer(playerNumber).updateKickRetStats(returnYds, fumbleFlag, touchdownFlag);
                    if (tackleflag && (tacklerNumber != -1))
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                    if (fumbleRecFlag && (defNumber != -1))
                        tempDefTeam.getPlayer(defNumber).updateDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                    break;
                case "Punt":
                    tempOffTeam.getPlayer(playerNumber).updatePuntRetStats(returnYds, fumbleFlag, touchdownFlag);
                    if (tackleflag && (tacklerNumber != -1))
                        tempDefTeam.getPlayer(tacklerNumber).updateDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                    if (fumbleRecFlag && (defNumber != -1))
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
                if (playerNumber != -1)
                    tempOffTeam.getPlayer(playerNumber).undoPassStats(gnLs, interceptionFlag, incompleteFlag, touchdownFlag, fumbleFlag);
                if (tackleflag && (tacklerNumber != -1))
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                if ((!interceptionFlag && !incompleteFlag) && (recNumber != -1))
                    tempOffTeam.getPlayer(recNumber).undoRecStats(gnLs, fumbleFlag, touchdownFlag);
                if ((interceptionFlag || fumbleRecFlag) && (defNumber != -1))
                    tempDefTeam.getPlayer(defNumber).undoDefStats(interceptionFlag, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Run":
                if (playerNumber != -1)
                    tempOffTeam.getPlayer(playerNumber).undoRunStats(gnLs, fumbleFlag, touchdownFlag);
                if (tackleflag && (tacklerNumber != -1))
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, lossFlag, false, fumbleFlag, sackflag, false);
                if (fumbleRecFlag && (defNumber != -1))
                    tempDefTeam.getPlayer(defNumber).undoDefStats(false, false, lossFlag, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Field Goal":
                break;
            case "Kickoff":
                if (playerNumber != -1)
                    tempOffTeam.getPlayer(playerNumber).undoKickRetStats(returnYds, fumbleFlag, touchdownFlag);
                if (tackleflag && (tacklerNumber != -1))
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                if (fumbleRecFlag && (defNumber != -1))
                    tempDefTeam.getPlayer(defNumber).undoDefStats(false, false, false, fumbleRecFlag, false, false, touchdownFlag);
                break;
            case "Punt":
                if (playerNumber != -1)
                    tempOffTeam.getPlayer(playerNumber).undoPuntRetStats(returnYds, fumbleFlag, touchdownFlag);
                if (tackleflag && (tacklerNumber != -1))
                    tempDefTeam.getPlayer(tacklerNumber).undoDefStats(false, tackleflag, false, false, fumbleFlag, false, false);
                if (fumbleRecFlag && (defNumber != -1))
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
        playerNumber = -1;
        recNumber = -1;
        defNumber = -1;
        tacklerNumber = -1;
        gnLs=0;
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
        result = "";
        recFlag = false; //flag that marks if there was a reception
        lossFlag = false; //flag that marks if there was a loss on the play
        returnFlag = 0; //flag that marks if there is a return on the play
        oppTerFlag = 0; //flag to mark the field position in opponent's territory
        firstDn = 0; //first down location
    }

    private void revertToLastPlay(Play lastPlay) {
        Log.d(logtag, "revert: " + playerNumber.toString());
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
        firstDn = lastPlay.firstDn;
        homeTeam.setTeamScore(lastPlay.homeScore);
        awayTeam.setTeamScore(lastPlay.awayScore);

        /*Team offTeam, defTeam;
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
        }*/

        if (possFlag != lastPlay.possFlag || playType.equals("Kickoff") || playType.equals("Punt")) {
            changePossession();
            possFlag = lastPlay.possFlag;
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
        endOfGame=false;
        if (possFlag != homeTeamOpeningKickoff)
            changePossession();
        playerNumber = -1;
        playType = "";
        qtr = 1;
        recNumber = -1;
        defNumber = -1;
        returnFlag = 0;
        firstDn = 0;
        result = "";
        fumbleRecFlag=false;
        tackleflag=false;
        sackflag=false;
        tacklerNumber = -1;
        lossFlag=false;
        defensivePenalty = false;
        ydLnTextView.setText("YdLn: " + 0);
        awayScoreTextView.setText(String.valueOf(0));
        homeScoreTextView.setText(String.valueOf(0));
        qtrTextView.setText(String.valueOf(0));
        downAndDistTextView.setText("");
    }

    private void openingKickoffDialog() {
        AlertDialog.Builder possBuilder = new AlertDialog.Builder(this);
        possBuilder
                .setMessage("Which team is kicking off to begin the game?")
                .setPositiveButton(hometeamname, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (!possFlag)
                            changePossession();
                        homeTeamOpeningKickoff = true;
                        Toast t = Toast.makeText(getApplicationContext(), hometeamname + " will kick to " + awayteamname + " to begin the game", Toast.LENGTH_SHORT);
                        t.show();
                    }
                })
                .setNegativeButton(awayteamname, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (possFlag)
                            changePossession();
                        homeTeamOpeningKickoff = false;
                        Toast t = Toast.makeText(getApplicationContext(), awayteamname + " will kick to " + hometeamname + " to begin the game", Toast.LENGTH_SHORT);
                        t.show();
                    }
                })
                .show();
    }

    private void endOfGameDialog() {
        AlertDialog.Builder possBuilder = new AlertDialog.Builder(this);
        possBuilder
                .setMessage("The game has ended. Would you like to save the game and export the data?")
                .setPositiveButton("Save and Export", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        saveGame();
                    }
                })
                .setNegativeButton("Don't Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void exitGameDialog() {
        AlertDialog.Builder possBuilder = new AlertDialog.Builder(this);
        possBuilder
                .setMessage("Save Game Before Exiting?")
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Don't Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setPositiveButton("Save Game", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        saveGame();
                        finish();
                    }
                })
                .show();
    }

    private void exportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder
                .setMessage("Are you sure you want to export the game stats?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (gamePlays.size() > 0)
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
    }

    Integer intParse(String word) {
        Integer number = -1;

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
                    else {
                        try {
                            number = Integer.parseInt(word);
                        }
                        catch (Exception e) {
                            invalidPlay = true;
                        }
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

    private void openGame() {
        File csvFile = new File(dirPath, csvGameData);

        if (!csvFile.exists())
            Toast.makeText(getApplicationContext(), "Game data has been deleted, unable to open game to previous state.", Toast.LENGTH_SHORT).show();

        //StringBuilder text = new StringBuilder();
        String line, currentStatus = "", offensiveTeam = "";
        String words[];
        int cntr=0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));

            while ((line = br.readLine()) != null) {
                /*text.append(line);
                text.append('\n');*/
                if (cntr==0) {
                    currentStatus = line;
                    words = currentStatus.split(",");
                    fieldSize = intParse(words[8]);
                    Log.d(logtag, "Field Size: " + fieldSize);
                }
                else {
                    words = line.split(",");

                    resetValues();

                    //creating a new play and adding its attributes, then adding it to the ArrayList of plays
                    Play currentPlay = new Play();

                    currentPlay.prevDist = intParse(words[0]); prevDist = intParse(words[0]);
                    currentPlay.prevDown = intParse(words[1]); prevDown = intParse(words[1]);
                    currentPlay.downNum = intParse(words[2]); downNum = intParse(words[2]);
                    currentPlay.dist = intParse(words[3]); dist = intParse(words[3]);
                    currentPlay.fgDistance = intParse(words[4]); fgDistance = intParse(words[4]);
                    currentPlay.fgMadeFlag = Boolean.parseBoolean(words[5]); fgMadeFlag = Boolean.parseBoolean(words[5]);
                    currentPlay.fieldPos = intParse(words[6]); fieldPos = intParse(words[6]);
                    currentPlay.ydLn = intParse(words[7]); ydLn = intParse(words[7]);
                    currentPlay.gnLs = intParse(words[8]); gnLs = intParse(words[8]);
                    currentPlay.incompleteFlag = Boolean.parseBoolean(words[9]); incompleteFlag = Boolean.parseBoolean(words[9]);
                    currentPlay.playCount = Integer.parseInt(words[10]); playCounter = Integer.parseInt(words[10]);
                    currentPlay.playerNumber = intParse(words[11]); playerNumber = intParse(words[11]);
                    currentPlay.playType = words[12]; playType = words[12];
                    currentPlay.qtr = intParse(words[13]); qtr = intParse(words[13]);
                    currentPlay.recNumber = intParse(words[14]); recNumber = intParse(words[14]);
                    currentPlay.returnFlag = intParse(words[15]); returnFlag = intParse(words[15]);
                    currentPlay.touchdownFlag = Boolean.parseBoolean(words[16]); touchdownFlag = Boolean.parseBoolean(words[16]);
                    currentPlay.defNumber = intParse(words[17]); defNumber = intParse(words[17]);
                    currentPlay.fumbleFlag = Boolean.parseBoolean(words[18]); fumbleFlag = Boolean.parseBoolean(words[18]);
                    currentPlay.interceptionFlag = Boolean.parseBoolean(words[19]); interceptionFlag = Boolean.parseBoolean(words[19]);
                    currentPlay.touchbackFlag = Boolean.parseBoolean(words[20]); touchbackFlag = Boolean.parseBoolean(words[20]);
                    currentPlay.faircatchFlag = Boolean.parseBoolean(words[21]); faircatchFlag = Boolean.parseBoolean(words[21]);
                    currentPlay.returnYds = intParse(words[22]); returnYds = intParse(words[22]);
                    currentPlay.fumbleRecFlag = Boolean.parseBoolean(words[23]); fumbleRecFlag = Boolean.parseBoolean(words[23]);
                    currentPlay.tackleflag = Boolean.parseBoolean(words[24]); tackleflag = Boolean.parseBoolean(words[24]);
                    currentPlay.sackflag = Boolean.parseBoolean(words[25]); sackflag = Boolean.parseBoolean(words[25]);
                    currentPlay.tacklerNumber = intParse(words[26]); tacklerNumber = intParse(words[26]);
                    currentPlay.possFlag = Boolean.parseBoolean(words[27]); possFlag = Boolean.parseBoolean(words[27]);
                    currentPlay.safetyFlag = Boolean.parseBoolean(words[28]); safetyFlag = Boolean.parseBoolean(words[28]);
                    currentPlay.defensivePenalty = Boolean.parseBoolean(words[29]); defensivePenalty = Boolean.parseBoolean(words[29]);
                    currentPlay.homeScore = intParse(words[30]); homeTeam.setTeamScore(intParse(words[30]));
                    currentPlay.awayScore = intParse(words[31]); awayTeam.setTeamScore(intParse(words[31]));
                    currentPlay.firstDn = intParse(words[32]); firstDn = intParse(words[32]);

                    for (int i=33; i<words.length; i++) {
                        if (i>33)
                            result += ",";
                        currentPlay.result += words[i];
                        result += words[i];
                    }

                    try {
                        gamePlays.set(playCounter - 1, currentPlay);
                    }
                    catch (IndexOutOfBoundsException e) {
                        gamePlays.add(currentPlay);
                    }

                    if (possFlag)
                        offensiveTeam = hometeamname;
                    else
                        offensiveTeam = awayteamname;

                    addButton(result, playCounter);

                    gameDataList.add(prevDist + "," + prevDown + "," + downNum + "," + dist + "," + fgDistance + "," + fgMadeFlag + "," + fieldPos
                            + "," + ydLn + "," + gnLs + "," + incompleteFlag + "," + playCounter + "," + playerNumber + "," + playType + "," +
                            qtr + "," + recNumber + "," + returnFlag + "," + touchdownFlag + "," + defNumber + "," + fumbleFlag + "," + interceptionFlag
                            + "," + touchbackFlag + "," + faircatchFlag + "," + returnYds + "," + fumbleRecFlag + "," + tackleflag + "," + sackflag + "," + tacklerNumber
                            + "," + possFlag + "," + safetyFlag + "," + defensivePenalty + "," + homeTeam.getTeamScore() + "," + awayTeam.getTeamScore() + "," + firstDn
                            + "," + result + "\n");

                    playList.add(playCounter + "," + offensiveTeam + "," + playType + "," + prevDown + "," + prevDist + "," + playerNumber + "," + recNumber +
                            "," + gnLs + "," + calcYdLn(fieldPos - gnLs) + "," + ydLn + "," + result + "\n");

                    if (possFlag != homeTeam.getOnOffense())
                        changePossession();

                    updateStats();
                }
                cntr++;
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        Log.d(logtag, currentStatus);
        words = currentStatus.split(",");

        awayTeam.setTeamScore(intParse(words[0]));
        homeTeam.setTeamScore(intParse(words[1]));
        possFlag = Boolean.parseBoolean(words[2]);
        qtr = intParse(words[3]);
        downNum = intParse(words[4]);
        dist = intParse(words[5]);
        fieldPos = intParse(words[6]);
        updateVisuals();
    }

    private void saveGame() {
        if (gamePlays.size() == 0) {
            Toast t = Toast.makeText(getApplicationContext(), "Minimum of one play must occur before saving.", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        if (!projDir.exists())
            projDir.mkdirs();

        File csvFile = new File(dirPath, csvGameData);
        String currentStatus = awayTeam.getTeamScore() + "," + homeTeam.getTeamScore() + "," + possFlag + "," + qtr
                + "," + downNum + "," + dist + "," + fieldPos  + "," + ydLn + "," + fieldSize + "\n";

        if (csvFile.exists())
            csvFile.delete();

        FileOutputStream fileStream = null;
        try {
            fileStream = new FileOutputStream(csvFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int cntr=0;
        for (String output : gameDataList) {
            try {
                if (cntr==0)
                    fileStream.write(currentStatus.getBytes());
                fileStream.write(output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            cntr++;
        }

        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        export();
        Toast.makeText(getApplicationContext(), "Game Saved", Toast.LENGTH_SHORT).show();
    }

    private void export() {
        if (gamePlays.size() == 0) {
            Toast t = Toast.makeText(getApplicationContext(), "Minimum of one play must occur before exporting.", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        if (!projDir.exists())
            projDir.mkdirs();

        String gamePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpeakStats/" + gameName;
        File homeOffStatsFile = new File(gamePath, csvOffhomestatslist);
        File awayOffStatsFile = new File(gamePath, csvOffawaystatslist);
        File homeDefStatsFile = new File(gamePath, csvDefhomestatslist);
        File awayDefStatsFile = new File(gamePath, csvDeffawaystatslist);
        File playListFile = new File(gamePath, csvplaylist);

        String offLabels = "Number, Pass Atmpts, Pass Comps, Pass Yds, Pass TDs, INTs, Run Atmpts, Run Yds, Run TDs, Receptions, Rec Yds, Rec TDs\n";
        String defLabels = "Number, Tackles, TFL, Sacks, Forced Fumbles, Fumble Recs, Interceptions, Def TDs\n";
        String header = "Play Number, Offensive Team, Play Type, Down, Distance, Passer/Runner Number, Receiver Number, Gain/Loss, Initial Yardline, " +
                "Resulting Yardline, Play Result\n";
        String temp = "";

        if (homeOffStatsFile.exists())
            homeOffStatsFile.delete();
        if (awayOffStatsFile.exists())
            awayOffStatsFile.delete();
        if (homeDefStatsFile.exists())
            homeDefStatsFile.delete();
        if (awayDefStatsFile.exists())
            awayDefStatsFile.delete();
        if (playListFile.exists())
            playListFile.delete();

        FileOutputStream fileStream = null;

        for (int i = 0; i < 5; i++) {
            File file = null;
            ArrayList<Player> playerList = new ArrayList<>();

            switch (i) {
                case 0:
                    file = homeOffStatsFile;
                    playerList = homeTeam.getPlayers();
                    break;
                case 1:
                    file = awayOffStatsFile;
                    playerList = awayTeam.getPlayers();
                    break;
                case 2:
                    file = homeDefStatsFile;
                    playerList = homeTeam.getPlayers();
                    break;
                case 3:
                    file = awayDefStatsFile;
                    playerList = awayTeam.getPlayers();
                    break;
                case 4:
                    file = playListFile;
                    break;
            }

            try {
                fileStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (i==4) {
                try {
                    fileStream.write(header.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (String current : playList) {
                    try {
                        fileStream.write(current.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            else if (i==0 || i==1){
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
        Log.d(logtag, "playNum: " + playNum);

        layout.getLayoutParams().height += buttonSize;

        //set the properties for button
        Button playBtn = new Button(this);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //int resID = getResources().getIdentifier(String.valueOf(prevId), "id", getPackageName());
        if (playNum != 1) {
            for (Button button : buttonArrayList) {
                if (button.getId() != buttonArrayList.size()) {
                    RelativeLayout.LayoutParams tempRl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    tempRl.addRule(RelativeLayout.BELOW, button.getId() + 1);
                    button.setLayoutParams(tempRl);
                }
            }
            //rl.addRule(RelativeLayout.ABOVE, prevId);
        }

        String temp = "Play " + String.valueOf(playNum) + " -  " + play;
        playBtn.setText(temp);
        playBtn.setId(playNum);
        playBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        playBtn.setHeight(buttonSize);
        ((RelativeLayout) findViewById(R.id.scrollRelLayout)).addView(playBtn, rl);
        playBtn.setLayoutParams(rl);
        if (playNum != 1) {
            RelativeLayout.LayoutParams tempRl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tempRl.addRule(RelativeLayout.BELOW, playNum);
            buttonArrayList.get(buttonArrayList.size() - 1).setLayoutParams(tempRl);
        }
        playBtn.setOnClickListener(this);
        buttonArrayList.add(playBtn);
    }

    private void removeButton(Integer playNum) {
        int id = playNum - 1;
        Button btn = buttonArrayList.get(id);
        btn.setVisibility(View.GONE);
        buttonArrayList.remove(id);
        Log.d(logtag, "ID: " + playNum + ", " + id);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.scrollRelLayout);
        layout.getLayoutParams().height -= buttonSize;
    }

    private String analyzePlay(String spokenText) {
        String temp = spokenText.toLowerCase();
        Log.d(logtag, temp);
        int tempFieldPos = -1;

        prevDown = downNum;
        prevDist = dist;

        String[] words = temp.split("[-\\s+]");

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

            if (Objects.equals(nextWord, "penalty") || Objects.equals(nextWord, "penalties")) {
                playType = "Penalty";
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

            if (curWord.equals("fumble") || curWord.equals("fumbles") || curWord.equals("fumbled") || curWord.equals("fully") || curWord.equals("tumblr")) {
                fumbleFlag=true;
                continue;
            }

            if (curWord.equals("sack") || curWord.equals("sacks") || curWord.equals("sacked") || curWord.equals("sac")) {
                tackleflag=true;
                sackflag=true;
                playType = "Run";
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
                    || curWord.equals("brand") || curWord.equals("train") || curWord.equals("ring") || curWord.equals("randy") || curWord.equals("carry") || curWord.equals("carries")
                    || curWord.equals("carried") || curWord.equals("rim")) {
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
                tempFieldPos = intParse(prevWord);
                if (oppTerFlag%2==1) {
                    tempFieldPos = fieldSize - tempFieldPos;
                }
                if (returnFlag > 0 && returnYds == 0 && !playType.equals("Punt")) {
                    if (fieldPos != 0)
                        returnYds = tempFieldPos - (fieldSize - fieldPos);
                    else
                        returnYds = tempFieldPos;
                }
                else if (gnLs == 0) {
                    gnLs = tempFieldPos - fieldPos;
                }
                if (!playType.equals("Penalty"))
                    fieldPos = tempFieldPos;

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
                    if (lossFlag) {
                        gnLs *= -1;
                    }
                    if (!playType.equals("Penalty"))
                        fieldPos += gnLs;
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
                    if (threewordsago.equals("tackle") || threewordsago.equals("tackled") || threewordsago.equals("tackles"))
                        tacklerNumber = intParse(curWord);
                    else
                        defNumber = intParse(curWord);
                }
                else if (tackleflag || (recNumber != -1)) {
                    tacklerNumber = intParse(curWord);
                }
                else if (recFlag) {
                    recNumber = intParse(curWord);
                }
                else {
                    playerNumber = intParse(curWord);
                }
            }

            if (Objects.equals(curWord, "quarter") || Objects.equals(curWord, "water") || Objects.equals(nextWord, "court") ||
                    (curWord.equals("game") && prevWord.equals("of") && twowordsago.equals("end"))) {
                playType = "End";
                continue;
            }

            if (Objects.equals(curWord, "touchdown") || (Objects.equals(curWord, "touch") && Objects.equals(nextWord, "down"))) {
                touchdownFlag = true;
                if (returnFlag == 0) {
                    gnLs = fieldSize - fieldPos;
                }
                fieldPos = fieldSize;
            }

            if (curWord.equals("safety") || curWord.equals("safe") || curWord.equals("safeties") || curWord.equals("saved") || curWord.equals("save")) {
                safetyFlag = true;
                if (gnLs == 0) {
                    gnLs = 0 - fieldPos;
                }
            }

            if (Objects.equals(curWord, "return") || Objects.equals(curWord, "returned") || Objects.equals(curWord, "returns")) {
                returnFlag++;
                twowordsago = prevWord;
                prevWord = curWord;
                continue;
            }

            if (Objects.equals(curWord, "punt") || Objects.equals(curWord, "punts") || Objects.equals(curWord, "punted") || Objects.equals(curWord, "punch") || curWord.equals("put")
                    || curWord.equals("points") || curWord.equals("hunt") || curWord.equals("hunts") || curWord.equals("bunt") || curWord.equals("bunts")
                    || curWord.equals("hunted") || curWord.equals("bunted") || curWord.equals("punk") || curWord.equals("want") || curWord.equals("petcock")) {

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

            if (Objects.equals(curWord, "fieldgoal") || curWord.equals("filgoal") || (Objects.equals(curWord, "field") && Objects.equals(nextWord, "goal"))) {
                playType = "Field Goal";
                fgDistance = (fieldSize - fieldPos) + 17;
            }

            if (Objects.equals(curWord, "conversion") || ((Objects.equals(prevWord, "point"))
                    && ((Objects.equals(twowordsago, "two")) || (Objects.equals(twowordsago, "2"))))) {
                playType = "2 Pt. Conversion";
            }

            if (Objects.equals(curWord, "pat") || (Objects.equals(curWord, "point") && Objects.equals(nextWord, "after")) ||
                    (curWord.equals("extra") && nextWord.equals("point"))) {
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
                oppTerFlag++;
            }

            if ((Objects.equals(curWord, "down") && (Objects.equals(prevWord, "first") || Objects.equals(curWord, "1st")))) {
                if (playType.equals("Penalty")) {
                    downNum = 1;
                    dist = 10;
                }
            }

            threewordsago = twowordsago;
            twowordsago = prevWord;
            prevWord = curWord;
        }

        Log.d(logtag, "fieldPos: " + fieldPos + "\n" + "ydLn: " + ydLn + "\n" + "gnLs: " + gnLs + "\n" + "fieldSize: " + fieldSize);

        if (((fieldPos) > fieldSize) && !playType.equals("PAT") && !playType.equals("Field Goal") && !playType.equals("2 Pt. Conversion") && (gnLs < 101)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder
                    .setMessage("Did a touchdown occur?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(getApplicationContext(), "Touchdown Confirmed", Toast.LENGTH_SHORT).show();
                            touchdownFlag = true;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            fieldPos = 99;
                            ydLn = calcYdLn(fieldPos);
                            touchdownFlag = false;
                            dialog.cancel();
                        }
                    })
                    .show();
        }
        else if (fieldPos < 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder
                    .setMessage("Did a safety occur?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(getApplicationContext(), "Safety Confirmed", Toast.LENGTH_SHORT).show();
                            safetyFlag = true;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            fieldPos = 1;
                            safetyFlag = false;
                            ydLn = calcYdLn(fieldPos);
                            dialog.cancel();
                        }
                    })
                    .show();
        }
        else {
            //sets ydLn to neg if in own territory, pos if in opponents territory
            if (playType.equals("Penalty")) {
                if (!defensivePenalty)
                    gnLs *= -1;
                if (tempFieldPos == -1) {
                    fieldPos += gnLs;
                    dist -= gnLs;
                }
                else {
                    dist += fieldPos - tempFieldPos;
                    fieldPos = tempFieldPos;
                }
            }
            ydLn = calcYdLn(fieldPos);
        }

        Log.d(logtag, "fieldPos: " + fieldPos + "\n" + "ydLn: " + ydLn + "\n" + "gnLs: " + gnLs + "\n" + "fieldSize: " + fieldSize);

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
        possFlag = homeTeam.getOnOffense();
    }

    private void flipField() {
        fieldPos = fieldSize - fieldPos;
        ydLn = calcYdLn(fieldPos);
    }

    private int calcYdLn(int fieldPos) {
        int yardline = ydLn;
        if ((fieldPos < (fieldSize/2)) && (fieldPos >= 0) && (oppTerFlag== 1)) {
            yardline = fieldPos;
            fieldPos = (fieldSize/2) - (yardline - (fieldSize/2));
        }
        if ((fieldPos < (fieldSize/2)) && (fieldPos >= 0))
            yardline = fieldPos * -1;
        else if (fieldPos >= (fieldSize/2))
            yardline = (fieldSize/2) - (fieldPos - (fieldSize/2));

        return yardline;
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
                    playResult =  "Number " + String.valueOf(playerNumber) + " pass intercepted by number " + String.valueOf(defNumber);
                    if (!updateFlag)
                        changePossession();
                    downNum = 1;
                    dist = 10;
                }
                else {
                    if (recNumber == -1)
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete";
                    else
                        playResult = "Number " + String.valueOf(playerNumber) + " pass incomplete to number " + String.valueOf(recNumber);
                }
                break;
            case "Run":
                playResult = "Number " + String.valueOf(playerNumber) + " ran" + " for " + String.valueOf(gnLs) + " yards";
                break;
            case "Kickoff":
                downNum=0;
                if ((returnFlag > 0) && !touchdownFlag && !safetyFlag) {
                    playResult = "Number " + String.valueOf(playerNumber) + " returned the kickoff " + String.valueOf(returnYds) + " yards to the " +
                            String.valueOf(ydLn) + " yardline";
                    downNum = 1;
                    dist = 10;
                }
                else if ((returnFlag > 0) && (touchdownFlag)) {
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
                if (!updateFlag)
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
                    //fair catch or touchback or something
                    if (touchbackFlag) {
                        playResult = "Punt goes for a touchback";
                    }
                    else if (faircatchFlag) {
                        playResult = "Punt caught for a fair catch at the " + String.valueOf(ydLn) + " yardline";
                    }
                    else {
                        playResult = "Punt downed at the " + ydLn + " yardline";
                        gnLs = 0;
                        returnYds = 0;
                    }
                    downNum = 1;
                    dist = 10;
                }
                if (!updateFlag)
                    changePossession();
                break;
            case "Field Goal":
                if (fgMadeFlag) {
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was good";
                    if (!updateFlag) {
                        if (homeTeam.getOnOffense()) {
                            homeTeam.setTeamScore(homeTeam.getTeamScore() + 3);
                        } else {
                            awayTeam.setTeamScore(awayTeam.getTeamScore() + 3);
                        }
                    }
                    if (fieldSize == 80) {
                        downNum = 1;
                        dist = 10;
                        fieldPos = 15;
                        ydLn = calcYdLn(fieldPos);
                        if (!updateFlag)
                            changePossession();
                    }
                    else {
                        downNum = 0;
                        dist = 0;
                    }
                }
                else {
                    playResult = "The " + String.valueOf(fgDistance) + "-yard field goal was no good";
                    if (!updateFlag) {
                        changePossession();
                        flipField();
                    }
                    downNum = 1;
                    dist = 10;
                }
                break;
            case "PAT":
                if (fgMadeFlag) {
                    playResult = "The PAT was good";
                    if (!updateFlag) {
                        if (homeTeam.getOnOffense()) {
                            homeTeam.setTeamScore(homeTeam.getTeamScore() + 1);
                        } else {
                            awayTeam.setTeamScore(awayTeam.getTeamScore() + 1);
                        }
                    }
                }
                else
                    playResult = "The PAT was no good";

                if (fieldSize == 80) {
                    downNum = 1;
                    dist = 10;
                    fieldPos = 15;
                    if (!updateFlag)
                        changePossession();
                }
                else {
                    downNum = 0;
                    dist = 0;
                    fieldPos = 0;
                }
                ydLn = calcYdLn(fieldPos);
                break;
            case "2 Pt. Conversion":
                playResult = "2 Pt. Conversion is ";
                if (fgMadeFlag && !updateFlag) {
                    if (homeTeam.getOnOffense()) {
                        homeTeam.setTeamScore(homeTeam.getTeamScore() + 2);
                    } else {
                        awayTeam.setTeamScore(awayTeam.getTeamScore() + 2);
                    }
                    playResult += "good";
                }
                else
                    playResult += "no good";

                if (fieldSize == 80) {
                    downNum = 1;
                    dist = 10;
                    fieldPos = 15;
                    if (!updateFlag)
                        changePossession();
                }
                else {
                    downNum = 0;
                    dist = 0;
                    fieldPos = 0;
                }
                ydLn = calcYdLn(fieldPos);
                break;
            case "Penalty":
                if (safetyFlag) {
                    playResult = "Penalty in endzone";
                    break;
                }
                playResult = String.valueOf(Math.abs(gnLs) + " yard penalty");
                if (!defensivePenalty) {
                    playResult = playResult + " on the offense";
                }
                else
                    playResult = playResult + " on the defense";

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
                        //asking who is kicking off to begin the second half

                        AlertDialog.Builder possBuilder = new AlertDialog.Builder(this);

                        possBuilder
                                .setMessage("Which team is kicking off to begin the second half?")
                                .setPositiveButton(hometeamname, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (!possFlag)
                                            changePossession();
                                        Toast t = Toast.makeText(getApplicationContext(), hometeamname + " will kick to " + awayteamname + " to begin the second half", Toast.LENGTH_SHORT);
                                        t.show();
                                    }
                                })
                                .setNegativeButton(awayteamname, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (possFlag)
                                            changePossession();
                                        Toast t = Toast.makeText(getApplicationContext(), awayteamname + " will kick to " + hometeamname + " to begin the second half", Toast.LENGTH_SHORT);
                                        t.show();
                                    }
                                })
                                .show();

                        break;
                    case 3:
                        playResult = "End of 3rd Quarter";
                        break;
                    case 4:
                        playResult = "End of Game";
                        endOfGame = true;
                        endOfGameDialog();
                        break;
                    default:
                        playResult = "The Game is already over";
                        break;
                }
                if (qtr != 2) {
                    Toast t = Toast.makeText(getApplicationContext(), playResult, Toast.LENGTH_SHORT);
                    t.show();
                }
                if (qtr < 4)
                    qtr++;
                break;
            default:
                invalidPlay = true;
                break;
        }
        if ((playType.equals("Pass") || playType.equals("Run")) && !interceptionFlag && !fumbleRecFlag && !updateFlag) {
            downNum++;
            dist -= gnLs;
        }

        if (dist <= 0) {
            downNum = 1;
            dist = 10;
        }
        if (downNum > 4) {
            if (!updateFlag) {
                changePossession();
                flipField();
            }
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
            if (!updateFlag)
                changePossession();
            downNum = 1;
            dist = 10;
        }

        if (returnFlag>0 && !(playType.equals("Kickoff") || playType.equals("Punt"))) {
            playResult = playResult + ", returned for " + String.valueOf(returnYds) + " yards";
            if (!touchdownFlag) {
                playResult = playResult + " to the " + String.valueOf(ydLn) + " yardline";
            }
        }

        if (touchdownFlag) {
            ydLn = 0;
            downNum = 0;
            dist = 3;
            playResult = playResult + " for a TOUCHDOWN!";
            if (!updateFlag) {
                if (possFlag) {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 6);
                } else {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 6);
                }
            }
        }

        if (safetyFlag) {
            playResult += ", result is a SAFETY!";

            if (fieldSize == 80) {
                downNum = 1;
                dist = 10;
                fieldPos = 15;
                if (!updateFlag)
                    changePossession();
            }
            else {
                downNum = 0;
                dist = 0;
                fieldPos = 0;
            }
            ydLn = calcYdLn(fieldPos);
            if (!updateFlag) {
                if (possFlag) {
                    awayTeam.setTeamScore(awayTeam.getTeamScore() + 2);
                } else {
                    homeTeam.setTeamScore(homeTeam.getTeamScore() + 2);
                }
            }
        }

        if (fieldPos > (fieldSize-10)  && (fieldPos < fieldSize) && (dist > (fieldSize - fieldPos))) {
            dist = fieldSize - fieldPos;
        }

        if ((gnLs>100) || (returnYds>100) || (playerNumber>99) || (recNumber>99))
            invalidPlay = true;

        return playResult;
    }

    /**
     * PLAY CLASS - NESTED INSIDE GAME
     */
    public class Play {

        Integer playerNumber, recNumber, defNumber, tacklerNumber, ydLn, gnLs,  fieldPos, downNum, dist, qtr, fgDistance, playCount, returnYds,
                prevDist, prevDown, homeScore, awayScore, firstDn;
        Integer lossFlag, returnFlag, oppTerFlag;
        boolean incompleteFlag, touchdownFlag, recFlag, touchbackFlag, faircatchFlag, interceptionFlag, fumbleFlag, fumbleRecFlag, tackleflag, sackflag, fgMadeFlag,
                possFlag, safetyFlag, defensivePenalty;
        String playType, result = "", notes = "";

    }

    /**
     * TEAM CLASS - NESTED INSIDE GAME
     */
    public static class Team {

        private int teamScore;
        private String teamName;
        private boolean onOffense;
        private ArrayList<Player> players = new ArrayList<Player>();

        Team(String name, boolean offense) {
            teamName = name;
            onOffense = offense;
            teamScore = 0;
        }

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
