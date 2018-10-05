/**
 * Copyright 2018 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */

package com.linkedin.tony.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TBODY;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TR;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TD;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InfoBlock extends HtmlBlock implements AMParams {
  private static final Log LOG = LogFactory.getLog(InfoBlock.class);

  @Override
  protected void render(Block html) {
    int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
    int numWorkers = Integer.parseInt($(WORKER_NUMBER));
    int numPS = Integer.parseInt($(PS_NUMBER));
    if (numContainers > 0) {
      TBODY<TABLE<Hamlet>> tbody = html.
          h2("All Containers:").
          table("#Containers").
          thead("ui-widget-header").
          tr().
          th("ui-state-default", "Container ID").
          th("ui-state-default", "Container Host").
          th("ui-state-default", "Container Role").
          th("ui-state-default", "Container Status").
          th("ui-state-default", "Start Time").
          th("ui-state-default", "Finish Time").
          th("ui-state-default", "Reporter Progress").
          __().__().
          tbody();

      for (int i = 0; i < numContainers; i++) {
        TD<TR<TBODY<TABLE<Hamlet>>>> td = tbody.
            __().tbody("ui-widget-content").
            tr().
            $style("text-align:center;").td();
        td.span().$title(String.format($(CONTAINER_ID + i))).__().
            a(String.format("http://%s/node/containerlogs/%s/%s",
                $(CONTAINER_HTTP_ADDRESS + i),
                $(CONTAINER_ID + i),
                $(USER_NAME)),
                String.format($(CONTAINER_ID + i)));
        String containerMachine = $(CONTAINER_HTTP_ADDRESS + i);

        if ($(CONTAINER_REPORTER_PROGRESS + i).equals("progress log format error")) {
          td.__().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td($(CONTAINER_REPORTER_PROGRESS + i)).__();
        } else if ($(CONTAINER_REPORTER_PROGRESS + i).equals("0.00%")) {
          td.__().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td("N/A").__();
        } else {
          td.__().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).td()
              .div().$class("ui-progressbar ui-widget ui-widget-content ui-corner-all").$title($(CONTAINER_REPORTER_PROGRESS + i))
              .div().$class("ui-progressbar-value ui-widget-header ui-corner-left").$style("width:" + $(CONTAINER_REPORTER_PROGRESS + i))
              .__().__().__().__();
        }

      }

      if (!$(BOARD_INFO).equals("no")) {
        if (!$(BOARD_INFO).contains("http")) {
          tbody.__().__().div().$style("margin:20px 2px;").__(" ").__().
              h2("View Board:").
              table("#Board").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "Board Info").
              __().__().
              tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td(String.format($(BOARD_INFO))).
              __().__().__();
        } else {
          tbody.__().__().div().$style("margin:20px 2px;").__(" ").__().
              h2("View Board:").
              table("#Board").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "Board Info").
              __().__().
              tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td().span().$title(String.format($(BOARD_INFO))).__().
              a(String.format($(BOARD_INFO)),
                  String.format($(BOARD_INFO))).
              __().__().__().__();
        }
      } else {
        tbody.__().__();
      }

      html.div().$style("margin:20px 2px;").__(" ").__();
      int saveModelTotal = Integer.parseInt($(SAVE_MODEL_TOTAL));
      int saveModelSize = Integer.parseInt($(OUTPUT_TOTAL));
      if ((saveModelTotal > 0) && (saveModelSize > 0)) {
        if (!Boolean.valueOf($(SAVE_MODEL))) {
          html.div().button().$id("saveModel").$onclick("savedModel()").b("Save Model").__().__();
          StringBuilder script = new StringBuilder();
          script.append("function savedModel(){")
              .append("document.getElementById(\"saveModel\").disable=true;")
              .append("document.location.href='/proxy/").append($(APP_ID))
              .append("/proxy/savedmodel';")
              .append("}");
          html.script().$type("text/javascript").__(script.toString()).__();
          if (!Boolean.valueOf($(LAST_SAVE_STATUS))) {
            html.div().$style("margin:20px 2px;").__(" ").__();
          } else {
            html.div().$style("margin:20px 2px;").__("saved the model completed!").__();
          }
        } else {
          html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
          if (!$(SAVE_MODEL_STATUS).equals($(SAVE_MODEL_TOTAL))) {
            html.div().$style("margin:20px 2px;").__(String.format("saving the model ... %s/%s",
                $(SAVE_MODEL_STATUS), $(SAVE_MODEL_TOTAL))).__();
          } else {
            StringBuilder script = new StringBuilder();
            script.append("location.href='/proxy/").append($(APP_ID))
                .append("';");
            html.script().$type("text/javascript").__(script.toString()).__();
          }
        }
      } else if (saveModelSize == 0) {
        html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
        html.div().$style("margin:20px 2px;").__("don't have the local output dir").__();
      } else if (saveModelTotal == 0) {
        html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
      }

      int modelSaveTotal = Integer.parseInt($(TIMESTAMP_TOTAL));
      if (modelSaveTotal > 0) {
        TBODY<TABLE<Hamlet>> tbodySave = html.
            h2("").
            table("#savedmodel").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "Saved timeStamp").
            th("ui-state-default", "Saved path").
            __().__().
            tbody();

        for (int i = 0; i < modelSaveTotal; i++) {
          String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          TD<TR<TBODY<TABLE<Hamlet>>>> td = tbodySave.
              __().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td(timeStamp).
              td();

          String pathStr = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          for (int j = 0; j < saveModelSize; j++) {
            td.p().__($(OUTPUT_PATH + j) + pathStr).__();
          }
          td.__().__();
        }
        tbodySave.__().__();
      }

      // resource applied info
      html.div().$style("margin:20px 2px;").__(" ").__();
      TBODY<TABLE<Hamlet>> resourceAppliedInfo = html.
          h2("Resource Applied Info:").
          table("#resourceAppliedInfo").
          thead("ui-widget-header").
          tr().
          th("ui-state-default", "Role").
          th("ui-state-default", "Number").
          th("ui-state-default", "CPU Memory(GB)").
          th("ui-state-default", "CPU Cores").
          __().__().
          tbody();
      if(numWorkers > 0){
        resourceAppliedInfo.
            __().tbody("ui-widget-content").
            tr().
            $style("text-align:center;").
            td("worker").
            td(String.valueOf(numWorkers)).
            td($(WORKER_MEMORY)).
            td($(WORKER_VCORES)).
            __();
      }
      if (numPS > 0){
        resourceAppliedInfo.
            __().tbody("ui-widget-content").
            tr().
            $style("text-align:center;").
            td("ps").
            td(String.valueOf(numPS)).
            td($(PS_MEMORY)).
            td($(PS_VCORES)).
            __();
      }
      resourceAppliedInfo.__().__();

      html.div().$style("margin:20px 2px;").__(" ").__();

      // worker/ps containers resource usage statistics info
      if (numWorkers > 0) {
        TBODY<TABLE<Hamlet>> workerCPUUsage = html.
            h2("Worker Containers CPU Usage Info:").
            table("#workerCPUUsageInfo").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "ContainerID").
            th("ui-state-default", "CPU memory average usages(GB)").
            th("ui-state-default", "CPU memory max usages(GB)").
            th("ui-state-default", "CPU utilization average usages(%)").
            th("ui-state-default", "CPU utilization max usages(%)").
            __().__().
            tbody();

        for (int i = 0; i < numWorkers; i++) {
          workerCPUUsage.
              __().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td($(CONTAINER_ID + i)).
              td($(CONTAINER_CPU_STATISTICS_MEM + USAGE_AVG + i)).
              td($(CONTAINER_CPU_STATISTICS_MEM + USAGE_MAX + i)).
              td($(CONTAINER_CPU_STATISTICS_UTIL + USAGE_AVG + i)).
              td($(CONTAINER_CPU_STATISTICS_UTIL + USAGE_MAX + i)).
              __();
        }
        workerCPUUsage.__().__();
      }

      if(numPS > 0) {
        html.div().$style("margin:20px 2px;").__(" ").__();
        TBODY<TABLE<Hamlet>> psCPUUsage = html.
            h2("PS Containers CPU Usage Info:").
            table("#psCPUUsageInfo").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "ContainerID").
            th("ui-state-default", "CPU memory average usages(GB)").
            th("ui-state-default", "CPU memory max usages(GB)").
            th("ui-state-default", "CPU utilization average usages(%)").
            th("ui-state-default", "CPU utilization max usages(%)").
            __().__().
            tbody();

        for (int i = numWorkers; i < numContainers; i++) {
          psCPUUsage.
              __().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td($(CONTAINER_ID + i)).
              td($(CONTAINER_CPU_STATISTICS_MEM + USAGE_AVG + i)).
              td($(CONTAINER_CPU_STATISTICS_MEM + USAGE_MAX + i)).
              td($(CONTAINER_CPU_STATISTICS_UTIL + USAGE_AVG + i)).
              td($(CONTAINER_CPU_STATISTICS_UTIL + USAGE_MAX + i)).
              __();
        }
        psCPUUsage.__().__();

      }

      html.div().$style("margin:20px 2px;").__(" ").__();
      if (Boolean.parseBoolean($(CONTAINER_CPU_METRICS_ENABLE))) {
        int i = 0;
        for (; i < numWorkers; i++) {
          if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
            html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " metrics:").__();

            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/jquery-3.1.1.min.js").__();
            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/highstock.js").__();
            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/exporting.js").__();

            String containerCpuMemID = "containerCpuMem" + i;
            String containerCpuUtilID = "containerCpuUtil" + i;
            String containerClass = "container" + i;
            String seriesCpuMemOptions = "[{\n" +
                "            name: 'cpu mem used',\n" +
                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                "            name: 'cpu util',\n" +
                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                "        }]";
            if (!$("cpuUtilMetrics" + i).equals("") && $("cpuUtilMetrics" + i) != null) {
              html.div()
                  .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .__();
            } else {
              html.div()
                  .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .__();
            }
            String css = "." + containerClass + "{\n" +
                "    display:inline-block;\n" +
                "}";
            html.style().$type("text/css").__(css).__();
            String striptHead = "Highcharts.setOptions({\n" +
                "    global: {\n" +
                "        useUTC: false\n" +
                "    }\n" +
                "});\n" +
                "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                "    chart: {\n" +
                "        width: 600\n" +
                "    },\n" +
                "\n" +
                "    rangeSelector: {\n" +
                "        buttons: [{\n" +
                "            count: 1,\n" +
                "            type: 'minute',\n" +
                "            text: '1M'\n" +
                "        }, {\n" +
                "            count: 5,\n" +
                "            type: 'minute',\n" +
                "            text: '5M'\n" +
                "        }, {\n" +
                "            type: 'all',\n" +
                "            text: 'All'\n" +
                "        }],\n" +
                "        inputEnabled: false,\n" +
                "        selected: 0\n" +
                "    },\n" +
                "\n" +
                "    title: {\n" +
                "        text: 'cpu memory used( GB )'\n" +
                "    },\n" +
                "\n" +
                "    credits: {\n" +
                "        enabled: false\n" +
                "    },\n" +
                "\n" +
                "    exporting: {\n" +
                "        enabled: false\n" +
                "    },\n" +
                "\n" +
                "    series: " + seriesCpuMemOptions + "\n" +
                "});\n";
            if(!$("cpuUtilMetrics" + i).equals("") && $("cpuUtilMetrics" + i) != null) {
              striptBody += "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                  "    chart: {\n" +
                  "        width: 600\n" +
                  "    },\n" +
                  "\n" +
                  "    rangeSelector: {\n" +
                  "        buttons: [{\n" +
                  "            count: 1,\n" +
                  "            type: 'minute',\n" +
                  "            text: '1M'\n" +
                  "        }, {\n" +
                  "            count: 5,\n" +
                  "            type: 'minute',\n" +
                  "            text: '5M'\n" +
                  "        }, {\n" +
                  "            type: 'all',\n" +
                  "            text: 'All'\n" +
                  "        }],\n" +
                  "        inputEnabled: false,\n" +
                  "        selected: 0\n" +
                  "    },\n" +

                  "\n" +
                  "    title: {\n" +
                  "        text: 'cpu utilization( % )'\n" +
                  "    },\n" +
                  "\n" +
                  "    credits: {\n" +
                  "        enabled: false\n" +
                  "    },\n" +
                  "\n" +
                  "    exporting: {\n" +
                  "        enabled: false\n" +
                  "    },\n" +
                  "\n" +
                  "    series: " + seriesCpuUtilOptions + "\n" +
                  "});\n";
            }
            html.script().$type("text/javascript").__(striptHead + striptBody).__();
          }
        }

        for (; i < numContainers; i++) {
          if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
            html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " metrics:").__();

            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/jquery-3.1.1.min.js").__();
            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/highstock.js").__();
            html.script().$src("/proxy/" + $(APP_ID) + "/static/xlWebApp/exporting.js").__();

            String containerCpuMemID = "containerCpuMem" + i;
            String containerCpuUtilID = "containerCpuUtil" + i;
            String containerClass = "container" + i;
            String seriesCpuMemOptions = "[{\n" +
                "            name: 'cpu mem used',\n" +
                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                "            name: 'cpu util',\n" +
                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                "        }]";
            if (!$("cpuUtilMetrics" + i).equals("") && $("cpuUtilMetrics" + i) != null) {
              html.div()
                  .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .__();
            } else {
              html.div()
                  .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                  .__();
            }
            String css = "." + containerClass + "{\n" +
                "    display:inline-block;\n" +
                "}";
            html.style().$type("text/css").__(css).__();
            String striptHead = "Highcharts.setOptions({\n" +
                "    global: {\n" +
                "        useUTC: false\n" +
                "    }\n" +
                "});\n" +
                "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                "    chart: {\n" +
                "        width: 600\n" +
                "    },\n" +
                "\n" +
                "    rangeSelector: {\n" +
                "        buttons: [{\n" +
                "            count: 1,\n" +
                "            type: 'minute',\n" +
                "            text: '1M'\n" +
                "        }, {\n" +
                "            count: 5,\n" +
                "            type: 'minute',\n" +
                "            text: '5M'\n" +
                "        }, {\n" +
                "            type: 'all',\n" +
                "            text: 'All'\n" +
                "        }],\n" +
                "        inputEnabled: false,\n" +
                "        selected: 0\n" +
                "    },\n" +
                "\n" +
                "    title: {\n" +
                "        text: 'cpu memory used( GB )'\n" +
                "    },\n" +
                "\n" +
                "    credits: {\n" +
                "        enabled: false\n" +
                "    },\n" +
                "\n" +
                "    exporting: {\n" +
                "        enabled: false\n" +
                "    },\n" +
                "\n" +
                "    series: " + seriesCpuMemOptions + "\n" +
                "});\n";
            if (!$("cpuUtilMetrics" + i).equals("") && $("cpuUtilMetrics" + i) != null) {
              striptBody += "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                  "    chart: {\n" +
                  "        width: 600\n" +
                  "    },\n" +
                  "\n" +
                  "    rangeSelector: {\n" +
                  "        buttons: [{\n" +
                  "            count: 1,\n" +
                  "            type: 'minute',\n" +
                  "            text: '1M'\n" +
                  "        }, {\n" +
                  "            count: 5,\n" +
                  "            type: 'minute',\n" +
                  "            text: '5M'\n" +
                  "        }, {\n" +
                  "            type: 'all',\n" +
                  "            text: 'All'\n" +
                  "        }],\n" +
                  "        inputEnabled: false,\n" +
                  "        selected: 0\n" +
                  "    },\n" +

                  "\n" +
                  "    title: {\n" +
                  "        text: 'cpu utilization( % )'\n" +
                  "    },\n" +
                  "\n" +
                  "    credits: {\n" +
                  "        enabled: false\n" +
                  "    },\n" +
                  "\n" +
                  "    exporting: {\n" +
                  "        enabled: false\n" +
                  "    },\n" +
                  "\n" +
                  "    series: " + seriesCpuUtilOptions + "\n" +
                  "});\n";
            }
            html.script().$type("text/javascript").__(striptHead + striptBody).__();
          }
        }
      }
    } else {
      html.div().$style("font-size:20px;").__("Waiting for all containers allocated......").__();
    }
  }
}