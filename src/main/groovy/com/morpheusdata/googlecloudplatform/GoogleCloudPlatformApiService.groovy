package com.morpheusdata.googlecloudplatform

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.util.ComputeUtility
import com.morpheusdata.core.util.HttpApiClient
import com.morpheusdata.model.AccountCredential
import com.morpheusdata.model.Cloud
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder

import com.bertramlabs.plugins.karman.CloudFile
import com.bertramlabs.plugins.karman.StorageProvider
import com.google.api.client.googleapis.GoogleUtils
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpStatusCodes
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.json.JsonHttpContent
import com.google.api.client.json.GenericJson
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.GenericData
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.api.services.cloudresourcemanager.model.ResourceId
import com.google.api.services.compute.Compute
import com.google.api.services.compute.ComputeScopes
import com.google.api.services.compute.model.AccessConfig
import com.google.api.services.compute.model.Address
import com.google.api.services.compute.model.AddressList
import com.google.api.services.compute.model.AttachedDisk
import com.google.api.services.compute.model.AttachedDiskInitializeParams
import com.google.api.services.compute.model.Disk
import com.google.api.services.compute.model.DiskAggregatedList
import com.google.api.services.compute.model.DiskList
import com.google.api.services.compute.model.DisksScopedList
import com.google.api.services.compute.model.FirewallList
import com.google.api.services.compute.model.Image
import com.google.api.services.compute.model.ImageList
import com.google.api.services.compute.model.Instance
import com.google.api.services.compute.model.MachineTypeList
import com.google.api.services.compute.model.Metadata
import com.google.api.services.compute.model.NetworkInterface
import com.google.api.services.compute.model.NetworkList
import com.google.api.services.compute.model.Operation
import com.google.api.services.compute.model.RegionList
import com.google.api.services.compute.model.ServiceAccount
import com.google.api.services.compute.model.Scheduling
import com.google.api.services.compute.model.Snapshot
import com.google.api.services.compute.model.Subnetwork
import com.google.api.services.compute.model.SubnetworkAggregatedList
import com.google.api.services.compute.model.SubnetworkList
import com.google.api.services.compute.model.SubnetworksScopedList
import com.google.api.services.compute.model.Tags
import com.google.api.services.compute.model.UsableSubnetwork
import com.google.api.services.compute.model.UsableSubnetworksAggregatedList
import com.google.api.services.compute.model.ZoneList
import com.google.api.services.storage.model.Bucket
import com.google.api.services.storage.model.ObjectAccessControl
import com.google.api.services.storage.model.StorageObject
import com.google.api.services.compute.model.DisksResizeRequest
import com.google.api.services.compute.model.InstancesSetMachineTypeRequest
import com.google.api.services.compute.model.InstancesSetLabelsRequest
import com.google.api.services.compute.model.ZoneSetLabelsRequest
import com.google.api.services.servicemanagement.*
import com.google.api.services.serviceusage.v1.ServiceUsage
import com.google.api.services.bigquery.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.morpheus.ComputeCapacityInfo
import com.morpheus.ComputeServer
import com.morpheus.ComputeServerType
import com.morpheus.ComputeZone
import com.google.api.services.compute.model.Project
import com.google.api.services.storage.*
import com.morpheus.MorpheusUtils
import com.morpheus.Network
import com.morpheus.VirtualImage
import com.morpheus.util.ApiUtility
import com.morpheus.util.CacheableInputStream
import com.morpheus.util.ComputeUtility
import groovy.json.JsonOutput
import groovy.util.logging.Commons
import groovyx.net.http.Method
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZUtils
import org.apache.http.client.HttpClient

/**
 * API service for interfacing with Nutanix Prism Element (NPE)
 *
 * Helpful links:
 * - What is each API version for? -- https://www.nutanix.dev/api-versions/
 */
@Slf4j
class GoogleCloudPlatformApiService {
    MorpheusContext morpheusContext
    ComputeUtility computeUtility
    HttpApiClient httpApiClient

    GoogleCloudPlatformApiService(MorpheusContext morpheusContext) {
        this.morpheusContext = morpheusContext
        this.computeUtility = new ComputeUtility(morpheusContext)
        this.httpApiClient = new HttpApiClient(morpheusContext)
    }

    /**
     * Get the base URL for the API
     * @param cloud The cloud to get the base URL for
     * @return The base URL for the API
     */
    static String getBaseUrl(Cloud cloud) {
        return "https://www.googleapis.com/compute/v1/projects/${cloud.name}/zones"
    }

    private static CloudResourceManager getGoogleCloudResourceManager(zone, apiConfig=null) {
        log.info("SPN getGoogleCloudResourceManager zone: ${zone} ###### apiConfig: ${apiConfig}")
        GoogleCredential credential = getGoogleCredentials(zone, apiConfig, Collections.singleton(com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes.CLOUD_PLATFORM))
        log.info("SPN getGoogleCloudResourceManager credential: ${credential}")
        def clientConfig = [:]
        def proxyOptions = zone ? zone.apiProxy : apiConfig?.proxyOptions
        if (proxyOptions) {
            clientConfig.proxyHost = proxyOptions.proxyHost
            clientConfig.proxyPort = proxyOptions.proxyPort
            clientConfig.proxyUser = proxyOptions.proxyUser
            clientConfig.proxyPassword = proxyOptions.proxyPassword
            clientConfig.proxyWorkstation = proxyOptions.proxyWorkstation
            clientConfig.proxyDomain = proxyOptions.proxyDomain
        }
        log.info("SPN getGoogleCloudResourceManager clientConfig: ${clientConfig}")
        log.info("SPN getGoogleCloudResourceManager proxyOptions: ${proxyOptions}")
        log.info("SPN getGoogleCloudResourceManager zone.apiProxy: ${zone?.apiProxy}")
        log.info("SPN getGoogleCloudResourceManager zone.apiProxy: HOST==${zone?.apiProxy?.proxyHost} PORT==${zone?.apiProxy?.proxyPort} USER==${zone?.apiProxy?.proxyUser} PASSWORD==${zone?.apiProxy?.proxyPassword} WORKSTATION==${zone?.apiProxy?.proxyWorkstation} DOMAIN==${zone?.apiProxy?.proxyDomain}")
        HttpClient httpClient = createHttpClient(clientConfig)
        ApacheHttpTransport httpTransport = new ApacheHttpTransport(httpClient)
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance()

        CloudResourceManager client =  new CloudResourceManager.Builder(
                httpTransport, JSON_FACTORY, null).setApplicationName("Morpheus/1.0")
                .setHttpRequestInitializer(credential).build()

        log.info("GCP Compute URL: ${client.baseUrl}")
        return client
    }

//    https://cloud.google.com/iam/docs/best-practices-for-managing-service-account-keys
//    Unlike user accounts, service accounts don't have passwords. Instead, service accounts use RSA key pairs for authentication. If you know the private key of a service account's key pair, you can use the private key to create a JWT bearer token and use the bearer token to request an access token. The resulting access token reflects the service account's identity and you can use it to interact with Google Cloud APIs on the service account's behalf.
    static listProjects(HttpApiClient client, RequestConfig reqConfig) {
        // ui embed code prepares service account config json (similar but NOT SAME) - https://github.com/gomorpheus/morpheus-ui/blob/eda0f1e61cd2974916748e94892d966b3c0c7c09/clouds/google/src/main/groovy/com/morpheus/compute/GoogleComputeUtility.groovy#L3372
        // UI makes call
//        Request URL: http://localhost:8080/infrastructure/google/projects
//        Request Method: POST
        log.info("SPN listProjects RequestConfig: ${reqConfig}")
        def rtn = [success:false, projects:[]]
        try {
            CloudResourceManager cloudResourceManager = getGoogleCloudResourceManager(opts.zone, opts.apiConfig)
            com.google.api.services.cloudresourcemanager.model.ListProjectsResponse response = cloudResourceManager.projects().list().execute()
            log.info("SPN listProjects response: ${response}")
            rtn.projects = response.getProjects()?.findAll { it.getLifecycleState() == 'ACTIVE' }
            rtn.success = true
        } catch(com.google.api.client.googleapis.json.GoogleJsonResponseException e2) {
            log.error("listProjects error: ${e2}", e2)
            if(e2.details?.errors) {
                rtn.errors = e2.details.errors.collect { it.message }
            } else {
                rtn.errors = [e2.message]
            }
        } catch(e) {
            log.error("listProjects error: ${e}", e)
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
