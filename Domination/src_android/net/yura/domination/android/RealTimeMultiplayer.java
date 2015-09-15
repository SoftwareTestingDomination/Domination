package net.yura.domination.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.yura.android.LoadingDialog;
import net.yura.domination.engine.RiskUtil;
import net.yura.domination.engine.translation.TranslationBundle;
import net.yura.lobby.client.ProtoAccess;
import net.yura.lobby.model.Game;
import net.yura.lobby.model.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.OnInvitationsLoadedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeReliableMessageSentListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.example.games.basegameutils.GameHelper;

/**
 * if 2 people (B and Q) create games with 1 friend each (C and G) and 2 auto-match players each:
 *
 * B players=[Q, C, G, B] creator=B GameX
 * Q players=[Q, C, B, G] creator=Q GameY
 * C players=[Q, C, G, B] creator=B
 * G players=[Q, C, B, G] creator=Q
 *
 * C sends name to B
 * G sends name to Q
 * B sends to everyone he is the creator
 * Q sends to everyone he is the creator
 * as B is less then Q, Q sends its name and the name it received from G to B
 * if Q only got the name of G after it found out B is a creator it will now send G's name to B
 * now B has everyones name and starts the game.
 */
public class RealTimeMultiplayer implements GameHelper.GameHelperListener {

    private static final int RC_SELECT_PLAYERS = 2;
    private static final int RC_CREATOR_WAITING_ROOM = 3;
    private static final int RC_JOINER_WAITING_ROOM = 4;

    private static final int GOOGLE_PLAY_GAME_MIN_OTHER_PLAYERS = 1;

    public static final String EXTRA_SHOW_AUTOMATCH = "com.google.android.gms.games.SHOW_AUTOMATCH";

    private static final Logger logger = Logger.getLogger(RealTimeMultiplayer.class.getName());

    // we want to be able to send Game objects, but we dont care about the GameTypes, as its always going to be the same game
    private ProtoAccess encodeDecoder = new ProtoAccess(new ProtoAccess.ObjectProvider() {
        public Object getObjectId(Object var1) {
            return -1;
        }
        public Object getObjetById(Object var1, Class var2) {
            return null;
        }
    });
    private Activity activity;
    private GameHelper mHelper;
    private Lobby lobby;

    private String gameCreator;
    private Room gameRoom;
    private Game lobbyGame;

    interface Lobby {
        void createNewGame(Game game);
        void playGame(Game gameId);
        void getUsername();
    }

    public RealTimeMultiplayer(GameHelper helper, Activity activity, Lobby lobby) {
        mHelper = helper;
        this.activity = activity;
        this.lobby = lobby;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                handlePlayersSelected(resultCode, data);
                break;
            case RC_CREATOR_WAITING_ROOM:
                handleReturnFromWaitingRoom(resultCode, true /* isCreator */);
                break;
            case RC_JOINER_WAITING_ROOM:
                handleReturnFromWaitingRoom(resultCode, false /* not creator */);
                break;
        }
    }

    @Override
    public void onSignInSucceeded() {
        logger.info("invitationId: " + mHelper.getInvitationId());
        if (mHelper.getInvitationId() != null) {
            acceptInvitation(mHelper.getInvitationId());
        }
        else {
            // there is a bug in GooglePlayGameServicesFroyo that mHelper.getInvitationId() returns null
            // even when there is a invitation, so we use this method instead to get it.
            // OR we may go into the app directly instead of clicking on the notification.
            mHelper.getGamesClient().loadInvitations(new OnInvitationsLoadedListener() {
                @Override
                public void onInvitationsLoaded(int statusCode, InvitationBuffer buffer) {
                    logger.info("onInvitationsLoaded: "+statusCode+" "+buffer.getCount()+" "+buffer);
                    for (Invitation invitation : buffer) {
                        logger.info("onInvitationsLoaded invitation: "+getCurrentPlayerState(invitation)+" "+invitation);
                        createAcceptDialog(invitation).show();
                    }
                    // LOL who closes a buffer!
                    buffer.close();
                }
            });
        }
        mHelper.getGamesClient().registerInvitationListener(new OnInvitationReceivedListener() {
            @Override
            public void onInvitationReceived(final Invitation invitation) {
                logger.info("Invitation received from: " + invitation.getInviter());
                createAcceptDialog(invitation).show();
            }
        });
    }

    @Override
    public void onSignInFailed() {
        // dont care
    }

    private int getCurrentPlayerState(Invitation invitation) {
        return getMe(invitation.getParticipants()).getStatus();
    }

    private Participant getMe(List<Participant> participants) {
        String myId = mHelper.getGamesClient().getCurrentPlayerId();
        for (Participant participant : participants) {
            Player player = participant.getPlayer();
            if (player != null && myId.equals(player.getPlayerId())) {
                return participant;
            }
        }
        throw new RuntimeException("me not found");
    }

    public void startGameGooglePlay(Game game) {
        logger.info("starting player selection");
        lobbyGame = game;
        gameCreator = null;
        if (lobbyGame.getNumOfPlayers() != 1) {
            throw new RuntimeException("should only have creator "+game.getPlayers());
        }

        Intent intent = mHelper.getGamesClient().getSelectPlayersIntent(GOOGLE_PLAY_GAME_MIN_OTHER_PLAYERS, game.getMaxPlayers() - 1);
        intent.putExtra(EXTRA_SHOW_AUTOMATCH, false);
        activity.startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    private void handlePlayersSelected(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            logger.info("Player selection failed. "+resultCode);
            return;
        }

        openLoadingDialog("mainmenu.googlePlayGame.waitRoom");

        ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
        // get auto-match criteria
        int minAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

        logger.info("Players selected. Creating room. "+invitees+" "+minAutoMatchPlayers+" "+maxAutoMatchPlayers);

        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(
            new BaseRoomUpdateListener() {
                @Override
                public void onRoomCreated(int statusCode, Room room) {
                    super.onRoomCreated(statusCode, room);
                    closeLoadingDialog();
                    if (statusCode != GamesClient.STATUS_OK) {
                        String error = "onRoomCreated failed. "+statusCode+" "+getErrorString(statusCode);
                        logger.warning(error);
                        toast(error);
                        return;
                    }
                    gameRoom = room;
                    logger.info("Starting waiting room activity.");
                    activity.startActivityForResult(mHelper.getGamesClient().getRealTimeWaitingRoomIntent(room, 1), RC_CREATOR_WAITING_ROOM);
                }
            })
            .setRoomStatusUpdateListener(new BaseRoomStatusUpdateListener(){
                @Override
                public void onRoomUpdated(Room room) {
                    gameRoom = room;
                }
            })
            .setMessageReceivedListener(new RealTimeMessageReceivedListener() {
                @Override
                public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
                    onMessageReceived(realTimeMessage);
                }
            })
            .addPlayersToInvite(invitees);

        if (minAutoMatchPlayers > 0) {
            Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }

        // The variant has to be positive, or else it will throw an Exception.
        roomConfigBuilder.setVariant(lobbyGame.getOptions().hashCode() & 0x7FFFFFFF);

        mHelper.getGamesClient().createRoom(roomConfigBuilder.build());
        logger.info("Room created, waiting for it to be ready");
    }

    private void onMessageReceived(RealTimeMessage realTimeMessage) {
        byte[] data = realTimeMessage.getMessageData();
        try {
            Message message = (Message)encodeDecoder.load(new ByteArrayInputStream(data), data.length);
            onMessageReceived(message, realTimeMessage.getSenderParticipantId());
        }
        catch (IOException ex) {
            logger.log(Level.WARNING, "can not decode", ex);
        }
    }

    private void onMessageReceived(Message message, String from) {
        logger.info("Room message received: " + message);
        String command = message.getCommand();
        if (ProtoAccess.REQUEST_JOIN_GAME.equals(command)) {
            String name = (String)message.getParam();

            if (lobbyGame != null) {
                lobbyGame.getPlayers().add(new net.yura.lobby.model.Player(name, 0));

                int joined = getParticipantStatusCount(Participant.STATUS_JOINED);
                logger.info("new player joined: "+name+" "+lobbyGame.getNumOfPlayers()+"/"+joined+"/"+gameRoom.getParticipantIds().size());
                if (lobbyGame.getNumOfPlayers() == joined) {
                    // TODO can we be not inside lobby???
                    // TODO can we be not logged in?
                    // in case we decided to start with less then the max number of human players
                    // we need to update the max number to the current number, so no one else can join
                    lobbyGame.setMaxPlayers(lobbyGame.getNumOfPlayers());
                    lobby.createNewGame(lobbyGame);
                }
            }
            else {
        	if (gameCreator == null) {
        	    throw new RuntimeException("someone sent me a lobby username, but i dont know what to do");
        	}
        	// we got a username but someone else is the real creator, forward the username to them.
        	sendLobbyUsername(name, gameCreator);
            }
        }
        else if (ProtoAccess.COMMAND_GAME_STARTED.equals(command)) {
            Object param = message.getParam();
            lobby.playGame((Game) param);
        }
        else if (ProtoAccess.REQUEST_HELLO.equals(command)) {
            String creator = (String) message.getParam();
            if (lobbyGame != null) {
        	String myPID = getMe(gameRoom.getParticipants()).getParticipantId();
        	if (creator.compareTo(myPID) == 0) {
        	    throw new RuntimeException("did we just say hello to ourselves?");
        	}
        	else if (creator.compareTo(myPID) < 0) {
        	    gameCreator = creator;
        	    Collection<net.yura.lobby.model.Player> players = lobbyGame.getPlayers();
        	    for (net.yura.lobby.model.Player player : players) {
        		sendLobbyUsername(player.getName(), gameCreator);
        	    }
        	    lobbyGame = null;
        	}
        	// we are the main creator, dont need to do anything
            }
            // we are not a creator, so dont care who is, as long as we send our name to one of the
            // creators it should be ok.
        }
        else {
            logger.warning("unknown command "+message);
        }
    }

    private int getParticipantStatusCount(int status) {
        int count=0;
        for (String id: gameRoom.getParticipantIds()) {
            if (gameRoom.getParticipantStatus(id) == status) {
                count++;
            }
        }
        return count;
    }

    private void handleReturnFromWaitingRoom(int resultCode, boolean isCreator) {
        logger.info("Returning from waiting room. isCreator="+isCreator);
        if (resultCode != Activity.RESULT_OK) {
            logger.info("Room was cancelled, result code = " + resultCode);
            return;
        }
        String myPID = getMe(gameRoom.getParticipants()).getParticipantId();
        logger.info("Room ready. me="+myPID+" "+gameRoom.getParticipantIds()+" creator="+gameRoom.getCreatorId()+" "+lobbyGame+" "+isCreator);
        openLoadingDialog("mainmenu.googlePlayGame.waitGame");
        if (isCreator) {
            if (gameRoom.getAutoMatchCriteria() != null) {
                // send a message to everyone that i think i am the creator.
                List<String> participants = gameRoom.getParticipantIds();
                Message message = new Message();
                message.setCommand(ProtoAccess.REQUEST_HELLO);
                message.setParam(myPID);
                for (String participant : participants) {
                    if (!participant.equals(myPID)) {
                        sendMessage(message, participant);
                    }
                }
            }
        }
        else {
            // send username to any of the game creator, they will know what to do with it.
            lobby.getUsername();
        }
    }

    public void setLobbyUsername(String username) {
        sendLobbyUsername(username, gameRoom.getCreatorId());
    }

    private void sendLobbyUsername(String username, String creator) {
        logger.info("Sending ID to creator. "+username+" "+creator);

        Message message = new Message();
        message.setCommand(ProtoAccess.REQUEST_JOIN_GAME);
        message.setParam(username);

        sendMessage(message, creator);
    }

    public void gameStarted(int id) {
        logger.info("lobby gameStarted " + id + " " + gameRoom + " " + lobbyGame);
        if (gameRoom != null) {
            Message message = new Message();
            message.setCommand(ProtoAccess.COMMAND_GAME_STARTED);
            lobbyGame.setId(id);
            message.setParam(lobbyGame);

            List<String> participants = gameRoom.getParticipantIds();
            for (String participant : participants) {
                sendMessage(message, participant);
            }
        }
    }

    void sendMessage(Message message, String recipientParticipantId) {
	
	// Play Games throws a error if i tell it to send a message to myself
	if (recipientParticipantId.equals(getMe(gameRoom.getParticipants()).getParticipantId())) {
	    onMessageReceived(message, recipientParticipantId);
	}
	else {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream( encodeDecoder.computeAnonymousObjectSize(message) );
            try {
                encodeDecoder.save(bytes, message);
            }
            catch (IOException ex) {
                throw new RuntimeException("can not encode", ex);
            }
            byte[] data = bytes.toByteArray();

            mHelper.getGamesClient().sendReliableRealTimeMessage(new RealTimeReliableMessageSentListener() {
                @Override
                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientId) {
                    logger.info(String.format("Message %d sent (%d) to %s", tokenId, statusCode, recipientId));
                }
            }, data, gameRoom.getRoomId(), recipientParticipantId);
	}
    }

    private AlertDialog createAcceptDialog(Invitation invitation) {
        ResourceBundle resb = TranslationBundle.getBundle();
        String title = resb.getString("mainmenu.googlePlayGame.acceptGame");
        String message = RiskUtil.replaceAll(resb.getString("mainmenu.googlePlayGame.invited"), "{0}", invitation.getInviter().getDisplayName());
        String accept = resb.getString("mainmenu.googlePlayGame.accept");
        String reject = resb.getString("mainmenu.googlePlayGame.reject");
        final String invitationId = invitation.getInvitationId();
        return new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        acceptInvitation(invitationId);
                    }
                })
                .setNegativeButton(reject, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mHelper.getGamesClient().declineRoomInvitation(invitationId);
                    }
                })
                .create();
    }

    private void acceptInvitation(String invitationId) {
        openLoadingDialog("mainmenu.googlePlayGame.waitRoom");
        lobbyGame = null;
        gameCreator = null;

        mHelper.getGamesClient().joinRoom(RoomConfig.builder(
                new BaseRoomUpdateListener() {
                    @Override
                    public void onJoinedRoom(int statusCode, Room room) {
                        super.onJoinedRoom(statusCode, room);
                        closeLoadingDialog();
                        if (statusCode != GamesClient.STATUS_OK) {
                            String error = "onJoinedRoom failed. "+statusCode+" "+getErrorString(statusCode);
                            logger.warning(error);
                            toast(error);
                            return;
                        }
                        gameRoom = room;
                        logger.info("Starting waiting room activity as joiner.");
                        activity.startActivityForResult(mHelper.getGamesClient().getRealTimeWaitingRoomIntent(room, 1), RC_JOINER_WAITING_ROOM);
                    }
                })
                .setRoomStatusUpdateListener(new BaseRoomStatusUpdateListener() {
                    @Override
                    public void onRoomUpdated(Room room) {
                        gameRoom = room;
                    }
                })
                .setMessageReceivedListener(new RealTimeMessageReceivedListener() {
                    @Override
                    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
                        onMessageReceived(realTimeMessage);
                    }
                })
                .setInvitationIdToAccept(invitationId)
                .build());
    }

    private void closeLoadingDialog() {
        Intent intent = new Intent(activity, LoadingDialog.class);
        intent.putExtra(LoadingDialog.PARAM_COMMAND, "hide");
        activity.startActivity(intent);
    }

    private void openLoadingDialog(String messageName) {
        Intent intent = new Intent(activity, LoadingDialog.class);
        intent.putExtra(LoadingDialog.PARAM_MESSAGE, TranslationBundle.getBundle().getString(messageName));
        intent.putExtra(LoadingDialog.PARAM_CANCELLABLE, true);
        activity.startActivity(intent);
    }

    void toast(String text) {
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
    }

    static String getErrorString(int statusCode) {
        switch(statusCode) {
            case GamesClient.STATUS_OK: return "OK"; // 0
            case GamesClient.STATUS_INTERNAL_ERROR: return "INTERNAL_ERROR"; // 1
            case GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED: return "CLIENT_RECONNECT_REQUIRED"; // 2
            case GamesClient.STATUS_NETWORK_ERROR_STALE_DATA: return "NETWORK_ERROR_STALE_DATA"; // 3
            case GamesClient.STATUS_NETWORK_ERROR_NO_DATA: return "NETWORK_ERROR_NO_DATA"; // 4
            case GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED: return "NETWORK_ERROR_OPERATION_DEFERRED"; // 5
            case GamesClient.STATUS_NETWORK_ERROR_OPERATION_FAILED: return "NETWORK_ERROR_OPERATION_FAILED"; // 6
            case GamesClient.STATUS_LICENSE_CHECK_FAILED: return "LICENSE_CHECK_FAILED"; // 7
            case 8: return "APP_MISCONFIGURED";

            case GamesClient.STATUS_ACHIEVEMENT_UNLOCK_FAILURE: return "ACHIEVEMENT_UNLOCK_FAILURE"; // 3000
            case GamesClient.STATUS_ACHIEVEMENT_UNKNOWN: return "ACHIEVEMENT_UNKNOWN"; // 3001
            case GamesClient.STATUS_ACHIEVEMENT_NOT_INCREMENTAL: return "ACHIEVEMENT_NOT_INCREMENTAL"; // 3002
            case GamesClient.STATUS_ACHIEVEMENT_UNLOCKED: return "ACHIEVEMENT_UNLOCKED"; // 3003

            case GamesClient.STATUS_MULTIPLAYER_ERROR_CREATION_NOT_ALLOWED: return "MULTIPLAYER_ERROR_CREATION_NOT_ALLOWED"; // 6000
            case GamesClient.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER: return "MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER"; // 6001
            case 6002: return "MULTIPLAYER_ERROR_INVALID_MULTIPLAYER_TYPE";
            case 6003: return "MULTIPLAYER_DISABLED";
            case 6004: return "MULTIPLAYER_ERROR_INVALID_OPERATION";

            case 6500: return "MATCH_ERROR_INVALID_PARTICIPANT_STATE";
            case 6501: return "MATCH_ERROR_INACTIVE_MATCH";
            case 6502: return "MATCH_ERROR_INVALID_MATCH_STATE";
            case 6503: return "MATCH_ERROR_OUT_OF_DATE_VERSION";
            case 6504: return "MATCH_ERROR_INVALID_MATCH_RESULTS";
            case 6505: return "MATCH_ERROR_ALREADY_REMATCHED";
            case 6506: return "MATCH_NOT_FOUND";
            case 6507: return "MATCH_ERROR_LOCALLY_MODIFIED";

            case GamesClient.STATUS_REAL_TIME_CONNECTION_FAILED: return "REAL_TIME_CONNECTION_FAILED"; // 7000
            case GamesClient.STATUS_REAL_TIME_MESSAGE_SEND_FAILED: return "REAL_TIME_MESSAGE_SEND_FAILED"; // 7001
            case GamesClient.STATUS_INVALID_REAL_TIME_ROOM_ID: return "INVALID_REAL_TIME_ROOM_ID"; // 7002
            case GamesClient.STATUS_PARTICIPANT_NOT_CONNECTED: return "PARTICIPANT_NOT_CONNECTED"; // 7003
            case GamesClient.STATUS_REAL_TIME_ROOM_NOT_JOINED: return "REAL_TIME_ROOM_NOT_JOINED"; // 7004
            case GamesClient.STATUS_REAL_TIME_INACTIVE_ROOM: return "REAL_TIME_INACTIVE_ROOM"; // 7005
            case GamesClient.STATUS_REAL_TIME_MESSAGE_FAILED: return "REAL_TIME_MESSAGE_FAILED"; // -1
            case 7007: return "OPERATION_IN_FLIGHT";
            default: return "unknown statusCode "+statusCode;
        }
    }
}
