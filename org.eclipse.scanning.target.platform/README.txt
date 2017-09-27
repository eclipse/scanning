
Eclipse Scanning Targets
************************


org.eclipse.scanning.target.platform.target
===========================================
Name: Scanning (Build)
What? Is the minimal/main target
Why? The travis build and test on github. The official dependencies
When? Build or you want just the scanning algorithms



org.eclipse.scanning.target.platform.fat.target
===============================================
Name: Scanning (Fat)
What? A target including the DAWN p2 site.
Why? Used to render a Scanning UI with a full client which can plot and map/reduce scans.
When? You want UI including plotting in the client



org.eclipse.scanning.target.platform.ispyb.target
=================================================
Name: Scanning (Fat+ISPyB)
What? Extends fat to include dependencies for the git@github.com:DiamondLightSource/gda-ispyb-api.git respository such as MariaDB
Why? Allows bundles to be developed which depend on scanning and ispyb-api
When? You want to talk to ISPyB using the API

