/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.yura.domination.engine;

import net.yura.domination.engine.core.RiskGame;
import junit.framework.TestCase;
import net.yura.domination.ui.flashgui.MainMenu;

/**
 *
 * @author Jake
 */
public class RiskTest extends TestCase {
    
    public RiskTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /* Helper Functions *******************************************************/
    
    /*
     * Create a new risk game and encapsulate this stupid construction code.
     */
    private Risk NewRisk()throws InterruptedException{
        RiskUIUtil.parseArgs(new String[] {});
        final Risk risk = new Risk();
        
        /* add extra listeners used for testing */
        risk.addRiskListener(new RiskAdapter() {
            public void needInput(int s) {
                synchronized (risk) {
                        risk.notifyAll();
                        }
            }
            public void noInput() {

            }
        } );
        risk.parser("closegame");
        synchronized (risk) {
            while (risk.getGame() != null) {
                risk.wait();
            }
	}
        return risk;
    }
    
    /*
     * Add an assortment of players to the game.
     */
    private void addPlayers(Risk risk){
        risk.parser("newplayer ai easy 6 6");
        risk.parser("newplayer ai average 2 2");
        risk.parser("newplayer ai hard 1 1");
        risk.parser("newplayer ai easy 3 3");
        risk.parser("newplayer ai average 4 4");
        risk.parser("newplayer ai hard 5 5");
    }
    
    /*
     * Wait until the game processing catches up after making a new
     * game, loading a game, or maybe just if weird things are happening
     * in your tests you can't explain.
     */
    private void syncGame(Risk risk)throws InterruptedException{
        synchronized (risk) {
            //wait for the risk game to finish processing your actions
            while (risk.getGame() == null) {
                risk.wait();
            }
        }
    }
    
    /* Tests ******************************************************************/
    
    public void testRenamePlayer() throws InterruptedException{
        final Risk risk = NewRisk();
    }
    
    //test processFromUI
    
    public void testLoadGame()throws InterruptedException{
        //set up
        final Risk risk = NewRisk();
        
        
        //do stuff in the parser outside of the syncronized block
        risk.parser("loadgame test/junit/res/game1.save");
        
        //Access Risk variables inside a syncronized block
        synchronized (risk) {
            //wait for the risk game to finish processing your actions
            while (risk.getGame() == null) {
                risk.wait();
            }
            //do your checks here
            RiskGame game = risk.getGame();
            assertEquals(game.getPlayers().size(), 4);
        }
        //tear down
        risk.kill();
	risk.join();
    }
    
        public void testDeletePlayer()throws InterruptedException{
        //set up
        final Risk risk = NewRisk();
        
        risk.parser("newgame");
        
        //add the player
        risk.parser("newplayer ai easy yellow player1");
        
        //delete the player
        risk.parser("delplayer player1");
        
        //Access Risk variables inside a syncronized block
        synchronized (risk) {
            //wait for the risk game to finish processing your actions
            while (risk.getGame() == null) {
                risk.wait();
                
            }
            //do your checks here
            RiskGame game = risk.getGame();
            //check that player1 is not in the game
            assertEquals(null,game.getPlayer("player1"));
        }
        //tear down
        risk.kill();
	risk.join();
    }
    
        public void testStartMission() throws InterruptedException {
            Risk risk = NewRisk();
            risk.parser("newgame");
            risk.parser("newplayer human blue player1");
            risk.parser("newplayer human green player2");
            risk.parser("startgame mission fixed recycle");
            
            System.out.println("TimeBeforeSync: "+System.currentTimeMillis());
            syncGame(risk);
            RiskGame game = risk.getGame();
            System.out.println("TimeAfterSync: "+System.currentTimeMillis());
                
            assertEquals(RiskGame.MODE_SECRET_MISSION, game.getGameMode());
            risk.kill();
            risk.join();
        }
        
        public void testStartCapital() throws InterruptedException {
            Risk risk = NewRisk();
            risk.parser("newgame");
            risk.parser("newplayer human blue player1");
            risk.parser("newplayer human green player2");
            risk.parser("startgame capital fixed recycle");
            
            syncGame(risk);
            RiskGame game = risk.getGame();
                
            assertEquals(RiskGame.MODE_CAPITAL, game.getGameMode());
            risk.kill();
            risk.join();
        }
    
    
    //TODO: test getCurrentMission <- Corb.co
    public void testGetCurrentMission()throws InterruptedException{
        //ResourceBundle resb = TranslationBundle.getBundle();
        String[] gameModes = {
            "domination", 
            "capital"
        };
        String[] gameDescs = {
            "Conquer the world by occupying every territory on the board, thus eliminating all your opponents.", 
            "Capture all opposing Headquarters-while still controlling your own territory."
        };
        for (int i = 0; i < gameModes.length; i++) {
            String mode = gameModes[i];
            String desc = gameDescs[i];
            
            //set up
            final Risk risk = NewRisk();

            risk.parser("newgame");
            
            //risk.parser("newplayer ai easy 0 0");
            //risk.parser("newplayer ai easy 1 1");
            risk.parser("newplayer human blue player1");
            risk.parser("newplayer human green player2");

            risk.parser("startgame " + mode + " fixed recycle");
            
            syncGame(risk);
            assertEquals(desc, risk.getCurrentMission());
            
            //tear down
            risk.kill();
            risk.join();
        }
        
        // Mission mode is different
        //set up
        final Risk risk = NewRisk();

        risk.parser("newgame");

        //risk.parser("newplayer ai easy 0 0");
        //risk.parser("newplayer ai easy 1 1");
        risk.parser("newplayer human blue player1");
        risk.parser("newplayer human green player2");

        risk.parser("startgame mission fixed recycle");

        syncGame(risk);
        try {
            RiskGame game = risk.getGame();
            net.yura.domination.engine.core.Player player = game.getCurrentPlayer();
            net.yura.domination.engine.core.Mission mission = player.getMission();

            assertEquals(mission.getDiscription(), risk.getCurrentMission());
        } catch (NullPointerException e) {
            fail("Mission mode exception:\n" + e.toString());
        }
        
        //tear down
        risk.kill();
        risk.join();
    }
    
    public void testNetworkPlayerLeave()throws InterruptedException{
        //set up
        final Risk risk = NewRisk();

        risk.parser("newgame");

        //risk.parser("newplayer ai easy 0 0");
        //risk.parser("newplayer ai easy 1 1");
        risk.parser("newplayer human blue player1");
        risk.parser("newplayer human green player2");
       
        syncGame(risk);
        
        net.yura.domination.engine.core.RiskGame game = risk.getGame();
        net.yura.domination.engine.core.Player player = game.getPlayer("player1");
        String address = player.getAddress();
        
        risk.parserFromNetwork("LEAVE " + address);
        
        // Check
        assertEquals(0, game.getPlayers().size());
        
        //tear down
        risk.kill();
        risk.join();
    }
    
    //TODO: test info, startgame mission/capital, choosemap, choosecards, GetPlayerColors
    
    //Don't test join, startserver, killserver
    //Don't test: autosetup, canAttack, canTrade
    
}
