package com.morpheusdata.googlecloudplatform

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.providers.CloudProvider
import com.morpheusdata.core.providers.ProvisionProvider
import com.morpheusdata.model.BackupProvider
import com.morpheusdata.model.Cloud
import com.morpheusdata.model.CloudFolder
import com.morpheusdata.model.CloudPool
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.ComputeServerType
import com.morpheusdata.model.Datastore
import com.morpheusdata.model.Icon
import com.morpheusdata.model.Network
import com.morpheusdata.model.NetworkSubnetType
import com.morpheusdata.model.NetworkType
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.StorageControllerType
import com.morpheusdata.model.StorageVolumeType
import com.morpheusdata.request.ValidateCloudRequest
import com.morpheusdata.response.ServiceResponse

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.data.DataFilter
import com.morpheusdata.core.data.DataQuery
import com.morpheusdata.core.providers.CloudProvider
import com.morpheusdata.core.providers.ProvisionProvider
import com.morpheusdata.core.util.ConnectionUtils
import com.morpheusdata.core.util.HttpApiClient
import com.morpheusdata.model.*

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
		def displayOrderStart = 0
		options << new OptionType(
				code: 'zoneType.google.credential',
				inputType: OptionType.InputType.CREDENTIAL,
				fieldContext: 'credential',
				fieldName: 'type',
				name: 'Credentials',
//				fieldLabel: 'Credentials',
				fieldCode: 'gomorpheus.label.credentials',
				required: true,
				global: false,
				helpBlock: '',
				defaultValue: 'local',
				displayOrder: displayOrderStart,
				optionSource: 'credentials',
				config: '{"credentialTypes":["email-private-key", "client-id-secret"]}'
		)

		options << new OptionType(
				name: 'Client Email',
				code: 'google-cloud-platform-client-email',
				displayOrder: displayOrderStart+=10,
				fieldContext: 'config',
				fieldName: 'email', // optionTypes[0].fieldName': rejected value [client email]
				fieldLabel: 'Client Email',
				fieldCode: 'gomorpheus.label.email',
				required: true,
				localCredential: true, // expose only when local credential is selected
				inputType: OptionType.InputType.TEXT,
		)

		options << new OptionType(
				name: 'Private Key',
				code: 'google-cloud-platform-private-key',
				displayOrder: displayOrderStart+=10,
				fieldContext: 'config',
				fieldName: 'private-key', // optionTypes[1].fieldName': rejected value [private key]
				fieldLabel: 'Private Key',
				fieldCode: 'gomorpheus.label.private-key',
				required: true,
				localCredential: true, // expose only when local credential is selected
				inputType: OptionType.InputType.PASSWORD,
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


	/*
		[http-nio-8080-exec-8] SPN Plugin ServiceResponse validate(Cloud cloudInfo, ValidateCloudRequest validateCloudRequest) called. cloudInfo.getConfigMap = [email:487639795848-compute@developer.gserviceaccount.com, private-key:-----BEGIN PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDHnGEHYmKbQmy7 Vh+WwPr5zVi+GKu0ybx3VwpyrOPGcUEjwKeBiOIE3e6XZSIPI0DcJnRj3t//Zti+ mG8Y3+Jzji6I9ahiVboGTpPmBDUS3YH8O7vq0gj5PsWLVGqeIgxmr9gDLKkNRjrT H3IDPjzOB1J7zYomn0NQefS+eS8ZQY26s+5LzJa6ElXafmO2YEq0zMeAuq7bst+/ YZ+4YBiZGkbrn85DlwavFDPuMTZUFXMsHEZbHVQdvVqK45AScBKi8JbHTAsbubdm AdTCdm8m1x+TpVy7GpQbYLpVW/NZJNiEtmuXpQqq9qGH7pQbVaQjcYnb4TUW3qt8 Fl6mhLRVAgMBAAECggEAHclAk10DkN6HIidWXGUcrnUNhoRg7v/hjz9eUEFrVdvl mAOyGzrFW2uQpGfTfsXIcnGMkbCa5V4//qT5gxE5lfvEBuz9fPsE4NcN69d54gBK 2sxBHOfNrf/Hc6SbqlAzYIlh5wtdiJ0Pvxxko4ZAhZADJ3VC5cODRHmGY3vq4s4p W0jtLVbc0EpQsqGuK3gfvsGDbQTx3iKxxTi93ETa9eqgPgO29rngJtfqaucz/eEj 6U+HY419GRIhUy9mYlM7EQ5EUgYoGEpMV1kMQobUOLIG7MzzDuDNFH9O4eWNvdw8 /gZJzMNWznqzm9HTLaVsaR1hdituUOGsFSjNTIw9AQKBgQDum25X6JqYe6WzUQOw kwIjR/p43zfgKm1icYRsxQySiGB82Z/aJu7thw40B7sUnmTAD/bljmXsxCU0MYaE /0ERQpZT+VmYcpRwYxv5ASJIfUf0gCTSpEnA3tcHXfZAto4ImGDDYLyzlG5ZSEJH VPmEa1RwO0H+OtCVuszNzZBXgQKBgQDWKUBC6hyQJs8j2pRYdNYvGSBPexYR+peY xCiWVPzfAiSMhiYy864wCuZHcoGgUEoTz1FFptqzIaXNzzvIhs4qZtKdKRwbFhW5 hQpwMOt5l1IAh5aVld7YGP3+lkS1/PC1ZhctO4zmwNdPFJZMSpQJdFiqEmrExWl7 z7AXBc7m1QKBgHvOMGnyqkyWU7sAPU7gaIqP7XnMMLziptEen5ykfcqGcrI1ZUkX TH/4xLjgjgS7zwENB4nC4kYA3GEBlY/qtgNTrpax2fhM85KBCmGgYf6E6tIFr5WQ YyhtN9t9uKmJT5dMIWEfD6qKSNuQr7s9bx5zZmO5i5tzPwmB3ISotc2BAoGBAM5X YHsYZfzd56Z764Ju0A8A/oiV52XbNEUUeqadrCaSQfTDK/rbnnDbj5Q3V33G3hjI H14krYL6YG4zxT4n5GwmNbcG7PWMCdJ9xNjjyX2VtoScHxJxHAzJx6+LyRgvZRzi CGKkhdWu58Rh6Y4ILSoO137Fz+lW65NHQDpudjMhAoGBALQKssHWW5Rjp7AVilOB rSNtRxRUigrEFh4YlSgQtlS4w5V1Y8wTVhxEEohadR0DEYVVBLlWwMqyTWl/2RXU xSIq4LxGLYSxh9yVbaJngLIJHoATJ10Jzs67lT7ra+Oh3ru9rnny99GwP69pMwp9 66ZtJFGtIQC58jXzi6yqBkFr -----END PRIVATE KEY-----, applianceUrl:1.1.1.1, datacenterName:1, networkServer.id:unmanaged, networkServer:[id:unmanaged], securityServer:off, backupMode:internal, replicationMode:-1] \n\n validateCloudRequest.opts = [stepIndex:2, cloudfilter:, zoneType:google-cloud-platform-plugin.cloud, zone.zoneType.id:1, zone:[zoneType.id:1, zoneType:[id:1, code:google-cloud-platform-plugin.cloud], zoneType.code:google-cloud-platform-plugin.cloud, name:spn-cloud, code:, labelString:, location:, _enabled:, enabled:on, _autoRecoverPowerState:, autoRecoverPowerState:on, networkDomain.id:1, networkDomain:[id:1], timezone:GMT, securityMode:internal, guidanceMode:off, costingMode:costing, agentMode:cloudInit, _defaultDatastoreSyncActive:, defaultDatastoreSyncActive:on, _defaultNetworkSyncActive:, defaultNetworkSyncActive:on, _applianceUrlProxyBypass:, applianceUrlProxyBypass:*******, noProxy:, userDataLinux:my-user-data-linux], zone.zoneType.code:google-cloud-platform-plugin.cloud, zone.name:spn-cloud, zone.code:, zone.labelString:, zone.location:, zone._enabled:, zone.enabled:on, zone._autoRecoverPowerState:, zone.autoRecoverPowerState:on, credential.type:local, credential:[type:local, integration.id:, integration:[id:]], credential.integration.id:, config.email:487639795848-compute@developer.gserviceaccount.com, config:[email:487639795848-compute@developer.gserviceaccount.com, private-key:-----BEGIN PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDHnGEHYmKbQmy7 Vh+WwPr5zVi+GKu0ybx3VwpyrOPGcUEjwKeBiOIE3e6XZSIPI0DcJnRj3t//Zti+ mG8Y3+Jzji6I9ahiVboGTpPmBDUS3YH8O7vq0gj5PsWLVGqeIgxmr9gDLKkNRjrT H3IDPjzOB1J7zYomn0NQefS+eS8ZQY26s+5LzJa6ElXafmO2YEq0zMeAuq7bst+/ YZ+4YBiZGkbrn85DlwavFDPuMTZUFXMsHEZbHVQdvVqK45AScBKi8JbHTAsbubdm AdTCdm8m1x+TpVy7GpQbYLpVW/NZJNiEtmuXpQqq9qGH7pQbVaQjcYnb4TUW3qt8 Fl6mhLRVAgMBAAECggEAHclAk10DkN6HIidWXGUcrnUNhoRg7v/hjz9eUEFrVdvl mAOyGzrFW2uQpGfTfsXIcnGMkbCa5V4//qT5gxE5lfvEBuz9fPsE4NcN69d54gBK 2sxBHOfNrf/Hc6SbqlAzYIlh5wtdiJ0Pvxxko4ZAhZADJ3VC5cODRHmGY3vq4s4p W0jtLVbc0EpQsqGuK3gfvsGDbQTx3iKxxTi93ETa9eqgPgO29rngJtfqaucz/eEj 6U+HY419GRIhUy9mYlM7EQ5EUgYoGEpMV1kMQobUOLIG7MzzDuDNFH9O4eWNvdw8 /gZJzMNWznqzm9HTLaVsaR1hdituUOGsFSjNTIw9AQKBgQDum25X6JqYe6WzUQOw kwIjR/p43zfgKm1icYRsxQySiGB82Z/aJu7thw40B7sUnmTAD/bljmXsxCU0MYaE /0ERQpZT+VmYcpRwYxv5ASJIfUf0gCTSpEnA3tcHXfZAto4ImGDDYLyzlG5ZSEJH VPmEa1RwO0H+OtCVuszNzZBXgQKBgQDWKUBC6hyQJs8j2pRYdNYvGSBPexYR+peY xCiWVPzfAiSMhiYy864wCuZHcoGgUEoTz1FFptqzIaXNzzvIhs4qZtKdKRwbFhW5 hQpwMOt5l1IAh5aVld7YGP3+lkS1/PC1ZhctO4zmwNdPFJZMSpQJdFiqEmrExWl7 z7AXBc7m1QKBgHvOMGnyqkyWU7sAPU7gaIqP7XnMMLziptEen5ykfcqGcrI1ZUkX TH/4xLjgjgS7zwENB4nC4kYA3GEBlY/qtgNTrpax2fhM85KBCmGgYf6E6tIFr5WQ YyhtN9t9uKmJT5dMIWEfD6qKSNuQr7s9bx5zZmO5i5tzPwmB3ISotc2BAoGBAM5X YHsYZfzd56Z764Ju0A8A/oiV52XbNEUUeqadrCaSQfTDK/rbnnDbj5Q3V33G3hjI H14krYL6YG4zxT4n5GwmNbcG7PWMCdJ9xNjjyX2VtoScHxJxHAzJx6+LyRgvZRzi CGKkhdWu58Rh6Y4ILSoO137Fz+lW65NHQDpudjMhAoGBALQKssHWW5Rjp7AVilOB rSNtRxRUigrEFh4YlSgQtlS4w5V1Y8wTVhxEEohadR0DEYVVBLlWwMqyTWl/2RXU xSIq4LxGLYSxh9yVbaJngLIJHoATJ10Jzs67lT7ra+Oh3ru9rnny99GwP69pMwp9 66ZtJFGtIQC58jXzi6yqBkFr -----END PRIVATE KEY-----, applianceUrl:1.1.1.1, datacenterName:1, networkServer.id:unmanaged, networkServer:[id:unmanaged], securityServer:off, backupMode:internal, replicationMode:-1], config.private-key:-----BEGIN PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDHnGEHYmKbQmy7 Vh+WwPr5zVi+GKu0ybx3VwpyrOPGcUEjwKeBiOIE3e6XZSIPI0DcJnRj3t//Zti+ mG8Y3+Jzji6I9ahiVboGTpPmBDUS3YH8O7vq0gj5PsWLVGqeIgxmr9gDLKkNRjrT H3IDPjzOB1J7zYomn0NQefS+eS8ZQY26s+5LzJa6ElXafmO2YEq0zMeAuq7bst+/ YZ+4YBiZGkbrn85DlwavFDPuMTZUFXMsHEZbHVQdvVqK45AScBKi8JbHTAsbubdm AdTCdm8m1x+TpVy7GpQbYLpVW/NZJNiEtmuXpQqq9qGH7pQbVaQjcYnb4TUW3qt8 Fl6mhLRVAgMBAAECggEAHclAk10DkN6HIidWXGUcrnUNhoRg7v/hjz9eUEFrVdvl mAOyGzrFW2uQpGfTfsXIcnGMkbCa5V4//qT5gxE5lfvEBuz9fPsE4NcN69d54gBK 2sxBHOfNrf/Hc6SbqlAzYIlh5wtdiJ0Pvxxko4ZAhZADJ3VC5cODRHmGY3vq4s4p W0jtLVbc0EpQsqGuK3gfvsGDbQTx3iKxxTi93ETa9eqgPgO29rngJtfqaucz/eEj 6U+HY419GRIhUy9mYlM7EQ5EUgYoGEpMV1kMQobUOLIG7MzzDuDNFH9O4eWNvdw8 /gZJzMNWznqzm9HTLaVsaR1hdituUOGsFSjNTIw9AQKBgQDum25X6JqYe6WzUQOw kwIjR/p43zfgKm1icYRsxQySiGB82Z/aJu7thw40B7sUnmTAD/bljmXsxCU0MYaE /0ERQpZT+VmYcpRwYxv5ASJIfUf0gCTSpEnA3tcHXfZAto4ImGDDYLyzlG5ZSEJH VPmEa1RwO0H+OtCVuszNzZBXgQKBgQDWKUBC6hyQJs8j2pRYdNYvGSBPexYR+peY xCiWVPzfAiSMhiYy864wCuZHcoGgUEoTz1FFptqzIaXNzzvIhs4qZtKdKRwbFhW5 hQpwMOt5l1IAh5aVld7YGP3+lkS1/PC1ZhctO4zmwNdPFJZMSpQJdFiqEmrExWl7 z7AXBc7m1QKBgHvOMGnyqkyWU7sAPU7gaIqP7XnMMLziptEen5ykfcqGcrI1ZUkX TH/4xLjgjgS7zwENB4nC4kYA3GEBlY/qtgNTrpax2fhM85KBCmGgYf6E6tIFr5WQ YyhtN9t9uKmJT5dMIWEfD6qKSNuQr7s9bx5zZmO5i5tzPwmB3ISotc2BAoGBAM5X YHsYZfzd56Z764Ju0A8A/oiV52XbNEUUeqadrCaSQfTDK/rbnnDbj5Q3V33G3hjI H14krYL6YG4zxT4n5GwmNbcG7PWMCdJ9xNjjyX2VtoScHxJxHAzJx6+LyRgvZRzi CGKkhdWu58Rh6Y4ILSoO137Fz+lW65NHQDpudjMhAoGBALQKssHWW5Rjp7AVilOB rSNtRxRUigrEFh4YlSgQtlS4w5V1Y8wTVhxEEohadR0DEYVVBLlWwMqyTWl/2RXU xSIq4LxGLYSxh9yVbaJngLIJHoATJ10Jzs67lT7ra+Oh3ru9rnny99GwP69pMwp9 66ZtJFGtIQC58jXzi6yqBkFr -----END PRIVATE KEY-----, zone.networkDomain.id:1, config.applianceUrl:1.1.1.1, zone.timezone:GMT, config.datacenterName:1, config.networkServer.id:unmanaged, zone.securityMode:internal, config.securityServer:off, config.backupMode:internal, config.replicationMode:-1, zone.guidanceMode:off, zone.costingMode:costing, zone.agentMode:cloudInit, zone._defaultDatastoreSyncActive:, zone.defaultDatastoreSyncActive:on, zone._defaultNetworkSyncActive:, zone.defaultNetworkSyncActive:on, zone._applianceUrlProxyBypass:, zone.applianceUrlProxyBypass:*******, zone.noProxy:, zone.userDataLinux:my-user-data-linux, controller:siteZone, action:step, user:Sathvik PN[sathvikpn - sathvikpn@hpe.com]] \n\n cloudInfo.account = com.morpheusdata.model.Account@43b822a4 cloudType = com.morpheusdata.model.CloudType@2a75b4e8 type = null serviceUrl = null serviceUsername = null servicePassword = null serviceToken = null \n\n accountCredentialData = null accountCredentialLoaded = false validateCloudRequest.credentialUsername = null validateCloudRequest.credentialPassword = null validateCloudRequest.credentialType = local
	 */

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
		def debug_log = """
		SPN Plugin ServiceResponse validate(Cloud cloudInfo, ValidateCloudRequest validateCloudRequest) called. \n\n
		cloudInfo.getConfigMap = ${cloudInfo.getConfigMap()}                      \\n\\n
		validateCloudRequest.opts = ${validateCloudRequest.opts}  \\n\\n

		cloudInfo.account = ${cloudInfo.account}
		cloudType = ${cloudInfo.cloudType}
		type = ${cloudInfo.type}
		serviceUrl = ${cloudInfo.serviceUrl}
		serviceUsername = ${cloudInfo.serviceUsername}
		servicePassword = ${cloudInfo.servicePassword}
		serviceToken = ${cloudInfo.serviceToken}   \\n\\n

		accountCredentialData = ${cloudInfo.accountCredentialData}
		accountCredentialLoaded = ${cloudInfo.accountCredentialLoaded}

		validateCloudRequest.credentialUsername = ${validateCloudRequest.credentialUsername}
		validateCloudRequest.credentialPassword = ${validateCloudRequest.credentialPassword}
		validateCloudRequest.credentialType = ${validateCloudRequest.credentialType}

		"""
		log.info(debug_log)
//		cloudInfo.account = com.morpheusdata.model.Account@391339af 
// cloudType = com.morpheusdata.model.CloudType@4ac9141 type = null serviceUrl = null serviceUsername = null servicePassword = null serviceToken = null 
// accountCredentialData = null accountCredentialLoaded = false
		try {
			if (cloudInfo) {
				String email = ""
				String privateKey = ""

				def cloudConfig = cloudInfo.getConfigMap() 

				// email = validateCloudRequest.opts?.config['email']
				// privateKey = validateCloudRequest.opts?.config['private-key']

				//
				if(validateCloudRequest.credentialType == 'email-private-key') {
					email = validateCloudRequest.credentialUsername
					privateKey = validateCloudRequest.credentialPassword
					log.info("SPN Plugin email-private-key creds from validateCloudRequest: email==${email} and pkey==${privateKey}")
				} else if (validateCloudRequest.credentialType == 'local') {
					email = validateCloudRequest.opts?.config['email']
					privateKey = validateCloudRequest.opts?.config['private-key']
					log.info("SPN Plugin local creds from validateCloudRequest: email==${email} and pkey==${privateKey}")

					email = cloudConfig?.email
					privateKey = cloudConfig?.'private-key'
					log.info("SPN Plugin local creds from cloudInfoConfigMap: email==${email} and pkey==${privateKey}")
				}

				if (email?.isBlank()) {
					return new ServiceResponse(success: false, msg: 'Enter the email')
				} else if (privateKey?.isBlank()) {
					return new ServiceResponse(success: false, msg: 'Enter the private key')
				} else {
					cloudInfo.accountCredentialData = [
							'email': email,
							'private-key': privateKey,
					]
					cloudInfo.accountCredentialLoaded = true

					log.info("SPN Plugin accountCredentialData: ${cloudInfo.accountCredentialData}")
					log.info("SPN Plugin accountCredentialLoaded: ${cloudInfo.accountCredentialLoaded}")
					// Call the API to validate the credentials
					RequestConfig requestConfig = new RequestConfig(
							email: email,
							privateKey: privateKey,
					)
					HttpApiClient apiClient = new HttpApiClient()
					apiClient.networkProxy = cloudInfo.apiProxy
					try {
						log.info("SPN Plugin projectList API Call Placeholder")
						def projectList = GoogleCloudPlatformApiService.listProjects(apiClient, requestConfig)
						if (projectList.success == true) {
							return new ServiceResponse(success: true, msg: 'SPN Plugin validation successful (api call)', data: projectList)
						} else {
							return new ServiceResponse(success: false, msg: 'SPN Plugin invalid credentials')
						}
					} finally {
						apiClient.shutdownClient()
					}
				}
			} else {
				return new ServiceResponse(success: false, msg: 'Plugin no cloud found')
			}
		} catch (e) {
			log.error('Error validating cloud: ', e)
			return new ServiceResponse(success: false, msg: 'Plugin error validating cloud', data: e)
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
		log.info("SPN Plugin ServiceResponse initializeCloud(Cloud cloudInfo)")
		log.info("SPN Plugin initializeCloud: ${cloudInfo}")

		if (cloudInfo) {
			if (cloudInfo.enabled) {
				return refresh(cloudInfo)
			}
		} else {
			return ServiceResponse.error('No cloud found')
		}

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
		log.info("SPN Plugin refresh(Cloud cloudInfo) called. cloud = ${cloudInfo}")
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
		log.info("SPN Plugin refreshDaily(Cloud cloudInfo) called. cloud = ${cloudInfo}")
	}

	/**
	 * Called when a Cloud From Morpheus is removed. This is a hook provided to take care of cleaning up any state.
	 * @param cloudInfo instance of the cloud object that is being removed.
	 * @return ServiceResponse
	 */
	@Override
	ServiceResponse deleteCloud(Cloud cloudInfo) {
		log.info("SPN Plugin deleteCloud(Cloud cloudInfo) called. cloud = ${cloudInfo}")
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
		log.info("SPN Plugin hasCloudInit")
		return true

//		[http-nio-8080-exec-4] SPN Plugin hasCloudInit
//		[http-nio-8080-exec-4] Google Cloud Platform Plugin initialized
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
		log.info("SPN Plugin startServer")
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




/* NOTES
	[http-nio-8080-exec-5] SPN ServiceResponse validate(Cloud cloudInfo, ValidateCloudRequest validateCloudRequest) called.
	Cloud = com.morpheusdata.model.Cloud@53783f14 validateCloudRequest = com.morpheusdata.request.ValidateCloudRequest@3995f524
	cloudInfo.getConfigMap = [
    email: "email-is-spn@email.com",
    private-key: "private-key-is-spn@12345",
    applianceUrl: "",
    datacenterName: "",
    networkServer.id: "unmanaged",
    networkServer: [
        id: "unmanaged"
    ],
    securityServer: "off",
    backupMode: "internal",
    replicationMode: -1
]


	validateCloudRequest.credentialUsername = null
	validateCloudRequest.credentialPassword = null
	validateCloudRequest.credentialType = local
	validateCloudRequest.opts = [
    stepIndex: 2,
    cloudfilter: "",
    zoneType: "google-cloud-platform-plugin.cloud",
    zone.zoneType.id: 27,
    zone: [
        zoneType.id: 27,
        zoneType: [
            id: 27,
            code: "google-cloud-platform-plugin.cloud"
        ],
        zoneType.code: "google-cloud-platform-plugin.cloud",
        name: "name-spn",
        code: "",
        labelString: "",
        location: "",
        _enabled: "",
        enabled: "on",
        _autoRecoverPowerState: "",
        autoRecoverPowerState: "on",
        apiProxy.id: "",
        apiProxy: [
            id: ""
        ],
        networkDomain.id: "",
        networkDomain: [
            id: ""
        ],
        timezone: "",
        securityMode: "off",
        guidanceMode: "off",
        costingMode: "off",
        agentMode: "cloudInit",
        _defaultDatastoreSyncActive: "",
        defaultDatastoreSyncActive: "on",
        _defaultNetworkSyncActive: "",
        defaultNetworkSyncActive: "on",
        provisioningProxy.id: "",
        provisioningProxy: [
            id: ""
        ],
        _applianceUrlProxyBypass: "",
        applianceUrlProxyBypass: "*******",
        noProxy: "",
        userDataLinux: ""
    ],
    zone.zoneType.code: "google-cloud-platform-plugin.cloud",
    zone.name: "name-spn",
    zone.code: "",
    zone.labelString: "",
    zone.location: "",
    zone._enabled: "",
    zone.enabled: "on",
    zone._autoRecoverPowerState: "",
    zone.autoRecoverPowerState: "on",
    zone.apiProxy.id: "",
    credential.type: "local",
    credential: [
        type: "local",
        integration.id: "",
        integration: [
            id: ""
        ]
    ],
    credential.integration.id: "",
    config.email: "email-is-spn@email.com",
    config: [
        email: "email-is-spn@email.com",
        private-key: "private-key-is-spn@12345",
        applianceUrl: "",
        datacenterName: "",
        networkServer.id: "unmanaged",
        networkServer: [
            id: "unmanaged"
        ],
        securityServer: "off",
        backupMode: "internal",
        replicationMode: -1
    ],
    config.private-key: "private-key-is-spn@12345",
    zone.networkDomain.id: "",
    config.applianceUrl: "",
    zone.timezone: "",
    config.datacenterName: "",
    config.networkServer.id: "unmanaged",
    zone.securityMode: "off",
    config.securityServer: "off",
    config.backupMode: "internal",
    config.replicationMode: -1,
    zone.guidanceMode: "off",
    zone.costingMode: "off",
    zone.agentMode: "cloudInit",
    zone._defaultDatastoreSyncActive: "",
    zone.defaultDatastoreSyncActive: "on",
    zone._defaultNetworkSyncActive: "",
    zone.defaultNetworkSyncActive: "on",
    zone.provisioningProxy.id: "",
    zone._applianceUrlProxyBypass: "",
    zone.applianceUrlProxyBypass: "*******",
    zone.noProxy: "",
    zone.userDataLinux: "",
    controller: "siteZone",
    action: "step",
    user: "Sathvik PN[sathvikpn - sathvik-pn@hpe.com]"
]

 */