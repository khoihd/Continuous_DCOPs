package behaviour;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.HashMap;

import agent.DCOP;
import table.Row;
import static agent.DcopInfo.*;
/*	1. IF X is a root
 * 		Send the value of root to all the children
 *		PRINT OUT the value picked
 *		STOP
 * 
 *  2. ELSE (not a root)
 *  	Waiting from message from the parent
 *  	From the received parent_value, pick X_value from the store (parent_value, X_value)
 *  	//which is the corresponding X_value to parent_value with the minimum utility
 *  	2.1 IF (X is not a leaf)
 *  		Send the value to all the children
 *  	PRINT_OUT the picked value
 *  	STOP 
 */
/**
 * @author khoihd
 *
 */
public class DPOP_VALUE extends OneShotBehaviour {

	private static final long serialVersionUID = 4288241761322913640L;
	
	DCOP agent;
	
	public DPOP_VALUE(DCOP agent) {
		super(agent);
		this.agent = agent;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void action() {
		agent.setValuesToSendInVALUEPhase(new HashMap<Double, Double>());
		if (agent.isRoot()) {
			agent.setCurrentStartTime(agent.getBean().getCurrentThreadUserTime());
			if (agent.algorithm == DCOP.DISCRETE_DPOP) {
				System.out.println(agent.getIdStr() + " choose value " + agent.getChosenValue());
			}
			
			agent.addupSimulatedTime(agent.getBean().getCurrentThreadUserTime() - agent.getCurrentStartTime());
			agent.addValuesToSendInValuePhase(Double.valueOf(agent.getIdStr()), agent.getChosenValue());
			for (AID childrenAgentAID:agent.getChildrenAIDList()) {
				agent.sendObjectMessageWithTime(childrenAgentAID, agent.getValuesToSendInVALUEPhase(),
								DPOP_VALUE, agent.getSimulatedTime());
			}
		}
		else {//leaf or internal nodes
			ACLMessage receivedMessage = waitingForValuesInItsAgentViewFromParent(DPOP_VALUE);
			agent.setCurrentStartTime(agent.getBean().getCurrentThreadUserTime());
			
			HashMap<Integer, Double> variableAgentViewIndexValueMap = new HashMap<Integer, Double>();
			HashMap<Double, Double> valuesFromParent = new HashMap<Double, Double>();
			try {
				valuesFromParent = (HashMap<Double, Double>) receivedMessage.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}

			for (Double agentKey : valuesFromParent.keySet()) {
				int positionInParentMessage = agent.getAgentViewTable().getDecVarLabel().indexOf(agentKey);
				if (positionInParentMessage == -1) //not in agentView
					continue;
				//if exist this agent in agent view, add to values to send
				agent.addValuesToSendInValuePhase(agentKey, valuesFromParent.get(agentKey));
				variableAgentViewIndexValueMap.put(positionInParentMessage						
											,valuesFromParent.get(agentKey));
			}
			int agentIndex = agent.getAgentViewTable().getDecVarLabel().indexOf(Double.valueOf(agent.getIdStr()));

			Row chosenRow = new Row();
			double maxUtility = Integer.MIN_VALUE;
			for (Row agentViewRow:agent.getAgentViewTable().getRowList()) {
				boolean isMatch = true;	

				//check for each of index, get values and compared to the agentViewRow's values
				//if one of the values is not match, set flag to false and skip to the next row
				for (Integer variableIndex:variableAgentViewIndexValueMap.keySet()) {
					if (agentViewRow.getValueAtPosition(variableIndex).equals(variableAgentViewIndexValueMap.get(variableIndex)) == false) {
						isMatch = false;
						break;
					}
				}
				if (isMatch == false)
					continue;

				if (agentViewRow.getUtility() > maxUtility) {
					maxUtility = agentViewRow.getUtility();
					chosenRow = agentViewRow;
				}
			}
			
			agent.setChosenValue(chosenRow.getValueAtPosition(agentIndex));

			//add its chosen value to the map to send to its children
			agent.addValuesToSendInValuePhase(Double.valueOf(agent.getIdStr()), agent.getChosenValue());			
			
			if (agent.algorithm == DCOP.DISCRETE_DPOP) {
				System.out.println("Chosen value is " + agent.getChosenValue());
			}
			//correct
//			else if (agent.algorithm == DCOP.LS_SDPOP || agent.algorithm == DCOP.SDPOP) {
//				agent.getValueAtEachTSMap().put(agent.getCurrentTS(), agent.getChosenValue());
//			}
//			else if (agent.algorithm == DCOP.HYBRID) {
//				agent.getValueAtEachTSMap().put(agent.getCurrentTS(), agent.getChosenValue());
//			}
//			else if (agent.algorithm == DCOP.REACT) {
//				agent.getValueAtEachTSMap().put(agent.getCurrentTS(), agent.getChosenValue());
//			}
			
			agent.addupSimulatedTime(agent.getBean().getCurrentThreadUserTime() - agent.getCurrentStartTime());
			
			if (agent.isLeaf() == false) {
//				ArrayList<Double> agent_value = new ArrayList<>();
//				agent_value.add(agent.getIdStr());
//				agent_value.add(agent.getChosenValue());
				if (agent.algorithm == DCOP.DISCRETE_DPOP) {
					System.out.println("Chosen value is " + agent.getChosenValue());
				}
//				agent_value.add(String.valueOf(agent.getCurrentGlobalUtility()));
				
				for (AID children:agent.getChildrenAIDList()) {
					agent.sendObjectMessageWithTime(children, agent.getValuesToSendInVALUEPhase()
													, DPOP_VALUE, agent.getSimulatedTime());
				}
			}
		}
		
		
		agent.incrementCurrentTS();
	}
	
	public ArrayList<ACLMessage> waitingForMessageFromPseudoParent(int msgCode) {
		ArrayList<ACLMessage> messageList = new ArrayList<ACLMessage>();
		//no of messages are no of pseudoParent + 1 (parent)
		while (messageList.size() < agent.getPseudoParentAIDList().size() + 1) {
			MessageTemplate template = MessageTemplate.MatchPerformative(msgCode);
			ACLMessage receivedMessage = myAgent.receive(template);
			if (receivedMessage != null) {
				messageList.add(receivedMessage);
			}
			else
				block();
		}
		return messageList;
	}
	
	public ACLMessage waitingForValuesInItsAgentViewFromParent(int msgCode) {
		ACLMessage receivedMessage = null;
		while (true) {
			
		MessageTemplate template = MessageTemplate.MatchPerformative(msgCode);
		receivedMessage = myAgent.receive(template);
		if (receivedMessage != null) {
			long timeFromReceiveMessage = Long.parseLong(receivedMessage.getLanguage());
			if (timeFromReceiveMessage > agent.getSimulatedTime())
				agent.setSimulatedTime(timeFromReceiveMessage);
			break;
		}
		else
			block();
		}
		
		agent.addupSimulatedTime(DCOP.getDelayMessageTime());
		return receivedMessage;
	}
	
//	public void writeChosenValueToFile_Not_FW() {
//		String algName = null;
//		if (agent.algorithm == DCOP.REACT)
//			algName = "react";
//		else if (agent.algorithm == DCOP.HYBRID)
//			algName = "hybrid";
//		
//		String line = "";
//		String alg = DCOP.algTypes[agent.algorithm];
//		String scType = (agent.scType == DCOP.CONSTANT) ? "constant" : "linear";
//		if (agent.getCurrentTS() == 0)
//			line = line + alg + "\t" + agent.inputFileName + "\t" + "sCost=" + agent.switchingCost
//			+ "\t" + "scType=" + scType + "\n";
//			
//		line = line + "ts=" + agent.getCurrentTS()
//					+ "\t" + "x=" + agent.getValueAtEachTSMap().get(agent.getCurrentTS())
//					+ "\t" + "y=" + agent.getPickedRandomAt(agent.getCurrentTS());
//		
//		//write switching cost after y (react/hybrid) or x (forward)
//		String switchOrNot = null;
//		if (agent.getCurrentTS() == 0)
//			switchOrNot = DCOP.switchNo;
//		else {
//			if (agent.getValueAtEachTSMap().get(agent.getCurrentTS()).equals
//					(agent.getValueAtEachTSMap().get(agent.getCurrentTS()-1)) == true) {
//				switchOrNot = DCOP.switchNo;
//			}
//			else {
//				switchOrNot = DCOP.switchYes;
//			}
//		}
//		
//		line = line + "\t" + "sw=" + switchOrNot + "\n";	
//	
//		String fileName = "id=" + agent.instanceD + "/sw=" + (int) agent.switchingCost + "/" + algName + "_" + agent.getIdStr() + ".txt";
//		byte data[] = line.getBytes();
//	    Path p = Paths.get(fileName);
//
//		try (OutputStream out = new BufferedOutputStream(
//			Files.newOutputStream(p, CREATE, APPEND))) {
//			out.write(data, 0, data.length);
//			out.flush();
//			out.close();
//		} catch (IOException x) {
//			System.err.println(x);
//		}
//	}
	
//	public void writeChosenValueToFileFW() {
//		String algName = "forward";
//		
//		String line = "";
//		String alg = DCOP.algTypes[agent.algorithm];
//		String scType = (agent.scType == DCOP.CONSTANT) ? "constant" : "linear";
//		if (agent.getCurrentTS() == 0)
//			line = line + alg + "\t" + agent.inputFileName + "\t" + "sCost=" + agent.switchingCost
//			+ "\t" + "scType=" + scType + "\n";
//				
//		//write switching cost after x (forward)
//		String switchOrNot = null;
//		if (agent.getCurrentTS() == 0) {
//			switchOrNot = DCOP.switchNo;
//			line = line + "ts=" + agent.getCurrentTS()
//			 		+ "\t" + "x=" + agent.getValueAtEachTSMap().get(agent.getCurrentTS())
//					+ "\t" + "sw=" + switchOrNot + "\n";
//		}
//		else if (agent.getCurrentTS() == agent.h-1) {
//			//no switching cost for now, wait for
//			line = line + "ts=" + agent.h
//			 		+ "\t" + "x=" + agent.getValueAtEachTSMap().get(agent.h);
//			agent.setLastLine(line);
//		}
//		else if (agent.getCurrentTS() == agent.h) {
//			line = line + "ts=" + (agent.h-1)
//	 					+ "\t" + "x=" + agent.getValueAtEachTSMap().get(agent.h-1);
//			
//			//compare value at h-1 with h-2
//			if (agent.getValueAtEachTSMap().get(agent.h-1).equals
//					(agent.getValueAtEachTSMap().get(agent.h-2)) == true) {
//				switchOrNot = DCOP.switchNo;
//			}
//			else {
//				switchOrNot = DCOP.switchYes;
//			}
//			
//			line = line + "\t" + "sw=" + switchOrNot + "\n" + agent.getLastLine();
//			
//			if (agent.getValueAtEachTSMap().get(agent.h).equals
//					(agent.getValueAtEachTSMap().get(agent.h-1)) == true) {
//				switchOrNot = DCOP.switchNo;
//			}
//			else {
//				switchOrNot = DCOP.switchYes;
//			}
//			
//			line = line + "\t" + "sw=" + switchOrNot + "\n";
//		}
//		else {
//			line = line + "ts=" + agent.getCurrentTS()
//	 					+ "\t" + "x=" + agent.getValueAtEachTSMap().get(agent.getCurrentTS());
//			if (agent.getValueAtEachTSMap().get(agent.getCurrentTS()).equals
//					(agent.getValueAtEachTSMap().get(agent.getCurrentTS()-1)) == true) {
//				switchOrNot = DCOP.switchNo;
//			}
//			else {
//				switchOrNot = DCOP.switchYes;
//			}
//			
//			line = line + "\t" + "sw=" + switchOrNot + "\n";
//		}
//				
//		//forward: at h-1, solve h, so not writing at all to wait for h (solve h-1, then write 2 at a time)
//		if (agent.getCurrentTS() != agent.h-1) {
//			String fileName = "id=" + agent.instanceD + "/sw=" + (int) agent.switchingCost + "/" + algName + "_" + agent.getIdStr() + ".txt";
//			byte data[] = line.getBytes();
//		    Path p = Paths.get(fileName);
//	
//			try (OutputStream out = new BufferedOutputStream(
//				Files.newOutputStream(p, CREATE, APPEND))) {
//				out.write(data, 0, data.length);
//				out.flush();
//				out.close();
//			} catch (IOException x) {
//				System.err.println(x);
//			}
//		}
//	}
}
