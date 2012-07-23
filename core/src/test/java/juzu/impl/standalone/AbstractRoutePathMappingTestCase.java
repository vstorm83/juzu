package juzu.impl.standalone;

import juzu.test.protocol.standalone.AbstractStandaloneTestCase;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URL;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class AbstractRoutePathMappingTestCase extends AbstractStandaloneTestCase {

  @Drone
  WebDriver driver;

  protected abstract String[] getApplication();

  @Test
  public void testRender() throws Exception {
    assertDeploy(getApplication());
    driver.get(deploymentURL.toString());
    WebElement trigger = driver.findElement(By.id("trigger"));
    String href = trigger.getAttribute("href");
    URL url = new URL(href);
    assertEquals("/juzu/foo", url.getPath());
    assertNull(url.getQuery());
    trigger.click();
    String pass = driver.findElement(By.tagName("body")).getText();
    assertEquals("pass", pass);
    assertUndeploy();
  }
}
