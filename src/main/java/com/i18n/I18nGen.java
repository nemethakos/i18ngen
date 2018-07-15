package com.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static com.i18n.Constants.*;
import static com.i18n.Util.*;

public class I18nGen {

	/**
	 * HashMap holding public-id and their xhtml entity definition file content
	 */
	private HashMap<String, String> entities = new HashMap<String, String>();

	/**
	 * HashMap holding resource bundle names and their original content
	 */
	private List<ResourceBundle> resourceBundles = new ArrayList<ResourceBundle>();

	/**
	 * Directory for xhtml files
	 */
	private String xhtmlDir;

	/**
	 * Directory for resource bundles
	 */
	private String resourceBundleDir;

	/**
	 * Character encoding for XHTML files.
	 */
	private String xhtmlEncoding;

	/**
	 * Constructor with parameters
	 * 
	 * @param xhtmlDir    working directory
	 * @param entitiesDir directory for entities
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private I18nGen(String xhtmlDir, String resourceBundleDir, String xhtmlEncoding)
			throws FileNotFoundException, IOException {

		this.xhtmlDir = xhtmlDir;
		this.xhtmlEncoding = xhtmlEncoding;
		this.resourceBundleDir = resourceBundleDir;

		System.out.format("i18nGen utility%n" + //
				"XHTML Directory: %s%n" + //
				"Resource Bundle Dir (message_xx.properties): %s%n" + //
				"Character encoding for XHTML files: %s%n%n", //
				xhtmlDir, resourceBundleDir, xhtmlEncoding);

	}

	/**
	 * Loads the resource bundles from the resourceBundleDir
	 * 
	 * @throws Exception
	 */
	private void loadResourceBundles() throws Exception {
		traverse(new File(resourceBundleDir), new Visitor() {

			public boolean visit(File f) {

				String name = f.getName();

				if (getLocale(name) != null) {
					return true;
				}

				return false;
			}

			public void process(File f) throws Exception {

				String name = f.getName();
				String locale = getLocale(name);

				Properties p = loadPropertyFile(f);

				resourceBundles.add(new ResourceBundle(locale, f, p));

			}
		});
	}

	/**
	 * Parameters:<br>
	 * 1. working dir (directory containing xhtml files)<br>
	 * 2. directory containing resource bundles
	 */
	public static void main(String[] args) throws Exception {

		if (args.length >= 2) {
			String workingDir = args[0];
			String resourceBundleDir = args[1];

			String xhtmlEncoding = "ISO-8859-1";
			if (args.length > 2) {
				xhtmlEncoding = args[2];
			}
			new I18nGen(workingDir, resourceBundleDir, xhtmlEncoding).generate();
		} else {
			System.out.println(PARAMETERS);
			System.out.println(_1_XHTML_DIR_DIRECTORY_CONTAINING_XHTML_FILES);
			System.out.println(_2_RESOURCE_BUNDLE_DIR_DIRECTORY_CONTAINING_MESSAGES_XX_PROPERTIES_FILES);
		}

	}

	/**
	 * Loads the XML entities from classpath
	 * 
	 * @throws IOException when error happened
	 */
	private void initEntities() throws IOException {
		entities.put(W3C_DTD_XHTML_1_0_TRANSITIONAL_EN, loadFromClasspath(XHTML1_TRANSITIONAL_DTD));

		entities.put(W3C_ENTITIES_LATIN_1_FOR_XHTML_EN, loadFromClasspath(XHTML_LAT1_ENT));

		entities.put(W3C_ENTITIES_SYMBOLS_FOR_XHTML_EN, loadFromClasspath(XHTML_SYMBOL_ENT));

		entities.put(W3C_ENTITIES_SPECIAL_FOR_XHTML_EN, loadFromClasspath(XHTML_SPECIAL_ENT));

	}

	/**
	 * Process the file as the xhtml source, collects texts and puts them into the
	 * ResourceBundles
	 * 
	 * @param file the file to be processed.
	 * @throws Exception on any problem
	 */
	private void processFile(File file) throws Exception {

		if (file.getName().endsWith(XHTML)) {

			System.out.print(file.getName());

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), xhtmlEncoding));

			InputSource fis = new InputSource(br);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();

			builder.setEntityResolver(new EntityResolver() {

				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

					String content = entities.get(publicId);

					InputSource is = new InputSource(new StringReader(content));

					return is;
				}
			});

			Document document = builder.parse(fis);

			int counter = processDocument(file, document, 0);

			System.out.print(OPEN_PARENTHESIS + counter + NEW_MESSAGE_S_FOUND);

			String modified = getString(document);
			FileWriter fw = new FileWriter(file);
			fw.write(modified);
			fw.close();

		}

	}

	/**
	 * The given labelName with the associated text will be inserted into the
	 * primary and secondary
	 * 
	 * @param labelValue the value of the property
	 * @param labelName  the name of the property
	 */
	private void processLabel(String labelValue, String labelName) {

		for (ResourceBundle rb : resourceBundles) {

			addLabel(rb, labelValue, labelName);
		}
	}

	/**
	 * Process xhtml elements which are not <s:graphicImage>
	 * 
	 * @param file the XHTML file for the label name
	 * @param childNode {@link Node}
	 * @throws DOMException on DOM errors
	 */
	private int processNonTextNode(File file, Node childNode) throws DOMException {

		String nodeName = getNodeNameWithoutNameSpace(childNode.getNodeName());

		if (!nodeName.equals(GRAPHICIMAGE)) {

			NamedNodeMap attributes = childNode.getAttributes();

			if (attributes != null) {
				for (int j = 0; j < attributes.getLength(); j++) {

					Node item = attributes.item(j);
				
					if (item.getNodeName().equals(PATTERN)) {
						item.setNodeValue(DATE_TIME_FORMAT_EL);
					}

					if (item.getNodeName().equals(VALUE)) {
						String text = trim(item.getNodeValue());

						if (!containsNoLetters(text)) {
							String labelName = getLabelName(file, nodeName, text);

							processLabel(text, labelName);

							item.setNodeValue(geti18nLabel(labelName));

							return 1;
						}
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Process Text Node in .xhtml
	 * 
	 * @param childNode
	 * @throws DOMException
	 */
	private int processTextNode(File file, Node childNode) throws DOMException {
		String textContent = childNode.getTextContent();
		String text = trim(textContent);

		if (!containsNoLetters(text)) {

			String labelName = getLabelName(file, TEXT_LABEL, text);

			processLabel(text, labelName);

			childNode.setTextContent(geti18nLabel(labelName));
			return 1;
		}
		return 0;
	}

	/**
	 * Works on a single file system entry and calls itself recursively if it turns
	 * out to be a directory.
	 * 
	 * @param file A file or a directory to process
	 * @throws Exception
	 */
	private void traverse(File file, Visitor visitor) throws Exception {

		if (file.isDirectory()) {

			String entries[] = file.list();

			if (entries != null) {

				for (String entry : entries) {

					File fileToProcess = new File(file, entry);
					if (visitor.visit(fileToProcess)) {
						traverse(fileToProcess, visitor);
					}
				}
			}
		} else {
			if (visitor.visit(file)) {
				visitor.process(file);
			}
		}
	}

	/**
	 * Scan the files and saves the results.
	 * 
	 * @throws Exception if something fails.
	 */
	private void generate() throws Exception {

		initEntities();

		loadResourceBundles();

		if (resourceBundles.isEmpty()) {
			System.out.format(
					"Error: No message_xx.properties files found at %s. You have to create the message_XX.properties first!",
					resourceBundleDir);
		} else {
			processXHTMLFiles();
			saveAllProperties(resourceBundles);
		}
	}

	/**
	 * Process the xhtml files
	 * 
	 * @throws Exception
	 */
	public void processXHTMLFiles() throws Exception {

		traverse(new File(xhtmlDir), new Visitor() {

			public boolean visit(File f) {

				if (f.isDirectory()) {
					return true;
				} else if (f.getName().endsWith(XHTML)) {
					return true;
				}

				return false;
			}

			public void process(File f) throws Exception {

				processFile(f);

			}
		});
	}

	/**
	 * Process the XML Document, collects the textual string
	 * 
	 * @param document the Document instance to be processed.
	 * @param file     the xhtml file
	 * @param counter  initial processed label number
	 * @return the number of the processed labels
	 */
	private int processDocument(File file, Node document, int counter) {

		NodeList children = document.getChildNodes();

		if (children != null) {

			for (int i = 0; i < children.getLength(); i++) {

				Node childNode = children.item(i);

				if (childNode.getNodeType() == Node.TEXT_NODE) {

					counter += processTextNode(file, childNode);

				} else {

					counter += processNonTextNode(file, childNode);
				}

				counter = processDocument(file, childNode, counter);
			}
		}
		return counter;
	}

}
