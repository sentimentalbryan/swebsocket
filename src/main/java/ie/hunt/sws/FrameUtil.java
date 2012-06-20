/*
 *  
 *  Copyright (C) 2012 Roderick Baier
 *  Modifed 2012 Bryan Hunt
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */
package ie.hunt.sws;
import java.nio.ByteBuffer;

/**
 * Isolating methods which can be Unit tested, the methods in this class do not
 * require any mutable state.
 * 
 * @author 
 * 
 */
public class FrameUtil {
	/**
	 * @param opcode
	 *            The operation code
	 * @param masking
	 *            Is it masking? TODO:What is meant by this.
	 * @param data
	 *            The data
	 * @return
	 */
	public static ByteBuffer generateFrame(byte opcode, boolean masking,
			byte[] data) {
		ByteBuffer frame = ByteBuffer.allocate(data.length + 2);
		byte fin = (byte) 0x80;
		byte x = (byte) (fin | opcode);
		frame.put(x);
		int length = data.length;
		int length_field = 0;
		if (length < 126) {
			if (masking) {
				length = 0x80 | length;
			}
			frame.put((byte) length);
		} else if (length <= 65535) {
			length_field = 126;
			if (masking) {
				length_field = 0x80 | length_field;
			}
			frame.put((byte) length_field);
			frame.put((byte) length);
		} else {
			length_field = 127;
			if (masking) {
				length_field = 0x80 | length_field;
			}
			frame.put((byte) length_field);
			frame.put((byte) length);
		}
		frame.put(data);
		return frame;
	}
}
