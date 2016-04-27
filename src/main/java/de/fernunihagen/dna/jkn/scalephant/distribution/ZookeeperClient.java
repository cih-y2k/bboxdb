package de.fernunihagen.dna.jkn.scalephant.distribution;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.ScalephantConfiguration;
import de.fernunihagen.dna.jkn.scalephant.ScalephantConfigurationManager;
import de.fernunihagen.dna.jkn.scalephant.ScalephantService;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstanceManager;

public class ZookeeperClient implements ScalephantService, Watcher {
	
	/**
	 * The list of the zookeeper hosts
	 */
	protected final Collection<String> zookeeperHosts;
	
	/**
	 * The name of the scalephant cluster
	 */
	protected final String clustername;
	
	/**
	 * The zookeeper client instance
	 */
	protected ZooKeeper zookeeper;
	
	/**
	 * The name of the instance
	 */
	protected String instancename = null;
	
	/**
	 * The timeout for the zookeeper session
	 */
	protected final static int DEFAULT_TIMEOUT = 3000;
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ZookeeperClient.class);

	public ZookeeperClient(final Collection<String> zookeeperHosts, final String clustername) {
		super();
		this.zookeeperHosts = zookeeperHosts;
		this.clustername = clustername;
	}

	/**
	 * Connect to zookeeper
	 */
	@Override
	public void init() {
		if(zookeeperHosts == null || zookeeperHosts.isEmpty()) {
			logger.warn("No Zookeeper hosts are defined, not connecting to zookeeper");
			return;
		}
		
		try {
			zookeeper = new ZooKeeper(generateConnectString(), DEFAULT_TIMEOUT, this);
			createDirectoryStructureIfNeeded();
			
			if(instancename != null) {
				registerInstance();
			}
			
			readMembershipAndRegisterWatch();
		} catch (Exception e) {
			logger.warn("Got exception while connecting to zookeeper", e);
		}
	}

	/**
	 * Disconnect from zookeeper
	 */
	@Override
	public void shutdown() {
		if(zookeeper != null) {
			try {
				zookeeper.close();
			} catch (InterruptedException e) {
				logger.warn("Got exception while closing zookeeper connection", e);
			}
			zookeeper = null;
		}
	}
	
	/**
	 * Register a watch on membership changes. A watch is a one-time operation, the watch
	 * is reregistered on each method call.
	 */
	protected void readMembershipAndRegisterWatch() {
		try {
			final List<String> instances = zookeeper.getChildren(getNodesPath(clustername), this);
			final DistributedInstanceManager distributedInstanceManager = DistributedInstanceManager.getInstance();
			
			final Set<DistributedInstance> instanceSet = new HashSet<DistributedInstance>();
			for(final String member : instances) {
				instanceSet.add(new DistributedInstance(member));
			}
			
			distributedInstanceManager.updateInstanceList(instanceSet);
			
		} catch (KeeperException | InterruptedException e) {
			logger.warn("Unable to read membership and create a watch", e);
		}
	}
	
	/**
	 * Build a comma separated list of the zookeeper nodes
	 * @return
	 */
	protected String generateConnectString() {
		
		// No zookeeper hosts are defined
		if(zookeeperHosts == null) {
			logger.warn("No zookeeper hosts are defined");
			return "";
		}
		
		final StringBuilder sb = new StringBuilder();
		
		for(final String zookeeperHost : zookeeperHosts) {
		
			if(sb.length() != 0) {
				sb.append(",");
			}
			
			sb.append(zookeeperHost);
		}
	
		return sb.toString();
	}

	/**
	 * Zookeeper watched event
	 */
	@Override
	public void process(final WatchedEvent watchedEvent) {	
		
		logger.debug("Got zookeeper event: " + watchedEvent);
		
		// Ignore null parameter
		if(watchedEvent == null) {
			logger.warn("process called with an null argument");
			return;
		}
		
		// Ignore type=none event
		if(watchedEvent.getType() == EventType.None) {
			return;
		}
		
		// Process events
		if(watchedEvent.getPath() != null) {
			if(watchedEvent.getPath().equals(getNodesPath(clustername))) {
				readMembershipAndRegisterWatch();
			}
		} else {
			logger.warn("Got unknown zookeeper event: " + watchedEvent);
		}
	}
	
	/**
	 * Register this instance of the scalephant
	 * @param clustername
	 * @param ownInstanceName
	 */
	public void registerScalephantInstanceAfterConnect(final String ownInstanceName) {
		this.instancename = ownInstanceName;
	}
	
	/**
	 * Register the scalephant instance
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	protected void registerInstance() throws KeeperException, InterruptedException {
		final String instanceZookeeperPath = getNodesPath(clustername) + "/" + instancename;
		logger.info("Register instance on: " + instanceZookeeperPath);
		zookeeper.create(instanceZookeeperPath, "".getBytes(), ZooDefs.Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}

	/**
	 * Register the name of the cluster in the zookeeper directory
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected void createDirectoryStructureIfNeeded() throws KeeperException, InterruptedException {
		// /clusterpath/nodes - Node membership
		final String nodesPath = getNodesPath(clustername);
		createDirectoryStructureRecursive(nodesPath);
	}

	/**
	 * Get the path of the zookeeper clustername
	 * @param clustername
	 * @return
	 */
	protected String getClusterPath(final String clustername) {
		return "/" + clustername;
	}
	
	/**
	 * Get the path of the zookeeper nodes
	 * @param clustername
	 * @return
	 */
	protected String getNodesPath(final String clustername) {
		return "/" + clustername + "/nodes";
	}
	
	/**
	 * Get the path for the distribution group id queue
	 * @param distributionGroup
	 * @return
	 */
	protected String getDistributionGroupIdQueuePath(final String distributionGroup) {
		return "/" + clustername + "/" + distributionGroup + "/nameprefixqueue";
	}

	@Override
	public String getServicename() {
		return "Zookeeper Client";
	}
	
	/**
	 * Create the given directory structure recursive
	 * @param path
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected void createDirectoryStructureRecursive(final String path) throws KeeperException, InterruptedException {
		
		// Does the full path already exists?
		if(zookeeper.exists(path, this) != null) {
			return;
		}
		
		// Otherwise check and create all sub paths
		final String[] allNodes = path.split("/");
		final StringBuilder sb = new StringBuilder();
		
		// Start by 1 to skip the initial / 
		for (int i = 1; i < allNodes.length; i++) {
			final String nextNode = allNodes[i];
			sb.append("/");
			sb.append(nextNode);
			
			final String partialPath = sb.toString();
						
			if(zookeeper.exists(partialPath, this) == null) {
				logger.info(partialPath + " not found, creating");
				
				zookeeper.create(partialPath, 
						"".getBytes(), 
						ZooDefs.Ids.OPEN_ACL_UNSAFE, 
						CreateMode.PERSISTENT);
			}
		}
	}
	
	/**
	 * Get the next table id for a given distribution group
	 * @return
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 * @throws ZookeeperException 
	 */
	public int getNextTableIdForDistributionGroup(final String distributionGroup) throws ZookeeperException, KeeperException, InterruptedException {
		
		final String nodeprefix = "id-";
		final String distributionGroupIdQueuePath = getDistributionGroupIdQueuePath(distributionGroup);
		
		createDirectoryStructureRecursive(distributionGroupIdQueuePath);
		
		final String nodename = zookeeper.create(distributionGroupIdQueuePath + "/" + nodeprefix, 
				"".getBytes(), 
				ZooDefs.Ids.OPEN_ACL_UNSAFE, 
				CreateMode.PERSISTENT_SEQUENTIAL);
		
		// id-0000000063
		// Element 0: id-
		// Element 1: The number of the node
		final String[] splittedName = nodename.split(nodeprefix);
	
		try {
			return Integer.parseInt(splittedName[1]);
		} catch(NumberFormatException e) {
			logger.warn("Unable to parse number: " + splittedName[1], e);
			throw new ZookeeperException(e);
		}
	}

	//===============================================================
	// Test * Test * Test * Test * Test 
	//===============================================================
	public static void main(String[] args) throws KeeperException, InterruptedException, ZookeeperException {
		
		final ScalephantConfiguration scalephantConfiguration 
		     = ScalephantConfigurationManager.getConfiguration();
		
		final ZookeeperClient zookeeperClient 
		     = new ZookeeperClient(scalephantConfiguration.getZookeepernodes(), 
				                   scalephantConfiguration.getClustername());
		
		zookeeperClient.init();
		
		for(int i = 0; i < 10; i++) {
			int nextId = zookeeperClient.getNextTableIdForDistributionGroup("mygroup1");
			System.out.println("The next id is: " + nextId);
		}
		
		zookeeperClient.shutdown();
	}
	
}
