package com.i18n;

import java.io.File;

/**
 * Interface for visiting and directories and files.
 */
public interface Visitor {

	/**
	 * Should return true if the file/directory should be processed
	 * 
	 * @param f the file or directory to be processed
	 * @return true if the file or directory should be processed
	 */
	public boolean visit(File f);

	/**
	 * Process the file
	 * 
	 * @param f the file to be processed
	 * @throws Exception
	 */
	public void process(File f) throws Exception;

}
