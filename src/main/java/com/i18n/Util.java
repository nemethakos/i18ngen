package com.i18n;

import static com.i18n.Constants.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class Util {

	/**
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters with a
	 * preceding slash. Copied from the java.util.Properties class.
	 * 
	 * @param theString     the String to convert
	 * @param escapeSpace   if true the spaces will be escaped (for the key)
	 * @param escapeUnicode if true noncs ASCII characters will be escaped as
	 *                      unicode literals backslash uXXXXX
	 */
	public static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);

			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == CHAR_BACKSLASH) {
					outBuffer.append(CHAR_BACKSLASH);
					outBuffer.append(CHAR_BACKSLASH);
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {

			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					appendCharToOutputBufferAsUnicodeLiteral(outBuffer, aChar);
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	/**
	 * Appends the {@code aChar} to the {@code outBuffer} as unicode literal
	 * 
	 * @param outBuffer the {@link StringBuffer}
	 * @param aChar     the character to append
	 */
	private static void appendCharToOutputBufferAsUnicodeLiteral(StringBuffer outBuffer, char aChar) {
		outBuffer.append(CHAR_BACKSLASH);
		outBuffer.append(U);
		outBuffer.append(Util.toHex((aChar >> 12) & 0xF));
		outBuffer.append(Util.toHex((aChar >> 8) & 0xF));
		outBuffer.append(Util.toHex((aChar >> 4) & 0xF));
		outBuffer.append(Util.toHex(aChar & 0xF));
	}

	/**
	 * Convert a nibble to a hex character
	 * 
	 * @param nibble the nibble to convert.
	 */
	public static char toHex(int nibble) {
		return hexDigit[(nibble & 0xF)];
	}

	/**
	 * Returns true if the text contains only whitespaces and other non letter
	 * characters (EL expression content excluded). This is used to avoid marking
	 * the new messages with three asterisks, since usually the translator has
	 * nothing to do with this kind of messages.
	 * 
	 * @param text the String to be examined
	 * @return true if the text contains no letter characters
	 */
	public static boolean containsNoLetters(String text) {

		int length = text.length();
		if (text == null || length == 0) {
			return true;
		}

		int i = 0;
		while (i < length) {

			if (i + 1 < length && text.substring(i, i + 2).equals(EL_START)) {

				while (i < length && text.charAt(i) != '}') {
					i++;
				}

				if (i > length - 1 && text.charAt(length - 1) != '}') {
					return false;
				}

				if (i > length - 1 && text.charAt(length - 1) == '}') {
					return true;
				}

				i++;
			}

			if (i < length - 1 && Character.isLetter(text.charAt(i))) {
				return false;
			}

			i++;
		}
		return true;
	}

	/**
	 * Returns the XML document as a String.
	 * 
	 * @param doc Document instance.
	 * 
	 * @return the document as a String.
	 * @throws TransformerException error while transforming
	 */
	public static String getString(Document doc) throws TransformerException {

		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();

		Transformer transformer = tf.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, YES);
		transformer.setOutputProperty(HTTP_XML_APACHE_ORG_XSLT_INDENT_AMOUNT, String.valueOf(INDENT_4));

		transformer.transform(domSource, result);
		return writer.toString();
	}

	/**
	 * Returns true if the last character is an underscore
	 * 
	 * @param sb the StringBuilder instance
	 * @return true if the sb's last char is underscore
	 */
	static boolean isLastCharUnderscore(StringBuilder sb) {
		if (sb == null || sb.length() == 0) {
			return false;
		}
		return sb.charAt(sb.length() - 1) != CHAR_UNDERSCORE;
	}

	/**
	 * Converts an arbitrary text string into a valid EL label name. "This is a
	 * label" -> This_is_a_label. When no letters contained in this text, the
	 * "no_letters" String will be returned.
	 * 
	 * @param text the text to convert
	 * @return the converted label
	 */
	static String convertTextToLabel(String text) {
		StringBuilder sb = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isLetter(c)) {
				if (isLastCharUnderscore(sb)) {
					sb.append(UNDERSCORE);
				}
			} else {
				sb.append(c);
			}
		}

		if (sb.length() == 0) {
			return NO_LETTERS;
		}

		return sb.toString();

	}

	/**
	 * Returns a valid property name/EL variable name for the given file, nodeName
	 * and text.<br>
	 * The scheme is the following:<br>
	 * <b>file name without extension</b>_<b>xhtml tag name
	 * <i>or</i>TEXT</b>_<b>textual value converted to label</b>
	 * 
	 * @param file     The file of the .xhtml file
	 * @param nodeName the name of the node (the xhtml tag's name) or "TEXT" if the
	 *                 node is a textual node.
	 * @param text     the text node or the property value of the node.
	 * @return the label name.
	 */
	public static String getLabelName(File file, String nodeName, String text) {

		String fileName = file.getName();

		int dotIndex = fileName.length();
		dotIndex = fileName.lastIndexOf(DOT);
		if (nodeName == null) {
			nodeName = EMPTY_STRING;
		}

		String labelName;

		if (nodeName.length() > 0) {
			labelName = fileName.substring(0, dotIndex) + UNDERSCORE + nodeName + UNDERSCORE + convertTextToLabel(text);
		} else {
			labelName = fileName.substring(0, dotIndex) + UNDERSCORE + convertTextToLabel(text);
		}

		return labelName;

	}

	/**
	 * Strips the namespace with the double colon from the node name
	 * 
	 * @param nodeName the node name (e.g.: "s:button" )
	 * @return the node name without namespace ("button")
	 */
	public static String getNodeNameWithoutNameSpace(String nodeName) {

		int index = nodeName.indexOf(DOUBLE_COLON);
		if (index == -1) {
			return nodeName;
		}

		return nodeName.substring(index + 1);
	}

	/**
	 * Adds the labelName as a key to the Properties p with the value of text.
	 * 
	 * @param labelValue the value of the new property
	 * @param labelName  the name of the new property
	 */
	public static void addLabel(ResourceBundle rb, String labelValue, String labelName) {

		String oldLabelValue = rb.getProperties().getProperty(labelName);

		if (oldLabelValue == null) {
			rb.getNewProperties().put(labelName, labelValue);
		} else {

			rb.getOldButUsed().put(labelName, labelValue);

			if (!oldLabelValue.equals(labelValue)) {
				writeConflictMessage(rb, labelValue, labelName, oldLabelValue);
			}
		}
	}

	/**
	 * Writes conflicting property values message to ResourceBundle
	 */
	private static void writeConflictMessage(ResourceBundle resourceBundle, String labelValue, String labelName,
			String oldLabelValue) {
		resourceBundle.getMessages().add("Property: \"" + labelName + "\" already exist with value: \"" + //
				oldLabelValue + "\", the new value would have been: \"" + labelValue + "\"");
	}

	/**
	 * Removes characters from the beginning and end from {@code text} to whom
	 * {@link Character#isWhitespace(char)} returns true.
	 * 
	 * @param text the {@link String} to remove whitespaces from
	 * @return a new {@link String} without the whitespaces.
	 */
	public static String trim(String text) {

		StringBuilder sb = new StringBuilder(text);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
			sb.deleteCharAt(0);
		}
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * Writes a line ({@link Constants#COMMENT_LINE}) into the buffer {@code bw}
	 * 
	 * @param bw the BufferedWriter
	 * @throws IOException
	 */
	public static void writeLine(BufferedWriter bw) throws IOException {
		bw.write(COMMENT_LINE);
	}

	/**
	 * Writes the comment block into the buffer
	 * 
	 * @param rb {@link ResourceBundle}
	 * @param bw {@link BufferedWriter}
	 */
	public static void writeComments(ResourceBundle rb, BufferedWriter bw) throws IOException {
		writeLine(bw);
		bw.write(GENERATED_ON + new Date() + CRLF);
		writeLine(bw);

		if (rb.getMessages().size() > 0) {
			writeWarningsAsComments(rb, bw);
		}
	}

	private static void writeWarningsAsComments(ResourceBundle rb, BufferedWriter bw) throws IOException {
		bw.write(COMMENTS);

		for (String s : rb.getMessages()) {
			bw.write(HASHMARK + saveConvert(s, true, false) + CRLF);
		}
		bw.write(CRLF);
	}

	/**
	 * Writes the properties into a BufferedWriter
	 * 
	 * @param header     the header String
	 * @param properties Properties
	 * @param bw         BufferedWriter
	 * @throws IOException when an error happens
	 */
	public static void writeProperties(String header, Properties properties, BufferedWriter bw) throws IOException {
		ArrayList<String> propLines = new ArrayList<String>(properties.size());

		Enumeration<Object> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = properties.getProperty(key);
			String line = saveConvert(key, true, true) + EQUALS + saveConvert(value, false, true);
			propLines.add(line);

		}

		Collections.sort(propLines);

		String lead = EMPTY_STRING;
		String prev = EMPTY_STRING;
		writeLine(bw);
		bw.write(HASHMARK + saveConvert(header, true, false) + CRLF);
		writeLine(bw);

		for (String line : propLines) {
			lead = getLead(line);
			if (!prev.equals(lead)) {

				bw.write(NEWLINE);
				bw.write(PROPERTY_FILE_COMMENT_START + lead + NEWLINE);
			}
			prev = lead;
			bw.write(line + NEWLINE);
		}
	}

	/**
	 * Writes the header
	 * 
	 * @param string the header String
	 * @param bw     BufferedWriter
	 * @throws IOException when error happens
	 */
	static void writeHeader(String string, BufferedWriter bw) throws IOException {

		writeLine(bw);
		bw.write(HASHMARK + saveConvert(string, true, true) + CRLF);
		writeLine(bw);
	}

	/**
	 * Saves the property data to the given file name (overwrites the original)
	 * 
	 * @param rb ResourceBundle
	 * @throws IOException throwen if the operation is unsuccessfull
	 */
	static void saveProperties(ResourceBundle rb) throws IOException {

		System.out.println(SAVING + rb.getPropertiesFile().getName());

		addDateTimeFormatIfNotFound(rb);

		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(rb.getPropertiesFile()), ISO_8859_1));

		writeComments(rb, bw);

		normalizeOldProperties(rb);

		if (rb.getOldButUsed().size() > 0) {

			writeProperties(OLD_BUT_USED_PROPERTIES, rb.getOldButUsed(), bw);
		} else {
			writeHeader(THERE_ARE_NO_OLD_BUT_USED_PROPERTIES, bw);
		}

		if (rb.getNewProperties().size() > 0) {

			writeProperties(NEW_PROPERTIES, rb.getNewProperties(), bw);
		} else {
			writeHeader(THERE_ARE_NO_NEW_PROPERTIES, bw);
		}

		if (rb.getOldNotUsed().size() > 0) {

			writeProperties(OLD_AND_UNUSED_PROPERTIES_SOME_ENTRIES_MAY_BE_REMOVABLE_EXCEPT_JSF_AND_SEAM_PROPERTIES,
					rb.getOldNotUsed(), bw);
		} else {
			writeHeader(THERE_ARE_NO_OLD_AND_NOT_USED_PROPERTIES, bw);
		}

		bw.close();
	}

	private static void addDateTimeFormatIfNotFound(ResourceBundle rb) {
		if (rb.getProperties().getProperty(DATE_TIME_FORMAT) == null) {
			rb.getNewProperties().put(DATE_TIME_FORMAT, YYYY_MM_DD_HH_MM_SS_Z);
		}
	}

	/**
	 * old and not used properties = [old properties] - [old and used]
	 * 
	 * @param rb the resource bundle
	 */
	private static void normalizeOldProperties(ResourceBundle rb) {
		Enumeration<Object> old = rb.getProperties().keys();
		while (old.hasMoreElements()) {
			String oldKey = (String) old.nextElement();
			String oldValue = rb.getProperties().getProperty(oldKey);

			String oldButUsedValue = (String) rb.getOldButUsed().get(oldKey);

			if (oldButUsedValue == null) {
				rb.getOldNotUsed().put(oldKey, oldValue);
			}
		}

	}

	/**
	 * Loads the content of the property file.
	 * 
	 * @param f the file to load from
	 * @throws IOException if I/O error occurs
	 */
	public static Properties loadPropertyFile(File f) throws FileNotFoundException, IOException {

		Properties p = new Properties();

		p.load(new FileInputStream(f));

		return p;
	}

	/**
	 * Loads the given file from the classpath and returns the content as a String
	 * 
	 * @param path the path to the resource
	 * @return the file's content as String
	 * @throws IOException
	 */
	public static String loadFromClasspath(String path) throws IOException {

		InputStream is = I18nGen.class.getResourceAsStream(RES + path);
		BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF_8));
		StringBuilder sb = new StringBuilder();
		String line = null;
		do {
			line = br.readLine();
			if (line != null) {
				sb.append(line);
				sb.append(NEWLINE);
			}
		} while (line != null);

		return sb.toString();
	}

	/**
	 * Returns the header for a block of lines in the property file.
	 * 
	 * @param line the first line the header is made of.
	 * @return the header.
	 */
	public static String getLead(String line) {
		int index = line.indexOf(CHAR_UNDERSCORE);
		if (index == -1) {
			index = line.indexOf(CHAR_DOT);
		}
		if (index == -1) {
			index = line.indexOf('=');
		}

		if (index == -1) {

			return line;
		} else {
			return line.substring(0, index);
		}

	}

	/**
	 * Returns the inserted string for the label. The "#{messages." and the "}" will
	 * be appended before and after the String.
	 * 
	 * @param label the label to convert to an insertable EL experssion
	 * @return
	 */
	public static String geti18nLabel(String label) {

		return EL_START_MESSAGES + label + EL_ENDS;
	}

	/**
	 * Saves all the property files
	 * 
	 * @throws IOException
	 */
	public static void saveAllProperties(List<ResourceBundle> resourceBundles) throws IOException {

		for (ResourceBundle rb : resourceBundles) {
			saveProperties(rb);
		}
	}

	/**
	 * Returns the locale name from the resource bundle file name, or null.
	 * 
	 * @param name the file name
	 * @return the locale name or null if the file name is not the name of a
	 *         resource bundle file
	 */
	static String getLocale(String name) {
		String locale = null;

		if (name.startsWith(MESSAGES_) && name.endsWith(_PROPERTIES)) {
			locale = name.substring(MESSAGES_.length(), name.length() - _PROPERTIES.length());
		}

		return locale;
	}

}
