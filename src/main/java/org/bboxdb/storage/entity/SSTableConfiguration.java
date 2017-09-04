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

public class SSTableConfiguration {
	
	/**
	 * Allow duplicates
	 */
	protected boolean allowDuplicates = false;
	
	/**
	 * The ttl
	 */
	protected long ttl = 0;
	
	/**
	 * The amount of versions per tuple
	 */
	protected int versions = 0;
	
	/**
	 * The spatial index writer
	 */
	protected String spatialIndexWriter = "org.bboxdb.storage.sstable.spatialindex.rtree.RTreeBuilder";
	
	/**
	 * The spatial index reader
	 */
	protected String spatialIndexReader = "org.bboxdb.storage.sstable.spatialindex.rtree.mmf.RTreeMMFReader";

	public boolean isAllowDuplicates() {
		return allowDuplicates;
	}

	public void setAllowDuplicates(final boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}

	public long getTTL() {
		return ttl;
	}

	public void setTtl(final long ttl) {
		this.ttl = ttl;
	}

	public int getVersions() {
		return versions;
	}

	public void setVersions(final int versions) {
		this.versions = versions;
	}

	public String getSpatialIndexWriter() {
		return spatialIndexWriter;
	}

	public void setSpatialIndexWriter(final String spatialIndexWriter) {
		this.spatialIndexWriter = spatialIndexWriter;
	}

	public String getSpatialIndexReader() {
		return spatialIndexReader;
	}

	public void setSpatialIndexReader(final String spatialIndexReader) {
		this.spatialIndexReader = spatialIndexReader;
	}

	@Override
	public String toString() {
		return "SSTableConfiguration [allowDuplicates=" + allowDuplicates + ", ttl=" + ttl + ", versions=" + versions
				+ ", spatialIndexWriter=" + spatialIndexWriter + ", spatialIndexReader=" + spatialIndexReader + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (allowDuplicates ? 1231 : 1237);
		result = prime * result + ((spatialIndexReader == null) ? 0 : spatialIndexReader.hashCode());
		result = prime * result + ((spatialIndexWriter == null) ? 0 : spatialIndexWriter.hashCode());
		result = prime * result + (int) (ttl ^ (ttl >>> 32));
		result = prime * result + versions;
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
		SSTableConfiguration other = (SSTableConfiguration) obj;
		if (allowDuplicates != other.allowDuplicates)
			return false;
		if (spatialIndexReader == null) {
			if (other.spatialIndexReader != null)
				return false;
		} else if (!spatialIndexReader.equals(other.spatialIndexReader))
			return false;
		if (spatialIndexWriter == null) {
			if (other.spatialIndexWriter != null)
				return false;
		} else if (!spatialIndexWriter.equals(other.spatialIndexWriter))
			return false;
		if (ttl != other.ttl)
			return false;
		if (versions != other.versions)
			return false;
		return true;
	}

}