package fedora.server.storage.translation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StreamWriteException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.DSBinding;
import fedora.server.utilities.StreamUtility;

/**
 *
 * <p><b>Title:</b> FOXMLDOSerializer.java</p>
 * <p><b>Description: Creates an XML serialization of a Fedora digital object 
 *       in accordance with the Fedora Object XML (FOXML) XML Schema defined at:
 *       http://www.fedora.info/definitions/1/0/foxml1-0.xsd.
 * 
 *       The serializer uses the currently instantiated digital object
 *       as input (see fedora.server.storage.types.DigitalObject). 
 * 
 *       The serializer will adapt its output to a specific translation contexts.
 *       See the static definitions of different translation contexts in 
 *       fedora.server.storage.translation.DOTranslationUtility. </b></p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2004 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class FOXMLDOSerializer
        implements DOSerializer {

	public static final String FOXML_NS="info:fedora/def:foxml/";
    public static final String FEDORA_AUDIT_NS="info:fedora/def:audit/";
    public static final String FOXML_PREFIX="foxml";

    public static final String FOXML_XSD_LOCATION="http://www.fedora.info/definitions/1/0/foxml1-0.xsd";
    public static final String XSI_NS="http://www.w3.org/2001/XMLSchema-instance";

    private String m_fedoraAuditPrefix="audit";
    private SimpleDateFormat m_formatter=
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private int m_transContext;

    public FOXMLDOSerializer() {
    }

    public DOSerializer getInstance() {
        return new FOXMLDOSerializer();
    }

	public void serialize(DigitalObject obj, OutputStream out, String encoding, int transContext)
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedEncodingException {
		System.out.println("Serializing using FOXMLDOSerializer for transContext: " + transContext);
		m_transContext=transContext;
        StringBuffer buf=new StringBuffer();
        appendXMLDeclaration(obj, encoding, buf);
        appendRootElementStart(obj, buf);
        appendProperties(obj, buf, encoding);
        appendAudit(obj, buf, encoding);
        appendDatastreams(obj, buf, encoding);
        appendDisseminators(obj, buf);
        appendRootElementEnd(buf);
        writeToStream(buf, out, encoding, true);
    }

    private void appendXMLDeclaration(DigitalObject obj, String encoding,
            StringBuffer buf) {
        buf.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
    }

    private void appendRootElementStart(DigitalObject obj, StringBuffer buf)
            throws ObjectIntegrityException {
        buf.append("<" + FOXML_PREFIX + ":digitalObject xmlns:" + FOXML_PREFIX + "=\""
                + StreamUtility.enc(FOXML_NS) + "\"\n");
        String indent="           ";
        // make sure XSI_NS is mapped...
        String xsiPrefix=(String) obj.getNamespaceMapping().get(XSI_NS);
        if (xsiPrefix==null) {
            xsiPrefix="fedoraxsi";
            obj.getNamespaceMapping().put(XSI_NS, "fedoraxsi"); // 99.999999999% chance this is unique
        }
        appendNamespaceDeclarations(indent,obj.getNamespaceMapping(),buf);
        // set schemaLocation to point to the official schema for FOXML.
        buf.append(indent + xsiPrefix + ":schemaLocation=\"" + 
        	StreamUtility.enc(FOXML_NS) + " "  + 
        	StreamUtility.enc(FOXML_XSD_LOCATION) + "\"\n");
        	
        if (obj.getPid()==null) {
            throw new ObjectIntegrityException("Object must have a pid.");
        }
		String uri = "info:fedora/" + obj.getPid();
		buf.append(indent + "PID=\"" + obj.getPid() 
				+ "\" URI=\"" + uri + "\"");
        buf.append(">\n");
    }

    private void appendNamespaceDeclarations(String prepend, Map URIToPrefix,
            StringBuffer buf) {
        Iterator iter=URIToPrefix.keySet().iterator();
        while (iter.hasNext()) {
            String URI=(String) iter.next();
            String prefix=(String) URIToPrefix.get(URI);           
            if (prefix!=null && !prefix.equals("")) {
                if (URI.equals(FEDORA_AUDIT_NS)) {
                    m_fedoraAuditPrefix=prefix;
                } else if (!URI.equals(FOXML_NS)) {
                    buf.append(prepend + "xmlns:" + prefix + "=\""
                            + StreamUtility.enc(URI) + "\"\n");
                }
            }
        }
        buf.append(prepend + "xmlns:" + m_fedoraAuditPrefix + "=\""
                + FEDORA_AUDIT_NS + "\"\n");
    }
    
	private void appendProperties(DigitalObject obj, StringBuffer buf, String encoding) 
			throws ObjectIntegrityException {
		
		String ftype = getTypeAttribute(obj);
		String state = obj.getState();
		String label = obj.getLabel();
		Date cdate = obj.getCreateDate();
		Date mdate = obj.getLastModDate();
		String cmodel = obj.getContentModelId();
		
		buf.append("    <" + FOXML_PREFIX + ":objectProperties>\n");
			
		if (ftype!=null) {
			buf.append("        <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:fType" + "\"" 
			+ " VALUE=\"" + ftype + "\"/>\n");
		}
		if (state!=null) {
			buf.append("        <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:state" + "\"" 
			+ " VALUE=\"" + state + "\"/>\n");
		}
		if (label!=null) {
			buf.append("        <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:label" + "\""
			+ " VALUE=\"" + StreamUtility.enc(label) + "\"/>\n"); 
		}
		if (cdate!=null) {
			buf.append("        <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:cDate" + "\""
			+ " VALUE=\"" + m_formatter.format(cdate) + "\"/>\n"); 
		}
		if (mdate!=null) {
			buf.append("        <" + FOXML_PREFIX  + ":property NAME=\"" + "info:fedora/def:dobj:mDate" + "\""
			+ " VALUE=\"" + m_formatter.format(mdate) + "\"/>\n"); 
		}
		if (cmodel!=null) {
			buf.append("        <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:cModel" + "\"" 
			+ " VALUE=\"" + StreamUtility.enc(cmodel) + "\"/>\n");	
		}
		Iterator iter = obj.getExtProperties().keySet().iterator();
		while (iter.hasNext()){
			String name = (String)iter.next();
			buf.append("        <" + FOXML_PREFIX + ":extproperty NAME=\"" + name + "\""
			+ " VALUE=\"" + obj.getExtProperty(name) + "\"/>\n"); 
		}
		buf.append("    </" + FOXML_PREFIX + ":objectProperties>\n");
	}
	
	private void appendDatastreams(DigitalObject obj, StringBuffer buf, String encoding)
			throws ObjectIntegrityException, UnsupportedEncodingException, 
			StreamIOException {
		Iterator iter=obj.datastreamIdIterator();
		while (iter.hasNext()) {
			String dsid = (String) iter.next();
			// AUDIT datastream is rebuilt from the latest in-memory audit trail
			// which is a separate array list in the DigitalObject class.
			// So, ignore it here.
			if (dsid.equals("AUDIT") || dsid.equals("FEDORA-AUDITTRAIL")) {
				continue;
			}
			// Given a datastream ID, get all the datastream versions.
			// Use the first version to pick up the attributes common to all versions.
			List dsList = obj.datastreams(dsid);
			for (int i=0; i<dsList.size(); i++) {
				//Datastream vds = validateDatastream((Datastream) dsList.get(i));
				Datastream vds = DOTranslationUtility.setDatastreamDefaults((Datastream) dsList.get(i));
				// insert the ds elements common to all versions.
				if (i==0) {
					buf.append("    <" + FOXML_PREFIX 
						+ ":datastream ID=\"" + vds.DatastreamID + "\""
						+ " URI=\"" + "info:fedora/" + obj.getPid() + "/" + vds.DatastreamID + "\"" 
						+ " STATE=\"" + vds.DSState + "\""
						+ " MIMETYPE=\"" + vds.DSMIME + "\""
						+ " FORMAT_URI=\"" + vds.DSFormatURI + "\""
						+ " CONTROL_GROUP=\"" + vds.DSControlGrp + "\""
						+ " VERSIONABLE=\"" + vds.DSVersionable + "\">\n");
				}
				// insert the ds version-level elements
				String dateAttr="";
				if (vds.DSCreateDT!=null) {
					dateAttr=" CREATED=\"" + m_formatter.format(vds.DSCreateDT) + "\"";
				}
				buf.append("        <" + FOXML_PREFIX 
					+ ":datastreamVersion ID=\"" + vds.DSVersionID + "\"" 
					+ " LABEL=\"" + StreamUtility.enc(vds.DSLabel) + "\""
					+ dateAttr
					+ " SIZE=\"" + vds.DSSize +  "\">\n");
			
				// if E or R insert ds content location as URL
				if (vds.DSControlGrp.equalsIgnoreCase("E") ||
					vds.DSControlGrp.equalsIgnoreCase("R") ) {
						buf.append("            <" + FOXML_PREFIX 
							+ ":contentLocation TYPE=\"" + "URL\""
							+ " REF=\"" 
							+ StreamUtility.enc(
								DOTranslationUtility.normalizeDSLocationURLs(
									obj.getPid(), vds, m_transContext).DSLocation) 
							+ "\"/>\n");	
				// if M insert ds content location as an internal identifier				
				} else if (vds.DSControlGrp.equalsIgnoreCase("M")) {
					buf.append("            <" + FOXML_PREFIX 
						+ ":contentLocation TYPE=\"" + "INTERNAL_ID\""
						+ " REF=\"" 
						+ StreamUtility.enc(
							DOTranslationUtility.normalizeDSLocationURLs(
								obj.getPid(), vds, m_transContext).DSLocation) 
						+ "\"/>\n");	
				// if X insert inline XML
				} else if (vds.DSControlGrp.equalsIgnoreCase("X")) {
					appendInlineXML(obj.getFedoraObjectType(), 
						(DatastreamXMLMetadata)vds, buf, encoding);
				}					
				// FUTURE: Add digest of datastream content 
				//(to be calculated in DefaultManagement).
				buf.append("            <" + FOXML_PREFIX + ":contentDigest TYPE=\"MD5\""
					+ " DIGEST=\"future: hash of content goes here\"/>\n"); 
				buf.append("        </" + FOXML_PREFIX + ":datastreamVersion>\n");
				// if it's the last version, wrap-up with closing datastream element.	
				if (i==(dsList.size() - 1)) {
					buf.append("    </" + FOXML_PREFIX + ":datastream>\n");
				}			
			}
		}
	}

	private void appendAudit(DigitalObject obj, StringBuffer buf, String encoding) 
			throws ObjectIntegrityException {
		if (obj.getAuditRecords().size()>0) {
			// Audit trail datastream re-created from audit records.
			// There is only ONE version of the audit trail datastream!
			buf.append("    <" + FOXML_PREFIX 
				+ ":datastream ID=\"" + "AUDIT" + "\"" 
				+ " URI=\"" + "info:fedora/" + obj.getPid() + "/AUDIT" + "\""
				+ " STATE=\"" + "A" + "\""
				+ " MIMETYPE=\"" + "text/xml" + "\""
				+ " FORMAT_URI=\"" + "info:fedora/format:xml:audit" + "\""
				+ " CONTROL_GROUP=\"" + "X" + "\""
				+ " VERSIONABLE=\"" + "NO" + "\">\n");
			// insert the ds version-level elements
			buf.append("        <" + FOXML_PREFIX 
				+ ":datastreamVersion ID=\"" + "AUDIT.0" + "\"" 
				+ " LABEL=\"" + "Fedora Object Audit Trail" + "\""
				+ " CREATED=\"" + m_formatter.format(obj.getCreateDate()) +  "\">\n");
			buf.append("            <" + FOXML_PREFIX + ":xmlContent>\n");
			buf.append("            <" + m_fedoraAuditPrefix + ":auditTrail xmlns:" 
						+ m_fedoraAuditPrefix + "=\"" + FEDORA_AUDIT_NS + "\">\n");
			for (int i=0; i<obj.getAuditRecords().size(); i++) {
				AuditRecord audit=(AuditRecord) obj.getAuditRecords().get(i);
				validateAudit(audit);
				buf.append("                <" + m_fedoraAuditPrefix + ":record ID=\""
						+ StreamUtility.enc(audit.id) + "\">\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":process type=\""
						+ StreamUtility.enc(audit.processType) + "\"/>\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":action>"
						+ StreamUtility.enc(audit.action)
						+ "</" + m_fedoraAuditPrefix + ":action>\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":componentID>"
						+ StreamUtility.enc(audit.componentID)
						+ "</" + m_fedoraAuditPrefix + ":componentID>\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":responsibility>"
						+ StreamUtility.enc(audit.responsibility)
						+ "</" + m_fedoraAuditPrefix + ":responsibility>\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":date>"
						+ m_formatter.format(audit.date)
						+ "</" + m_fedoraAuditPrefix + ":date>\n");
				buf.append("                    <" + m_fedoraAuditPrefix + ":justification>"
						+ StreamUtility.enc(audit.justification)
						+ "</" + m_fedoraAuditPrefix + ":justification>\n");
				buf.append("                </" + m_fedoraAuditPrefix + ":record>\n");
			}
			buf.append("            </" + m_fedoraAuditPrefix + ":auditTrail" + ">\n");
			buf.append("            </" + FOXML_PREFIX + ":xmlContent>\n");
			// FUTURE: Add digest of datastream content 
			// (to be calculated in DefaultManagement).
			buf.append("            <" + FOXML_PREFIX + ":contentDigest TYPE=\"MD5\""
				+ " DIGEST=\"future: hash of content goes here\"/>\n"); 
			buf.append("        </" + FOXML_PREFIX + ":datastreamVersion>\n");				
			buf.append("    </" + FOXML_PREFIX + ":datastream>\n");
		}
	}

	private void appendInlineXML(int fedoraObjectType, DatastreamXMLMetadata ds, 
		StringBuffer buf, String encoding)
		throws ObjectIntegrityException, UnsupportedEncodingException, StreamIOException {
			
		buf.append("            <" + FOXML_PREFIX + ":xmlContent>\n");
				
		// Relative Repository URLs: If it's a WSDL or SERVICE-PROFILE datastream 
		// in a BMech object search for any embedded URLs that are relative to
		// the local repository (like internal service URLs) and make sure they
		// are converted appropriately for the translation context.
        if ( fedoraObjectType==DigitalObject.FEDORA_BMECH_OBJECT &&
             (ds.DatastreamID.equals("SERVICE-PROFILE") || 
			  ds.DatastreamID.equals("WSDL")) ) {
			  	// FIXME! We need a more efficient way than to search
			  	// the whole block of inline XML. We really only want to 
			  	// look at service URLs in the XML.
	            buf.append(DOTranslationUtility.normalizeInlineXML(
	            	new String(ds.xmlContent, "UTF-8"), m_transContext));
        } else {
            appendXMLStream(ds.getContentStream(), buf, encoding);
        }
        buf.append("            </" + FOXML_PREFIX + ":xmlContent>\n");
    }

    private void appendXMLStream(InputStream in, StringBuffer buf, String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {
        if (in==null) {
            throw new ObjectIntegrityException("Object's inline xml "
                    + "stream cannot be null.");
        }
        try {
            byte[] byteBuf = new byte[4096];
            int len;
            while ( ( len = in.read( byteBuf ) ) != -1 ) {
                buf.append(new String(byteBuf, 0, len, encoding));
            }
        } catch (UnsupportedEncodingException uee) {
            throw uee;
        } catch (IOException ioe) {
            throw new StreamIOException("Error reading from inline xml datastream.");
        } finally {
            try {
                in.close();
            } catch (IOException closeProb) {
                throw new StreamIOException("Error closing read stream.");
            }
        }
    }

    private void appendDisseminators(DigitalObject obj, StringBuffer buf)
            throws ObjectIntegrityException {

        Iterator dissIdIter=obj.disseminatorIdIterator();
        while (dissIdIter.hasNext()) {
            String did=(String) dissIdIter.next();
            Iterator dissIter=obj.disseminators(did).iterator();
            List dissList = obj.disseminators(did);
            
            for (int i=0; i<dissList.size(); i++) {
                Disseminator vdiss = 
					DOTranslationUtility.setDisseminatorDefaults((Disseminator) obj.disseminators(did).get(i));              
				// insert the disseminator elements common to all versions.
				if (i==0) {
					buf.append("    <" + FOXML_PREFIX + ":disseminator ID=\"" + did
							+ "\" BDEF_CONTRACT_PID=\"" + vdiss.bDefID 
							+ "\" STATE=\"" + vdiss.dissState 
							+ "\" VERSIONABLE=\"" + vdiss.dissVersionable +"\">\n");
				}
				// insert the disseminator version-level elements
				String dissLabelAttr="";
				if (vdiss.dissLabel!=null && !vdiss.dissLabel.equals("")) {
					dissLabelAttr=" LABEL=\"" + StreamUtility.enc(vdiss.dissLabel) + "\"";
				}
				String dateAttr="";
				if (vdiss.dissCreateDT!=null) {
					dateAttr=" CREATED=\"" + m_formatter.format(vdiss.dissCreateDT) + "\"";
				}
				buf.append("        <" + FOXML_PREFIX 
					+ ":disseminatorVersion ID=\"" + vdiss.dissVersionID + "\"" 
					+ dissLabelAttr
					+ " BMECH_SERVICE_PID=\"" + vdiss.bMechID + "\""
					+ dateAttr 
					+  ">\n");
				
				// datastream bindings...	
				DSBinding[] bindings = vdiss.dsBindMap.dsBindings;
				buf.append("            <" + FOXML_PREFIX + ":serviceInputMap>\n");
				for (int j=0; j<bindings.length; j++){
					if (bindings[j].seqNo==null) { 
						bindings[j].seqNo = "";
					}
					String labelAttr="";
					if (bindings[j].bindLabel!=null && !bindings[j].bindLabel.equals("")) {
						labelAttr=" LABEL=\"" + StreamUtility.enc(bindings[j].bindLabel) + "\"";
					}
					String orderAttr="";
					if (bindings[j].seqNo!=null && !bindings[j].seqNo.equals("")) {
						orderAttr=" ORDER=\"" + bindings[j].seqNo + "\"";
					}				
	                buf.append("                <" + FOXML_PREFIX + ":datastreamBinding KEY=\""
	                        + bindings[j].bindKeyName + "\""
							+ " DATASTREAM_ID=\"" + bindings[j].datastreamID + "\""
							+ labelAttr
	                        + orderAttr
							+ "/>\n");
				}
				buf.append("            </" + FOXML_PREFIX + ":serviceInputMap>\n");
				buf.append("        </" + FOXML_PREFIX + ":disseminatorVersion>\n");
            }
			buf.append("    </" + FOXML_PREFIX + ":disseminator>\n");
        }
    }

    private void appendRootElementEnd(StringBuffer buf) {
        buf.append("</" + FOXML_PREFIX + ":digitalObject>");
    }
	
	private void validateAudit(AuditRecord audit) throws ObjectIntegrityException {
		if (audit.id==null) {
			throw new ObjectIntegrityException("Audit record must have id.");
		}
		if (audit.date==null) {
			throw new ObjectIntegrityException("Audit record must have date.");
		}
		if (audit.processType==null) {
			throw new ObjectIntegrityException("Audit record must have processType.");
		}
		if (audit.action==null) {
			throw new ObjectIntegrityException("Audit record must have action.");
		}
		if (audit.componentID==null) {
			audit.componentID = ""; // for backwards compatibility, no error on null
			// throw new ObjectIntegrityException("Audit record must have componentID.");
		}
		if (audit.responsibility==null) {
			throw new ObjectIntegrityException("Audit record must have responsibility.");
		}
		if (audit.justification==null) {
			throw new ObjectIntegrityException("Audit record must have justification.");
		}
	}
	
	private String getTypeAttribute(DigitalObject obj)
			throws ObjectIntegrityException {
		int t=obj.getFedoraObjectType();
		if (t==DigitalObject.FEDORA_BDEF_OBJECT) {
			return "FedoraBDefObject";
		} else if (t==DigitalObject.FEDORA_BMECH_OBJECT) {
			return "FedoraBMechObject";
		} else if (t==DigitalObject.FEDORA_OBJECT) {
			return "FedoraObject";
		} else {
			throw new ObjectIntegrityException("Object must have a FedoraObjectType.");
		}
	}
	
    private void writeToStream(StringBuffer buf, OutputStream out,
            String encoding, boolean closeWhenFinished)
            throws StreamIOException, UnsupportedEncodingException {
        try {
            out.write(buf.toString().getBytes(encoding));
            out.flush();
        } catch (IOException ioe) {
            throw new StreamWriteException("Problem serializing to FOXML: "
                    + ioe.getMessage());
        } finally {
            if (closeWhenFinished) {
                try {
                    out.close();
                } catch (IOException ioe2) {
                    throw new StreamWriteException("Problem closing stream after "
                            + " serializing to FOXML: " + ioe2.getMessage());
                }
            }
        }
    }
 }