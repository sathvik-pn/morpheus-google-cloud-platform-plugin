package com.morpheusdata.googlecloudplatform.utils


import com.morpheusdata.model.Cloud
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.JsonFactory
import com.morpheusdata.model.NetworkProxy
import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

import groovy.util.logging.Slf4j

@Slf4j
class GoogleCloudComputeUtility {
    static CloseableHttpClient createHttpClient(ClientConfig clientConfig) {
        // clientConfig holds any custom configurations for the HttpClient
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
        
        if (clientConfig?.proxyHost && clientConfig?.proxyPort) {
            HttpHost proxy = new HttpHost(clientConfig.proxyHost, clientConfig.proxyPort)
            httpClientBuilder.setProxy(proxy)
            log.info("Using proxy: ${proxy.getHostName()}:${proxy.getPort()}")
        }

        // Configure a pooling connection manager for better performance under high load
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager()
        connectionManager.setMaxTotal(200) // Maximum total connections
        connectionManager.setDefaultMaxPerRoute(20) // Maximum connections per route
        httpClientBuilder.setConnectionManager(connectionManager)

        // Build and return the HttpClient
        return httpClientBuilder.build()
    }

    static protected CloudResourceManager getCloudResourceManager(String email, String privateKey, NetworkProxy networkProxy = null, String projectId = '') {
        if(!email || !privateKey) {
            throw new IllegalArgumentException("Email and private key are required to create CloudResourceManager instance.")
        }

        privateKey.replace('\r', '\\r')
        privateKey.replace('\n', '\\n')
        String credentialsString = """
{
  "type": "service_account",
  "project_id": "${projectId}",
  "private_key_id": "",
  "private_key": "${privateKey}",
  "client_email": "${email}",
  "client_id": "",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://accounts.google.com/o/oauth2/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs"
}
"""
        InputStream is = new ByteArrayInputStream(credentialsString.getBytes())
        GoogleCredential credential = GoogleCredential.fromStream(is).createScoped(Collections.singleton(CloudResourceManagerScopes.CLOUD_PLATFORM))
        ClientConfig clientConfig = new ClientConfig()
        if(networkProxy?.getProxyHost()) clientConfig.proxyHost = networkProxy.getProxyHost()
        if(networkProxy?.getProxyPort()) clientConfig.proxyPort = networkProxy.getProxyPort()
        HttpClient httpClient = createHttpClient(clientConfig)
        ApacheHttpTransport httpTransport = new ApacheHttpTransport(httpClient)
        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance()

        CloudResourceManager client =  new CloudResourceManager.Builder(
                httpTransport, JSON_FACTORY, null).setApplicationName("Morpheus/1.0")
                .setHttpRequestInitializer(credential).build()

        log.info("Created GoogleCloudResourceManager client. Base URL ${client.getBaseUrl()}")
        return client
    }

    static testConnection(String email, String privateKey, NetworkProxy networkProxy = null) {
        log.info("Testing connection to Google Cloud Platform ...")
        Map connectionResponse = [success: false, projects: [], activeProjects: []]
        try {
            CloudResourceManager cloudResourceManager = getCloudResourceManager(email, privateKey, networkProxy)
            ListProjectsResponse listProjectsResponse = cloudResourceManager.projects().list().execute()

            connectionResponse.success = true
            connectionResponse.projects = listProjectsResponse.getProjects()
            connectionResponse.activeProjects = listProjectsResponse.getProjects().findAll {
                it.getLifecycleState() == 'ACTIVE'
            }
            log.info("Successfully connected to Google Cloud Platform. \
                Found projects ${connectionResponse.projects}")
        } catch(GoogleJsonResponseException e2) {
            log.error("Failed to connect to Google Cloud Platform, GoogleJsonResponseException. \
                Error message: ${e2.getMessage()} Error details: ${e2.getDetails()} ")
        } catch (Exception e) {
            log.error("Failed to connect to Google Cloud Platform. \
                Error message: ${e.message}")
        }
        return connectionResponse
    }
}

class ClientConfig {
    String proxyHost
    Integer proxyPort
}