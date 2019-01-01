/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 the BBoxDB project
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
package org.bboxdb.network.server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import org.bboxdb.commons.math.Hyperrectangle;
import org.bboxdb.distribution.partitioner.SpacePartitioner;
import org.bboxdb.distribution.partitioner.SpacePartitionerCache;
import org.bboxdb.distribution.region.DistributionRegionIdMapper;
import org.bboxdb.distribution.zookeeper.ZookeeperException;
import org.bboxdb.misc.BBoxDBException;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.packages.response.MultipleTupleEndResponse;
import org.bboxdb.network.packages.response.MultipleTupleStartResponse;
import org.bboxdb.network.packages.response.PageEndResponse;
import org.bboxdb.network.query.ContinuousQueryPlan;
import org.bboxdb.network.server.connection.ClientConnectionHandler;
import org.bboxdb.storage.StorageManagerException;
import org.bboxdb.storage.entity.JoinedTuple;
import org.bboxdb.storage.entity.Tuple;
import org.bboxdb.storage.entity.TupleStoreName;
import org.bboxdb.storage.tuplestore.manager.TupleStoreManager;
import org.bboxdb.storage.tuplestore.manager.TupleStoreManagerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuousClientQuery implements ClientQuery {

	/**
	 * The bounding box of the query
	 */
	private final Hyperrectangle boundingBox;

	/**
	 * The client connection handler
	 */
	private final ClientConnectionHandler clientConnectionHandler;

	/**
	 * The package sequence of the query
	 */
	private final short querySequence;

	/**
	 * The request table
	 */
	private final TupleStoreName requestTable;

	/**
	 * The total amount of send tuples
	 */
	private long totalSendTuples;
	
	/**
	 * Is the continuous query active
	 */
	private boolean queryActive = true;

	/**
	 * The tuples for the given key
	 */
	private final BlockingQueue<Tuple> tupleQueue;

	/**
	 * The maximal queue capacity
	 */
	private final static int MAX_QUEUE_CAPACITY = 1024;
	
	/**
	 * The number of tuples per page
	 */
	private final static long TUPLES_PER_PAGE = 1;

	/**
	 * The tuple insert callback
	 */
	private final Consumer<Tuple> tupleInsertCallback;

	/**
	 * The tuple store manager
	 */
	private TupleStoreManager storageManager;

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ContinuousClientQuery.class);


	public ContinuousClientQuery(final ContinuousQueryPlan queryPlan,
			final ClientConnectionHandler clientConnectionHandler,
			final short querySequence) {

			this.boundingBox = queryPlan.getQueryRange();
			this.requestTable = new TupleStoreName(queryPlan.getStreamTable());
			
			this.clientConnectionHandler = clientConnectionHandler;
			this.querySequence = querySequence;
			this.tupleQueue = new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY);

			this.totalSendTuples = 0;

			// Add each tuple to our tuple queue
			this.tupleInsertCallback = (t) -> {

				// Is the tuple important for the query?
				if(! t.getBoundingBox().intersects(boundingBox)) {
					return;
				}

				final boolean insertResult = tupleQueue.offer(t);

				if(! insertResult) {
					logger.error("Unable to add tuple to continuous query, queue is full");
				}
			};

			try {
				init();
			} catch (BBoxDBException e) {
				logger.error("Got exception on init", e);
				queryActive = false;
			}
	}

	/**
	 * Init the query
	 * @param tupleStoreManagerRegistry
	 * @throws BBoxDBException
	 */
	private void init() throws BBoxDBException {

		try {
			final TupleStoreManagerRegistry storageRegistry
				= clientConnectionHandler.getStorageRegistry();

			final String fullname = requestTable.getDistributionGroup();
			final SpacePartitioner spacePartitioner = SpacePartitionerCache.getInstance()
					.getSpacePartitionerForGroupName(fullname);

			final DistributionRegionIdMapper regionIdMapper = spacePartitioner
					.getDistributionRegionIdMapper();

			final List<TupleStoreName> localTables
				= regionIdMapper.getLocalTablesForRegion(boundingBox, requestTable);

			if(localTables.size() != 1) {
				logger.error("Got more than one table for the continuous query {}", localTables);
				close();
				return;
			}

			final TupleStoreName tupleStoreName = localTables.get(0);

			storageManager = QueryHelper.getTupleStoreManager(storageRegistry, tupleStoreName);

			storageManager.registerInsertCallback(tupleInsertCallback);

			// Remove tuple store insert listener on connection close
			clientConnectionHandler.addConnectionClosedHandler((c) -> close());
		} catch (StorageManagerException | ZookeeperException e) {
			logger.error("Got an exception during query init", e);
			close();
		}
	}

	@Override
	public void fetchAndSendNextTuples(final short packageSequence) throws IOException, PackageEncodeException {

		try {
			long sendTuplesInThisPage = 0;
			clientConnectionHandler.writeResultPackage(new MultipleTupleStartResponse(packageSequence));

			while(queryActive) {
				if(sendTuplesInThisPage >= TUPLES_PER_PAGE) {
					clientConnectionHandler.writeResultPackage(new PageEndResponse(packageSequence));
					clientConnectionHandler.flushPendingCompressionPackages();
					return;
				}

				// Send next tuple or wait
				final Tuple tuple = tupleQueue.take();

				final JoinedTuple joinedTuple = new JoinedTuple(tuple, requestTable.getFullname());

				clientConnectionHandler.writeResultTuple(packageSequence, joinedTuple, true);
				totalSendTuples++;
				sendTuplesInThisPage++;
			}

			// All tuples are send
			clientConnectionHandler.writeResultPackage(new MultipleTupleEndResponse(packageSequence));
			clientConnectionHandler.flushPendingCompressionPackages();

		} catch (InterruptedException e) {
			logger.debug("Got interrupted excetion");
			close();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public boolean isQueryDone() {
		return (! queryActive);
	}

	@Override
	public void close() {
		logger.debug("Closing query {} (send {} result tuples)", querySequence, totalSendTuples);

		if(storageManager != null) {
			storageManager.removeInsertCallback(tupleInsertCallback);
		}

		queryActive = false;
	}

	@Override
	public long getTotalSendTuples() {
		return totalSendTuples;
	}
}
