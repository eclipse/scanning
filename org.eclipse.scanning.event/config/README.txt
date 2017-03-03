
This config directory is used to configure logging for the example server and client applications.

It does not configure logging for binaries made from the project and is purposely not included in the build.properties for that reason.
It is not in the classpath and is specified using -Dlogback.configurationFile=${project_loc:org.eclipse.scanning.event}/config/logback.xml