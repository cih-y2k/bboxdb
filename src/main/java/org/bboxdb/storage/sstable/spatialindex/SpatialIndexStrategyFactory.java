/*******************************************************************************
 *
 *    Copyright (C) 2015-2016
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
package org.bboxdb.storage.sstable.spatialindex;

import org.bboxdb.BBoxDBConfiguration;
import org.bboxdb.BBoxDBConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpatialIndexStrategyFactory {
	
	/**
	 * The cached instance of the factory
	 */
	protected static SpatialIndexStrategy cachedInstance = null;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(SpatialIndexStrategyFactory.class);

	/**
	 * Get an instance of the configured factory
	 * @return
	 */
	public static SpatialIndexStrategy getInstance() {
		
		if(cachedInstance != null) {
			return cachedInstance;
		}
		
		final BBoxDBConfiguration configuration = BBoxDBConfigurationManager.getConfiguration();
		final String factoryClass = configuration.getStorageSpatialIndexerFactory();
		
		if("none".equals(factoryClass)) {
			return null;
		}
		
		// Instance the classname
		try {
			final Class<?> classObject = Class.forName(factoryClass);
			
			if(classObject == null) {
				throw new ClassNotFoundException("Unable to locate class: " + factoryClass);
			}
			
			final Object factoryObject = classObject.newInstance();
			
			if(! (factoryObject instanceof SpatialIndexStrategy)) {
				throw new ClassNotFoundException(factoryClass + " is not a instance of SpatialIndexer");
			}
			
			cachedInstance = (SpatialIndexStrategy) factoryObject;
			
			return cachedInstance;
			
		} catch (Exception e) {
			logger.warn("Unable to instance class" + factoryClass, e);
			throw new RuntimeException(e);
		} 
	}
	
	/**
	 * Clear the cached instance
	 */
	public static void clearCache() {
		cachedInstance = null;
	}
	
}
