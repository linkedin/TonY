/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.tony.azkaban;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import azkaban.utils.FileIOUtils;
import azkaban.utils.Props;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static azkaban.ServiceProvider.SERVICE_PROVIDER;


public class TestTensorFlowJob {

  private final Logger log = Logger.getLogger(TestTensorFlowJob.class);

  // Taken from azkaban.test.Utils in azkaban-common.
  private static void initServiceProvider() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {}
    });
    // Because SERVICE_PROVIDER is a singleton and it is shared among many tests,
    // need to reset the state to avoid assertion failures.
    SERVICE_PROVIDER.unsetInjector();

    SERVICE_PROVIDER.setInjector(injector);
  }

  @BeforeTest
  public void setup() {
    initServiceProvider();
  }

  @Test
  public void testMainArguments() {
    final Props jobProps = new Props();
    jobProps.put(TensorFlowJobArg.HDFS_CLASSPATH.azPropName, "hdfs://nn:8020");
    jobProps.put(TensorFlowJob.WORKER_ENV_PREFIX + "E1", "e1");
    jobProps.put(TensorFlowJob.WORKER_ENV_PREFIX + "E2", "e2");

    final TensorFlowJob tfJob = new TensorFlowJob("test_tf_job", new Props(), jobProps, log) {
      @Override
      public String getWorkingDirectory() {
        return System.getProperty("java.io.tmpdir");
      }
    };
    String args = tfJob.getMainArguments();
    Assert.assertTrue(new File(tfJob.getWorkingDirectory(), "_tony-conf-test_tf_job/tony.xml").exists());
    Assert.assertTrue(args.contains(TensorFlowJobArg.HDFS_CLASSPATH.tonyParamName + " hdfs://nn:8020"));
    Assert.assertTrue(args.contains(TensorFlowJobArg.SHELL_ENV.tonyParamName + " E2=e2"));
    Assert.assertTrue(args.contains(TensorFlowJobArg.SHELL_ENV.tonyParamName + " E1=e1"));
  }

  @Test
  public void testClassPaths() {
    final Props sysProps = new Props();
    final Props jobProps = new Props();
    jobProps.put(TensorFlowJob.WORKING_DIR, FilenameUtils.getFullPath(FileIOUtils.getSourcePathFromClass(Props.class)));
    sysProps.put("jobtype.classpath", "123,456,789");
    sysProps.put("plugin.dir", "Plugins");
    final TensorFlowJob tfJob = new TensorFlowJob("test_tf_job_class_path", sysProps, jobProps, log) {
      @Override
      public String getWorkingDirectory() {
        return System.getProperty("java.io.tmpdir");
      }
    };
    List<String> paths = tfJob.getClassPaths();
    int counter = 0;
    for (String path : paths) {
      if (path.contains("Plugins/123") || path.contains("Plugins/456") || path.contains("Plugins/789")) {
        counter += 1;
      }
    }
    Assert.assertTrue(counter == 3);
    Assert.assertTrue(paths.contains(new File(tfJob.getWorkingDirectory(), "_tony-conf-test_tf_job_class_path").toString()));
  }
}
