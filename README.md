# Create a Java Troubleshooting Assistant leveraging Generative AI and LangChain4j

This is an example program demonstrating integration of Oracle Cloud Infrastructure (OCI) Generative AI into a Java program. The program creates a Java Troubleshooting Assistant leveraging Generative AI and LangChain4j. 

## Prerequisites
- If not installed, download and install [JDK 23](https://www.oracle.com/java/technologies/downloads/#java23). You can use [JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) as well; just change the source and target requirements in pom.xml to 21.
- Set up an OCI configuration file by following these [instructions](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/cliinstall.htm#configfile). Going through these steps will result in creating a `config` file whose location depends on your operating system; for example, for Unix-compatible environments, this is available at `~/.oci/config.`

## Build the project

You can either use your IDE to build this Maven project, or if you prefer a terminal window, use the following `mvn` command.
```shell
mvn verify
```
This action will create the `troubleshoot-assist-1.0.0.jar` file inside the `target` directory

## Run the project

* Set the COMPARTMENT_ID as an environment variable. 

If you are running from a terminal window, set it as shown below, replacing the value with your compartment identifier:
```shell
export COMPARTMENT_ID="ocid1.compartment.oc1..xxx"
```

Otherwise, update your IDE's configuration to add `COMPARTMENT_ID` in its environment variables. 

* Run the project with the following `mvn` command.
```shell
mvn exec:java -Dexec.mainClass=JavaTroubleshootingAssistant

```