package de.fernunihagen.dna.jkn.scalephant.distribution.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstance;

public class RandomResourcePlacementStrategy extends ResourcePlacementStrategy {

	/**
	 * The random generator
	 */
	protected final Random randomGenerator;
	
	/**
	 * The Logger
	 */
	protected final static Logger logger = LoggerFactory.getLogger(RandomResourcePlacementStrategy.class);
	
	public RandomResourcePlacementStrategy() {
		randomGenerator = new Random();
	}
	
	@Override
	public DistributedInstance getInstancesForNewRessource(final List<DistributedInstance> systems, final Collection<DistributedInstance> blacklist) throws ResourceAllocationException {
		
		if(systems.isEmpty()) {
			throw new ResourceAllocationException("Unable to choose a system, list of systems is empty");
		}
		
		if(systems.size() == blacklist.size()) {
			throw new ResourceAllocationException("Unable to choose a system, size of blacklist and system list are equal: " + blacklist.size());
		}
		
		final List<DistributedInstance> availableSystems = new ArrayList<DistributedInstance>(systems);
		availableSystems.removeAll(blacklist);
		
		final int element = Math.abs(randomGenerator.nextInt()) % availableSystems.size();
		return availableSystems.get(element);
	}
}
