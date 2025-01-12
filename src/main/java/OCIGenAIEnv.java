
/**
 * This class encapsulates environment-specific configuration settings for Oracle Cloud Infrastructure (OCI)
 * Generative AI service.
 *
 * It provides access to endpoint, region, compartment ID, and configuration file location/profile used by
 * OCI clients.
 */
public class OCIGenAIEnv {
    /**
     * The OCI Generative AI service is available in several regions.
     * https://docs.oracle.com/en-us/iaas/Content/generative-ai/overview.htm#regions
     * Here, we use the Generative AI service endpoint in Chicago region.
     */
    private static final String ENDPOINT = "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com";

    /**
     * The location of the OCI configuration file.
     */
    private static final String CONFIG_LOCATION = "~/.oci/config";

    /**
     * The profile name within the OCI configuration file to use.
     */
    private static final String CONFIG_PROFILE = "DEFAULT";

    /**
     * Returns the compartment ID that has the required policies setup for accessing OCI Gen AI.
     * Compartment ID is set in an environment variable COMPARTMENT_ID
     *
     * @return the compartment ID as a string
     */
    public static String getCompartmentId() {
        String compartment_id = System.getenv("COMPARTMENT_ID");
        return compartment_id;
    }

    /**
     * Returns the location of the OCI configuration file.
     *
     * @return the path to the configuration file
     */
    public static String getConfigLocation() {
        return CONFIG_LOCATION;
    }

    /**
     * Returns the profile name from the OCI configuration file to use.
     *
     * @return the profile name
     */
    public static String getConfigProfile() {
        return CONFIG_PROFILE;
    }

    /**
     * Returns the base URL of the OCI Generative AI service endpoint.
     *
     * @return the endpoint URL
     */
    public static String getEndpoint() {
        return ENDPOINT;
    }
}
