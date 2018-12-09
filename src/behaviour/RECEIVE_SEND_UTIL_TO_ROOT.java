//package behaviour;
//
//import agent.DCOP;
//import static agent.DcopInfo.*;
//import utilities.*;
//import jade.core.behaviours.Behaviour;
//import jade.lang.acl.ACLMessage;
//import jade.lang.acl.MessageTemplate;
//import jade.lang.acl.UnreadableException;
//
//public class RECEIVE_SEND_UTIL_TO_ROOT extends Behaviour {
//
//	private static final long serialVersionUID = 4766760189659187968L;
//
//	DCOP agent;
//	
//	public RECEIVE_SEND_UTIL_TO_ROOT(DCOP agent) {
//		super(agent);
//		this.agent = agent;
//	}
//	
//	@Override
//	public void action() {
//		// no need to back up simulated time, since stop condition occurs in done condition.
//		int msgCount = 0;
//		while (msgCount < agent.getNeighborAIDList().size()) {
//			MessageTemplate template = MessageTemplate.MatchPerformative(LS_ITERATION_DONE);
//			ACLMessage receivedMessage = myAgent.receive(template);
//			if (receivedMessage != null) {
//				msgCount++;
//				
//				long timeFromReceiveMessage = Long.parseLong(receivedMessage.getLanguage());
//				if (timeFromReceiveMessage > agent.getSimulatedTime())
//					agent.setSimulatedTime(timeFromReceiveMessage);
//			}
//			else
//				block();	
//		}
//		
//		agent.addupSimulatedTime(DCOP.getDelayMessageTime());
//		
//		int noMessageCount = 0;
//		agent.setUtilFromChildrenLS(0);
//		while (noMessageCount < agent.getChildrenAIDList().size()) {
//			MessageTemplate template = MessageTemplate.MatchPerformative(LS_UTIL);
//			ACLMessage receivedMessage = myAgent.receive(template);
//			if (receivedMessage != null) {
//				noMessageCount++;					
//				try {
//					agent.addtoUtilFromChildrenLS(Double.parseDouble((String) receivedMessage.getContentObject()));
//				} catch (NumberFormatException e) {
//					e.printStackTrace();
//				} catch (UnreadableException e) {
//					e.printStackTrace();
//				}
//				
//				long timeFromReceiveMessage = Long.parseLong(receivedMessage.getLanguage());
//				if (timeFromReceiveMessage > agent.getSimulatedTime())
//					agent.setSimulatedTime(timeFromReceiveMessage);
//			}
//			else
//				block();
//		}
//		
//		agent.addupSimulatedTime(DCOP.getDelayMessageTime());
//		
//		agent.setUtilityAndCost(agent.getUtilFromChildrenLS() + 
//				agent.utilityWithParentAndPseudoAndUnary() - agent.calculcatingSwitchingCost());
//
//		if (!agent.isRoot())
//			agent.sendObjectMessageWithTime(agent.getParentAID(), 
//					String.valueOf(agent.getUtilityAndCost()), LS_UTIL, agent.getSimulatedTime());
//		else {
//			agent.setEndTime(System.currentTimeMillis());
//			Utilities.writeUtil_Time_LS(agent);
//			agent.setOldLSRunningTime(agent.getEndTime() - agent.getStartTime());
//			agent.setOldLSUtility(agent.getUtilityAndCost());
//			
//			System.out.println("RUNNING TIME: " + (agent.getEndTime() - agent.getStartTime()) + "ms");
//			System.out.println("SIMULATED TIME: " + agent.getSimulatedTime()/1000000 + "ms");
//			int countIteration = agent.getLsIteration() + 1;
////			if (agent.algorithm == DCOP.LS_SDPOP) {
////				System.err.println("Utility of Local-search DPOP at iteration " + countIteration + ": " + agent.getUtilityAndCost());
////			}
////			else if (agent.algorithm == DCOP.LS_RAND)
////				System.err.println("Utility of Local-search RAND at iteration " + countIteration + ": " + agent.getUtilityAndCost());
//		}
//		
//		agent.incrementLsIteration();
//		
//		if (agent.getLsIteration() < DCOP.MAX_ITERATION)
//			agent.sendImprove();
//	}
//
//	@Override
//	public boolean done() {
//		return agent.getLsIteration() == DCOP.MAX_ITERATION;
//	}
//}
