package controllers;

import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithApplication;

import static org.junit.Assert.*;
import static play.mvc.Http.Status.*;


public class JobsConfigPageControllerTest extends WithApplication {

  @Override
  protected Application provideApplication() {
    return new GuiceApplicationBuilder().build();
  }

  @Test
  public void testIndex() {
    // TODO: Write tests
    assertEquals(OK, OK);
  }
}
