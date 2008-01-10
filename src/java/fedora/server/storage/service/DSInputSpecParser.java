/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.service;

import java.io.InputStream;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.storage.types.BMechDSBindRule;
import fedora.server.storage.types.BMechDSBindSpec;

/**
 * A class for parsing the special XML format in Fedora for a Datastream Input
 * Specification (DSInputSpec). A DSInputSpec exists within a Behavior Mechanism
 * Object (bmech), and is used to define the "data contract" between the service
 * represented by the bmech and a data object.
 * 
 * @author Sandy Payette
 */
public class DSInputSpecParser
        extends DefaultHandler
        implements Constants {

    /**
     * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
     */
    private HashMap nsPrefixMap;

    private boolean inDSInputLabel = false;

    private boolean inDSInputInstructions = false;

    private boolean inDSInputMIME = false;

    // Fedora Datastream Binding Spec objects
    private BMechDSBindSpec dsInputSpec;

    private BMechDSBindRule dsInputRule;

    private final String BMechPID;

    // Working variables...
    private Vector tmp_InputRules;

    /**
     * Constructor to enable another class to initiate the parsing
     */
    public DSInputSpecParser(String parentPID) {
        BMechPID = parentPID;
    }

    /**
     * Constructor allows this class to initiate the parsing
     */
    public DSInputSpecParser(String parentPID, InputStream in)
            throws RepositoryConfigurationException, ObjectIntegrityException {
        BMechPID = parentPID;
        XMLReader xmlReader = null;
        try {
            SAXParserFactory saxfactory = SAXParserFactory.newInstance();
            saxfactory.setValidating(false);
            SAXParser parser = saxfactory.newSAXParser();
            xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler(this);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces",
                                 false);
            xmlReader
                    .setFeature("http://xml.org/sax/features/namespace-prefixes",
                                false);
        } catch (Exception e) {
            throw new RepositoryConfigurationException("Internal SAX error while "
                    + "preparing for DSInputSpec datastream deserialization: "
                    + e.getMessage());
        }
        try {
            xmlReader.parse(new InputSource(in));
        } catch (Exception e) {
            throw new ObjectIntegrityException("Error parsing DSInputSpec datastream"
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public BMechDSBindSpec getServiceDSInputSpec() {
        return dsInputSpec;
    }

    @Override
    public void startDocument() throws SAXException {
        nsPrefixMap = new HashMap();
        tmp_InputRules = new Vector();
        dsInputSpec = new BMechDSBindSpec();
    }

    @Override
    public void endDocument() throws SAXException {
        dsInputSpec.dsBindRules =
                (BMechDSBindRule[]) tmp_InputRules
                        .toArray(new BMechDSBindRule[0]);
        tmp_InputRules = null;
        nsPrefixMap = null;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        nsPrefixMap.put(uri, prefix);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        StringBuffer sb = new StringBuffer();
        sb.append('&');
        sb.append(name);
        sb.append(';');
        char[] text = new char[sb.length()];
        sb.getChars(0, sb.length(), text, 0);
        characters(text, 0, text.length);
    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (inDSInputLabel) {
            dsInputRule.bindingLabel = new String(ch, start, length);
        } else if (inDSInputInstructions) {
            dsInputRule.bindingInstruction = new String(ch, start, length);
        } else if (inDSInputMIME) {
            StringTokenizer st =
                    new StringTokenizer(new String(ch, start, length), " ");
            String[] MIMETypes = new String[st.countTokens()];
            for (int i = 0; i < MIMETypes.length; i++) {
                MIMETypes[i] = st.nextToken();
            }
            dsInputRule.bindingMIMETypes = MIMETypes;
        }
    }

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes attrs) throws SAXException {
        if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputSpec")) {
            dsInputSpec.bDefPID = attrs.getValue("bDefPID");
            //SDP: bMechPID no longer embedded within DSBindingSpec inline metadata.
            //dsInputSpec.bMechPID = attrs.getValue("bMechPID");
            dsInputSpec.bMechPID = BMechPID;
            dsInputSpec.bindSpecLabel = attrs.getValue("label");
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInput")) {
            dsInputRule = new BMechDSBindRule();
            dsInputRule.bindingKeyName = attrs.getValue("wsdlMsgPartName");
            dsInputRule.maxNumBindings =
                    new Integer(attrs.getValue("DSMax")).intValue();
            dsInputRule.minNumBindings =
                    new Integer(attrs.getValue("DSMin")).intValue();
            dsInputRule.ordinality =
                    Boolean.parseBoolean(attrs.getValue("DSOrdinality"));
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputLabel")) {
            inDSInputLabel = true;
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputInstruction")) {
            inDSInputInstructions = true;
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSMIME")) {
            inDSInputMIME = true;
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputSpec")) {
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInput")) {
            tmp_InputRules.add(dsInputRule);
            dsInputRule = null;
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputLabel")) {
            inDSInputLabel = false;
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSInputInstruction")) {
            inDSInputInstructions = false;
        } else if (namespaceURI.equalsIgnoreCase(BINDING_SPEC.uri)
                && localName.equalsIgnoreCase("DSMIME")) {
            inDSInputMIME = false;
        }
    }
}
