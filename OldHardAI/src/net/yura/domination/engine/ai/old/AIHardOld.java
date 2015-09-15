package net.yura.domination.engine.ai.old;

import net.yura.domination.engine.ai.AI;
import net.yura.domination.engine.core.RiskGame;

public class AIHardOld implements AI {

    private final AI domination;
    private final AI mission;
    private final AI capital;

    private AI current;

    public AIHardOld() {
        domination = new AIHardDomination();
        mission = new AIHardMission();
        capital = new AIHardCapital();
    }

    public int getType() {
        return 6;
    }

    public String getCommand() {
        return "old";
    }

    public void setGame(RiskGame game) {
        int mode = game.getGameMode();

        if (mode==RiskGame.MODE_CAPITAL) {
            current = capital;
        }
        else if (mode==RiskGame.MODE_SECRET_MISSION) {
            current = mission;
        }
        else {
            current = domination;
        }
        
        current.setGame(game);
    }

    public String getBattleWon() {
        return current.getBattleWon();
    }
    public String getTacMove() {
        return current.getTacMove();
    }
    public String getTrade() {
        return current.getTrade();
    }
    public String getPlaceArmies() {
        return current.getPlaceArmies();
    }
    public String getAttack() {
        return current.getAttack();
    }
    public String getRoll() {
        return current.getRoll();
    }
    public String getCapital() {
        return current.getCapital();
    }
    public String getAutoDefendString() {
        return current.getAutoDefendString();
    }

}
