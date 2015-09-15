//  Group D

package net.yura.domination.engine.ai.old;

import java.util.Vector;
import net.yura.domination.engine.ai.AITest;
import net.yura.domination.engine.core.Continent;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Player;

/**
 * <p> Class for AIHardPlayer </p>
 * @author SE Group D
 */
public class AIHardDomination extends AITest {

    public String getPlaceArmies() {

		String output;

		   if ( game.NoEmptyCountries()==false ) {

		       Continent[] cont = game.getContinents();

		       /* ai looks at all the continents and tries to see which one it should place on
			The formula for placement is :
			extraArmiesForContinent - numberOfBorders - (neighborTerritories - numberOfBorders)+(territorynum * 0.9)-(IIF(numberofEnemyUnits=0,-3,numberofEnemyUnits) * 1.2)
			 */
			
		       int extraArmiesForContinent = 0;
		       int numberOfBorders = 0;
		       int neighborTerritories = 0;
		       int numberofEnemyUnits = -3;
		       int territorynum = 0;
		       boolean isBorder = false;
		       double check = -20;
		       double ratio = -20;
		       String name=null;
		       int val = -1;

		       for (int i=0; i<cont.length; i++) {
		     	  Vector ct = new Vector();
			  Continent co = cont[i];

			  extraArmiesForContinent = co.getArmyValue();

			  ct = co.getTerritoriesContained();

			  for (int j=0; j<ct.size(); j++) {
			     if ( ((Country)ct.elementAt(j)).getOwner() == player ) { 
				/* This territory belongs to the player */
			        territorynum++; 
				}
			     else {
				if (((Country)ct.elementAt(j)).getOwner() != null) {
				   /* This territory belongs to an enemy */
			           if (numberofEnemyUnits == -3) {
				      numberofEnemyUnits = 1;
				   }
			           else {
				      numberofEnemyUnits++;
			           }
				}
			     }
			     Vector w = ((Country)ct.elementAt(j)).getNeighbours();
                                for (int k=0; k<w.size(); k++) {
                                   if (((Country)w.elementAt(k)).getContinent() != co) {
				      /* This is a territory to protect from */
                                      neighborTerritories++;				      
				      isBorder = true;
                                   }
				}
				if (isBorder) {
				   numberOfBorders++;
				}
			  }

			  /* Calculate the value of that continent */ 

			  ratio = extraArmiesForContinent - numberOfBorders - (neighborTerritories - numberOfBorders)+(territorynum * 0.9) - (numberofEnemyUnits * 1.2);

			  if (check <= ratio && hasFreeTerritories(ct) == true) {
			      check = ratio;
			      val = i;
			  }

			  territorynum = 0;
		          numberofEnemyUnits = -3;
			  neighborTerritories = 0;
                          numberOfBorders = 0;
		       }
		       if (val==-1) { val=0; } // YURA: val is not being set

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

// YURA: not sure what this does??? does it do anything???
//                                Vector v = ((Country)ct.elementAt(j)).getNeighbours();
//                                for (int k=0; k<v.size(); k++) { // YURA: was ct.size()
//                                   if (((Country)v.elementAt(k)).getOwner() != player && ((Country)ct.elementAt(j)).isNeighbours((Country)v.elementAt(k))) {
//                                      name=((Country)ct.elementAt(j)).getColor()+"";
//                                      break;
//                                   }
//                                }
                             }
                          }
                       }

                       String s = blockOpponent(player);
                       if( s != null )
                          name = s;

		       if (name == null)
			  output = "autoplace";

		       else
			output = "placearmies " + name +" 1";
		   }
		   else {


		       Vector t = player.getTerritoriesOwned();
		       Vector n;
		       String name = null;

		       for (int a=0; a< t.size() ; a++) {

                          if ( ownsNeighbours( (Country)t.elementAt(a)) == false && ((Country)t.elementAt(a)).getArmies() <= 11 ) {

                              name=((Country)t.elementAt(a)).getColor()+"";
                              break;
                          }

		          if ( name != null ) { break; }

		       }

                       String s = keepBlocking(player);
                       if( s != null )
                          name = s;

                       String f = freeContinent(player);
                       if( f != null )
                          name = f;

       		   String k = NextToEnemyToEliminate();
       		   if (k != null)
       			   name = k;
                       
               if (name == null)
            	   name = findAttackableTerritory(player);

               if ( name == null )
			   output = "placearmies " + ((Country)t.elementAt(0)).getColor() +" "+player.getExtraArmies();

		       else if (game.getSetup() )
			   output = "placearmies " + name +" "+player.getExtraArmies();

		       else
			   output = "placearmies " + name +" 1";
		   }

		return output;

    }

    public String getBattleWon() {

	String output;

	/* Attempt to safeguard critical areas when moving armies to won country */

	Country attacker = game.getAttacker();
	Continent continent = attacker.getContinent();
	if (continent.isOwned(player) == true || mostlyOwned(continent) == true) {

		/* Set 50% security limit */
		if (attacker.getArmies() > 6)
			output = "move " + (attacker.getArmies()-1);  // bug request ID: 1478706
		else if (attacker.getArmies() > 3 && attacker.getArmies() <= 6)
			output = "move " + (attacker.getArmies()-1);
		else
			output = "move all";
	}
	else
	      output="move all";

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
			if (difference > highest)  {
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

    public String getAttack() {
	String output=null;
	Vector t = player.getTerritoriesOwned();
	Vector n;
	boolean chosen = false;
	Continent[] cont = game.getContinents();
	Vector options = new Vector();
	Attack temp=null;
	Attack move=null;

	/**   // replace with findAttackableNeighbors
	for (int a=0; a< t.size() ; a++) {
	    if ( ((Country)t.elementAt(a)).getArmies() > 1 ) {
		n = ((Country)t.elementAt(a)).getNeighbours();
		// attack ratio set to 50% of attack force
		int ratio = ((Country)t.elementAt(a)).getArmies();
		for (int b=0; b< n.size() ; b++) {
		    if ( ((Country)n.elementAt(b)).getOwner() != player && ((Country)n.elementAt(b)).getArmies() + (ratio / 2) < ratio  ) {
			Vector attack = new Vector();
			attack.add(((Country)t.elementAt(a)).getColor()+"");
			attack.add((Country)n.elementAt(b));
			//output= "attack " + ((Country)t.elementAt(a)).getColor() + " " + ((Country)n.elementAt(b)).getColor();
			options.add(attack);
			}
		}
	    }
	}  */
	options = findAttackableNeighbors(t,2);

	//System.out.println("--< " + player.getName() + " >---");	
	Player[] playersGreatestToLeast = OrderPlayers(player);  
	outer: for (int j=0; j<playersGreatestToLeast.length; j++) {
	    for (int i=0; i<options.size(); i++) {
		temp = (Attack)options.get(i);
		if ( ((Country) temp.destination).getOwner().equals(playersGreatestToLeast[j]) ){
			output = temp.toString();
			//System.out.println("Most powerful opponent/attack: " + temp.destination.getOwner() + ": "+ output);    //TESTING
			break outer;
		}
	/*	if ( ((Country) ((Vector)options.get(i)).get(1)).getOwner().equals(playersGreatestToLeast[j])  ) {
			output = "attack " + ((String) ((Vector)options.get(i)).get(0)) + " " + ((Country) ((Vector)options.get(i)).get(1)).getColor();
			break outer;
		    	} */
		}  
	}

	/* attempt to attack continent which is almost all owned, if scenario is there */
	//
	// Refactoring: remove need for 'complex' boolean by sorting continents by how much you control them
	// 	either by absolute count or percentage
	//
	int count = 0;
	Country attackfrom = null;
	boolean complex = false;
	for (int i=0; i<cont.length; i++) {
	    if ( mostlyOwned( cont[i] ) == true) {
		/*
		n = cont[i].getTerritoriesContained();
		// find your country with the largest forces
		for (int j=0; j<n.size(); j++) {
     			if ( ((Country)n.elementAt(j)).getArmies() > count && ((Country)n.elementAt(j)).getOwner() == player ) {
				count = ((Country)n.elementAt(j)).getArmies();
				attackfrom = (Country)n.elementAt(j);
                        }
		}
		// then find a neighbor with fewer armies to attack
		for (int b=0; b< n.size() ; b++) {
		        if ( ((Country)n.elementAt(b)).getOwner() != player && (((Country)n.elementAt(b)).getArmies()+1) < count && attackfrom.isNeighbours(((Country)n.elementAt(b))) == true ) {
				output= "attack " + attackfrom.getColor() + " " + ((Country)n.elementAt(b)).getColor();
				complex = true;
				break;
                        }
		} */

		// now attacks from outside continent too!
		options = targetTerritories(cont[i].getTerritoriesContained());
		options = filterAttacks(options,1);    // after simple advantage
		/*   // replace with function
		for(int j=0; j<options.size(); j++){
			temp=(Attack)options.get(j);
			//count=0;
			if ( ((Country)temp.source).getArmies() > count) {
				move=temp;
				count=((Country)temp.source).getArmies();
			}
		}  */
		if (options.size() > 0) {
			move = (Attack) options.elementAt( (int)Math.round(Math.random() * (options.size()-1) ) );
			output = move.toString();
			complex = true;
			if (cont[i] == move.destination.getContinent()){
				//System.out.println("Attempting to take over " + cont[i].getName() + ": " + move.toString() );   //Testing
			}
		}
		
	    }
	}

	// else attempt to attack continent with the greatest territories owned, that has yet to be conquered.
	if (complex == false) {
		int value = 0;
		int check = 0;   // pull out of for loop
		Continent choice = null;
		for (int i=0; i<cont.length; i++) {
			if ( cont[i].isOwned(player) == false) {
                             /*
			     n = cont[i].getTerritoriesContained();
                             int check = 0;    // Replace with count function
                             for (int j=0; j<n.size(); j++) {
                                if ( ((Country)n.elementAt(j)).getOwner() == player )
                                   check++;
                             }
			     */
			     check = countTerritoriesOwned(cont[i].getTerritoriesContained(), player);
                             if (check > value){
                                choice = cont[i];
				value = check;  // bug: should value be updated too?
			     }
			}
		}

		if (choice !=null) {
                        //System.out.println(choice.getName()); TESTING
			//n = ((Continent)choice).getTerritoriesContained();
			/*
			n = choice.getTerritoriesContained();
			for (int j=0; j<n.size(); j++) {
                             if ( ((Country)n.elementAt(j)).getArmies() > count && ((Country)n.elementAt(j)).getOwner() == player ) {
                                   count = ((Country)n.elementAt(j)).getArmies();
                                   attackfrom = (Country)n.elementAt(j);
                             }
                         } */
			options = getPossibleAttacks(choice.getTerritoriesContained());

			// Refactoring: Find attack with largest attacking force
			/*
			for (int b=0; b< n.size() ; b++) {
			     if ( ((Country)n.elementAt(b)).getOwner() != player && (((Country)n.elementAt(b)).getArmies()+1) < count && attackfrom.isNeighbours(((Country)n.elementAt(b))) == true ) {
                                   output= "attack " + attackfrom.getColor() + " " + ((Country)n.elementAt(b)).getColor();
                                   break;
                             }
			}  */
			options = filterAttacks(options,1);
			if (options.size() > 0) {
				move = (Attack) options.elementAt( (int)Math.round(Math.random() * (options.size()-1) ) );
				output = move.toString();
			}
		}
	}

                   // check to see if there are any continents that can be broken
                   
                   Vector continentsToBreak = GetContinentsToBreak(player);
                   String tmp = null;
//                   for (int i=0; i<continentsToBreak.size() && tmp == null; i++) {
//                	   Vector countriesInContinent = ((Continent)continentsToBreak.get(i)).getTerritoriesContained();
//                	   for (int j=0; j<t.size() && tmp == null; j++) {
//                		   for (int k=0; k<countriesInContinent.size() && tmp == null; k++)
//                			   if ( ((Country)t.get(j)).isNeighbours( ((Country)countriesInContinent.get(k)) ) &&
//                					   ((Country)t.get(j)).getArmies() - 1 > ((Country)countriesInContinent.get(k)).getArmies()   )
//                				   tmp = "attack " + ((Country)t.get(j)).getColor() + " " + ((Country)countriesInContinent.get(k)).getColor();
//                	   }
//                   }
                   
                   //Attempt to find a path to the continent - distance of 1, then 2, then 3 away.
                   if (continentsToBreak != null) {
                	   outer: for (int q=1; q<4; q++) { 
                		   for (int i=0; i<continentsToBreak.size(); i++) {
	                		   for (int j=0; j<t.size(); j++) {
	                			   Vector tNeighbors = ((Country)t.get(j)).getNeighbours();
	                			   for (int k=0; k<tNeighbors.size(); k++) {
	                				   //Fight to the death on the last step of breaking a continent bonus
	                				   if (  (((Country)t.get(j)).getArmies()-1 > ((Country)tNeighbors.get(k)).getArmies() 
										   || (q==1 && ((Country)t.get(j)).getArmies() > 1)) &&
	                						   ShortPathToContinent((Continent)continentsToBreak.get(i), (Country)t.get(j), (Country)tNeighbors.get(k), q)  ) {
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
                      if (( (Player) players.elementAt(i)).getNoTerritoriesOwned() < 4 && ( (Player) players.elementAt(i)) != player)
                         cankill.addElement((Player) players.elementAt(i));
                   }

                   if (cankill.size() > 0) {
                      for (int i=0; i<cankill.size(); i++) {
			 /*
                         Vector territories = ((Player) cankill.elementAt(i)).getTerritoriesOwned();
                         for (int j=0; j<territories.size(); j++) {

                        	Vector neighbours = ((Country)territories.elementAt(j)).getNeighbours();
                            for (int k=0; k<neighbours.size(); k++) {
                            	//Fight when outnumbered to eliminate a player.
                            	if (((Country) neighbours.elementAt(k)).getOwner() == player && 
                            	  ((Country)territories.elementAt(j)).getArmies() - 2 < ((Country) neighbours.elementAt(k)).getArmies() &&  
                            	  ((Country) neighbours.elementAt(k)).getArmies() > 1 && 
                            	  ((Country) neighbours.elementAt(k)).isNeighbours((Country)territories.elementAt(j))) {
                                      output= "attack " + ((Country) neighbours.elementAt(k)).getColor() + " " + ((Country)territories.elementAt(j)).getColor();
                                      //System.out.println("eliminate: " + output); TESTING
                                      break;
                                  }
                            }
                         }  */
			 options = targetTerritories( ((Player)cankill.elementAt(i)).getTerritoriesOwned()  );
			 options = filterAttacks(options, -2);
			 if (options.size() > 0) {
				move = (Attack) options.elementAt( (int)Math.round(Math.random() * (options.size()-1) ) );
				output = move.toString();
				//System.out.println("Targeting player: " + ((Player)cankill.elementAt(i)).getName() + " - " + output); //TESTING
			}
                      }
                   }

	      	   if ( output == null ) {
		  	output="endattack";
		   }
		   //System.out.println("Final Choice: " + output);

		return output;

    }

    public String getRoll() {

		String output;

		int n=((Country)game.getAttacker()).getArmies() - 1;


		/* Only roll for as long as while attacking player has more armies than defending player */
		int m=((Country)game.getDefender()).getArmies();

		// If we are trying to eliminate a player, fight longer.
		if (game.getDefender().getOwner().getNoTerritoriesOwned() < 4)
			m -= 3;
		
		//If we are trying to break a continent bonus, fight to the death.
		if (game.getDefender().getContinent().isOwned(game.getDefender().getOwner()))
			m = 0;
		
		if (n > 3 && n > m) {
		    output= "roll "+3;
		}
		else if (n > 0 && n <= 3 && n > m) {
		    output= "roll "+n;
		}
		else {
		    output= "retreat";
		}

		return output;

    }


    /**
     * Checks if the player owns most of the territories within a continent
     * @param p player object
     * @return boolean True if the player owns most of the territories within a continent,
     * otherwise false if the player does not own most of the territories
     */
    public boolean mostlyOwned(Continent w) {
	int ownedByPlayer=0;
	Vector territoriesContained = w.getTerritoriesContained();

	/*
	// Extract into callable function for reuse
	for (int c=0; c< territoriesContained.size() ; c++) {
	    if ( ((Country)territoriesContained.elementAt(c)).getOwner() == player ) {
		ownedByPlayer++;
	    }
	} */
	ownedByPlayer = countTerritoriesOwned(territoriesContained, player);

	if ( ownedByPlayer>=(territoriesContained.size()/2) ) {
	    return true;
	}
	else {
	   return false;
	}

    }

    /**
     * @name countTerritoriesOwned 
     * @param t Vector of Territories
     * @param p player object
     * @return count of territories in vector t owned by player p
     */
    public int countTerritoriesOwned(Vector t, Player p){
	int count=0;
        for (int i=0; i<t.size(); i++){
		if ( ((Country)t.elementAt(i)).getOwner() == p) { count++; }
	}
	return count;
    }
    	
    
    /************
     * @name targetTerritories
     * @param t Vector of teritories you wish to obtain
     * @return a Vector of possible attacks for a given list of territories
     * 	(complement function to getPossibleAttacks())
     **************/
    public Vector targetTerritories(Vector t){
	Vector output = new Vector();
	Vector n=new Vector();
    	Country source,target;
	for (int a=0; a< t.size() ; a++) {
	    target=(Country)t.elementAt(a);
	    if ( target.getOwner() != player ) {
		n = target.getNeighbours();
		for (int b=0; b< n.size() ; b++) {
		    source=(Country)n.elementAt(b);
		    if ( source.isNeighbours(target) && source.getOwner() == player && source.getArmies() > 1) {     // simplify logic
			//output.add( "attack " + source.getColor() + " " + target.getColor() );
			output.add(new Attack(source,target));
		    }
		}
	    }
	}
	return output;
    }
    
    /**
     * Checks if the player can make a valid tactical move
     * @param p player object, b Country object
     * @return Country if the tactical move is valid, else returns null.
     */
    public Country check1(Country b) {

      Vector neighbours = b.getNeighbours();
      Country c = null;

      for (int i=0; i<neighbours.size(); i++) {
         if ( ownsNeighbours( (Country)neighbours.elementAt(i)) == false && ((Country)neighbours.elementAt(i)).getOwner() == player)
              return (Country)neighbours.elementAt(i);
      }
     return c;
    }

    /**
     * Checks if the player can make a valid tactical move
     * @param p player object, b Country object
     * @return booelean True if the tactical move is valid, else returns false.
     */
    public boolean check2(Country b) {

      Vector neighbours = b.getNeighbours();
      Country c = null;

      for (int i=0; i<neighbours.size(); i++) {
         if ( ownsNeighbours( (Country)neighbours.elementAt(i)) == false && ((Country)neighbours.elementAt(i)).getOwner() == player)
              return true;
      }
     return false;
    }


    /**
     * Attempts to block an opposing player gaining a continent during the initial placement
     * @param p player object
     * @return String name if a move to block the opponent is required/possible, else returns null
     */
    public String blockOpponent(Player p) {
       Continent[] continents = game.getContinents();
       Vector players = game.getPlayers();

       for (int i=0; i<players.size(); i++) {
          for (int j=0; j<continents.length; j++) {
              if ( almostOwned((Player) players.elementAt(i), continents[j] ) == true
                   && continents[j].isOwned( (Player)players.elementAt(i) ) == false
                   && (Player) players.elementAt(i) != p ) {
                        Vector v = continents[j].getTerritoriesContained();
                        for (int k=0; k<v.size(); k++) {
                             if ( ((Country) v.elementAt(k)).getOwner() == null) {
                                 return ((Country) v.elementAt(k)).getColor()+"";
                             }
                        }
              }
          }
       }
    return null;
    }

    /**
     * Attempts to block an opposing player gaining a continent during the actual game
     * @param p player object
     * @return String name if a move to block the opponent is required/possible, else returns null
     */
    public String keepBlocking(Player p) {
       Continent[] continents = game.getContinents();
       Vector players = game.getPlayers();

       for (int i=0; i<players.size(); i++) {
          for (int j=0; j<continents.length; j++) {
              if ( almostOwned((Player) players.elementAt(i), continents[j]) == true
                   && continents[j].isOwned((Player) players.elementAt(i)) == false
                   && (Player) players.elementAt(i) != p ) {
                        Vector v = continents[j].getTerritoriesContained();
                        for (int k=0; k<v.size(); k++) {
                             if ( ((Country) v.elementAt(k)).getOwner() == p && ((Country) v.elementAt(k)).getArmies() < 5) {
                                 return ((Country) v.elementAt(k)).getColor()+"";
                             }
                        }
              }
          }
       }
    return null;
    }

    /**
     * Attempts to free a continent from an enemy player if it is possible to do so
     * @param p player object
     * @return Sring name is a move to free a continent is required/possible, else returns null
     */
    public String freeContinent(Player p) {
    	Vector continentsToBreak = GetContinentsToBreak(p);
    	Vector t = p.getTerritoriesOwned();
    	for (int q=1; q<4; q++) {
    		for (int k=0; k<continentsToBreak.size(); k++) {
		    	for (int i=0; i<t.size(); i++) {
		    		Vector tNeighbors = ((Country)t.get(i)).getNeighbours();
		    		for (int j=0; j<tNeighbors.size(); j++) {
		    			if ( //((Country)t.get(i)).getArmies() + p.getExtraArmies() - 1 > ((Country)tNeighbors.get(j)).getArmies() && 
		    					ShortPathToContinent((Continent)continentsToBreak.get(k), (Country)t.get(i), (Country)tNeighbors.get(j), q))
		    				return ((Country)t.get(i)).getColor()+"";
		    		}
		    	}
	    	}
    	}
    	
//       Continent[] continents = game.getContinents();
//       Vector players = game.getPlayers();
//
//       for (int i=0; i<players.size(); i++) {
//          for (int j=0; j<continents.length; j++) {
//              if ( continents[j].isOwned((Player) players.elementAt(i)) == true
//                   && (Player) players.elementAt(i) != p ) {
//
//                        Vector v = continents[j].getTerritoriesContained();
//                        for (int k=0; k<v.size(); k++) {
//                            Vector neighbours = ((Country) v.elementAt(k)).getNeighbours();
//                             for (int l=0; l<neighbours.size(); l++) {             // .equals(p.getName())
//                                 if ( (((Country) neighbours.elementAt(l)).getOwner()) == p                && ((Country) neighbours.elementAt(l)).getArmies() < 5) {
//                                        return ((Country) neighbours.elementAt(l)).getColor()+"";
//                                 }
//                             }
//                        }
//                }
//            }
//        }
        return null;
    }

    /**
     * Checks if the player owns almost all of the territories within a continent
     * @param p player object
     * @return booelan True if the player owns almost all of the territories within a continent,
     * otherwise false if the player does not own most of the territories
     */
    public boolean almostOwned(Player p, Continent co) {

	int ownedByPlayer=0;
        Vector territoriesContained = co.getTerritoriesContained();

	for (int c=0; c< territoriesContained.size() ; c++) {

	    if ( ((Country)territoriesContained.elementAt(c)).getOwner() == p ) {
		ownedByPlayer++;
	    }

	}

	if ( ownedByPlayer>=(territoriesContained.size()-2) ) {
	    return true;
	}
	else {
	   return false;
	}

    }

    /**
     * Checks whether a country owns its neighbours
     * @param p player object, c Country object
     * @return boolean True if the country owns its neighbours, else returns false
     */
    public boolean ownsNeighbours(Country c) {
        Vector neighbours = c.getNeighbours();
        int count = 0;

        for (int i=0; i<neighbours.size(); i++) {
           if ( ((Country) neighbours.elementAt(i)).getOwner() == player)
               count++;
        }

        if (count == neighbours.size())
            return true;

    return false;
    }

    /**
     * Checks if a player is still playing the game
     * @param p player object
     * @return booelan True if player is present, else return false
     */
    public boolean playerKilled(Player p) {
       Vector play = game.getPlayers();

       for (int i=0; i<play.size(); i++) {
         if ( (Player) play.elementAt(i) == p)
             return true;
       }
       return false;
     }


    /**
     * Checks if a continent has a free territory
     * @param ct Vector of countries in the continent
     * @return boolean true if there is a free country, otherwise returns false
     */
    public boolean hasFreeTerritories(Vector ct) {
	for (int i=0; i<ct.size(); i++)
	    if (((Country) ct.elementAt(i)).getOwner() == null)
	    	return true;
	return false;
    }
    
    /**
     * Checks for continents that are owned by a single player which is not the active player
     * @param player Player
     * @return Vector containing all of the continents with one owner, which is not the active player.  null
     * if none exist.
     */
    public Vector GetContinentsToBreak(Player player) {
    	Continent[] continents = game.getContinents();
    	//sort the continents based on worth
    	for (int i=0; i<continents.length-1; i++) {
    		for (int j=0; j<continents.length-1; j++) {
    			if (continents[j].getArmyValue() < continents[j+1].getArmyValue()) {
    				Continent tmp = continents[j];
    				continents[j] = continents[j+1];
    				continents[j+1] = tmp;
    			}
    		}
    	}
    	
    	Vector players = game.getPlayers();
    	Vector continentsToBreak = new Vector();
	Player owner;
    	for (int i=0; i<continents.length; i++) {
    		/*
		for (int j=0; j<players.size(); j++) {
    			if (!((Player)players.get(j)).equals(player) && continents[i].isOwned((Player)players.get(j))) {
    				continentsToBreak.add(continents[i]);
    			//	System.out.println("Continent to break: " + continents[i]);
    			}
    		} */
		// replace with new method getOwner()
		owner=continents[i].getOwner();
		if (owner != null && owner != player ){
			continentsToBreak.add(continents[i]);
		}
    	}
    	return continentsToBreak;
    	
    }

    /**
     * Orders the players other than the active player in order from greatest to least
     * @param player Player
     * @return Player[] with the players ordered from greatest to least by (territiores + armies).
     */
    public Player[] OrderPlayers(Player player) {
    	Vector players = game.getPlayers();
    	Player[] orderedPlayers = new Player[players.size()-1];
    	int num = 0;
    	for (int i=0; i<players.size(); i++) {
    		if (  !((Player)players.get(i)).equals(player) )
    			orderedPlayers[num++] = (Player)players.get(i);
    	}
    	
    	//Simple Bubble Sort to sort the players in order.    	
    	for (int i=0; i<orderedPlayers.length-1; i++) {
    		for (int j=0; j<orderedPlayers.length-1; j++) {
    			if (orderedPlayers[j].getNoTerritoriesOwned() + orderedPlayers[j].getNoArmies() <
    				orderedPlayers[j+1].getNoTerritoriesOwned() + orderedPlayers[j+1].getNoArmies()) {
    				Player tmp = orderedPlayers[j];
    				orderedPlayers[j] = orderedPlayers[j+1];
    				orderedPlayers[j+1] = tmp;
    			}
    		}
    	}
    	
    	return orderedPlayers;
    }

    /**
     * Determines if a short path exists to the continent through that country
     * @param Continent cont, Country attackFrom, Country attackThrough, int acceptableDistance
     * @return boolean of true if there is a path existing, and false otherwise
     */
    // does not take into account troops of countries on path.
    public boolean ShortPathToContinent(Continent cont, Country attackFrom, Country attackThrough, int acceptableDistance) {
    	//Countries are not valid for attacking
    	if (!attackFrom.isNeighbours(attackThrough) || attackThrough.getOwner().equals(attackFrom.getOwner()))
    		return false;
    	
    	if (acceptableDistance <= 0 && !attackFrom.getContinent().equals(cont))
    		return false;
    	else if (attackFrom.getContinent().equals(cont))
    		return true;
    	else if (acceptableDistance > 0 && attackThrough.getContinent().equals(cont))
    		return true;
    	else {
    		Vector throughNeighbors = attackThrough.getNeighbours();
	    	for (int i=0; i<throughNeighbors.size(); i++) {
	    		if (ShortPathToContinent(cont, attackThrough, (Country)throughNeighbors.get(i), acceptableDistance-1  )  )
	    			return true;
	    	}
    	}
    
    	return false;
    
    }
    
    public String NextToEnemyToEliminate() {
    	Vector weakPlayers = new Vector();
    	for (int i=0; i<game.getPlayers().size(); i++) {
    		if (((Player)game.getPlayers().get(i)).getNoTerritoriesOwned() < 4)
    			weakPlayers.add(game.getPlayers().get(i));
    	}
    	if (weakPlayers.size() == 0)
    		return null;
    	Vector t = player.getTerritoriesOwned();
    	Vector targetCountries = new Vector();
    	for (int i=0; i<weakPlayers.size(); i++) {
    		for (int j=0; j<((Player)weakPlayers.get(i)).getNoTerritoriesOwned(); j++) {
    			targetCountries.add(((Player)weakPlayers.get(i)).getTerritoriesOwned().get(j));
    		}
    	}
    	for (int i=0; i<t.size(); i++) {
    		for (int j=0; j<targetCountries.size(); j++) {
    			if (((Country)t.get(i)).isNeighbours((Country)targetCountries.get(j)))
    				return ((Country)t.get(i)).getColor() + "";
    		}
    	}
    	
    	return null;
    }
    


    /************
     * @name findAttackableNeighbors
     * @param t Vector of teritories
     * @param ratio - threshold of attack to defence armies to filter out
     * @return a Vector of possible attacks for a given list of territories
     * 	where the ratio of source/target armies is above ratio
     **************/
    public Vector getPossibleAttacks(Vector t){
	Vector output = new Vector();
	Vector n=new Vector();
    	Country source,target;
	for (int a=0; a< t.size() ; a++) {
	    source=(Country)t.elementAt(a);
	    if ( source.getOwner() == player && source.getArmies() > 1 ) {
		n = source.getNeighbours();
		for (int b=0; b< n.size() ; b++) {
		    target=(Country)n.elementAt(b);
		    if ( target.getOwner() != player ) {     // simplify logic
			//output.add( "attack " + source.getColor() + " " + target.getColor() );
			output.add(new Attack(source,target));
		    }
		}
	    }
	}
	return output;
    }

    /*******************
     * @name filterAttacks
     * @param options - Vector of Attacks
     * @param advantage - how much of an absolute advantage to have
     * @return Vector of attacks with specified advantage
     *******************/

    public Vector filterAttacks(Vector options, int advantage){
	Attack temp = null;
	Vector moves = new Vector();
	for(int j=0; j<options.size(); j++){
		temp=(Attack)options.get(j);
		if ( ( ((Country)temp.source).getArmies() - ((Country)temp.destination).getArmies()) > advantage) {
			moves.add(temp);
		}
	}
	return moves;
    }
    
}
