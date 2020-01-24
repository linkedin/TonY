package controllers;

import cache.CacheWrapper;
import com.google.common.cache.Cache;
import com.linkedin.tony.models.JobEvent;
import com.linkedin.tony.util.HdfsUtils;
import com.linkedin.tony.util.ParserUtils;
import hadoop.Requirements;
import java.util.List;
import javax.inject.Inject;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import play.mvc.Controller;
import play.mvc.Result;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import com.linkedin.tony.models.JobMetadata;

public class JobEventPageController extends Controller {
  private FileSystem myFs;
  private Cache<String, List<JobEvent>> cache;
  private Path interm;
  private Path finished;
  private YarnConfiguration yarnConf;
  private Cache<String, JobMetadata> metaDataCache;

  @Inject
  public JobEventPageController(Requirements requirements, CacheWrapper cacheWrapper) {
    myFs = requirements.getFileSystem();
    cache = cacheWrapper.getEventCache();
    interm = requirements.getIntermediateDir();
    finished = requirements.getFinishedDir();
    yarnConf = cacheWrapper.getYarnConf();
    metaDataCache = cacheWrapper.getMetadataCache();
  }

  public Result index(String jobId) {
    List<JobEvent> listOfEvents;
    if (myFs == null) {
      return internalServerError("Failed to initialize file system in " + this.getClass());
    }

    // Check cache
    listOfEvents = cache.getIfPresent(jobId);
    if (listOfEvents != null) {
      return ok(views.html.event.render(listOfEvents));
    }

    // Check finished dir
    Path jobFolder = HdfsUtils.getJobDirPath(myFs, finished, jobId);
    if (jobFolder != null) {
      String userName = getUserNameFromMetaDataCache(jobId);
      listOfEvents = ParserUtils.mapEventToJobEvent(ParserUtils.parseEvents(myFs, jobFolder), yarnConf, userName);
      cache.put(jobId, listOfEvents);
      return ok(views.html.event.render(listOfEvents));
    }

    // Check intermediate dir
    jobFolder = HdfsUtils.getJobDirPath(myFs, interm, jobId);
    if (jobFolder != null) {
      return internalServerError("Cannot display events because job is still running");
    }

    return internalServerError("Failed to fetch events");
  }

  /**
   *
   * @param jobID jobId provided by the user
   * @return user who launch the application
   */
  private String getUserNameFromMetaDataCache(String jobID) {
    String userName = null;
    if (metaDataCache != null) {
      userName = metaDataCache.getIfPresent(jobID).getUser();
    }
    return userName;
  }
}
