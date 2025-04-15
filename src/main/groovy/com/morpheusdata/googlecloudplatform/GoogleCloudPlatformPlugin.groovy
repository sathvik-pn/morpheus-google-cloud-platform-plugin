/*
* Copyright 2022 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.morpheusdata.googlecloudplatform

import com.morpheusdata.core.Plugin
import groovy.util.logging.Slf4j

@Slf4j
class GoogleCloudPlatformPlugin extends Plugin {

    @Override
    String getCode() {
        return 'google-cloud-platform-plugin'
    }

    @Override
    void initialize() {
        this.setName("Google Cloud Platform")
        this.setDescription("Morpheus Plugin for Google Cloud Platform")

        this.registerProvider(new GoogleCloudPlatformCloudProvider(this,this.morpheus))
        this.registerProvider(new GoogleCloudPlatformProvisionProvider(this,this.morpheus))

        log.info("SPN Google Cloud Platform Plugin initialized")
    }

    /**
     * Called when a plugin is being removed from the plugin manager (aka Uninstalled)
     */
    @Override
    void onDestroy() {
        log.info("SPN Google Cloud Platform Plugin Uninstalled")
    }
}
