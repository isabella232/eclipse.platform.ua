/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.help.standalone;

import org.eclipse.help.internal.standalone.StandaloneInfocenter;

/**
 * This program is used to start or stop Eclipse
 * Infocenter application.
 * This class can be used instantiated and used in a Java program,
 * or can be launched from command line.
 * 
 * Usage as a Java component: 
 * <ul>
 * <li> create an instantance of this class</li>
 * <li> call start() </li>
 * <li> infocenter will run, when no longer needed call shutdown(). </li>
 * </ul>
 */
public class Infocenter {
	private StandaloneInfocenter infocenter;
	/**
	* Constructs Infocenter
	* @param options array of String options and their values
	* 	Option <code>-eclipseHome dir</code> specifies Eclipse
	*  installation directory.
	*  It must be provided, when current directory is not the same
	*  as Eclipse installation directory.
	*  Additionally, most options accepted by Eclipse execuable are supported.
	*/
	public Infocenter(String[] options) {
		infocenter = new StandaloneInfocenter(options);
	}
	/**
	 * Starts the infocenter application.
	 */
	public void start() {
		infocenter.start();
	}
	/**
	 * Shuts-down the infocenter application.
	 */
	public void shutdown() {
		infocenter.shutdown();
	}
	/**
	 * Controls start up and shut down of infocenter from command line.
	 * @param args array of String containng options
	 *  Options are:
	 * 	<code>-command start | shutdown [-eclipsehome eclipseInstallPath] [platform options] [-vmargs [Java VM arguments]]</code>
	 *  where
	 *  <ul>
	 * 	<li><code>dir</code> specifies Eclipse installation directory;
	 * 	  it must be provided, when current directory is not the same
	 *    as Eclipse installation directory,</li>
	 *   <li><code>platform options</code> are other options that are supported by Eclipse Executable.</li>
	 *  <ul>
	 */
	public static void main(String[] args) {
		StandaloneInfocenter.main(args);
	}
}
