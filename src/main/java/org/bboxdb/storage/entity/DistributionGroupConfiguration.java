/*******************************************************************************
 *
 *    Copyright (C) 2015-2017 the BBoxDB project
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package org.bboxdb.storage.entity;

import org.bboxdb.misc.Const;

public class DistributionGroupConfiguration {

	/** 
	 * The replication factor
	 */
	protected short replicationFactor = 3;
	
	/**
	 * The dafault region size
	 */
	protected int regionSize = Const.DEFAULT_REGION_SIZE;
	
	/**
	 * The default placement strategy
	 */
	protected String placementStrategy = Const.DEFAULT_PLACEMENT_STRATEGY;
	
	/**
	 * The placement strategy config
	 */
	protected String placementStrategyConfig = Const.DEFAULT_PLACEMENT_CONFIG;
	
	/**
	 * The space paritioner
	 */
	protected String spacePartitioner = Const.DEFAULT_SPACE_PARTITIONER;
	
	/**
	 * The space partitioner config
	 */
	protected String spacePartitionerConfig = Const.DEFAULT_SPACE_PARTITIONER_CONFIG;

	public short getReplicationFactor() {
		return replicationFactor;
	}

	public void setReplicationFactor(short replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	public int getRegionSize() {
		return regionSize;
	}

	public void setRegionSize(int regionSize) {
		this.regionSize = regionSize;
	}

	public String getPlacementStrategy() {
		return placementStrategy;
	}

	public void setPlacementStrategy(String placementStrategy) {
		this.placementStrategy = placementStrategy;
	}

	public String getPlacementStrategyConfig() {
		return placementStrategyConfig;
	}

	public void setPlacementStrategyConfig(String placementStrategyConfig) {
		this.placementStrategyConfig = placementStrategyConfig;
	}

	public String getSpacePartitioner() {
		return spacePartitioner;
	}

	public void setSpacePartitioner(String spacePartitioner) {
		this.spacePartitioner = spacePartitioner;
	}

	public String getSpacePartitionerConfig() {
		return spacePartitionerConfig;
	}

	public void setSpacePartitionerConfig(String spacePartitionerConfig) {
		this.spacePartitionerConfig = spacePartitionerConfig;
	}

	@Override
	public String toString() {
		return "DistributionGroupConfiguration [replicationFactor=" + replicationFactor + ", regionSize=" + regionSize
				+ ", placementStrategy=" + placementStrategy + ", placementStrategyConfig=" + placementStrategyConfig
				+ ", spacePartitioner=" + spacePartitioner + ", spacePartitionerConfig=" + spacePartitionerConfig + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((placementStrategy == null) ? 0 : placementStrategy.hashCode());
		result = prime * result + ((placementStrategyConfig == null) ? 0 : placementStrategyConfig.hashCode());
		result = prime * result + regionSize;
		result = prime * result + replicationFactor;
		result = prime * result + ((spacePartitioner == null) ? 0 : spacePartitioner.hashCode());
		result = prime * result + ((spacePartitionerConfig == null) ? 0 : spacePartitionerConfig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DistributionGroupConfiguration other = (DistributionGroupConfiguration) obj;
		if (placementStrategy == null) {
			if (other.placementStrategy != null)
				return false;
		} else if (!placementStrategy.equals(other.placementStrategy))
			return false;
		if (placementStrategyConfig == null) {
			if (other.placementStrategyConfig != null)
				return false;
		} else if (!placementStrategyConfig.equals(other.placementStrategyConfig))
			return false;
		if (regionSize != other.regionSize)
			return false;
		if (replicationFactor != other.replicationFactor)
			return false;
		if (spacePartitioner == null) {
			if (other.spacePartitioner != null)
				return false;
		} else if (!spacePartitioner.equals(other.spacePartitioner))
			return false;
		if (spacePartitionerConfig == null) {
			if (other.spacePartitionerConfig != null)
				return false;
		} else if (!spacePartitionerConfig.equals(other.spacePartitionerConfig))
			return false;
		return true;
	}
	
	
	
}
