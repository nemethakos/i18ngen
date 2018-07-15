package com.i18n;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class for holding information about a resource bundle
 */
public class ResourceBundle {

	/**
	 * The locale from the file name
	 */
	private String locale;
	/**
	 * Conflicting properties messages
	 */
	private List<String> messages = new ArrayList<String>();
	
	/**
	 * Properties newly added
	 */
	private Properties newProperties = new Properties();
	
	/**
	 * Old properties which are referenced in the xhtml files
	 */
	private Properties oldButUsed = new Properties();
	
	/**
	 * Old property which is not used (not found)
	 */
	private Properties oldNotUsed = new Properties();
	
	/**
	 * The properties from the file
	 */
	private Properties properties;
	
	/**
	 * Reference to the file
	 */
	private File propertiesFile;
	/**
	 * Constructor
	 * @param locale name of the locale for the resource bundle (e.g.: "en")
	 * @param propertiesFile the reference to the file
	 * @param properties the loaded properties
	 */
	protected ResourceBundle(String locale, File propertiesFile, Properties properties) {
		super();
		this.locale = locale;
		this.propertiesFile = propertiesFile;
		this.properties = properties;
	}
	/**
	 * @return the locale
	 */
	public String getLocale() {
		return this.locale;
	}

	/**
	 * @return the messages
	 */
	public List<String> getMessages() {
		return this.messages;
	}
	/**
	 * @return the newProperties
	 */
	public Properties getNewProperties() {
		return this.newProperties;
	}
	/**
	 * @return the oldButUsed
	 */
	public Properties getOldButUsed() {
		return this.oldButUsed;
	}
	/**
	 * @return the oldNotUsed
	 */
	public Properties getOldNotUsed() {
		return this.oldNotUsed;
	}
	
	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return this.properties;
	}
	
	/**
	 * @return the propertiesFile
	 */
	public File getPropertiesFile() {
		return this.propertiesFile;
	}
	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}
	/**
	 * @param messages the messages to set
	 */
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}
	/**
	 * @param newProperties the newProperties to set
	 */
	public void setNewProperties(Properties newProperties) {
		this.newProperties = newProperties;
	}
	/**
	 * @param oldButUsed the oldButUsed to set
	 */
	public void setOldButUsed(Properties oldButUsed) {
		this.oldButUsed = oldButUsed;
	}
	/**
	 * @param oldNotUsed the oldNotUsed to set
	 */
	public void setOldNotUsed(Properties oldNotUsed) {
		this.oldNotUsed = oldNotUsed;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	/**
	 * @param propertiesFile the propertiesFile to set
	 */
	public void setPropertiesFile(File propertiesFile) {
		this.propertiesFile = propertiesFile;
	}
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourceBundle [locale=");
		builder.append(this.locale);
		builder.append(", messages=");
		builder.append(this.messages);
		builder.append(", newProperties=");
		builder.append(this.newProperties);
		builder.append(", oldButUsed=");
		builder.append(this.oldButUsed);
		builder.append(", oldNotUsed=");
		builder.append(this.oldNotUsed);
		builder.append(", properties=");
		builder.append(this.properties);
		builder.append(", propertiesFile=");
		builder.append(this.propertiesFile);
		builder.append("]");
		return builder.toString();
	}
	
}
