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
package org.bboxdb.storage.sstable.spatialindex.rtree;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.bboxdb.storage.StorageManagerException;
import org.bboxdb.storage.sstable.SSTableConst;
import org.bboxdb.storage.sstable.spatialindex.SpatialIndexEntry;
import org.bboxdb.util.io.DataEncoderHelper;

public class RTreeSerializer {

	/**
	 * The maximal node size
	 */
	protected int maxNodeSize;
	
	/**
	 * The root node
	 */
	protected RTreeDirectoryNode rootNode;

	/**
	 * The queue null element
	 */
	private static final RTreeDirectoryNode NULL_RTREE_ELEMENT = new RTreeDirectoryNode(-1);

	private final Deque<RTreeDirectoryNode> nodesQueue = new ArrayDeque<>();

	public RTreeSerializer(final RTreeDirectoryNode rootNode, final int maxNodeSize) {
		this.rootNode = rootNode;
		this.maxNodeSize = maxNodeSize;
	}

	public void writeToStream(final OutputStream outputStream) throws StorageManagerException {
		
		nodesQueue.clear();
		
		try {
			// Write the magic bytes
			outputStream.write(SSTableConst.MAGIC_BYTES_SPATIAL_RTREE_INDEX);

			// Write the tree configuration
			final ByteBuffer nodeSizeBytes = DataEncoderHelper.intToByteBuffer(maxNodeSize);
			outputStream.write(nodeSizeBytes.array());

			nodesQueue.push(rootNode);

			while(! nodesQueue.isEmpty()) {
				final RTreeDirectoryNode node = nodesQueue.pop();
				handleNewNode(outputStream, node);			    
			}
			
		} catch (IOException e) {
			throw new StorageManagerException(e);
		}
	}

	/**
	 * Handle the nodes
	 * @param outputStream
	 * @param nodesQueue
	 * @param node
	 * @throws IOException
	 */
	protected void handleNewNode(final OutputStream outputStream, final RTreeDirectoryNode node) 
			throws IOException {
		
		// Child does not exist
		if(node == NULL_RTREE_ELEMENT) {
			outputStream.write(RTreeSpatialIndexBuilder.MAGIC_CHILD_NODE_NOT_EXISTING);
			return;
		}

		outputStream.write(RTreeSpatialIndexBuilder.MAGIC_CHILD_NODE_FOLLOWING);
		final ByteBuffer nodeIdBytes = DataEncoderHelper.intToByteBuffer(node.getNodeId());
		outputStream.write(nodeIdBytes.array());

		// Write entry nodes
		writeEntryNodes(outputStream, node);

		// Write directory nodes
		addDirectoryNodesToQueue(nodesQueue, node);
	}

	/**
	 * Write the entry nodes to the output stream
	 * @param outputStream
	 * @param node
	 * @throws IOException
	 */
	protected void writeEntryNodes(final OutputStream outputStream, final RTreeDirectoryNode node) 
			throws IOException {
		
		final List<SpatialIndexEntry> indexEntries = node.getIndexEntries();
		for(int i = 0; i < maxNodeSize; i++) {
			if(i < indexEntries.size()) {
				outputStream.write(RTreeSpatialIndexBuilder.MAGIC_CHILD_NODE_FOLLOWING);
				indexEntries.get(i).writeToStream(outputStream);
			} else {
				outputStream.write(RTreeSpatialIndexBuilder.MAGIC_CHILD_NODE_NOT_EXISTING);
			}
		}
	}

	/**
	 * Add the directory nodes to the queue
	 * @param nodesQueue
	 * @param node
	 */
	protected void addDirectoryNodesToQueue(final Deque<RTreeDirectoryNode> nodesQueue, 
			final RTreeDirectoryNode node) {
		
		final List<RTreeDirectoryNode> directoryNodeChilds = node.getDirectoryNodeChilds();
		for(int i = maxNodeSize - 1; i >= 0; i--) {
			if(i < directoryNodeChilds.size()) {
				nodesQueue.addFirst(directoryNodeChilds.get(i));
			} else {
				nodesQueue.addFirst(NULL_RTREE_ELEMENT);
			}
		}
	}
	
}