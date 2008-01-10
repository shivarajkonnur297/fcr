/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage;

import java.io.InputStream;

import java.util.Date;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.BMechDSBindSpec;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodDefOperationBind;
import fedora.server.storage.types.MethodParmDef;

/**
 * Interface for reading Behavior Mechanism Objects.
 * 
 * @author Sandy Payette
 */
public interface BMechReader
        extends DOReader {

    public MethodDef[] getServiceMethods(Date versDateTime)
            throws ServerException;

    public MethodDefOperationBind[] getServiceMethodBindings(Date versDateTime)
            throws ServerException;

    public InputStream getServiceMethodsXML(Date versDateTime)
            throws ServerException;

    public BMechDSBindSpec getServiceDSInputSpec(Date versDateTime)
            throws ServerException;

    public MethodParmDef[] getServiceMethodParms(String methodName,
                                                 Date versDateTime)
            throws ServerException;
}
