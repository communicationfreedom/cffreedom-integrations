/**
 * Information for using the CA Service Desk integration
 * 
 * @author markjacobsen.net (http://mjg2.net/code)
 * Copyright: Communication Freedom, LLC - http://www.communicationfreedom.com
 * 
 * Free to use, modify, redistribute.  Must keep full class header including 
 * copyright and note your modifications.
 * 
 * If this helped you out or saved you time, please consider...
 * 1) Donating: http://www.communicationfreedom.com/go/donate/
 * 2) Shoutout on twitter: @MarkJacobsen or @cffreedom
 * 3) Linking to: http://visit.markjacobsen.net
 */
 
To use the CA Service Desk integration you need to generate some files first.

1) Make sure you're in this directory [base]/config/servicedesk
2) Modify build.bat to include information specific to your environment
3) Run build.bat to generate and compile java class files from your WSDL
4) Run build.xml via ANT to generate a JAR file you can use
5) Add the JAR file to your classpath
