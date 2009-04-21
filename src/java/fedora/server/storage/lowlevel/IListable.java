/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://www.fedora.info/license/).
 */
package fedora.server.storage.lowlevel;

import java.util.Iterator;

/**
 * Interface for {@link ILowlevelStorage} implementations that are
 * capable of listing their contents. This capability is now required in order
 * to support Fedora's rebuilder utility.
 *
 * @author Chris Wilper
 * @since Fedora 3.2
 */
public interface IListable {

    /**
     * Lists all stored objects in no particular order.
     *
     * @return an iterator of all stored object pids.
     */
    Iterator<String> listObjects();

    /**
     * Lists all stored datastream versions in no particular order.
     *
     * @return an iterator of all stored datastream version keys, each in the
     *         following format: $pid "+" $dsId "+" $dsVersionId
     */
    Iterator<String> listDatastreams();

}
