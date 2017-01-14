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
package org.bboxdb.network.packages.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import org.bboxdb.network.NetworkConst;
import org.bboxdb.network.NetworkHelper;
import org.bboxdb.network.NetworkPackageDecoder;
import org.bboxdb.network.packages.NetworkRequestPackage;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.routing.RoutingHeader;

public class CompressionEnvelopeRequest extends NetworkRequestPackage {
	
	/**
	 * The package to encode
	 */
	protected NetworkRequestPackage networkRequestPackage;
	
	/**
	 * The compression type
	 */
	protected byte compressionType;

	public CompressionEnvelopeRequest(final short sequenceNumber,
			final NetworkRequestPackage networkRequestPackage, final byte compressionType) {
		
		super(sequenceNumber);
		
		this.networkRequestPackage = networkRequestPackage;
		this.compressionType = compressionType;
	}

	public void writeToOutputStream(final OutputStream outputStream) throws PackageEncodeException {
		try {
			if(compressionType != NetworkConst.COMPRESSION_TYPE_GZIP) {
				throw new PackageEncodeException("Unknown compression method: " + compressionType);
			}
			
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final OutputStream os = new GZIPOutputStream(baos);
			networkRequestPackage.writeToOutputStream(os);
			os.close();
			final byte[] compressedBytes = baos.toByteArray();
			
			// Body length
			final long bodyLength = 1 + compressedBytes.length;

			// Unrouted package
			final RoutingHeader routingHeader = new RoutingHeader(false);
			appendRequestPackageHeader(bodyLength, routingHeader, outputStream);
			
			// Write body
			outputStream.write(compressionType);
			outputStream.write(compressedBytes);

		} catch (IOException e) {
			throw new PackageEncodeException("Got an IO Exception while writing compressed data");
		}
	}

	/**
	 * Decode the encoded package into a uncompressed byte stream 
	 * 
	 * @param encodedPackage
	 * @return
	 * @throws IOException 
	 * @throws PackageEncodeException 
	 */
	public static byte[] decodePackage(final ByteBuffer encodedPackage) throws PackageEncodeException {
		final boolean decodeResult = NetworkPackageDecoder.validateRequestPackageHeader(encodedPackage, NetworkConst.REQUEST_TYPE_COMPRESSION);
		
		if(decodeResult == false) {
			throw new PackageEncodeException("Unable to decode package");
		}
		
		final byte compressionType = encodedPackage.get();
		
		if(compressionType != NetworkConst.COMPRESSION_TYPE_GZIP) {
			throw new PackageEncodeException("Unknown compression type: " + compressionType);
		}
		
		final byte[] compressedBytes = new byte[encodedPackage.remaining()];
		encodedPackage.get(compressedBytes, 0, encodedPackage.remaining());
		
		return NetworkHelper.uncompressBytes(compressionType, compressedBytes);
	}
	
	@Override
	public byte getPackageType() {
		return NetworkConst.REQUEST_TYPE_COMPRESSION;
	}
	
}