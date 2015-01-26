import structs.FrameData;
import structs.GameData;
import structs.Key;
import structs.CharacterData;
import structs.MotionData;
import gameInterface.AIInterface;
import commandcenter.CommandCenter;
import enumerate.Action;

/**
 * @author Huxiang Wang + Xinwei Xiong
 * Final Project for CS7032-A-Y-201415: ARTIFICAL INTELLIGENCE
 * Trinity College Dublin, Ireland
 */

public class TrinityKiller implements AIInterface {

	//
	// Class Members
	
	//
	GameData      m_gameData;
	FrameData     m_frameData;
	CommandCenter m_commandCenter; // This class is used for calling a command by AI.
	CharacterData m_myData, m_enemyData;
	
	Key m_inputKey;
	boolean m_playerNumber;
	String m_action = null;
	int m_attackType = 0;
		
	int m_distanceFromEnemy = 0;
	int m_skillCounter = 0;
	int m_punchCounter = 0;
	
	
	//
	// Class methods (the methods are override using a thread safe way)
	//
	
	/**
	 * This method initializes AI, and it will be executed only once in the beginning of a game. 
	 * Its execution will load the data that cannot be changed and load the flag of 
	 * player's side ("Boolean player", true for P1 or false for P2)
	 * If there is anything that needs to be initialized, you had better do it in this method. 
	 * It will return 0 when such initialization finishes correctly, otherwise the error code.
	 */
	@Override
	public synchronized int initialize(GameData gameData, boolean playerNumber) {
		
		m_gameData = gameData;
		m_playerNumber = playerNumber;
		m_inputKey = new Key();
		m_commandCenter = new CommandCenter();
		m_frameData = new FrameData();
		return 0;
	}
	
	/**
	 * This method gets information from the game status of each frame. 
	 * Such information is stored in the parameter fd. 
	 * If fd.getRemaningTime() returns a negative value, 
	 * the current round has not started yet.
	 */
	@Override
	public void getInformation(FrameData frameData) {
		
		m_frameData = frameData;
		m_commandCenter.setFrameData(this.m_frameData, m_playerNumber);
	}
	
	/**
	 * This method processes the data from AI. It is executed in each frame.
	 * Here, we implemented a state machine and Measure - Sense - Action based AI behavior.
	 */
	
	// Skills Reference:
	// STAND_D_DB_BB sprint and stun long range 250
	// STAND_D_DB_BA long punch  range 170
	// STAND_F_D_DFA close punch range 130
	// STAND_F_D_DFB slide
	// STAND_D_DF_FA direct energy ball
	// STAND_D_DF_FB up energy ball
	// AIR_UB 		jump kick
	// AIR_F_D_DFB  long jump kick
	// AIR_DB 		jump stun
	@Override
	public synchronized void processing() {
		
		// condition check, otherwise, NullPointerException will occur
		if (!m_frameData.emptyFlag && m_frameData.getRemainingTime() > 0) {
			
			// get the data from command center
			m_myData    = m_commandCenter.getMyCharacter();
			m_enemyData = m_commandCenter.getEnemyCharacter();
			
			// get the positions of both
			int myPositionX   = m_myData.getX();
			int enemyPositionX= m_enemyData.getX();
	
			//get the distance on X axis
			m_distanceFromEnemy = m_commandCenter.getDistanceX();
			
			//get the actions of both
			m_action               = m_myData.getAction().name();
			Action enemyAction 	   = m_enemyData.getAction();
			String enemyActionName = enemyAction.name();
			
			// get the enemy motion accordingly (P1 or P2)
			MotionData enemyMotion = new MotionData();
			if (m_playerNumber)
				enemyMotion = m_gameData.getPlayerTwoMotion().elementAt(enemyAction.ordinal());
				
			else
				enemyMotion = m_gameData.getPlayerOneMotion().elementAt(enemyAction.ordinal());
			
			// apply the same attack type as the enemy
			m_attackType = enemyMotion.getAttackType();
			
			
			
			// check if this command is being input
			if (m_commandCenter.getskillFlag()) {
				m_inputKey = m_commandCenter.getSkillKey();
				return;
			}
			
			
			// reset the input
			m_inputKey.empty();
			m_commandCenter.skillCancel();
			
			///////////////////////////////
			//long distance
			///////////////////////////////
			// special skill
			if (m_commandCenter.getMyEnergy() >= 400) {
				
				m_commandCenter.commandCall("STAND_D_DF_FC");
				// reset limit skill to 0 
				m_skillCounter = 0;
								
			}
		
			// attack with skill that use energy //slide attack
			else if(m_distanceFromEnemy < 220
					&& m_distanceFromEnemy > 150
					&& m_commandCenter.getMyEnergy() < 400
					&& m_commandCenter.getMyEnergy() > 50
					&& m_skillCounter!=15
					&& !m_enemyData.getState().name().equals("AIR")){
				
				m_commandCenter.commandCall("STAND_F_D_DFB");
				//limit skill use
				++m_skillCounter;
				
			}
			
			// cheap attacks (not using energy)
			else if(m_distanceFromEnemy > 63
					&& m_distanceFromEnemy < 150
					&& m_commandCenter.getMyEnergy() < 400){
				m_commandCenter.commandCall("STAND_FB");
			}
			
			
			///////////////////////////////
			//close combat
			///////////////////////////////
			//punch in very close range
			else if(m_distanceFromEnemy < 32){
				
				if(m_punchCounter != 5){
					m_commandCenter.commandCall("STAND_B");
					++m_punchCounter;
				}
				//combo with punch
				else {
					m_commandCenter.commandCall("CROUCH_FB");
					m_punchCounter = 0;					
				}
			}
			
			//throw
			else if (m_distanceFromEnemy > 32
					 && m_distanceFromEnemy  < 63
					 && !enemyActionName.startsWith("AIR")) {
				 m_commandCenter.commandCall("THROW_B");
			} 
			//combo with throw
			else if (enemyActionName.equals("THROW_SUFFER")
					 && m_commandCenter.getMyEnergy() >= 30) {
				m_commandCenter.commandCall("STAND_D_DF_FB ");
			}
			
			// end of close combat
			///////////////////////////////
			
			//counter attack
			//counter special attack
			else if (enemyActionName.equals("STAND_D_DF_FC")) {
				m_commandCenter.commandCall("FOR_JUMP");
			}			
			
			else if (m_action.equals("FOR_JUMP")) {
				m_commandCenter.commandCall("AIR_DB");
			} 
			
			/////////////////////////////////
			//defense tactics
			//attack type 
			//1=high
			//2=middle
			//3=low 
			//4=throw
			/////////////////////////////////
			//high attack type
			else if (m_attackType == 1 && m_distanceFromEnemy < 200
					&& !enemyActionName.equals("STAND_D_DF_FC")) {
				m_commandCenter.commandCall("CROUCH_FB");
			} 
			//middle attack type
			else if (m_attackType == 2 && m_distanceFromEnemy < 200) {
				m_commandCenter.commandCall("STAND_FA");
			}
			//low attack type
			else if(m_attackType == 3 && m_distanceFromEnemy < 200
					&& !enemyActionName.equals("STAND_D_DF_FC")){
				m_commandCenter.commandCall("CROUCH_FB");
			}
			// enemy uses air skill 
			else if(enemyActionName.startsWith("AIR") 
					&&m_distanceFromEnemy < 230){
				m_commandCenter.commandCall("AIR_UB");
			} 
			
			//jump out when at the end of corner
			else if(myPositionX == 710 || 
					myPositionX == -170
					&&m_commandCenter.getMyEnergy() < 400){
				m_commandCenter.commandCall("FOR_JUMP");
			}
			// end of defense tactics
		    ////////////////////////////////////
	
			// move close to the enemy
			else if(myPositionX > enemyPositionX){
			    m_inputKey.L =true;
			}
			else
			{
				m_inputKey.R =true;
			}
		}
	}
	
	/**
	 * The input method receives key input from AI. 
	 * It is executed in each frame and returns a value in the Key type.
	 */
	@Override
	public synchronized Key input() {
		return m_inputKey;
	}

	/**
	 * This method finalizes AI. It runs only once at the end of each game.
	 */
	@Override
	public void close() {
		// Do nothing here (Java has the garbage collection mechanism)
	}
	
	/**
	 * This method is for deciding which character to use among ZEN, GARNET, LUD, and KFM.
	 * In our approach, we return LUD
	 */
	@Override
	public String getCharacter() {
		// TODO Auto-generated method stub
		return CHARACTER_LUD;
	}
}
