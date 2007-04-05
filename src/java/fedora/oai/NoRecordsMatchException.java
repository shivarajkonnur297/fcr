/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.oai;

/**
 *
 * <p><b>Title:</b> NoRecordsMatchException.java</p>
 * <p><b>Description:</b> Signals that the combination of the values of the
 * from, until, set and metadataPrefix arguments results in an empty list.</p>
 *
 * <p>This may occur while fulfilling a ListIdentifiers or ListRecords request.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class NoRecordsMatchException
        extends OAIException {

	private static final long serialVersionUID = 1L;
	
    public NoRecordsMatchException() {
        super("noRecordsMatch", null);
    }

    public NoRecordsMatchException(String message) {
        super("noRecordsMatch", message);
    }

}