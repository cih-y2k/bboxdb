package de.fernunihagen.dna.jkn.scalephant.storage;

import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.ScalephantService;
import de.fernunihagen.dna.jkn.scalephant.ScalephantConfiguration;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.BoundingBox;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.DeletedTuple;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.Tuple;
import de.fernunihagen.dna.jkn.scalephant.storage.sstable.SSTableManager;
import de.fernunihagen.dna.jkn.scalephant.util.State;

public class StorageManager implements ScalephantService, Storage {
	
	protected final String table;
	protected final ScalephantConfiguration configuration;
	protected final SSTableManager sstableManager;
	protected final State state;

	protected Memtable memtable;
	
	private final static Logger logger = LoggerFactory.getLogger(StorageManager.class);

	public StorageManager(final String table, final ScalephantConfiguration configuration) {
		super();
		this.table = table;
		this.configuration = configuration;
		this.state = new State(false);
		this.sstableManager = new SSTableManager(state, table, configuration);
	}

	public void init() {
		logger.info("Initalize the storage manager for table: " + table);
		
		// Init the memtable before the sstablemanager. This ensures, that the
		// sstable recovery can put entries into the memtable
		initNewMemtable();
		sstableManager.init();
		
		state.setReady(true);
	}

	/**
	 * Shutdown the instance of the storage manager
	 */
	public void shutdown() {
		state.setReady(false);
		memtable.shutdown();
		sstableManager.shutdown();
	}

	/**
	 * Is the shutdown complete
	 */
	public boolean isShutdownComplete() {
		
		if(sstableManager == null) {
			return true;
		}
		
		return sstableManager.isShutdownComplete();
	}
	
	/**
	 * Create a new storage manager
	 */
	protected void initNewMemtable() {
		memtable = new Memtable(table, 
				configuration.getMemtableEntriesMax(), 
				configuration.getMemtableSizeMax());
		
		memtable.init();
	}
	
	@Override
	public void put(final Tuple tuple) throws StorageManagerException {
		
		if(! state.isReady()) {
			throw new StorageManagerException("Storage manager is not ready");
		}
		
		synchronized (this) {	
			if(memtable.isFull()) {
				sstableManager.flushMemtable(memtable);
				initNewMemtable();
			}
		
			memtable.put(tuple);
		}
	}

	@Override
	public Tuple get(final String key) throws StorageManagerException {
		
		if(! state.isReady()) {
			throw new StorageManagerException("Storage manager is not ready");
		}
		
		// Read from memtable
		final Tuple memtableTuple = memtable.get(key);
		
		if(memtableTuple instanceof DeletedTuple) {
			return null;
		}
		
		if(memtableTuple != null) {
			return memtableTuple;
		}

		// Read from storage
		final Tuple storageTuple = sstableManager.get(key);
		
		if(storageTuple instanceof DeletedTuple) {
			return null;
		}
		
		return storageTuple;
	}
	
	/**
	 * Get all tuples inside of the bounding box
	 */
	@Override
	public Collection<Tuple> getTuplesInside(final BoundingBox boundingBox)
			throws StorageManagerException {
		
		final HashMap<String, Tuple> allTuples = new HashMap<String, Tuple>();
		
		// Query all memtables
		final Collection<Tuple> memtableTuples = memtable.getTuplesInside(boundingBox);
		
		// Query the sstables
		final Collection<Tuple> sstableResult = sstableManager.getTuplesInside(boundingBox);
		
		// Merge results
		memtableTuples.addAll(sstableResult);
		
		// Find the most recent version of the tuple
		for(final Tuple tuple : memtableTuples) {
			
			final String tupleKey = tuple.getKey();
			
			if(! allTuples.containsKey(tupleKey)) {
				allTuples.put(tupleKey, tuple);
			} else {
				// Update with an newer version
				if(allTuples.get(tupleKey).compareTo(tuple) < 0) {
					allTuples.put(tupleKey, tuple);
				}
			}
		}
		
		// Remove deleted tuples from result
		for(final Tuple tuple : allTuples.values()) {
			if(tuple instanceof DeletedTuple) {
				allTuples.remove(tuple.getKey());
			}
		}

		return allTuples.values();
	}

	@Override
	public void delete(final String key) throws StorageManagerException {
		
		if(! state.isReady()) {
			throw new StorageManagerException("Storage manager is not ready");
		}
		
		memtable.delete(key);
	}
	
	/**
	 * Clear all entries in the table
	 * 
	 * 1) Reject new writes to this table 
	 * 2) Clear the memtable
	 * 3) Shutdown the sstable flush service
	 * 4) Wait for shutdown complete
	 * 5) Delete all persistent sstables
	 * 6) Restart the service
	 * 7) Accept new writes
	 * 
	 */
	@Override
	public void clear() {
		shutdown();
		
		memtable.clear();
		
		while(! sstableManager.isShutdownComplete()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		try {
			sstableManager.deleteExistingTables();
		} catch (StorageManagerException e) {
			logger.error("Error during deletion", e);
		}
		
		init();
	}
	
	/**
	 * Returns if the storage manager is ready or not
	 * @return
	 */
	public boolean isReady() {
		
		if(sstableManager.isReady() == false) {
			return false;
		}
		
		return state.isReady();
	}

	@Override
	public String getServicename() {
		return "Storage Manager";
	}
}