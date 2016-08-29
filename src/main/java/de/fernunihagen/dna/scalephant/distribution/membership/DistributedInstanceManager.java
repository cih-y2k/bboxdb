package de.fernunihagen.dna.scalephant.distribution.membership;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.scalephant.distribution.membership.event.DistributedInstanceAddEvent;
import de.fernunihagen.dna.scalephant.distribution.membership.event.DistributedInstanceChangedEvent;
import de.fernunihagen.dna.scalephant.distribution.membership.event.DistributedInstanceDeleteEvent;
import de.fernunihagen.dna.scalephant.distribution.membership.event.DistributedInstanceEvent;
import de.fernunihagen.dna.scalephant.distribution.membership.event.DistributedInstanceEventCallback;

public class DistributedInstanceManager {

	/**
	 * The event listener
	 */
	protected final List<DistributedInstanceEventCallback> listener = new CopyOnWriteArrayList<DistributedInstanceEventCallback>();
	
	/**
	 * The active scalephant instances
	 */
	protected final Map<InetSocketAddress, DistributedInstance> instances = new HashMap<InetSocketAddress, DistributedInstance>();
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(DistributedInstanceManager.class);

	/**
	 * The instance
	 */
	protected static DistributedInstanceManager instance;
	
	/**
	 * Get the instance
	 * @return
	 */
	public static synchronized DistributedInstanceManager getInstance() {
		if(instance == null) {
			instance = new DistributedInstanceManager();
		}
		
		return instance;
	}
	
	/**
	 * Private constructor to prevent instantiation
	 */
	private DistributedInstanceManager() {
	}
	
	/**
	 * Singletons are not clonable
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Unable to clone a singleton");
	}
	
	/**
	 * Update the instance list, called from zookeeper client
	 * @param newInstances
	 */
	public void updateInstanceList(final Set<DistributedInstance> newInstances) {
		
		// Are members removed?
		final List<InetSocketAddress> deletedInstances = new ArrayList<InetSocketAddress>(instances.size());
		deletedInstances.addAll(instances.keySet());
		
		// Remove still existing instances from 'to delete list'
		for(final DistributedInstance newInstance : newInstances) {
			deletedInstances.remove(newInstance.getInetSocketAddress());
		}
		
		for(final InetSocketAddress inetSocketAddress : deletedInstances) {
			final DistributedInstance deletedInstance = instances.remove(inetSocketAddress);
			sendEvent(new DistributedInstanceDeleteEvent(deletedInstance));
		}

		for(final DistributedInstance instance : newInstances) {
			
			final InetSocketAddress inetSocketAddress = instance.getInetSocketAddress();
			
			// Any new member?
			if( ! instances.containsKey(inetSocketAddress)) {
				instances.put(inetSocketAddress, instance);
				sendEvent(new DistributedInstanceAddEvent(instance));
			} else {
				// Changed member?
				if(! instances.get(inetSocketAddress).equals(instance)) {
					instances.put(inetSocketAddress, instance);
					sendEvent(new DistributedInstanceChangedEvent(instance));
				}
			}
		}
	}
	
	/**
	 * Send a event to all the listeners
	 * 
	 * @param event
	 */
	protected void sendEvent(final DistributedInstanceEvent event) {
		logger.info("Sending event: " + event);

		for(final DistributedInstanceEventCallback callback : listener) {
			callback.distributedInstanceEvent(event);
		}
	}
	
	/**
	 * Register a callback listener
	 * @param callback
	 */
	public void registerListener(final DistributedInstanceEventCallback callback) {
		listener.add(callback);
	}
	
	/**
	 * Remove a callback listener
	 * @param callback
	 */
	public void removeListener(final DistributedInstanceEventCallback callback) {
		listener.remove(callback);
	}
	
	/**
	 * Remove all listener
	 */
	public void removeAllListener() {
		listener.clear();
	}
	
	/**
	 * Get the list of the instances
	 * @return
	 */
	public List<DistributedInstance> getInstances() {
		return new ArrayList<DistributedInstance>(instances.values());
	}
	
	/**
	 * Disconnect from zookeeper, all instances are unregistered
	 */
	public void zookeeperDisconnect() {
		
		logger.info("Zookeeper disconnected, sending delete events for all instances");
		
		for(final DistributedInstance instance : instances.values()) {
			sendEvent(new DistributedInstanceDeleteEvent(instance));
		}
		
		instances.clear();
	}
}
