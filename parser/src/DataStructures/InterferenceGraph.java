package DataStructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import VariableManagement.SSA;
import VariableManagement.VariableManager;

import ControlFlowGraph.BasicBlock;
import ControlFlowGraph.ControlFlowGraph;

public class InterferenceGraph {
	private List<Cluster> clusters = new ArrayList<Cluster>();
	private static int spillMemory = 2000;
	
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
	
	public Cluster getArbitraryCluster() {
		for(Cluster cluster : clusters) {
				return cluster;
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
			
			this.connectPhiFuncs(liveJoin, joinBlock);
			
			//liveness before the else block
			Set<SSA> liveElseInitial = this.elseBlockLive(liveJoin, joinBlock, curBB);
			
			//liveness before the if block
			Set<SSA> liveDirectInitial = this.directBlockLive(liveJoin, joinBlock, curBB);
			
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
			
			this.connectPhiFuncs(liveLoopHead, loopHead);
			
			//liveness at the end of loop body
			Set<SSA> liveLoopBodyInitial = this.loopBodyLive(liveLoopHead, loopHead);
			
			//merge liveDirect and liveELse
			liveLoopBodyInitial.addAll(liveFollow);
			//liveness before the loop head again
			Set<SSA> liveLoopHeadNew = buildIGFromBlock(loopHead, liveLoopBodyInitial);
			
			this.connectPhiFuncs(liveLoopHeadNew, loopHead);
			
			//the block before the loop head
			Set<SSA> liveBeforeLoop = liveLoopHeadNew;
			liveBeforeLoop.addAll(loopHead.getPhiManager().getPhiElses());
			Set<SSA> liveInitial = buildIGFromBlock(curBB, liveBeforeLoop);
			
			return liveInitial;
			
		} else {
			//if recursion reaches a normal block
			Set<SSA> liveInitial = buildIGFromBlock(curBB, liveLast);
			return liveInitial;

		}
		
	}
	
	private void connectPhiFuncs(Set<SSA> liveJoin, BasicBlock joinBlock) {
		for(SSA result : joinBlock.getPhiManager().getPhiResults()) {
			liveJoin.remove(result);
			Cluster resultCluster = this.getCluster(result);
			for(SSA x : liveJoin) {
				Cluster xCluster = this.getCluster(x);
				//add edge i <-> x
				xCluster.connectWith(resultCluster);
			}
		}	
	}
	
	private Set<SSA> directBlockLive( Set<SSA> liveJoin, BasicBlock joinBlock, BasicBlock headBlock ) {
		
		Set<SSA> liveDirectLast = new HashSet<SSA>();
		liveDirectLast.addAll(liveJoin);
		liveDirectLast.addAll(joinBlock.getPhiManager().getPhiDirects());
		return buildIGRecursive(headBlock.getDirectSuccessor(), liveDirectLast);
	}
	
	private Set<SSA> elseBlockLive( Set<SSA> liveJoin, BasicBlock joinBlock, BasicBlock headBlock ) {
		
		if(headBlock.getElseSuccessor() != headBlock.getJoinSuccessor()) {
			Set<SSA> liveElseLast = new HashSet<SSA>();
			liveElseLast.addAll(liveJoin);
			liveElseLast.addAll(joinBlock.getPhiManager().getPhiElses());
			return buildIGRecursive(headBlock.getElseSuccessor(), liveElseLast);
		}
		return new HashSet<SSA>();
	}
	
	private Set<SSA> loopBodyLive( Set<SSA> liveLoopBodyLast, BasicBlock loopHead) {
		liveLoopBodyLast.addAll(loopHead.getPhiManager().getPhiDirects());
		return buildIGRecursive(loopHead.getDirectSuccessor(), liveLoopBodyLast);
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
	
	public void clustering(List<Instruction> instList) {
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
		Cluster x = this.getArbitraryCluster(24);
		//remove x along with attached edges from G
		if(x != null) {
			this.deleteCluster(x);
		} else {
			//System.out.println("need to spill");
			x = this.getArbitraryCluster();
			this.deleteCluster(x);
		}
		
		//if G not empty color(remaining G)
		if(!this.clusters.isEmpty()) {
			color();
		}
		//add x and its edges back in;
		this.addCluster(x);
		//choose a register for x that is different from neighbors
		boolean regAssigned = false;
		for( int i = 1; i < 25; i ++) {
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
			//insert store code
			this.insertSpillCode(x);
			//out.println(x.getSSA().getIdentifier()+"spilled");
			
			spillMemory += 4;
		}
		
	}
	
	private void insertSpillCode(Cluster x) {
		for(int i = 0; i < x.getSSAList().size(); i ++) {
			Instruction assignInst = ControlFlowGraph.getInstruction(x.getSSAList().get(i).getVersion());
			BasicBlock blockOfAssign = ControlFlowGraph.findBlockOf(assignInst);
			
			Operand storeAddr = Operand.makeConst(spillMemory);
			if(assignInst.getOperand1().kind == Operand.constant) {
				Operand temp = Operand.makeReg(25);
				Instruction store = Instruction.noUseInstruction(Instruction.store, temp, storeAddr);
				store.setId(assignInst.getId());
				blockOfAssign.replaceInst(assignInst, store);
				Instruction move = Instruction.noUseInstruction(Instruction.move, assignInst.getOperand1(), temp);
				move.setId(store.getId() - 1);
				blockOfAssign.insertBefore(store, move);
			} else {
				Instruction store = Instruction.noUseInstruction(Instruction.store, assignInst.getOperand1(), storeAddr);
				store.setId(assignInst.getId());
				blockOfAssign.replaceInst(assignInst, store);
			}
			


			
			for(Operand use: x.getSSAList().get(i).getUseChain()) {
				Instruction useInst = ControlFlowGraph.getInstruction(use.inst);
				if(useInst != null && use.inst != x.getSSAList().get(i).getVersion()) {
					BasicBlock blockOfUse = ControlFlowGraph.findBlockOf(useInst);
					
					Operand loadAddr = Operand.makeConst(spillMemory);
					Operand loadTemp = Operand.makeReg(26);
					
					Instruction load = Instruction.noUseInstruction(Instruction.load, loadAddr, loadTemp);
					blockOfUse.insertBefore(useInst, load);
					
					if(useInst.getOperand1() != null && useInst.getOperand1().ssa == x.getSSAList().get(i)) {
						useInst.setOperand1(loadTemp);
					}
					if(useInst.getOperand2() != null && useInst.getOperand2().ssa == x.getSSAList().get(i)) {
						useInst.setOperand2(loadTemp);
					}
				}
				
			}
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
//			for( Cluster adjaCluster : cluster.getNeighborClusters()) {
//				System.out.println("interfere with" + adjaCluster.getSSA().getVersion());
//			}
		}
	}
	
	
	
}
