# README
This bundle is used to show a user interface submitting jobs to scan. 
Do the following steps to use the example:

1. Install and start an instance of activemq and start it
2. Set the broker in config.xml
3. Start the server. The example has a run config which starts an IApplication and looks at the config file 'config.xml' to create the objects.
4. Start the user interface including a java system property "org.eclipse.scanning.broker" set to the URI of your broker. Start an eclipse application debug instance ensuring that the org.eclise.scanning.xxx bundles are in the run. Go to the Xcen perspective and submit some runs.