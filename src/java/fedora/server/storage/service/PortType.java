/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.service;

/**
 *
 * <p><b>Title:</b> PortType.java</p>
 * <p><b>Description:</b> A data structure for holding WSDL Port Type which
 * defines a set of abstract operations.</p>
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class PortType
{
  public String portTypeName;
  public AbstractOperation[] operations;
}