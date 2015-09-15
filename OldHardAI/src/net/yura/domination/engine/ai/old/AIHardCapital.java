//  Group D

package net.yura.domination.engine.ai.old;

import java.util.Vector;
import net.yura.domination.engine.core.Continent;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Player;

/**
 * <p> Class for AIHardPlayer </p>
 * @author SE Group D
 */
public class AIHardCapital extends AIHardDomination {

	public String getPlaceArmies() {

		String output;

		    if ( game.NoEmptyCountries()==false ) {

			Continent[] cont = game.getContinents();

			/* ai attempts to gain control of a continent during initial placement */

			int territorynum = 0;
			int check = -1;
			String name=null;
			int val = -1;

			for (int i=0; i<cont.length; i++) {
			    Vector ct = new Vector();
			    Continent co = cont[i];
			    ct = co.getTerritoriesContained();

			    for (int j=0; j<ct.size(); j++) {
			        if ( ((Country)ct.elementAt(j)).getOwner() == player ) { territorynum++; }
			    }

			    if (check <= territorynum) {
				check = territorynum;
				val = i;
			    }
			    territorynum = 0;
			}

			/* ..pick country from that continent */
			boolean picked = false;
			if (check > 0) {
			    Continent co = cont[val];
			    Vector ct = co.getTerritoriesContained();

			    for (int j=0; j<ct.size(); j++) {
				if ( ((Country)ct.elementAt(j)).getOwner() == null )  {
				    name=((Country)ct.elementAt(j)).getColor()+"";
				    picked = true;
				    break;
				}
			    }
			}

			if (picked == false) {
			    Continent co = cont[val];
			    Vector ct = co.getTerritoriesContained();

			    for (int j=0; j<ct.size(); j++) {
				if ( ((Country)ct.elementAt(j)).getOwner() == null )  {
				    name=((Country)ct.elementAt(j)).getColor()+"";

// YURA: no idea what this block does but it crashes with:
// choosemap CRO.map, startgame capital italianlike
//				    Vector v = ((Country)ct.elementAt(j)).getNeighbours();
//				    for (int k=0; k<ct.size(); k++) {
//					if (((Country)v.elementAt(k)).getOwner() != player) {
//					    name=((Country)ct.elementAt(j)).getColor()+"";
//					    break;
//					}
//				    }
				}
			    }
			}

			String str = expandBase(player.getTerritoriesOwned());
			if( str != null )
                            name = str;

                        String s = blockOpponent(player);
                        if( s != null )
                            name = s;

		        if (name == null)
			    output = "autoplace";

		        else
			    output = "placearmies " + name +" 1";
		    }
		    else {

			Vector pt = player.getTerritoriesOwned();

			String name=null;
			Country capital = player.getCapital();
			if (capital == null)
			    capital = findCapital();

			for (int a=0; a< pt.size() ; a++) {

			    if ( ownsNeighbours( (Country)pt.elementAt(a)) == false && ((Country)pt.elementAt(a)).getArmies() <= 11 ) {
                                name=((Country)pt.elementAt(a)).getColor()+"";
                                break;
                            }

		            if ( name != null ) { break; }

			}

			String st = attackCapital();
			if( st != null ) 
                            name = st;

                        String s = keepBlocking(player);
                        if( s != null ) 
                            name = s;

                        String f = freeContinent(player);
                        if( f != null ) 
                            name = f;

			String str = reinforceBase(capital);
			if (str != null)
			    name = str;

		    if (name == null)
		    	name = findAttackableTerritory(player);
		    
			if ( name == null ) 
		    	output = "placearmies " + ((Country)pt.elementAt(0)).getColor() +" "+player.getExtraArmies();

//Removed so that it will add armies one at a time, thus reinforcing the base until it has enough defense
//at which time it will place in other territories as well.  Re-added so that it would not take forever
//for the computer to place armies. Re-removed after seeing that the time is neglibile.
		    else if (game.getSetup() ) 
			    output = "placearmies " + name +" "+player.getExtraArmies();

		    else
			    output = "placearmies " + name +" 1";
		    }

		return output;

	}


	public String getAttack() {

		String output=null;
		boolean attackFromCap = player.getCapital().getArmies() > addEnemies(player.getCapital())*2+5; 
		    boolean chosen = false;
		    Continent[] cont = game.getContinents();
		Vector t = player.getTerritoriesOwned();
		Vector n;
		Vector options = new Vector();
		    for (int a=0; a< t.size(); a++) {
		        if ( ((Country)t.elementAt(a)).getArmies() > 1 ) {
			    if ( (Country)t.elementAt(a) != player.getCapital() ||
			    	attackFromCap ) {
				n = ((Country)t.elementAt(a)).getNeighbours();

				/* attack ratio set to 50% of attack force */

				int ratio = ((Country)t.elementAt(a)).getArmies();
				for (int b=0; b< n.size() ; b++) {
				    if ( ((Country)n.elementAt(b)).getOwner() != player && ((Country)n.elementAt(b)).getArmies() + (ratio / 2) < ratio) {
					//output= "attack " + ((Country)t.elementAt(a)).getColor() + " " + ((Country)n.elementAt(b)).getColor();
				    	 Vector attack = new Vector();
			        	 attack.add(((Country)t.elementAt(a)).getColor()+"");
			        	 attack.add((Country)n.elementAt(b));
			        	 options.add(attack);
				    }
				}
			    }
		        }
		    }
		    Player[] playersGreatestToLeast = OrderPlayers(player);
	    	outer: for (int j=0; j<playersGreatestToLeast.length; j++) {
	    		for (int i=0; i<options.size(); i++) {
		    		if ( ((Country) ((Vector)options.get(i)).get(1)).getOwner().equals(playersGreatestToLeast[j])  ) {
		    			output = "attack " + ((String) ((Vector)options.get(i)).get(0)) + " " + ((Country) ((Vector)options.get(i)).get(1)).getColor();
		    			break outer;
		    		}
		    	}
		    }

		    /* attempt to attack continent which is almost all owned, if scenario is there */

		    int count = 0;
		    Country attackfrom = null;
                    boolean complex = false;

                    for (int i=0; i<cont.length; i++) {

			if ( mostlyOwned( cont[i] ) == true) {

			    n = cont[i].getTerritoriesContained();

			    for (int j=0; j<n.size(); j++) {
		    	
				if ( ((Country)n.elementAt(j)).getArmies() > count
				    && ((Country)n.elementAt(j)).getOwner() == player
				    && ((Country)n.elementAt(j)) != player.getCapital()) {
					count = ((Country)n.elementAt(j)).getArmies();
					attackfrom = (Country)n.elementAt(j);
				}
			    }

			    for (int b=0; b< n.size() ; b++) {
				if ( ((Country)n.elementAt(b)).getOwner() != player && (((Country)n.elementAt(b)).getArmies()+1) < count && attackfrom.isNeighbours((Country)n.elementAt(b)) == true) {
				    output= "attack " + attackfrom.getColor() + " " + ((Country)n.elementAt(b)).getColor();
				    complex = true;
				    break;
				}
			    }
			}
		    }

		    // else attempt to attack continent with the greatest territories owned, that has yet to be conquered.

		    if (complex == false) {
			int value = 0;
			Continent choice = null;

			for (int i=0; i<cont.length; i++) {

			    if ( cont[i].isOwned(player) == false) {

				n = cont[i].getTerritoriesContained();

				int check = 0;
				for (int j=0; j<n.size(); j++) {
				     if ( ((Country)n.elementAt(j)).getOwner() == player && (((Country)n.elementAt(j)) != player.getCapital() || attackFromCap))
					check++;
				}

				if (check > value)
				    choice = cont[i];
			    }
			}

			if (choice !=null) {

                            //System.out.println(choice.getName()); TESTING
			    n = ((Continent)choice).getTerritoriesContained();

			    for (int j=0; j<n.size(); j++) {

				if ( ((Country)n.elementAt(j)).getArmies() > count
				    && ((Country)n.elementAt(j)).getOwner() == player
				    && (((Country)n.elementAt(j)) != player.getCapital() || attackFromCap)) {
				    count = ((Country)n.elementAt(j)).getArmies();
				    attackfrom = (Country)n.elementAt(j);
				}
			    }

			    for (int b=0; b< n.size() ; b++) {
				if ( ((Country)n.elementAt(b)).getOwner() != player && (((Country)n.elementAt(b)).getArmies()+1) < count && attackfrom.isNeighbours((Country)n.elementAt(b)) == true ) {
				    output= "attack " + attackfrom.getColor() + " " + ((Country)n.elementAt(b)).getColor();
                                    break;
				}
			    }
			}
		    }

            Vector continentsToBreak = GetContinentsToBreak(player);
            String tmp = null;
            
            //Attempt to find a path to the continent - distance of 1, then 2, then 3 away.
            if (continentsToBreak != null) {
         	   outer: for (int q=1; q<4; q++) { 
         		   for (int i=0; i<continentsToBreak.size(); i++) {
             		   for (int j=0; j<t.size(); j++) {
             			   Vector tNeighbors = ((Country)t.get(j)).getNeighbours();
             			   for (int k=0; k<tNeighbors.size(); k++) {
             				   if (((Country)t.get(j)).getArmies()-1 > ((Country)tNeighbors.get(k)).getArmies() &&
             						   ShortPathToContinent((Continent)continentsToBreak.get(i), (Country)t.get(j), (Country)tNeighbors.get(k), q)  &&
             						  (((Country)t.get(j)).getColor() != player.getCapital().getColor() || attackFromCap)) {
             					   tmp = "attack " + ((Country)t.get(j)).getColor() + " " + ((Country)tNeighbors.get(k)).getColor();
             					   break outer;
             				   }
             						   
             			   }
             		   }
             	   }
         	   }
            }
            if (tmp != null)
         	   output = tmp;
		    
		    
		    
		    // check to see if there are any players to eliminate

		    Vector players = game.getPlayers();
                    Vector cankill = new Vector();

                    for (int i=0; i<players.size(); i++) {
			if (( (Player) players.elementAt(i)).getNoTerritoriesOwned() < 3 && ( (Player) players.elementAt(i)) != player)
			    cankill.addElement((Player) players.elementAt(i));
                    }

		    if (cankill.size() > 0) {
			for (int i=0; i<cankill.size(); i++) {
			Vector territories = ((Player) cankill.elementAt(i)).getTerritoriesOwned();
			for (int j=0; j<territories.size(); j++) {

			    Vector neighbours = ((Country)territories.elementAt(j)).getNeighbours();
                            for (int k=0; k<neighbours.size(); k++) {
				if (((Country) neighbours.elementAt(k)).getOwner() == player
				    && (((Country)territories.elementAt(j)).getArmies() + 1 < ((Country) neighbours.elementAt(k)).getArmies())
				    && ((Country) neighbours.elementAt(k) != player.getCapital() || attackFromCap)
				    &&  ((Country) neighbours.elementAt(k)).isNeighbours((Country)territories.elementAt(j))) {
				    output= "attack " + ((Country) neighbours.elementAt(k)).getColor() + " " + ((Country)territories.elementAt(j)).getColor();
                                    //System.out.println("eliminate: " + output); TESTING
                                    break;
				}
			    }
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

		   /* Attempt to safeguard critical areas when moving armies to won country */

		   Country attacker = game.getAttacker();
		   Continent continent = attacker.getContinent();
		   //If attacking from the capital, try to leave enough defense.
		   if (attacker.getColor() == player.getCapital().getColor()) {
			   if (attacker.getArmies() >= addEnemies(player.getCapital())*2 + 3)
				   output = "move " + (attacker.getArmies() - (addEnemies(player.getCapital())*2 + 1));
			   else 
				   output = "move " + (attacker.getArmies()-1);
		   }
		   else if (continent.isOwned(player) == true || mostlyOwned(continent) == true) {

		       /* Set 50% security limit */
		       if (attacker.getArmies() > 6)
		   	   output = "move " + (attacker.getArmies()-1);

		       else if (attacker.getArmies() > 3 && attacker.getArmies() <= 6)
			   output = "move " + (attacker.getArmies()-1);

		       else
			   output = "move all";
                   }
		   else {
		      output="move all";
		   }

		return output;

	}



	public String getCapital() {

		return "capital " + findCapital().getColor();

	}

	/**
	 * Returns the number of armies in most fortified neighbor.  If player owns all of the countries around it,
	 * return 1, because there is still a potential for a threat
	 * @param Country c which is the country that you want to know the enemies of the given player that are around
	 * @return int with the number of Enemies around or 1, whichever is higher.
	 */
	public int addEnemies(Country c) {

		int nearbyEnemies = 0;
		//Country cap = player.getCapital();
		Country[] countries = game.getCountries();
		for (int j=0; j<countries.length; j++) {
			if (countries[j].isNeighbours(c) && countries[j].getOwner() != player)
				if (countries[j].getArmies() > nearbyEnemies)
					nearbyEnemies = countries[j].getArmies();
		}
		return nearbyEnemies == 0 ? 1 : nearbyEnemies;
	}
	
	/**
	 * Checks if the player's capital has enough armies and checks if its neighbours has enough armies
	 * @param capital The capital country
	 * @return String Returns the name of the capital
	 */
	public String reinforceBase(Country capital) {
	 String str = null;
	 boolean foundCap = false;
	 Vector players = game.getPlayers();
	 //Reinforce base if there are not 3 times the number of surrounding enemies present.
	 for (int i=0; i<players.size(); i++) {
	    Player p2 = (Player) players.elementAt(i);
	    Country cap = p2.getCapital();
	    if (p2 == player)
		cap = capital;
	    if (cap != null && cap.getOwner() == player) {
	    	if (cap.getArmies() < addEnemies(player.getCapital())*2 && cap.getArmies() < 50) {
		        str = cap.getColor()+"";
			    foundCap = true;
			    break;
	    	}
	    }
	 }

	 if (foundCap == false) {
	    boolean found = false;
	    for (int i=0; i<players.size(); i++) {
		Player p2 = (Player) players.elementAt(i);
		Country cap = p2.getCapital();
		if (p2 == player)
		    cap = capital;
		if (cap != null && cap.getOwner() == player) {
		    Vector v = cap.getNeighbours();
		    for (int j=0; j<v.size(); j++) {
			Country c = (Country) v.elementAt(j);
			if (c.getOwner() == player && c.getArmies() < 4) {
			    str = c.getColor()+"";
			    found = true;
			    break;
			}
		    }
		}
		if (found == true)
		    break;
    	    }
         }
         return str;
	}


	/**
	 * get the best capital position for the current player
	 *
	 */
	public Country findCapital() {

		// TODO someone needs to document how this is working!

		Vector pt = player.getTerritoriesOwned();
		Country country = null;
		int difference1 = Integer.MAX_VALUE;
		int difference2 = Integer.MAX_VALUE;

		for (int i=0; i<pt.size(); i++) {

			int count = 0;
			Country c1 = (Country) pt.elementAt(i);
			Vector v = c1.getNeighbours();

			for (int j=0; j<v.size(); j++) {
				Country c2 = (Country) v.elementAt(j);
				if (c2.getOwner() == player) {
					count++;
				}
			}

			int diff1 = v.size() - count;
			int diff2 = addEnemies(c1) - c1.getArmies();

			if (diff1 <= difference1 && diff2 <= difference2) {

				difference1 = diff1;
				difference2 = diff2;

				country = c1;
			}
			// count = 0; // ??? wtf is this for ???
		}

//		Country country = pt.size() > 0 ? (Country)pt.get(0) : null;
//		for (int i=0; i<pt.size(); i++) {
//			Country tmp = (Country)pt.get(i);
//			if (tmp.getArmies() > country.getArmies())
//				country = tmp;
//		}
		return country;

	}

    /**
     * Checks through the enemy's capital and finds a neighbouring territory owned by the reinforcing player
     * @return String Returns the name of the reinforcing territory
     */
    public String attackCapital() {
	String str = null;
	Vector players = game.getPlayers();
	Country capital = null;
	for (int i=0; i<players.size(); i++) {
	    Player p2 = (Player) players.elementAt(i);
	    capital = p2.getCapital();
	    if (capital != null && capital.getOwner() != player)
	    	break;
    	}
    	if (capital != null) {
	    Vector v = capital.getNeighbours();
	    for (int i=0; i<v.size(); i++) {
		Country c = (Country) v.elementAt(i);
		if (c.getOwner() == player && c.isNeighbours(capital)) {
		    str = c.getColor()+"";
		    break;
	        }
	    }
        }
        return str;
    }

    /**
     * Checks through the player's current owned territories and picks a neighbour if it is free
     * @param pt The player's territories
     * @return String Returns the name of the country if found, returns null otherwise
     */
    public String expandBase(Vector pt) {
        String str = null;
	boolean found = false;
	for (int i=0; i<pt.size(); i++) {
	    Country c1 = (Country) pt.elementAt(i);
	    Vector v = c1.getNeighbours();
	    for (int j=0; j<v.size(); j++) {
		Country c2 = (Country) v.elementAt(j);
		if (c2.getOwner() == null) {
		    str = c2.getColor()+"";
		    found = true;
		    break;
	        }
            }
	    if (found == true)
	        break;
        }
        return str;
    }

    public String getRoll() {

		String output;

		int n=((Country)game.getAttacker()).getArmies() - 1;


		/* Only roll for as long as while attacking player has more armies than defending player */
		int m=((Country)game.getDefender()).getArmies();

		if (((Country)game.getAttacker()).getColor() == player.getCapital().getColor()) {
			if (player.getCapital().getArmies() > addEnemies(player.getCapital())*2+5)
				output = "roll "+3;
			else
				output = "retreat";
		} 
		else if (n > 3 && n > m) {
		    output= "roll "+3;
		}
		else if (n <= 3 && n > m) {
		    output= "roll "+n;
		}
		else {
		    output= "retreat";
		}

		return output;

    }

    public String getTacMove() {

		String output=null;

		    /* reinforces armies from less critical to more critical areas */

		    Vector t = player.getTerritoriesOwned();
		    Vector n;
		    boolean possible = false;
                    int difference = 0;
                    Country sender = null;
                    Country receiver = null;
                    Country temp = null;
                    int highest = 1;

		    for (int a=0; a<t.size(); a++) {

			Country country = ((Country)t.elementAt(a));
			difference  = country.getArmies();
			if (difference > highest && country.getColor() != player.getCapital().getColor())  {
			    highest = difference;
			    sender = country;
			}
		    }

		    if (sender != null)  {

			receiver = check1(sender);

			if (receiver == null) {
			    n = sender.getNeighbours();
			    for (int i=0; i<n.size(); i++) {
				temp = (Country) n.elementAt(i);
				if (temp.getOwner() == player) {
				    possible = check2(temp);
				}
				if (possible == true)  { receiver = temp; break; }
			    }
			}
		    }

		    if (receiver != null && receiver != sender) {
			output= "movearmies " + ((Country)sender).getColor() + " " + ((Country)receiver).getColor() + " " + (((Country)sender).getArmies()-1);
			//System.out.println(((Country)sender).getName()); TESTING
			//System.out.println(((Country)receiver).getName()); TESTING
		    }

		    if ( output == null ) {
			output = "nomove";
		    }

		return output;

    }

    
}
