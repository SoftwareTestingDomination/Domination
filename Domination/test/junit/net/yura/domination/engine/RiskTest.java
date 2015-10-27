/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.yura.domination.engine;

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
    
    public void testRenamePlayer() {
        RiskUIUtil.parseArgs(new String[] {}); //new Risk() throws NPE without this
        Risk risk = new Risk();
    }
    
    //test processFromUI
    //loadgame, join, startserver, killserver
    
    //inGameParser
    // choosemap, choosecards, delplayer, info, autosetup, startgame mission/capital
    
    // canAttack, getCurrentMission, getPlayerColors, canTrade
    
}
