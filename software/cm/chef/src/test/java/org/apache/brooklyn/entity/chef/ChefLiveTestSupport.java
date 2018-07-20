/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.chef;

import java.io.File;
import java.io.InputStream;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.location.MachineProvisioningLocation;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.entity.EntityInternal;
import org.apache.brooklyn.core.test.BrooklynAppLiveTestSupport;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.io.FileUtil;
import org.apache.brooklyn.util.stream.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;

import com.google.common.io.Files;

public class ChefLiveTestSupport extends BrooklynAppLiveTestSupport {

    private static final Logger log = LoggerFactory.getLogger(ChefLiveTestSupport.class);
    
    protected MachineProvisioningLocation<? extends SshMachineLocation> targetLocation;

    @BeforeMethod(alwaysRun=true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        targetLocation = createLocation();
    }

    protected MachineProvisioningLocation<? extends SshMachineLocation> createLocation() {
        return createLocation(mgmt);
    }
    
    /** convenience for setting up a pre-built / fixed IP machine
     * (because you might not want to set up Chef on localhost) 
     * and ensuring tests against Chef use the same configured location 
     **/
    public static MachineProvisioningLocation<? extends SshMachineLocation> createLocation(ManagementContext mgmt) {
        LocationSpec<?> bestLocation = mgmt.getLocationRegistry().getLocationSpec("named:ChefTests").orNull();
        if (bestLocation==null) {
            log.info("using AWS for chef tests because named:ChefTests does not exist");
            bestLocation = mgmt.getLocationRegistry().getLocationSpec("jclouds:aws-ec2:us-east-1").orNull();
        }
        if (bestLocation==null) {
            throw new IllegalStateException("Need a location called named:ChefTests or AWS configured for these tests");
        }
        @SuppressWarnings("unchecked")
        MachineProvisioningLocation<? extends SshMachineLocation> result = (MachineProvisioningLocation<? extends SshMachineLocation>) 
        mgmt.getLocationManager().createLocation(bestLocation);
        return result;
    }
    
    private static String defaultConfigFile = null; 
    public synchronized static String installBrooklynChefHostedConfig() {
        if (defaultConfigFile!=null) return defaultConfigFile;
        File tempDir = Files.createTempDir();
        ResourceUtils r = ResourceUtils.create(ChefServerTasksIntegrationTest.class);
        for (String f: new String[] { "knife.rb", "brooklyn-tests.pem", "brooklyn-validator.pem" }) {
            InputStream in = r.getResourceFromUrl("classpath:///org/apache/brooklyn/entity/chef/hosted-chef-brooklyn-credentials/"+f);
            try {
                FileUtil.copyTo(in, new File(tempDir, f));
            } finally {
                Streams.closeQuietly(in);
            }
        }
        File knifeConfig = new File(tempDir, "knife.rb");
        defaultConfigFile = knifeConfig.getPath();
        return defaultConfigFile;
    }

    public static void installBrooklynChefHostedConfig(Entity entity) {
        ((EntityInternal)entity).config().set(ChefConfig.KNIFE_CONFIG_FILE, ChefLiveTestSupport.installBrooklynChefHostedConfig());
    }

}
