package DataStructures;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
	private List<SSA> SSAList = new ArrayList<SSA>();

	private List<Cluster> neighborClusters = new ArrayList<Cluster>();
	
	public void setReg(int reg) {
		for(SSA ssa : this.SSAList) {
			ssa.setReg(reg);
		}
	}
	
	public int getNeighborNum() {
		return neighborClusters.size();
	}

	public List<Cluster> getNeighborClusters() {
		return neighborClusters;
	}
	
	public List<SSA> getSSAList() {
		return SSAList;
	}

	public Cluster(SSA ssa) {
		SSAList.add(ssa);
	}
	
	public void addSSA(SSA ssa) {
		SSAList.add(ssa);
	}
	
	public void addCluster(Cluster cluster) {
		for(SSA ssa : cluster.getSSAList()) {
			if(!this.SSAList.contains(ssa)) {
				this.SSAList.add(ssa);
			}
		}
		
	}
	
	public void addEdgesOf(Cluster cluster) {
		List<Cluster> srcClusterEdges = cluster.neighborClusters;
		for(int i = srcClusterEdges.size() - 1; i >= 0; i --) {
			if(!this.neighborClusters.contains(srcClusterEdges.get(i))) {
				//connect with each other
				this.neighborClusters.add(srcClusterEdges.get(i));
				srcClusterEdges.get(i).neighborClusters.add(this);
			}
		}

	}
	
	public SSA getSSA() {
		return SSAList.get(0);
	}
	
	public void connectWith(Cluster cluster) {
		if(!this.isConnectedWith(cluster)) {
			this.neighborClusters.add(cluster);
			cluster.neighborClusters.add(this);
		}
	}
	
	public boolean isConnectedWith(Cluster cluster) {
		return this.neighborClusters.contains(cluster);
	}
	
	public boolean interfere(Cluster cluster) {
		return this.getNeighborClusters().contains(cluster);
	}
}
