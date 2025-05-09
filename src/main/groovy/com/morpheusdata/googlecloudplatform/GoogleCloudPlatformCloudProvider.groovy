package com.morpheusdata.googlecloudplatform

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.providers.CloudProvider
import com.morpheusdata.core.providers.ProvisionProvider
import com.morpheusdata.googlecloudplatform.utils.GoogleCloudComputeUtility
import com.morpheusdata.model.BackupProvider
import com.morpheusdata.model.Cloud
import com.morpheusdata.model.CloudFolder
import com.morpheusdata.model.CloudPool
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.ComputeServerType
import com.morpheusdata.model.Datastore
import com.morpheusdata.model.Icon
import com.morpheusdata.model.Network
import com.morpheusdata.model.NetworkProxy
import com.morpheusdata.model.NetworkSubnetType
import com.morpheusdata.model.NetworkType
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.StorageControllerType
import com.morpheusdata.model.StorageVolumeType
import com.morpheusdata.request.ValidateCloudRequest
import com.morpheusdata.response.ServiceResponse
import groovy.util.logging.Slf4j

@Slf4j

class GoogleCloudPlatformCloudProvider implements CloudProvider {
	public static final String CLOUD_PROVIDER_CODE = 'google-cloud-platform-plugin.cloud'

	protected MorpheusContext context
	protected Plugin plugin

	public GoogleCloudPlatformCloudProvider(Plugin plugin, MorpheusContext ctx) {
		super()
		this.@plugin = plugin
		this.@context = ctx
	}

	/**
	 * Grabs the description for the CloudProvider
	 * @return String
	 */
	@Override
	String getDescription() {
		return """Google Cloud Platform is a suite of cloud computing services offered by Google"""
	}

	/**
	 * Returns the Cloud logo for display when a user needs to view or add this cloud. SVGs are preferred.
	 * @since 0.13.0
	 * @return Icon representation of assets stored in the src/assets of the project.
	 */
	@Override
	Icon getIcon() {
		return new Icon(path:'google-cloud.svg', darkPath:'google-cloud-dark.svg')
	}

	/**
	 * Returns the circular Cloud logo for display when a user needs to view or add this cloud. SVGs are preferred.
	 * @since 0.13.6
	 * @return Icon
	 */
	@Override
	Icon getCircularIcon() {
		return new Icon(path:'google-cloud-circular.svg', darkPath:'google-cloud-dark-circular.svg')
	}

	/**
	 * Provides a Collection of OptionType inputs that define the required input fields for defining a cloud integration
	 * @return Collection of OptionType
	 */
	@Override
	Collection<OptionType> getOptionTypes() {
		Collection<OptionType> options = []

		int displayOrderStart = 0
		options << new OptionType(
				name: 'Credentials',
				code: 'zoneType.google.credential',
				inputType: OptionType.InputType.CREDENTIAL,
				required: true,
				defaultValue: 'username-api-key',
				displayOrder: displayOrderStart,
				optionSource: 'credentials',
				config: '{"credentialTypes":["local", "email-private-key", "username-api-key"]}',
				// loads validateCloudRequest.{credentialUsername, credentialPassword}

				fieldName: 'credential', 
				fieldLabel: 'Credential',  
				fieldCode: 'gomorpheus.optionType.credential',
				fieldContext: 'config',
		)

		options << new OptionType(
				name: 'Client Email',
				helpText: 'email of google service account',
				code: 'google-cloud-platform-client-email',
				inputType: OptionType.InputType.TEXT,
				required: true,
				placeHolderText: """xxxxxxxxxx@developer.gserviceaccount.com""",
				displayOrder: displayOrderStart+=10,
				localCredential: true,	// expose only when local credential is selected

				fieldName: 'email', // loads cloudConfig.email
				fieldLabel: 'Client Email', 
				fieldCode: 'gomorpheus.optionType.email',
				fieldContext: 'config',
		)

		options << new OptionType(
				name: 'Private Key',
				helpText: 'private key of the google service account',
				code: 'google-cloud-platform-private-key',
				inputType: OptionType.InputType.TEXT,
				required: true,
				placeHolderText: """-----BEGIN PRIVATE KEY-----\\nxxxxx\\n-----END PRIVATE KEY-----\\n""",
				displayOrder: displayOrderStart+=10,
				localCredential: true,	// expose only when local credential is selected

				fieldName: 'private-key', // loads cloudConfig.private-key
				fieldLabel: 'Private Key', 
				fieldCode: 'gomorpheus.optionType.private-key',
				fieldContext: 'config',
		)

		return options
	}

	/**
	 * Grabs available provisioning providers related to the target Cloud Plugin. Some clouds have multiple provisioning
	 * providers or some clouds allow for service based providers on top like (Docker or Kubernetes).
	 * @return Collection of ProvisionProvider
	 */
	@Override
	Collection<ProvisionProvider> getAvailableProvisionProviders() {
	    return this.@plugin.getProvidersByType(ProvisionProvider) as Collection<ProvisionProvider>
	}

	/**
	 * Grabs available backup providers related to the target Cloud Plugin.
	 * @return Collection of BackupProvider
	 */
	@Override
	Collection<BackupProvider> getAvailableBackupProviders() {
		Collection<BackupProvider> providers = []
		return providers
	}

	/**
	 * Provides a Collection of {@link NetworkType} related to this CloudProvider
	 * @return Collection of NetworkType
	 */
	@Override
	Collection<NetworkType> getNetworkTypes() {
		Collection<NetworkType> networks = []
		return networks
	}

	/**
	 * Provides a Collection of {@link NetworkSubnetType} related to this CloudProvider
	 * @return Collection of NetworkSubnetType
	 */
	@Override
	Collection<NetworkSubnetType> getSubnetTypes() {
		Collection<NetworkSubnetType> subnets = []
		return subnets
	}

	/**
	 * Provides a Collection of {@link StorageVolumeType} related to this CloudProvider
	 * @return Collection of StorageVolumeType
	 */
	@Override
	Collection<StorageVolumeType> getStorageVolumeTypes() {
		Collection<StorageVolumeType> volumeTypes = []
		return volumeTypes
	}

	/**
	 * Provides a Collection of {@link StorageControllerType} related to this CloudProvider
	 * @return Collection of StorageControllerType
	 */
	@Override
	Collection<StorageControllerType> getStorageControllerTypes() {
		Collection<StorageControllerType> controllerTypes = []
		return controllerTypes
	}

	/**
	 * Grabs all {@link ComputeServerType} objects that this CloudProvider can represent during a sync or during a provision.
	 * @return collection of ComputeServerType
	 */
	@Override
	Collection<ComputeServerType> getComputeServerTypes() {
		Collection<ComputeServerType> serverTypes = []
		return serverTypes
	}

	/**
	 * Validates the submitted cloud information to make sure it is functioning correctly.
	 * If a {@link ServiceResponse} is not marked as successful then the validation results will be
	 * bubbled up to the user.
	 * @param cloudInfo cloud
	 * @param validateCloudRequest Additional validation information
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse validate(Cloud cloudInfo, ValidateCloudRequest validateCloudRequest) {
		Map cloudConfig = cloudInfo.getConfigMap()
		String email, privateKey

		try {
			if(!cloudInfo) {
				return ServiceResponse.error("Cloud not found")
			}

			switch (validateCloudRequest.credentialType) {
				case 'local':
					email = cloudConfig.get('email')
					privateKey = cloudConfig.get('private-key')
					break

				// current UI version v.8.0.5.dev does not support credentialType 'email-private-key'
				// private-key field is unsupported to enter value in UI form
				// private-key field is unsupported to save at internal secret store (infrastructure > trust > credentials)
				// until UI is fixed,
				// workaround implementation in plugin
				// to support the feature - 'load credential data from internal secret store'
				// is to use 'username-api-key' credentialType as fallthrough option
				// to save and load email and private-key from internal secret store as username and api-key
				case 'email-private-key':
				case 'username-api-key':
					email = validateCloudRequest.credentialUsername
					privateKey = validateCloudRequest.credentialPassword
					break
				default:
					return ServiceResponse.error("Invalid credential type")
			}

			if(!email) return new ServiceResponse(success: false, msg: "Email is required")
			if(!privateKey) return new ServiceResponse(success: false, msg: "Private Key is required")
			NetworkProxy networkProxy = cloudInfo.getApiProxy()
			def connectionResponse = GoogleCloudComputeUtility.testConnection(email, privateKey, networkProxy)
			if(!connectionResponse.success)  return new ServiceResponse(
					success: false,
					msg: "Invalid Credentials. Connection failed for Google Cloud Platform."
			)
			return ServiceResponse.success()
		} catch (Exception e) {
			log.error("Error validating cloud: ${e.message}")
			return new ServiceResponse(
					success: false,
					msg: "Error validating cloud: ${e.message}",
			)
		}
	}

	/**
	 * Called when a Cloud From Morpheus is first saved. This is a hook provided to take care of initial state
	 * assignment that may need to take place.
	 * @param cloudInfo instance of the cloud object that is being initialized.
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse initializeCloud(Cloud cloudInfo) {
		return ServiceResponse.success()
	}


	/**
	 * Zones/Clouds are refreshed periodically by the Morpheus Environment. This includes things like caching of brownfield
	 * environments and resources such as Networks, Datastores, Resource Pools, etc.
	 * @param cloudInfo cloud
	 * @return ServiceResponse. If ServiceResponse.success == true, then Cloud status will be set to Cloud.Status.ok. If
	 * ServiceResponse.success == false, the Cloud status will be set to ServiceResponse.data['status'] or Cloud.Status.error
	 * if not specified. So, to indicate that the Cloud is offline, return `ServiceResponse.error('cloud is not reachable', null, [status: Cloud.Status.offline])`
	 */
	@Override
	ServiceResponse refresh(Cloud cloudInfo) {
		return ServiceResponse.success()
	}

	/**
	 * Zones/Clouds are refreshed periodically by the Morpheus Environment. This includes things like caching of brownfield
	 * environments and resources such as Networks, Datastores, Resource Pools, etc. This represents the long term sync method that happens
	 * daily instead of every 5-10 minute cycle
	 * @param cloudInfo cloud
	 */
	@Override
	void refreshDaily(Cloud cloudInfo) {
	}

	/**
	 * Called when a Cloud From Morpheus is removed. This is a hook provided to take care of cleaning up any state.
	 * @param cloudInfo instance of the cloud object that is being removed.
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse deleteCloud(Cloud cloudInfo) {
		return ServiceResponse.success()
	}

	/**
	 * Returns whether the cloud supports {@link CloudPool}
	 * @return Boolean
	 */
	@Override
	Boolean hasComputeZonePools() {
		return false
	}

	/**
	 * Returns whether a cloud supports {@link Network}
	 * @return Boolean
	 */
	@Override
	Boolean hasNetworks() {
		return true
	}

	/**
	 * Returns whether a cloud supports {@link CloudFolder}
	 * @return Boolean
	 */
	@Override
	Boolean hasFolders() {
		return false
	}

	/**
	 * Returns whether a cloud supports {@link Datastore}
	 * @return Boolean
	 */
	@Override
	Boolean hasDatastores() {
		return true
	}

	/**
	 * Returns whether a cloud supports bare metal VMs
	 * @return Boolean
	 */
	@Override
	Boolean hasBareMetal() {
		return false
	}

	/**
	 * Indicates if the cloud supports cloud-init. Returning true will allow configuration of the Cloud
	 * to allow installing the agent remotely via SSH /WinRM or via Cloud Init
	 * @return Boolean
	 */
	@Override
	Boolean hasCloudInit() {
		return true
	}

	/**
	 * Indicates if the cloud supports the distributed worker functionality
	 * @return Boolean
	 */
	@Override
	Boolean supportsDistributedWorker() {
		return false
	}

	/**
	 * Called when a server should be started. Returning a response of success will cause corresponding updates to usage
	 * records, result in the powerState of the computeServer to be set to 'on', and related instances set to 'running'
	 * @param computeServer server to start
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse startServer(ComputeServer computeServer) {
		return ServiceResponse.success()
	}

	/**
	 * Called when a server should be stopped. Returning a response of success will cause corresponding updates to usage
	 * records, result in the powerState of the computeServer to be set to 'off', and related instances set to 'stopped'
	 * @param computeServer server to stop
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse stopServer(ComputeServer computeServer) {
		return ServiceResponse.success()
	}

	/**
	 * Called when a server should be deleted from the Cloud.
	 * @param computeServer server to delete
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse deleteServer(ComputeServer computeServer) {
		return ServiceResponse.success()
	}

	/**
	 * Grabs the singleton instance of the provisioning provider based on the code defined in its implementation.
	 * Typically Providers are singleton and instanced in the {@link Plugin} class
	 * @param providerCode String representation of the provider short code
	 * @return the ProvisionProvider requested
	 */
	@Override
	ProvisionProvider getProvisionProvider(String providerCode) {
		return getAvailableProvisionProviders().find { it.code == providerCode }
	}

	/**
	 * Returns the default provision code for fetching a {@link ProvisionProvider} for this cloud.
	 * This is only really necessary if the provision type code is the exact same as the cloud code.
	 * @return the provision provider code
	 */
	@Override
	String getDefaultProvisionTypeCode() {
		return GoogleCloudPlatformProvisionProvider.PROVISION_PROVIDER_CODE
	}

	/**
	 * Returns the Morpheus Context for interacting with data stored in the Main Morpheus Application
	 * @return an implementation of the MorpheusContext for running Future based rxJava queries
	 */
	@Override
	MorpheusContext getMorpheus() {
		return this.@context
	}

	/**
	 * Returns the instance of the Plugin class that this provider is loaded from
	 * @return Plugin class contains references to other providers
	 */
	@Override
	Plugin getPlugin() {
		return this.@plugin
	}

	/**
	 * A unique shortcode used for referencing the provided provider. Make sure this is going to be unique as any data
	 * that is seeded or generated related to this provider will reference it by this code.
	 * @return short code string that should be unique across all other plugin implementations.
	 */
	@Override
	String getCode() {
		return CLOUD_PROVIDER_CODE
	}

	/**
	 * Provides the provider name for reference when adding to the Morpheus Orchestrator
	 * NOTE: This may be useful to set as an i18n key for UI reference and localization support.
	 *
	 * @return either an English name of a Provider or an i18n based key that can be scanned for in a properties file.
	 */
	@Override
	String getName() {
		return 'Google Cloud Platform'
	}
}

/*
localCreds
	[http-nio-8080-exec-2] code: cloudConfig.get('google-cloud-platform-private-key'): null fieldName: cloudConfig.get('private-key'): -----BEGIN PRIVATE KEY----- xxxxx -----END PRIVATE KEY----- cloudConfig.'private-key': -----BEGIN PRIVATE KEY----- xxxxx -----END PRIVATE KEY----- cloudInfo: getConfigMap(): [email:112233445566-compute@developer.gserviceaccount.com, private-key:-----BEGIN PRIVATE KEY----- xxxxx -----END PRIVATE KEY-----, region:gotham, applianceUrl:, datacenterName:, networkServer.id:unmanaged, networkServer:[id:unmanaged], securityServer:off, backupMode:internal, replicationMode:-1] validateCloudRequest: credentialType: local credentialUsername: null credentialPassword: null opts: [stepIndex:2, cloudfilter:, zoneType:google-cloud-platform-plugin.cloud, zone.zoneType.id:1, zone:[zoneType.id:1, zoneType:[id:1, code:google-cloud-platform-plugin.cloud], zoneType.code:google-cloud-platform-plugin.cloud, name:name-pn, code:, labelString:, location:, _enabled:, enabled:on, _autoRecoverPowerState:, autoRecoverPowerState:on, networkDomain.id:, networkDomain:[id:], timezone:, securityMode:off, guidanceMode:off, costingMode:off, agentMode:cloudInit, _defaultDatastoreSyncActive:, defaultDatastoreSyncActive:on, _defaultNetworkSyncActive:, defaultNetworkSyncActive:on, _applianceUrlProxyBypass:, applianceUrlProxyBypass:*******, noProxy:, userDataLinux:], zone.zoneType.code:google-cloud-platform-plugin.cloud, zone.name:name-pn, zone.code:, zone.labelString:, zone.location:, zone._enabled:, zone.enabled:on, zone._autoRecoverPowerState:, zone.autoRecoverPowerState:on, credential.type:local, credential:[type:local, integration.id:, integration:[id:]], credential.integration.id:, config.email:112233445566-compute@developer.gserviceaccount.com, config:[email:112233445566-compute@developer.gserviceaccount.com, private-key:-----BEGIN PRIVATE KEY----- xxxxx -----END PRIVATE KEY-----, region:gotham, applianceUrl:, datacenterName:, networkServer.id:unmanaged, networkServer:[id:unmanaged], securityServer:off, backupMode:internal, replicationMode:-1], config.private-key:-----BEGIN PRIVATE KEY----- xxxxx -----END PRIVATE KEY-----, config.region:gotham, zone.networkDomain.id:, config.applianceUrl:, zone.timezone:, config.datacenterName:, config.networkServer.id:unmanaged, zone.securityMode:off, config.securityServer:off, config.backupMode:internal, config.replicationMode:-1, zone.guidanceMode:off, zone.costingMode:off, zone.agentMode:cloudInit, zone._defaultDatastoreSyncActive:, zone.defaultDatastoreSyncActive:on, zone._defaultNetworkSyncActive:, zone.defaultNetworkSyncActive:on, zone._applianceUrlProxyBypass:, zone.applianceUrlProxyBypass:*******, zone.noProxy:, zone.userDataLinux:, controller:siteZone, action:step, user:Sathvik PN[sathvikpn - sathvikpn@hpe.com]]



 */