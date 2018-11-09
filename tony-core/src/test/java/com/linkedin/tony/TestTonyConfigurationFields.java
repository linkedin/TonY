/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.tony;

import org.apache.hadoop.conf.TestConfigurationFieldsBase;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestTonyConfigurationFields extends TestConfigurationFieldsBase {

  @Override
  public void initializeMemberVariables() {
    xmlFilename = new String(Constants.TONY_DEFAULT_XML);
    configurationClasses = new Class[] { TonyConfigurationKeys.class };

    // Set error modes
    errorIfMissingConfigProps = true;
    errorIfMissingXmlProps = true;

    // We don't explicitly declare constants for these, since the configured TensorFlow job names
    // are determined at runtime. But we still need default values for them in tony-default.xml.
    // So ignore the fact that they exist in tony-default.xml and not in TonyConfigurationKeys.
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getInstancesKey(Constants.PS_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getMemoryKey(Constants.PS_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getVCoresKey(Constants.PS_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getResourcesKey(Constants.PS_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getInstancesKey(Constants.WORKER_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getMemoryKey(Constants.WORKER_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getVCoresKey(Constants.WORKER_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getGPUsKey(Constants.WORKER_JOB_NAME));
    xmlPropsToSkipCompare.add(TonyConfigurationKeys.getResourcesKey(Constants.WORKER_JOB_NAME));
    configurationPropsToSkipCompare.add(TonyConfigurationKeys.DOCKER_IMAGE);
  }

  @BeforeTest
  public void setupTestConfigurationFields() throws Exception {
    super.setupTestConfigurationFields();
  }

  @Test
  public void testCompareConfigurationClassAgainstXml() {
    super.testCompareConfigurationClassAgainstXml();
  }

  @Test
  public void testCompareXmlAgainstConfigurationClass() {
    super.testCompareXmlAgainstConfigurationClass();
  }

  @Test
  public void testXmlAgainstDefaultValuesInConfigurationClass() {
    super.testXmlAgainstDefaultValuesInConfigurationClass();
  }
}
