/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> ObjectNotInLowLevelStorage.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
public class ObjectNotInLowlevelStorageException extends LowlevelStorageException {
	private static final long serialVersionUID = 1L;
	
	public ObjectNotInLowlevelStorageException(String message, Throwable cause) {
		super(false, message, cause);
	}
	public ObjectNotInLowlevelStorageException(String message) {
		this(message, null);
	}
}
