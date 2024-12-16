/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hadoop.fs.s3a;

import java.io.IOException;

import org.junit.Test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import static org.apache.hadoop.fs.s3a.Constants.ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX;
import static org.apache.hadoop.fs.s3a.Constants.ANALYTICS_ACCELERATOR_ENABLED_KEY;
import static org.apache.hadoop.fs.s3a.Constants.ANALYTICS_ACCELERATOR_CRT_ENABLED;
import static org.apache.hadoop.fs.s3a.S3ATestUtils.removeBaseAndBucketOverrides;

import software.amazon.s3.analyticsaccelerator.S3SeekableInputStreamConfiguration;
import software.amazon.s3.analyticsaccelerator.common.ConnectorConfiguration;
import software.amazon.s3.analyticsaccelerator.util.PrefetchMode;

public class ITestS3AS3SeekableStream extends AbstractS3ATestBase {

  final String PHYSICAL_IO_PREFIX = "physicalio";
  final String LOGICAL_IO_PREFIX = "logicalio";

  public void testConnectorFrameWorkIntegration(boolean useCrtClient) throws IOException {
    describe("Verify S3 connector framework integration");

    Configuration conf = getConfiguration();
    removeBaseAndBucketOverrides(conf, ANALYTICS_ACCELERATOR_ENABLED_KEY);
    conf.setBoolean(ANALYTICS_ACCELERATOR_ENABLED_KEY, true);
    conf.setBoolean(ANALYTICS_ACCELERATOR_CRT_ENABLED, useCrtClient);

    String testFile = "s3a://noaa-cors-pds/raw/2023/017/ohfh/OHFH017d.23_.gz";
    S3AFileSystem s3AFileSystem = (S3AFileSystem) FileSystem.newInstance(new Path(testFile).toUri(), conf);
    byte[] buffer = new byte[500];

    try (FSDataInputStream inputStream = s3AFileSystem.open(new Path(testFile))) {
      inputStream.seek(5);
      inputStream.read(buffer, 0, 500);
    }

  }

  @Test
  public void testConnectorFrameWorkIntegrationWithCrtClient() throws IOException {
    testConnectorFrameWorkIntegration(true);
  }

  @Test
  public void testConnectorFrameWorkIntegrationWithoutCrtClient() throws IOException {
    testConnectorFrameWorkIntegration(false);
  }

  public void testConnectorFrameworkConfigurable(boolean useCrtClient) {
    describe("Verify S3 connector framework reads configuration");

    Configuration conf = getConfiguration();
    removeBaseAndBucketOverrides(conf);

    //Disable Predictive Prefetching
    conf.set(ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX + "." + LOGICAL_IO_PREFIX + ".prefetching.mode", "all");

    //Set Blobstore Capacity
    conf.setInt(ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX + "." + PHYSICAL_IO_PREFIX + ".blobstore.capacity", 1);

    conf.setBoolean(ANALYTICS_ACCELERATOR_CRT_ENABLED, useCrtClient);

    ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration(conf, ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX);

    S3SeekableInputStreamConfiguration configuration = S3SeekableInputStreamConfiguration.fromConfiguration(connectorConfiguration);

    assertSame("S3ASeekableStream configuration is not set to expected value", PrefetchMode.ALL, configuration.getLogicalIOConfiguration().getPrefetchingMode());

    assertEquals("S3ASeekableStream configuration is not set to expected value", 1, configuration.getPhysicalIOConfiguration().getBlobStoreCapacity());
  }

  @Test
  public void testConnectorFrameworkConfigurableWithoutCrtClient() throws IOException {
    testConnectorFrameworkConfigurable(false);
  }

  @Test
  public void testConnectorFrameworkConfigurableWithCrtClient() throws IOException {
    testConnectorFrameworkConfigurable(true);
  }

  @Test
  public void testInvalidConfigurationThrows() {
    describe("Verify S3 connector framework throws with invalid configuration");

    Configuration conf = getConfiguration();
    removeBaseAndBucketOverrides(conf);
    //Disable Sequential Prefetching
    conf.setInt(ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX + "." + PHYSICAL_IO_PREFIX + ".blobstore.capacity", -1);

    ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration(conf, ANALYTICS_ACCELERATOR_CONFIGURATION_PREFIX);
    assertThrows("S3ASeekableStream illegal configuration does not throw", IllegalArgumentException.class, () ->
        S3SeekableInputStreamConfiguration.fromConfiguration(connectorConfiguration));
  }
}
