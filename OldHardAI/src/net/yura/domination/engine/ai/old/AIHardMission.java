//  Group D

package net.yura.domination.engine.ai.old;

import java.util.Vector;
import net.yura.domination.engine.core.Continent;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Mission;
import net.yura.domination.engine.core.Player;


/**
 * <p> Class for AIHardPlayer </p>
 * @author SE Group D
 */
public class AIHardMission extends AIHardDomination {

	public String getPlaceArmies() {

		String output=null;

	           if ( game.NoEmptyCountries()==false ) {
                       String name=null;
                       Mission m = player.getMission();

                       /* - Mission Types
                       Mission type A - conquer X continent
                       Mission type B - Occupy X territories, each with atleast Y armies
                       Mission type C - Destroy all of player X's troops. If you are
                                        player X, then your mission is: occupy Y territories
                       */

                       /* initial placement strategy for mission type A */

                       if (m.getContinent1() != null && m.getContinent2() != null) {

                          Continent c1 = m.getContinent1();
                          Continent c2 = m.getContinent2();

                          name = missionPlacement0(c1);

                          if (name == null)
                             name = missionPlacement0(c2);
                       }

                       if (m.getContinent1() != null && m.getContinent2() == null) {
                          Continent c1 = m.getContinent1();
                          name = missionPlacement0(c1);
                       }

                       /* initial placement strategy for mission type B */

                       /*  ...as usual at present..... */

                       /* initial placement strategy for mission type C */

                       if (m.getPlayer() != null && m.getPlayer() != player && playerKilled(player) ) {

                          Vector enemy = ((Player) m.getPlayer()).getTerritoriesOwned();
                          for (int i=0; i<enemy.size(); i++) {
                             Vector neighbours = ((Country) enemy.elementAt(i)).getNeighbours();
                             for (int j=0; j<neighbours.size(); j++) {
                                 if ( ((Country) neighbours.elementAt(j)).getOwner() == null)
                                      name = ((Country)neighbours.elementAt(j)).getColor()+"";
                             }
                          }
                       }

                       if (m.getPlayer() != null && m.getPlayer() == player)
                          /* ...as usual at present.... */

                       if (name == null)
                          output = "autoplace";
                       else
                          output = "placearmies " + name +" 1";
	           }

	           else {
		       String name=null;
                       Mission m = player.getMission();
                       Vector t = player.getTerritoriesOwned();

                       /* game placement strategy for mission type A */

                       if (m.getContinent1() != null && m.getContinent2() != null) {

                          Continent c1 = m.getContinent1();
                          Continent c2 = m.getContinent2();

                          name = missionPlacement1(c1);

                          if (name == null)
                             name = missionPlacement1(c2);

                          if (name == null)
                             name = missionPlacement2(c1);

                          if (name == null)
                             name = missionPlacement2(c2);

                       }

                       if (m.getContinent1() != null && m.getContinent2() == null) {

                          Continent c1 = m.getContinent1();
                          name = missionPlacement1(c1);

                          if (name == null)
                             name = missionPlacement2(c1);
                       }

                        /* game placement strategy for mission type B */

                       /*  ...as usual at present..... */

                       /* game placement strategy for mission type c */

                       if (m.getPlayer() != null && m.getPlayer() != player && playerKilled(player) ) {
                          Vector enemy = ((Player) m.getPlayer()).getTerritoriesOwned();
                          for (int i=0; i<enemy.size(); i++) {
                             Vector neighbours = ((Country) enemy.elementAt(i)).getNeighbours();
                             for (int j=0; j<neighbours.size(); j++) {
                                 if ( ((Country) neighbours.elementAt(j)).getOwner() == player && ((Country) neighbours.elementAt(j)).getArmies() < 5) {
                                      name = ((Country)neighbours.elementAt(j)).getColor()+"";
                                      break;
                                 }
                             }
                          }
                       }

                       if (m.getPlayer() != null && m.getPlayer() == player) {;}


                       if (name == null) {
                           Vector n;

                           for (int a=0; a< t.size() ; a++) {

			      n = ((Country)t.elementAt(a)).getNeighbours();

			      for (int b=0; b< n.size() ; b++) {
			          if ( ((Country)n.elementAt(b)).getOwner() != player ) { name=((Country)t.elementAt(a)).getColor()+""; break; }
			      }

			      if ( name != null ) { break; }

		          }
                       }
                       //System.out.println(m.getDiscription()); TESTING

		       if ( name == null )
			   output = "placearmies " + ((Country)t.elementAt(0)).getColor() +" "+player.getExtraArmies()  ;

		       else if (game.getSetup() )
			   output = "placearmies " + name +" "+player.getExtraArmies();

		       else
		   	   output = "placearmies " + name +" 1";
	           }

		return output;

	}


	public String getAttack() {

		String output=null;

		Vector t = player.getTerritoriesOwned();
		Vector n;

               for (int a=0; a< t.size() ; a++) {
	          if ( ((Country)t.elementAt(a)).getArmies() > 1 ) {
		     n = ((Country)t.elementAt(a)).getNeighbours();

                     /* attack ratio set to 50% of attack force */

                     int ratio = ((Country)t.elementAt(a)).getArmies();
		     for (int b=0; b< n.size() ; b++) {
			if ( ((Country)n.elementAt(b)).getOwner() != player && ((Country)n.elementAt(b)).getArmies() + (ratio / 2) < ratio  ) {
                            output= "attack " + ((Country)t.elementAt(a)).getColor() + " " + ((Country)n.elementAt(b)).getColor();
	                }
		     }
                  }
               }

               Mission m = player.getMission();

               /* - Mission Types
               Mission type A - conquer X continent
               Mission type B - Occupy X territories, each with atleast Y armies
               Mission type C - Destroy all of player X's troops. If you are
                                        player X, then your mission is: occupy Y territories
               */

               /* attacking strategy for mission type A */

               if ( (m.getContinent1() != null && m.getContinent2() != null) ) {

               boolean done = false;

                  for (int i=0; i<t.size(); i++) {
                     Country s = (Country) t.elementAt(i);
                     if (s.getContinent() == m.getContinent1() && ((Continent) m.getContinent1()).isOwned(player) == false) {
                         Vector neighbours = s.getNeighbours();
                         for (int j=0; j<neighbours.size(); j++) {
                            Country r = (Country) neighbours.elementAt(j);
                            if (r.getOwner() != player && r.getArmies() < s.getArmies()-1) {
                               output= "attack " + s.getColor() + " " + r.getColor();
                               done = true;
                            }
                         }
                     }
                  }

                  if (done == false) {
                     for (int i=0; i<t.size(); i++) {
                        Country s = (Country) t.elementAt(i);
                        if (s.getContinent() == m.getContinent2() && ((Continent) m.getContinent2()).isOwned(player) == false) {
                            Vector neighbours = s.getNeighbours();
                            for (int j=0; j<neighbours.size(); j++) {
                               Country r = (Country) neighbours.elementAt(j);
                               if (r.getOwner() != player && r.getArmies() < s.getArmies()-1)
                                  output= "attack " + s.getColor() + " " + r.getColor();
                            }
                        }
                     }
                  }
               }

               if ( (m.getContinent1() != null && m.getContinent2() == null) ) {
                  for (int i=0; i<t.size(); i++) {
                     Country s = (Country) t.elementAt(i);
                     if (s.getContinent() == m.getContinent1() && ((Continent) m.getContinent1()).isOwned(player) == false) {
                         Vector neighbours = s.getNeighbours();
                         for (int j=0; j<neighbours.size(); j++) {
                            Country r = (Country) neighbours.elementAt(j);
                            if (r.getOwner() != player && r.getArmies() < s.getArmies()-1)
                               output= "attack " + s.getColor() + " " + r.getColor();
                         }
                     }
                  }
               }

               /* attacking strategy for mission type C */

               if (m.getPlayer() != null && m.getPlayer() != player && playerKilled(player)) {
                  for (int i=0; i<t.size(); i++) {
                     Country s = (Country) t.elementAt(i);
                     Vector neighbours = s.getNeighbours();
                     for (int j=0; j<neighbours.size(); j++) {
                        Country r = (Country) neighbours.elementAt(j);
                        if (r.getOwner() == m.getPlayer() && r.getArmies() < s.getArmies()-1)
                               output= "attack " + s.getColor() + " " + r.getColor();
                     }
                  }
               }

            if ( output==null ) {
		  output="endattack";
            }

		return output;

	}



	public String getBattleWon() {

		String output;

                  Country attacker = game.getAttacker();
                  Mission m = player.getMission();
                  //System.out.println("Check 1"); TESTING

                  if ( m.getNoofarmies() > 1 && attacker.getArmies() > m.getNoofarmies() && (attacker.getArmies()-m.getNoofarmies()) >= game.getMustMove() ) {
                      output = "move " + (attacker.getArmies()-m.getNoofarmies());
                  }
                  else {
                     output = "move all";
		  }

		return output;

	}


    /**
     * Checks if the player can choose a country in a continent in Mission Risk
     * @param p player object, c Continent object
     * @return String name
     */
    public String missionPlacement0(Continent c) {

        String name = null;
        Vector territories = c.getTerritoriesContained();

        for (int i=0; i<territories.size(); i++) {
           if ( ((Country)territories.elementAt(i)).getOwner() == null && ((Country) territories.elementAt(i)).getArmies() < 13 ) {
               name = ((Country)territories.elementAt(i)).getColor()+"";
               break;
           }
        }
        return name;
     }


    /**
     * Checks if the player can place armies in a continent in Mission Risk
     * @param p player object, c Continent object
     * @return String name
     */
    public String missionPlacement1(Continent c) {

        String name = null;
        Vector territories = c.getTerritoriesContained();

        for (int i=0; i<territories.size(); i++) {
           if ( ((Country)territories.elementAt(i)).getOwner() == player && ((Country) territories.elementAt(i)).getArmies() < 13
              && ownsNeighbours( (Country)territories.elementAt(i)) == false ) {
               name = ((Country)territories.elementAt(i)).getColor()+"";
               break;
           }
        }
        return name;
     }

    /**
     * Checks if the player can place armies in a adjacent territory in Mission Risk
     * @param p player object, c Continent object
     * @return String name
     */
    public String missionPlacement2(Continent c) {

        String name = null;
        Vector territories = c.getTerritoriesContained();

        for (int i=0; i<territories.size(); i++) {
           Vector neighbours = ((Country)territories.elementAt(i)).getNeighbours();
           for (int j=0; j<neighbours.size(); j++) {
             if ( ((Country) neighbours.elementAt(j)).getOwner() == player && ((Country) neighbours.elementAt(j)).getArmies() < 13
                && ownsNeighbours( (Country)territories.elementAt(i)) == false ) {
                 name = ((Country) neighbours.elementAt(j)).getColor()+"";
                 break;
             }
           }
        }
        return name;
     }



}
