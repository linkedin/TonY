/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.tony;

import com.linkedin.minitony.cluster.HDFSUtils;
import com.linkedin.minitony.cluster.MiniCluster;
import com.linkedin.minitony.cluster.MiniTonyUtils;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Before running these tests in your IDE, you should run {@code ./gradlew
 * :tony-core:setupHdfsLib} first. If you make any changes to {@code
 * tony-core/src/main/java} code, you'll need to run the above command again.
 *
 * If you get an exception saying there's "no such file or directory tony-core/out/libs",
 * you will need to update the working directory in your test configuration
 * to {@code /path/to/linkedin/TonY}.
 */
public class TestTonyE2E {

  private MiniCluster cluster;
  private String yarnConf;
  private String hdfsConf;
  private Configuration conf = new Configuration();
  private TonyClient client;
  private String libPath;

  @BeforeClass
  public void doBeforeClass() throws Exception {
    // Set up mini cluster.
    cluster = new MiniCluster(3);
    cluster.start();
    yarnConf = Files.createTempFile("yarn", ".xml").toString();
    hdfsConf = Files.createTempFile("hdfs", ".xml").toString();
    MiniTonyUtils.saveConfigToFile(cluster.getYarnConf(), yarnConf);
    MiniTonyUtils.saveConfigToFile(cluster.getHdfsConf(), hdfsConf);
    FileSystem fs = FileSystem.get(cluster.getHdfsConf());
    libPath = cluster.getHdfsConf().get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY) + "/yarn/lib";
    // This is the path we gonna store required libraries in the local HDFS.
    Path cachedLibPath = new Path(libPath);
    if (fs.exists(cachedLibPath)) {
      fs.delete(cachedLibPath, true);
    }
    fs.mkdirs(cachedLibPath);
    HDFSUtils.copyDirectoryFilesToFolder(fs, "tony-core/out/libs", libPath);
    HDFSUtils.copyDirectoryFilesToFolder(fs, "tony-core/src/test/resources/log4j.properties", libPath);
  }

  @AfterClass
  public void doAfterClass() {
    cluster.stop();
  }

  @BeforeMethod
  public void doBeforeMethod() {
    conf = new Configuration();
    conf.setBoolean(TonyConfigurationKeys.SECURITY_ENABLED, false);
    conf.set(TonyConfigurationKeys.HDFS_CONF_LOCATION, hdfsConf);
    conf.set(TonyConfigurationKeys.YARN_CONF_LOCATION, yarnConf);
    client = new TonyClient(conf);
  }

  @Test
  public void testSingleNodeTrainingShouldPass() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.IS_SINGLE_NODE, true);
    client = new TonyClient(conf);
    client.init(new String[] {
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0_check_env.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--shell_env", "ENV_CHECK=ENV_CHECK",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }

  @Test
  public void testPSWorkerTrainingShouldFailMissedHeartbeat() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.SECURITY_ENABLED, false);
    conf.setInt(TonyConfigurationKeys.TASK_MAX_MISSED_HEARTBEATS, 2);
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0_check_env.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true",
        "--container_env", Constants.TEST_TASK_EXECUTOR_NUM_HB_MISS + "=5"
    });
    int exitCode = client.start();
    Assert.assertNotEquals(exitCode, 0);
  }

  @Test
  public void testPSSkewedWorkerTrainingShouldPass() throws ParseException, IOException {
    conf.setInt(TonyConfigurationKeys.getInstancesKey("worker"), 2);
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0_check_env.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--shell_env", "ENV_CHECK=ENV_CHECK",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true",
        "--container_env", Constants.TEST_TASK_EXECUTOR_SKEW + "=worker#0#30000"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }

  @Test
  public void testPSWorkerTrainingShouldPass() throws ParseException, IOException {
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "'python check_env_and_venv.py'",
        "--hdfs_classpath", libPath,
        "--shell_env", "ENV_CHECK=ENV_CHECK",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true",
        "--python_venv", "tony-core/src/test/resources/test.zip",
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }

  @Test
  public void testPSWorkerTrainingPyTorchShouldPass() throws ParseException, IOException {
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0_check_pytorchenv.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--shell_env", "ENV_CHECK=ENV_CHECK",
        "--conf", "tony.application.framework=pytorch",
        "--conf", "tony.ps.instances=0",
        "--conf", "tony.worker.instances=2",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }

  @Test
  public void testPSWorkerTrainingShouldFail() throws ParseException, IOException {
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_1.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, -1);
  }

  @Test
  public void testSingleNodeTrainingShouldFail() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.IS_SINGLE_NODE, true);
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_1.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, -1);
  }

  @Test
  public void testAMCrashTonyShouldFail() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.IS_SINGLE_NODE, true);
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--container_env", Constants.TEST_AM_CRASH + "=true",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, -1);
  }

  /**
   * Test that makes sure if a worker is killed due to OOM, AM should stop the training (or retry).
   * This test might hang if there is a regression in handling the OOM scenario.
   *
   * The reason why we use a Constants.TEST_WORKER_TERMINATED flag instead of simply requesting more memory than
   * allocated is that Physical Memory Enforcement doesn't seem to work under MiniYARN.
   */
  @Test
  public void testAMStopsJobAfterWorker0Killed() throws ParseException, IOException {
    client.init(new String[]{"--src_dir", "tony-core/src/test/resources/scripts", "--executes", "exit_0.py",
        "--hdfs_classpath", libPath, "--python_binary_path", "python", "--container_env",
        Constants.TEST_WORKER_TERMINATED + "=true"});
    int exitCode = client.start();
    Assert.assertEquals(exitCode, -1);
  }

  /**
   * Test amRpcClient is nulled out after client finishes.
   */
  @Test
  public void testNullAMRpcClient() throws ParseException, IOException {
    String[] args = new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_0.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python"
    };
    client.init(args);
    Assert.assertTrue(client.start() == 0);
    Assert.assertNull(client.getAMRpcClient());
  }

  @Test
  public void testNonChiefWorkerFail() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.IS_SINGLE_NODE, false);
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "exit_1.py",
        "--hdfs_classpath", libPath,
        "--python_binary_path", "python",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true"
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, -1);
  }

  @Test
  public void testTonyResourcesFlag() throws ParseException, IOException {
    conf.setBoolean(TonyConfigurationKeys.IS_SINGLE_NODE, false);
    client = new TonyClient(conf);
    String resources = "tony-core/src/test/resources/test.zip" + ",tony-core/src/test/resources/test2.zip#archive,"
            + libPath;
    client.init(new String[]{
        "--executes", "'python check_archive_file_localization.py'",
        "--hdfs_classpath", libPath,
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true",
        "--conf", "tony.worker.resources=" + resources,
        "--conf", "tony.ps.instances=0",
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }

  @Test
  public void testTensorBoardPortSetOnlyOnChiefWorker() throws ParseException, IOException {
    client = new TonyClient(conf);
    client.init(new String[]{
        "--src_dir", "tony-core/src/test/resources/scripts",
        "--executes", "'python check_tb_port_set_in_chief_only.py'",
        "--hdfs_classpath", libPath,
        "--container_env", Constants.SKIP_HADOOP_PATH + "=true",
        "--conf", "tony.chief.instances=1",
        "--conf", "tony.ps.instances=1",
        "--conf", "tony.worker.instances=1",
    });
    int exitCode = client.start();
    Assert.assertEquals(exitCode, 0);
  }
}
