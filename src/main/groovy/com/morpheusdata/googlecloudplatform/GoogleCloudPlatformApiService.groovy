package com.morpheusdata.googlecloudplatform

import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse
import com.morpheusdata.core.MorpheusContext

import com.morpheusdata.core.util.HttpApiClient

import com.morpheusdata.model.Cloud
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.resourcemanager.v3.ProjectsClient
import com.google.cloud.resourcemanager.v3.ProjectsSettings
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.apache.ApacheHttpTransport

import com.google.api.client.json.JsonFactory
import com.google.api.services.servicemanagement.*
import com.google.api.services.bigquery.*
import com.google.api.services.storage.*

import org.apache.http.client.HttpClient
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.api.services.cloudresourcemanager.model.Project
//import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.api.client.http.javanet.NetHttpTransport
//import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.auth.oauth2.ServiceAccountCredentials


import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
 * API service for interfacing with Nutanix Prism Element (NPE)
 *
 * Helpful links:
 * - What is each API version for? -- https://www.nutanix.dev/api-versions/
 */
@Slf4j
class GoogleCloudPlatformApiService {
    MorpheusContext morpheusContext
    HttpApiClient httpApiClient

    GoogleCloudPlatformApiService(MorpheusContext morpheusContext) {
        this.morpheusContext = morpheusContext
        this.httpApiClient = new HttpApiClient()
    }

    static CloseableHttpClient createHttpClient(Map clientConfig) {
        // Configure connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager()
        connectionManager.setMaxTotal(100)
        connectionManager.setDefaultMaxPerRoute(20)

        // Build and return the HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build()
    }

    /**
     * Get the base URL for the API
     * @param cloud The cloud to get the base URL for
     * @return The base URL for the API
     */
    static String getBaseUrl(Cloud cloud) {
        return "https://www.googleapis.com/compute/v1/projects/${cloud.name}/zones"
    }

    static CloudResourceManager getCloudResourceManager(RequestConfig reqConfig) {
        def projectId = ''
        def clientEmail = reqConfig.email
        def privateKey = reqConfig.privateKey
        privateKey.replace('\r', '\\r')
        privateKey.replace('\n', '\\n')
        String credentialsString = """
{
  "type": "service_account",
  "project_id": "${projectId}",
  "private_key_id": "",
  "private_key": "${privateKey}",
  "client_email": "${clientEmail}",
  "client_id": "",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://accounts.google.com/o/oauth2/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs"
}
"""
        InputStream is = new ByteArrayInputStream( credentialsString.getBytes())
        GoogleCredential credential = GoogleCredential.fromStream(is).createScoped(Collections.singleton(CloudResourceManagerScopes.CLOUD_PLATFORM))
        log.info("SPN getGoogleCloudResourceManager credential: ${credential}")
        def clientConfig = [:]
        def proxyOptions
        HttpClient httpClient = createHttpClient(clientConfig)
        ApacheHttpTransport httpTransport = new ApacheHttpTransport(httpClient)
        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance()

        CloudResourceManager client =  new CloudResourceManager.Builder(
                httpTransport, JSON_FACTORY, null).setApplicationName("Morpheus/1.0")
                .setHttpRequestInitializer(credential).build()

        log.info("GCP Compute URL: ${client.baseUrl}")
        return client
    }

//    https://cloud.google.com/iam/docs/best-practices-for-managing-service-account-keys
//    https://developers.google.com/identity/protocols/oauth2/service-account#httprest
//    Unlike user accounts, service accounts don't have passwords. Instead, service accounts use RSA key pairs for authentication. If you know the private key of a service account's key pair, you can use the private key to create a JWT bearer token and use the bearer token to request an access token. The resulting access token reflects the service account's identity and you can use it to interact with Google Cloud APIs on the service account's behalf.
    static listProjects(HttpApiClient client, RequestConfig reqConfig) {
        // ui embed code prepares service account config json (similar but NOT SAME) - https://github.com/gomorpheus/morpheus-ui/blob/eda0f1e61cd2974916748e94892d966b3c0c7c09/clouds/google/src/main/groovy/com/morpheus/compute/GoogleComputeUtility.groovy#L3372
        // UI makes call
//        Request URL: http://localhost:8080/infrastructure/google/projects
//        Request Method: POST
        log.info("SPN Plugin listProjects RequestConfig: ${reqConfig}")
        def rtn = [success:false, projects:[]]
        try {
            CloudResourceManager cloudResourceManager = getCloudResourceManager(reqConfig)
            ListProjectsResponse response = cloudResourceManager.projects().list().execute()
            log.info("SPN Plugin listProjects response: ${response}")
            rtn.projects = response.getProjects()?.findAll { it.getLifecycleState() == 'ACTIVE' }
            rtn.success = true
            log.info("SPN Plugin listProjects success: ${rtn.success} projects: ${rtn.projects}")
//            [http-nio-8080-exec-6] SPN Plugin listProjects success: true projects: [[createTime:2025-04-16T11:19:27.147658Z, lifecycleState:ACTIVE, name:Sathvik Palakshappa Nulageri, parent:[id:1050191444432, type:folder], projectId:sathvik-palakshappa-nulageri, projectNumber:487639795848]]
        } catch(GoogleJsonResponseException e2) {
            log.error("SPN Plugin listProjects error: ${e2}", e2)
            if(e2.details?.errors) {
                rtn.errors = e2.details.errors.collect { it.message } as Serializable
            } else {
                rtn.errors = [e2.message]
            }
        } catch(e) {
            log.error("SPN Plugin listProjects error: ${e}", e)
        }

        return  rtn

    }

    public Map ListProjectsRaw(RequestConfig reqConfig) {
        Map<String, Object> result = [:]
        // 1. Service Account Details
        def serviceAccountEmail = reqConfig.email // "your-service-account@project-id.iam.gserviceaccount.com" // Replace
        def privateKeyString = reqConfig.privateKey // "PASTE_YOUR_PRIVATE_KEY_HERE" // Replace with the *string* of your private key
        privateKeyString = privateKeyString.replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "") //remove any whitespace

        // 2. Decode the Private Key (assuming PKCS#8 format)
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString)
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes)
            KeyFactory keyFactory = KeyFactory.getInstance("RSA") // Or "EC" if it's an elliptic curve key
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec)

            // 3. Create GoogleCredential
            def credential = new GoogleCredential.Builder()
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(new GsonFactory())
                    .setServiceAccountId(serviceAccountEmail)
                    .setServiceAccountPrivateKey(privateKey)
                    .setServiceAccountScopes(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"))
                    .build()

            // 4. Adapt GoogleCredential to ServiceAccountCredentials
            def serviceAccountCredentials = new ServiceAccountCredentials(credential)


            // 5. Create ProjectsClient
            ProjectsSettings projectsSettings = ProjectsSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(serviceAccountCredentials))
                    .build()

            ProjectsClient projectsClient = ProjectsClient.create(projectsSettings)

            // 6. List Projects
            String parent = "organizations/476859037668" // Replace with your Organization ID or "" for all projects.  sathvik-palakshappa-nulageri
            for (Project project : projectsClient.listProjects(parent).iterateAll()) {
                logger.info("Project Name: ${project.getDisplayName()}")
                logger.info("Project ID: ${project.getProjectId()}")
                logger.info("Project Number: ${project.getName()}")
                logger.info("State: ${project.getState()}")
                logger.info("---")
            }

            // 7. Close Client
            projectsClient.close()

        } catch (Exception e) {
            logger.error("Error: " + e.getMessage(), e)
        }

    }
}


class RequestConfig {
    String email
    String privateKey
    String apiUrl

    /**
     * Sets the API URL for the request, ensuring that the apiUrl is the base url without any path.
     * <p>
     * This allows us to be a bit more flexible with the apiUrl configuration, allowing for the full URL to be provided
     * without any negative consequences for the user.
     *
     * @param apiUrl the API URL
     */
    void setApiUrl(String apiUrl) {
        if (apiUrl) {
            if (apiUrl.startsWith('http')) {
                URIBuilder uriBuilder = new URIBuilder("${apiUrl}")
                uriBuilder.setPath('')
                apiUrl= uriBuilder.build().toString()
            } else {
                apiUrl = 'https://' + apiUrl + ':9440'
            }
        }
        this.apiUrl = apiUrl
    }
}
