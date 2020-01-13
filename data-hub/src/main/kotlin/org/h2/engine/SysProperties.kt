package org.h2.engine

/**
 * The constants defined in this class are initialized from system properties.
 * Some system properties are per machine settings, and others are as a last
 * resort and temporary solution to work around a problem in the application or
 * database engine. Also there are system properties to enable features that
 * are not yet fully tested or that are not backward compatible.
 * <p>System properties can be set when starting the virtual machine:</p>
 *
 * <pre>
 *     java -Dh2.baseDir=/temp
 *     </pre>
 * They can be set within the application, but this must be done before loading
 * any classes of the database (before loading the JDBC driver):
 * <pre>
 *     System.setProperty(&quot;h2.baseDir&quot;, &quot;/temp&quot;);
 *     </pre>
 */
object SysProperties {

}