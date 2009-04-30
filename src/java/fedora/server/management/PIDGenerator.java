/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.io.IOException;

import fedora.common.PID;

/**
 * Interface for generating Fedora PIDs.
 * 
 * @author Paul Charlton
 * @author Chris Wilper
 */
public interface PIDGenerator {

    /**
     * Generate a new pid that is guaranteed to be unique, within the given
     * namespace.
     * 
     * @param namespace
     * @return the PID
     * @throws IOException
     */
    public PID generatePID(String namespace) throws IOException;

    /**
     * Get the last pid that was generated.
     * 
     * @return the last PID that was generated
     * @throws IOException
     */
    public PID getLastPID() throws IOException;

    /**
     * Cause the given PID to never be generated by the PID generator.
     * 
     * @param pid
     * @throws IOException
     */
    public void neverGeneratePID(String pid) throws IOException;
}
