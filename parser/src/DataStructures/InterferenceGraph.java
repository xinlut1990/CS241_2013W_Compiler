package DataStructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InterferenceGraph {
	private List<Cluster> clusters = new ArrayList<Cluster>();
	
	public List<Cluster> getClusters() {
		return clusters;
	}
	
	public Cluster getArbitraryCluster(int regLimit) {
		for(Cluster cluster : clusters) {
			if(cluster.getNeighborNum() < regLimit) {
				return cluster;
			}
		}
		return null;
	}
	
	private Cluster getCluster(SSA ssa) {
		Cluster cluster = this.findCluster(ssa);
		if(cluster == null) {
			cluster = new Cluster(ssa);
			this.clusters.add(cluster);
		}
		return cluster;
	}

	private Cluster findCluster(SSA ssa) {
		for(Cluster cluster : clusters) {
			if(cluster.getSSAList().contains(ssa)) {
				return cluster;
			}
		}
		return null;
	}
	
	private void initLiveness(Set<SSA> live, BasicBlock curBB) {
		List<Instruction> instList = curBB.getInstructions();
		//only for the last instruction of the program
		Instruction lastInst = instList.get(instList.size()-1);
		live.add(VariableManager.getSSAByVersion(lastInst.getId()));
	}
	
	public void buildIG(ControlFlowGraph cfg) {
		BasicBlock curBB = null;
		Set<SSA> live = new HashSet<SSA>();
		
		curBB = cfg.getFirstBlock();
		this.buildIGRecursive(curBB, live);
		
		for(BasicBlock bb : cfg.getFirstBlockOfFuncs()) {
			curBB = bb;
			this.buildIGRecursive(curBB, live);
		}
		
	}
	
	public Set<SSA> buildIGRecursive(BasicBlock curBB, Set<SSA> liveLast) {
		if(curBB.getJoinSuccessor() != null && curBB.getDirectSuccessor() != null) {
			
			//liveness before the join block
			BasicBlock joinBlock = curBB.getJoinSuccessor();
			Set<SSA> liveJoin = buildIGRecursive(curBB.getJoinSuccessor(), liveLast);
			
			for(SSA result : joinBlock.getPhiResults()) {
				liveJoin.remove(result);
				Cluster resultCluster = this.getCluster(result);
				for(SSA x : liveJoin) {
					Cluster xCluster = this.getCluster(x);
					//add edge i <-> x
					xCluster.connectWith(resultCluster);
				}
			}		
			
			//liveness before the else block
			Set<SSA> liveElseInitial = new HashSet<SSA>();
			if(curBB.getElseSuccessor() != curBB.getJoinSuccessor()) {
				Set<SSA> liveElseLast = new HashSet<SSA>();
				liveElseLast.addAll(liveJoin);
				liveElseLast.addAll(joinBlock.getPhiElses());
				liveElseInitial = buildIGRecursive(curBB.getElseSuccessor(), liveElseLast);
			}
			
			//liveness before the if block
			Set<SSA> liveDirectLast = liveJoin;
			liveDirectLast.addAll(liveJoin);
			liveDirectLast.addAll(joinBlock.getPhiDirects());
			Set<SSA> liveDirectInitial = buildIGRecursive(curBB.getDirectSuccessor(), liveDirectLast);
			
			//merge liveDirect and liveELse
			liveDirectInitial.addAll(liveElseInitial);
			//build IG from initial block
			Set<SSA> liveInitial = buildIGFromBlock(curBB, liveDirectInitial);
			
			return liveInitial;
			
		} else if(curBB.getDirectSuccessor() != null && curBB.getDirectSuccessor().isLoop()) {
			
			BasicBlock loopHead = curBB.getDirectSuccessor();
			//liveness before the follow block
			BasicBlock followBlock = loopHead.getElseSuccessor();
			Set<SSA> liveFollow = buildIGRecursive(followBlock, liveLast);
			
			//liveness before the loop head
			Set<SSA> liveLoopHead = buildIGFromBlock(loopHead, new HashSet<SSA>(liveFollow));
			
			for(SSA result : loopHead.getPhiResults()) {
				liveLoopHead.remove(result);
				Cluster resultCluster = this.getCluster(result);
				for(SSA x : liveLoopHead) {
					Cluster xCluster = this.getCluster(x);
					//add edge i <-> x
					xCluster.connectWith(resultCluster);
				}
			}
			
			//liveness at the end of loop body
			Set<SSA> liveLoopBodyLast = liveLoopHead;
			liveLoopBodyLast.addAll(loopHead.getPhiDirects());
			Set<SSA> liveLoopBodyInitial = buildIGRecursive(loopHead.getDirectSuccessor(), liveLoopBodyLast);
			
			//merge liveDirect and liveELse
			liveLoopBodyInitial.addAll(liveFollow);
			//liveness before the loop head again
			Set<SSA> liveLoopHeadNew = buildIGFromBlock(loopHead, liveLoopBodyInitial);
			
			for(SSA result : loopHead.getPhiResults()) {
				liveLoopHeadNew.remove(result);
				Cluster resultCluster = this.getCluster(result);
				for(SSA x : liveLoopHeadNew) {
					Cluster xCluster = this.getCluster(x);
					//add edge i <-> x
					xCluster.connectWith(resultCluster);
				}
			}
			
			//the block before the loop head
			Set<SSA> liveBeforeLoop = liveLoopHeadNew;
			liveBeforeLoop.addAll(loopHead.getPhiElses());
			Set<SSA> liveInitial = buildIGFromBlock(curBB, liveBeforeLoop);
			
			return liveInitial;
			
		} else {
			//if recursion reaches a normal block
			Set<SSA> liveInitial = buildIGFromBlock(curBB, liveLast);
			return liveInitial;
//			if(curBB.getDirectSuccessor() != null) {
//				buildIGRecursive(curBB.getDirectSuccessor());
//			}
//			if(curBB.getElseSuccessor() != null) {
//				buildIGRecursive(curBB.getElseSuccessor());
//			}
		}
		
	}
	
	private Set<SSA> buildIGFromBlock(BasicBlock curBB, Set<SSA> live) {
		List<Instruction> instList = curBB.getInstructions();
		for(int i = instList.size() - 1; i >= 0; i--) {
			Instruction inst = instList.get(i);
			//ignore phi function
			if(inst.getOperator() == Instruction.phi) {
				return live;
			}
			if(inst.getOperator() == Instruction.subroutine) {
				live = this.buildIGRecursive(inst.getOperand2().block, live);
				continue;
			}
			List<SSA> results = VariableManager.getSSAsByVersion(inst.getId());
			//live = live - {i}
			live.removeAll(results);
			live.remove(null);
			//for all x belong to live do
			
			for(SSA result : results) {
				Cluster resultCluster = this.getCluster(result);
				for(SSA x : live) {
					Cluster xCluster = this.getCluster(x);
					//add edge i <-> x
					xCluster.connectWith(resultCluster);
				}
			}	
			
			//live = live + {j, k}
			if(inst.getOperand1() != null) {
				SSA j = inst.getOperand1().ssa;
				if(j != null) {
					live.add(j);
				}
			}
			
			if(inst.getOperand2() != null) {
				SSA k = inst.getOperand2().ssa;
				if(k != null) {
					live.add(k);
				}
			}
		}
		return live;
	}
	
//	private void mergeLive(Set<SSA> live1, Set<SSA> live2) {
//		
//	}
	
	private void updateInterferenceGraph() {
		for(Cluster cluster : clusters) {
			for(Cluster otherCluster : clusters) {
				
				if(cluster.interfere(otherCluster) && cluster != otherCluster && !otherCluster.getNeighborClusters().contains(cluster)) {
					cluster.connectWith(otherCluster);
				}
			}
		}

	}
	
	public void clustering() {
		List<Instruction> instList = ControlFlowGraph.getInstList();
		//for all phi instructions "x = phi(y1,y2)"
		for(Instruction inst:instList) {
			if(inst.getOperator() == Instruction.phi) {
				//cluster = {x},
				Cluster phiCluster = this.findCluster(VariableManager.getSSAByVersion(inst.getId()));
				if(phiCluster != null) {
					
					this.mergeCluster(phiCluster, inst.getOperand1());
					
					this.mergeCluster(phiCluster, inst.getOperand2());
				}
				
			}
		}

		this.updateInterferenceGraph();



	}
	
	private void mergeCluster(Cluster phiCluster, Operand phiOperand) {
		//for all yi that are not constants do
		if(phiOperand.kind != Operand.constant) {
			SSA ssa = phiOperand.ssa;
			Cluster origCluster = this.findCluster(ssa);
			if(origCluster != null && phiCluster != origCluster) {
				//if yi does not interfere with cluster then
				if(!origCluster.interfere(phiCluster)) {
					//add yi and its edges to cluster
					phiCluster.addCluster(origCluster);
					phiCluster.addEdgesOf(origCluster);
						//remove yi from graph
					this.deleteCluster(origCluster);
				}
			}
			
		}
	}
	
	public void color() {
		//get an arbitrary node x with fewer than N neighbors
		Cluster x = this.getArbitraryCluster(25);
		//remove x along with attached edges from G
		this.deleteCluster(x);
		//if G not empty color(remaining G)
		if(!this.clusters.isEmpty()) {
			color();
		}
		//add x and its edges back in;
		this.addCluster(x);
		//choose a register for x that is different from neighbors
		boolean regAssigned = false;
		for( int i = 1; i < 26; i ++) {
			boolean regAvailable = true;
			for(Cluster neighbor : x.getNeighborClusters()) {
				if(neighbor.getSSA().getReg() == i) {
					regAvailable = false;
				}
			}
			if(regAvailable) {
				x.setReg(i);
				regAssigned = true;
				//System.out.println("reg " + i + " assigned");
				break;
			}
		}
		if(!regAssigned) {
			System.out.println("spilled");
		}
		
	}
	
	public void deleteCluster(Cluster cluster) {
		this.clusters.remove(cluster); 
		for(Cluster otherCluster : this.clusters) {
			otherCluster.getNeighborClusters().remove(cluster);
		}
	}
	
	public void addCluster(Cluster cluster) {
		this.clusters.add(cluster); 
		//link neighbor clusters back to new cluster
		List<Cluster> neighbors = cluster.getNeighborClusters();
		for(int i = 0; i < neighbors.size(); i++) {
			neighbors.get(i).getNeighborClusters().add(cluster);
		}
	}
	
	public void printGraph() {
		for(Cluster cluster : this.clusters) {
			//System.out.println("reg: " + cluster.getSSA().getReg());
			for(SSA ssa : cluster.getSSAList()) {
				System.out.println("born at " + ssa.getVersion());
			}
			for( Cluster adjaCluster : cluster.getNeighborClusters()) {
				System.out.println("interfere with" + adjaCluster.getSSA().getVersion());
			}
		}
	}
	
	
	
}
