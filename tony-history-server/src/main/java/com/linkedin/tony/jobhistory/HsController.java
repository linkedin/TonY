/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */

package com.linkedin.tony.jobhistory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.linkedin.tony.conf.THSConfiguration;
import com.linkedin.tony.webapp.AMParams;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.v2.app.webapp.App;
import org.apache.hadoop.yarn.webapp.Controller;
import org.apache.hadoop.yarn.webapp.View;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

import static org.apache.hadoop.yarn.util.StringHelper.*;

public class HsController extends Controller implements AMParams {
  private final App app;
  public Path jobLogPath;
  private final Configuration conf;
  private static final Log LOG = LogFactory.getLog(HsController.class);

  @Inject
  HsController(App app, Configuration conf, RequestContext ctx) {
    super(ctx);
    this.app = app;
    this.conf = conf;
  }

  @Override
  public void index() {
    setTitle("JobHistory");
  }

  public void job() throws IOException {
    THSConfiguration xlearningConf = new THSConfiguration();
    jobLogPath = new Path(conf.get(THSConfiguration.THS_HISTORY_LOG_DIR,
        THSConfiguration.DEFAULT_THS_HISTORY_LOG_DIR) + "/" + $(APP_ID) + "/" + $(APP_ID));
    LOG.info("jobLogPath:" + jobLogPath);
    String line = null;
    try {
      FSDataInputStream in = jobLogPath.getFileSystem(xlearningConf).open(jobLogPath);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      line = br.readLine();
      in.close();
    } catch (IOException e) {
      LOG.info("open and read the log from " + jobLogPath + " error, " + e);
    }

    if (line == null) {
      set(CONTAINER_NUMBER, String.valueOf(0));
    } else {
      Gson gson = new Gson();
      Map<String, Object> readLog = new TreeMap<>();
      readLog = (Map) gson.fromJson(line, readLog.getClass());
      int i = 0;
      int workeri = 0;
      int psi = 0;
      set(OUTPUT_TOTAL, String.valueOf(0));
      set(TIMESTAMP_TOTAL, String.valueOf(0));
      set(WORKER_NUMBER, String.valueOf(0));
      set(PS_NUMBER, String.valueOf(0));
      Boolean cpuStatisticsFlag = false;
      Boolean cpuMetricsFlag = false;
      set(CONTAINER_CPU_METRICS_ENABLE, "false");
      for (String info : readLog.keySet()) {
        if (info.equals(AMParams.APP_TYPE)) {
          if (readLog.get(info) != null) {
            if (readLog.get(info).equals("THS")) {
              set(APP_TYPE, "XLearning");
            } else {
              char[] appType = String.valueOf(readLog.get(info)).toLowerCase().toCharArray();
              appType[0] -= 32;
              set(APP_TYPE, String.valueOf(appType));
            }
          } else {
            set(APP_TYPE, "XLearning");
          }
        } else if (info.equals(AMParams.BOARD_INFO)) {
          set(BOARD_INFO_FLAG, "true");
          set(BOARD_INFO, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.OUTPUT_PATH)) {
          if (readLog.get(info) instanceof ArrayList<?>) {
            List<String> outputList = (ArrayList<String>) readLog.get(info);
            if (outputList.size() == 0 || outputList.get(0).equals("-")) {
              set(OUTPUT_TOTAL, String.valueOf(0));
            } else {
              int j = 0;
              for (String output : outputList) {
                set(OUTPUT_PATH + j, output + conf.get(THSConfiguration.THS_INTERREAULST_DIR, THSConfiguration.DEFAULT_THS_INTERRESULT_DIR));
                j++;
              }
              set(OUTPUT_TOTAL, String.valueOf(j));
            }
          } else {
            set(OUTPUT_TOTAL, String.valueOf(0));
          }
        } else if (info.equals(AMParams.TIMESTAMP_LIST)) {
          if (readLog.get(info) instanceof ArrayList<?>) {
            List<String> savedTimeList = (ArrayList<String>) readLog.get(info);
            if (savedTimeList.size() == 0 || savedTimeList.get(0).equals("-")) {
              set(TIMESTAMP_TOTAL, String.valueOf(0));
            } else {
              int j = 0;
              for (String timeStamp : savedTimeList) {
                set(TIMESTAMP_LIST + j, timeStamp);
                j++;
              }
              set(TIMESTAMP_TOTAL, String.valueOf(j));
            }
          } else {
            set(TIMESTAMP_TOTAL, String.valueOf(0));
          }
        } else if (info.indexOf("container") > -1) {
          set(CONTAINER_ID + i, info);
          if (readLog.get(info) instanceof Map<?, ?>) {
            Map<String, String> containerMessage = (Map<String, String>) readLog.get(info);
            set(CONTAINER_HTTP_ADDRESS + i, containerMessage.get(AMParams.CONTAINER_HTTP_ADDRESS));
            set(CONTAINER_ROLE + i, containerMessage.get(AMParams.CONTAINER_ROLE));
            set(CONTAINER_STATUS + i, containerMessage.get(AMParams.CONTAINER_STATUS));
            set(CONTAINER_START_TIME + i, containerMessage.get(AMParams.CONTAINER_START_TIME));
            set(CONTAINER_FINISH_TIME + i, containerMessage.get(AMParams.CONTAINER_FINISH_TIME));
            set(CONTAINER_REPORTER_PROGRESS + i, containerMessage.get(AMParams.CONTAINER_REPORTER_PROGRESS));
            set(CONTAINER_LOG_ADDRESS + i, containerMessage.get(AMParams.CONTAINER_LOG_ADDRESS));
            if (containerMessage.containsKey(AMParams.CONTAINER_CPU_METRICS)) {
              String cpuMetrics = containerMessage.get(AMParams.CONTAINER_CPU_METRICS);
              if (cpuMetrics != null) {
                Gson gson2 = new GsonBuilder()
                    .registerTypeAdapter(
                        new TypeToken<ConcurrentHashMap<String, Object>>() {
                        }.getType(),
                        new JsonDeserializer<ConcurrentHashMap<String, Object>>() {
                          @Override
                          public ConcurrentHashMap<String, Object> deserialize(
                              JsonElement json, Type typeOfT,
                              JsonDeserializationContext context) throws JsonParseException {
                            ConcurrentHashMap<String, Object> treeMap = new ConcurrentHashMap<>();
                            JsonObject jsonObject = json.getAsJsonObject();
                            Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
                            for (Map.Entry<String, JsonElement> entry : entrySet) {
                              treeMap.put(entry.getKey(), entry.getValue());
                            }
                            return treeMap;
                          }
                        }).create();

                Type type = new TypeToken<ConcurrentHashMap<String, Object>>() {
                }.getType();
                ConcurrentHashMap<String, Object> map = gson2.fromJson(cpuMetrics, type);
                if (map.size() > 0) {
                  cpuMetricsFlag = true;
                  if (containerMessage.get(AMParams.CONTAINER_ROLE).equals("worker")) {
                    set("workerCpuMemMetrics" + workeri, new Gson().toJson(map.get("CPUMEM")));
                    if (map.containsKey("CPUUTIL")) {
                      set("workerCpuUtilMetrics" + workeri, new Gson().toJson(map.get("CPUUTIL")));
                    }
                  } else {
                    set("psCpuMemMetrics" + psi, new Gson().toJson(map.get("CPUMEM")));
                    if (map.containsKey("CPUUTIL")) {
                      set("psCpuUtilMetrics" + psi, new Gson().toJson(map.get("CPUUTIL")));
                    }
                  }
                }
              }
            }

            if (containerMessage.containsKey(AMParams.CONTAINER_CPU_STATISTICS)) {
              String cpuStatistics = containerMessage.get(AMParams.CONTAINER_CPU_STATISTICS);
              if (cpuStatistics != null && !cpuStatistics.equals("")) {
                Type type = new TypeToken<Map<String, List<Double>>>() {
                }.getType();
                Map<String, List<Double>> map = new Gson().fromJson(cpuStatistics, type);
                if (map.size() > 0) {
                  if (containerMessage.get(AMParams.CONTAINER_ROLE).equals("worker")) {
                    set("worker" + CONTAINER_CPU_STATISTICS_MEM + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUMEM").get(0)));
                    set("worker" + CONTAINER_CPU_STATISTICS_MEM + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUMEM").get(1)));
                    set("worker" + CONTAINER_CPU_STATISTICS_UTIL + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUUTIL").get(0)));
                    set("worker" + CONTAINER_CPU_STATISTICS_UTIL + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUUTIL").get(1)));
                  } else {
                    set("ps" + CONTAINER_CPU_STATISTICS_MEM + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUMEM").get(0)));
                    set("ps" + CONTAINER_CPU_STATISTICS_MEM + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUMEM").get(1)));
                    set("ps" + CONTAINER_CPU_STATISTICS_UTIL + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUUTIL").get(0)));
                    set("ps" + CONTAINER_CPU_STATISTICS_UTIL + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUUTIL").get(1)));
                  }
                  cpuStatisticsFlag = true;
                }
              }
            }

            if (containerMessage.containsKey(AMParams.CONTAINER_CPU_USAGE_WARN_MEM)) {
              if (containerMessage.get(AMParams.CONTAINER_ROLE).equals("worker")) {
                set("worker" + CONTAINER_CPU_USAGE_WARN_MEM + workeri, containerMessage.get(CONTAINER_CPU_USAGE_WARN_MEM));
              } else {
                set("ps" + CONTAINER_CPU_USAGE_WARN_MEM + psi, containerMessage.get(CONTAINER_CPU_USAGE_WARN_MEM));
              }
            }

            if (containerMessage.get(AMParams.CONTAINER_ROLE).equals("worker")) {
              set("WORKER_CONTAINER_ID" + workeri, info);
              workeri++;
            } else {
              set("PS_CONTAINER_ID" + psi, info);
              psi++;
            }
            i++;
          }
        } else if (info.equals(AMParams.WORKER_NUMBER)) {
          set(WORKER_NUMBER, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.PS_NUMBER)) {
          set(PS_NUMBER, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.WORKER_VCORES)) {
          set(WORKER_VCORES, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.PS_VCORES)) {
          set(PS_VCORES, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.WORKER_MEMORY)) {
          set(WORKER_MEMORY, String.valueOf(readLog.get(info)));
        } else if (info.equals(AMParams.PS_MEMORY)) {
          set(PS_MEMORY, String.valueOf(readLog.get(info)));
        }
      }
      set(CONTAINER_NUMBER, String.valueOf(i));
      if (cpuMetricsFlag) {
        set(CONTAINER_CPU_METRICS_ENABLE, String.valueOf(true));
      } else {
        set(CONTAINER_CPU_METRICS_ENABLE, String.valueOf(false));
      }
      if (cpuStatisticsFlag) {
        set(CONTAINER_CPU_STATISTICS, String.valueOf(true));
      } else {
        set(CONTAINER_CPU_STATISTICS, String.valueOf(false));
      }

      if ($(BOARD_INFO).equals("-")) {
        String boardInfo = "Board server don't start, You can set argument \"--boardEnable true\" in your submit script to start.";
        set(BOARD_INFO, boardInfo);
      } else {
        String boardLogDir = $(BOARD_INFO);
        if ($(APP_TYPE).equals("Tensorflow")) {
          set(BOARD_INFO, String.format("tensorboard --logdir=%s", boardLogDir));
        } else {
          set(BOARD_INFO, String.format("VisualDL not support the hdfs path for logdir. Please download the log from %s first. Then using \" visualDL \" to start the board", boardLogDir));
        }
      }
    }

    if (Boolean.parseBoolean($(CONTAINER_CPU_METRICS_ENABLE))) {
      try {
        WebApps.Builder.class.getMethod("build", WebApp.class);
      } catch (NoSuchMethodException e) {
        if (Controller.class.getClassLoader().getResource("webapps/static/xlWebApp") == null) {
          LOG.warn("Don't have the xlWebApp Resource.");
          set(CONTAINER_CPU_METRICS_ENABLE, String.valueOf(false));
        }
      }
    }
    setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
    render(jobPage());
  }

  protected Class<? extends View> jobPage() {
    return HsJobPage.class;
  }

  /**
   * Render the logs page.
   */
  public void logs() {
    render(HsLogsPage.class);
  }

}
