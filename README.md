# simple-ping-service
Simple JAVA based ping service. Checks if web application is allived. Result is a HTML page. Can configured for sending notification via email when some web application status changed.

## Configuration 
See property sample file **ping.properties.sample**. Create your own file with name **ping.properties**

## REST ping service

Configuration keys:
- ping.module.X.url = request URL for check
- ping.module.X.name = module name for report
 
The JSON for REST ping service must contain two attributes **aeg** and **moodul**:

	{
	  "aeg": "2015-09-14T14:43:13.262+03:00",
	  "moodul": "pms_klient"
	}

## Simple URL check service
Configuration keys:
- check.module.X.url = request URL for check
- check.module.X.name = module name for report

Checks if the website is responding.

## Creating a jar File in Eclipse
In Eclipse Help contents, expand "Java development user guide" ==> "Tasks" ==> "Creating JAR files."  Follow the instructions for "Creating a new JAR file" or "Creating a new runnable JAR file."

## Creating a jar file using maven
mvn clean compile assembly:single