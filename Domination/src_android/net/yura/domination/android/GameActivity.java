package net.yura.domination.android;

import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.example.games.basegameutils.GameHelper;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import net.yura.android.AndroidMeActivity;
import net.yura.android.AndroidMeApp;
import net.yura.android.AndroidPreferences;
import net.yura.domination.BuildConfig;
import net.yura.domination.engine.Risk;
import net.yura.domination.engine.RiskUtil;
import net.yura.domination.engine.translation.TranslationBundle;
import net.yura.domination.mobile.flashgui.DominationMain;
import net.yura.domination.mobile.flashgui.MiniFlashRiskAdapter;
import net.yura.lobby.mini.MiniLobbyClient;
import net.yura.lobby.model.Game;

public class GameActivity extends AndroidMeActivity implements GameHelper.GameHelperListener,DominationMain.GooglePlayGameServices {

    private static final Logger logger = Logger.getLogger(GameActivity.class.getName());

    /**
     * this code needs to not clash with other codes such as the ones in
     * {@link GameHelper} 9001,9002
     * {@link RealTimeMultiplayer} 2,3,4
     */
    private static final int RC_REQUEST_ACHIEVEMENTS = 1;

    private GameHelper mHelper;
    private RealTimeMultiplayer realTimeMultiplayer;

    private String pendingAchievement;
    private boolean pendingShowAchievements;
    private net.yura.lobby.model.Game pendingStartGameGooglePlay;
    private boolean pendingSendLobbyUsername;
    private Game pendingOpenGame;

    /**
     * need to create everything owned by the activity, but the game/static objects may already exist
     */
    @Override
    protected void onSingleCreate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        DominationMain.appPreferences = new AndroidPreferences(preferences);
        System.setProperty("debug", String.valueOf(BuildConfig.DEBUG)); // Temp hack to get around http://b.android.com/52962
        super.onSingleCreate();

        mHelper = new GameHelper(this);
        mHelper.enableDebugLog(true, "DominationPlay");

        realTimeMultiplayer = new RealTimeMultiplayer(mHelper, this, new RealTimeMultiplayer.Lobby() {
            @Override
            public void createNewGame(Game game) {
                getUi().lobby.createNewGame(game);
            }
            @Override
            public void playGame(Game game) {
                getUi().lobby.playGame(game);
            }
            @Override
            public void getUsername() {
                MiniFlashRiskAdapter ui = getUi();
                if (ui.lobby != null) {
                    if (ui.lobby.whoAmI() != null) {
                        realTimeMultiplayer.setLobbyUsername(ui.lobby.whoAmI());
                    }
                    else {
                        pendingSendLobbyUsername = true;
                        logger.warning("lobby open but we do not have a username");
                    }
                }
                else {
                    pendingSendLobbyUsername = true;
                    ui.openLobby();
                }
            }
        });

        final GameHelper.GameHelperListener[] listeners = {this, realTimeMultiplayer};
        mHelper.setup(new GameHelper.GameHelperListener() {
	    @Override
	    public void onSignInSucceeded() {
		for (GameHelper.GameHelperListener listener : listeners) {
	            listener.onSignInSucceeded();
	        }
	    }
	    @Override
	    public void onSignInFailed() {
		for (GameHelper.GameHelperListener listener : listeners) {
	            listener.onSignInFailed();
	        }
	    }
	}, GameHelper.CLIENT_GAMES);

        if (preferences.getBoolean("fullscreen", getDefaultFullScreen(this))) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String gameId = intent.getStringExtra(MiniLobbyClient.EXTRA_GAME_ID);
        String options = intent.getStringExtra(MiniLobbyClient.EXTRA_GAME_OPTIONS);

        if (gameId != null) {
            Game game = new Game();
            game.setId(Integer.parseInt(gameId));
            game.setOptions(options);

            MiniFlashRiskAdapter ui = getUi();
            if (ui != null) {
                if (ui.lobby != null) {
                    if (ui.lobby.whoAmI() != null) {
                        ui.lobby.playGame(game);
                    }
                    else {
                        pendingOpenGame = game;
                        logger.warning("lobby open but we are not logged in yet");
                    }
                }
                else {
                    pendingOpenGame = game;
                    ui.openLobby();
                }
            }
            else {
        	// the game has not initialized yet
        	pendingOpenGame = game;
            }

            // as we have handled this open game request, clear it
            intent.removeExtra(MiniLobbyClient.EXTRA_GAME_ID);
            intent.removeExtra(MiniLobbyClient.EXTRA_GAME_OPTIONS);
        }
    }

    @Override
    public boolean hasPendingOpenLobby() {
        return pendingOpenGame != null;
    }

    public static boolean getDefaultFullScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL;
    }

    /**
     * This method is called only once ever for the entire app.
     */
    @Override
    public void onMidletStarted() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            DominationMain dmain = (DominationMain)AndroidMeApp.getMIDlet();
            // TODO WE ARE LEAKING THE ACTIVITY HERE!!!
            // an Activity can be created and destroyed by the system when it feels like it
            dmain.setGooglePlayGameServices(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHelper.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHelper.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        realTimeMultiplayer.onActivityResult(requestCode, resultCode, data);
        mHelper.onActivityResult(requestCode, resultCode, data);
    }

    // ----------------------------- GameHelper.GameHelperListener -----------------------------

    @Override
    public void onSignInSucceeded() {
        logger.info("onSignInSucceeded()");
        MiniFlashRiskAdapter ui = getUi();
        if (ui!=null) {
            ui.playGamesStateChanged();
        }
        if (pendingAchievement!=null) {
            unlockAchievement(pendingAchievement);
            pendingAchievement=null;
        }
        if (pendingShowAchievements) {
            pendingShowAchievements = false;
            showAchievements();
        }
        if (pendingStartGameGooglePlay != null) {
            startGameGooglePlay(pendingStartGameGooglePlay);
            pendingStartGameGooglePlay = null;
        }
    }

    @Override
    public void onSignInFailed() {
        MiniFlashRiskAdapter ui = getUi();
        if (ui!=null) {
            ui.playGamesStateChanged();
        }
        // user must have cancelled signing in, so they must not want to see achievements.
        pendingShowAchievements = false;
    }

    // ----------------------------- GooglePlayGameServices -----------------------------

    @Override
    public void beginUserInitiatedSignIn() {
        mHelper.beginUserInitiatedSignIn();
    }

    @Override
    public void signOut() {
        mHelper.signOut();
        mHelper.getGamesClient().unregisterInvitationListener();
    }

    @Override
    public boolean isSignedIn() {
        return mHelper.isSignedIn();
    }

    @Override
    public void startGameGooglePlay(net.yura.lobby.model.Game game) {
        logger.info("startGameGooglePlay");
        if (isSignedIn()) {
            realTimeMultiplayer.startGameGooglePlay(game);
        }
        else {
            logger.info("redirecting to sign in");
            pendingStartGameGooglePlay = game;
            beginUserInitiatedSignIn();
        }
    }

    @Override
    public void setLobbyUsername(String username) {
        if (pendingSendLobbyUsername) {
            pendingSendLobbyUsername = false;
            realTimeMultiplayer.setLobbyUsername(username);
        }
        if (pendingOpenGame != null) {
            getUi().lobby.playGame(pendingOpenGame);
            pendingOpenGame = null;
        }
    }

    @Override
    public void gameStarted(int id) {
        realTimeMultiplayer.gameStarted(id);
    }

    @Override
    public void showAchievements() {
        if (isSignedIn()) {
            startActivityForResult(mHelper.getGamesClient().getAchievementsIntent(), RC_REQUEST_ACHIEVEMENTS);
        }
        else {
            pendingShowAchievements = true;
            beginUserInitiatedSignIn();
        }
    }

    @Override
    public void unlockAchievement(String id) {
        if (isSignedIn()) {
            mHelper.getGamesClient().unlockAchievement(id);
        }
        else {
            pendingAchievement = id;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ResourceBundle resb = TranslationBundle.getBundle();
                    new AlertDialog.Builder(GameActivity.this)
                    .setTitle(resb.getString("achievement.achievementUnlocked"))
                    .setMessage(resb.getString("achievement.signInToSave"))
                    .setPositiveButton(resb.getString("achievement.signInToSave.ok"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            beginUserInitiatedSignIn();
                        }
                     })
                    .setNegativeButton(resb.getString("achievement.signInToSave.cancel"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                     })
                     .show();
                }
            });
        }
    }

    // ----------------------------- GAME SAVE -----------------------------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        logger.info("[GameActivity] onSaveInstanceState");
        // if the system wants to kill our activity we need to save the game if we have one
        if ( shouldSaveGame() ) {
            logger.info("[GameActivity] SAVING TO AUTOSAVE");
            // in game thread, we do not want to do it there as we will not know when its finished
            //getRisk().parser("savegame "+getAutoSaveFileURL());

            try {
                final Risk risk = getRisk();
                final File autoSaveFile = DominationMain.getAutoSaveFile();
                final File tempSaveFile = new File(autoSaveFile.getParent(),autoSaveFile.getName()+".part");

                risk.parserAndWait("savegame "+DominationMain.getAutoSaveFile()+".part");
                // if we may have closed the game while also closing the activity
                // the save probably failed, and the rename will fail for sure.
                if ( shouldSaveGame() ) {
                    RiskUtil.rename(tempSaveFile, autoSaveFile);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.info("[GameActivity] onPause");
        // if everything is shut down and there is no current game
        // make sure we clean up so no game is loaded on next start

        // TODO we may have been paused WHILE the game is starting,
        // and then we may end up deleting the file we are trying to load.
        if ( !shouldSaveGame() ) {
            File file = DominationMain.getAutoSaveFile();
            if (file.exists()) {
                logger.info("[GameActivity] DELETING AUTOSAVE");
                file.delete();
            }
        }
    }

    private boolean shouldSaveGame() {
        Risk risk = getRisk();
        return risk!=null && risk.getGame()!=null && risk.getLocalGame();
    }

    private Risk getRisk() {
        DominationMain dmain = (DominationMain)AndroidMeApp.getMIDlet();
        return dmain==null?null:dmain.risk;
    }

    private MiniFlashRiskAdapter getUi() {
        DominationMain dmain = (DominationMain)AndroidMeApp.getMIDlet();
        return dmain==null?null:dmain.adapter;
    }
}
