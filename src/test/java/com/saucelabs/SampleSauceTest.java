package com.saucelabs;

import com.saucelabs.common.SauceOnDemandAuthentication;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.saucelabs.junit.Parallelized;
import com.saucelabs.junit.ConcurrentParameterized;
import com.saucelabs.junit.SauceOnDemandTestWatcher;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.lang.Thread;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.saucerest.SauceREST;


/**
 * Demonstrates how to write a JUnit test that runs tests against Sauce Labs using multiple browsers in parallel.
 * <p/>
 * The test also includes the {@link SauceOnDemandTestWatcher} which will invoke the Sauce REST API to mark
 * the test as passed or failed.
 *
 * @author Ross Rowe
 */
@SuppressWarnings("unused")
@RunWith(ConcurrentParameterized.class)
public class SampleSauceTest implements SauceOnDemandSessionIdProvider {

    /**
     * Constructs a {@link SauceOnDemandAuthentication} instance using the supplied user name/access key.  To use the authentication
     * supplied by environment variables or from an external file, use the no-arg {@link SauceOnDemandAuthentication} constructor.
     */
	public static final String SAUCE_USERNAME = System.getenv("SAUCE_USERNAME");
	public static final String SAUCE_ACCESS_KEY = System.getenv("SAUCE_ACCESS_KEY");
	public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication(SAUCE_USERNAME, SAUCE_ACCESS_KEY);
    
    /**
     * JUnit Rule which will mark the Sauce Job as passed/failed when the test succeeds or fails.
     */
    @Rule
    public SauceOnDemandTestWatcher resultReportingTestWatcher = new SauceOnDemandTestWatcher(this, authentication);

    /**
     * Represents the browser to be used as part of the test run.
     */
    private String browser;
    /**
     * Represents the operating system to be used as part of the test run.
     */
    private String os;
    /**
     * Represents the version of the browser to be used as part of the test run.
     */
    private String version;
    /**
     * Instance variable which contains the Sauce Job Id.
     */
    private String sessionId;

    /**
     * The {@link WebDriver} instance which is used to perform browser interactions with.
     */
    private WebDriver driver;
    
    private SauceREST sauceClient;
    
    @Rule
    public TestName name = new TestName() {
        public String getMethodName() {
           return String.format("%s", super.getMethodName());
        }
    };
    
    public static String buildTag;
    private String pageSource;
    

    /**
     * Constructs a new instance of the test.  The constructor requires three string parameters, which represent the operating
     * system, version and browser to be used when launching a Sauce VM.  The order of the parameters should be the same
     * as that of the elements within the {@link #browsersStrings()} method.
     * @param os
     * @param version
     * @param browser
     */
    public SampleSauceTest(String os, String version, String browser) {
        super();
        this.os = os;
        this.version = version;
        this.browser = browser;
    }
    
    /*
    public SampleSauceTest() {
        super();
    }*/

    /**
     * @return a LinkedList containing String arrays representing the browser combinations the test should be run against. The values
     * in the String array are used as part of the invocation of the test constructor
     */
    @ConcurrentParameterized.Parameters
    public static LinkedList<String[]> browsersStrings() {
        LinkedList<String[]> browsers = new LinkedList<String[]>();
        browsers.add(new String[]{"Windows 7", "42", "chrome"});
        return browsers;
    }

    /**
     * Constructs a new {@link RemoteWebDriver} instance which is configured to use the capabilities defined by the {@link #browser},
     * {@link #version} and {@link #os} instance variables, and which is configured to run against ondemand.saucelabs.com, using
     * the username and access key populated by the {@link #authentication} instance.
     *
     * @throws Exception if an error occurs during the creation of the {@link RemoteWebDriver} instance.
     */
    @Before
    public void setUp() throws Exception {

    	DesiredCapabilities caps = new DesiredCapabilities();
    	caps.setBrowserName(System.getenv("SELENIUM_BROWSER"));
    	//caps.setVersion(System.getenv("SELENIUM_VERSION"));
    	//caps.setCapability(CapabilityType.BROWSER_NAME, System.getenv(SELENIUM_BROWSER));
        if (version != null) {
            //caps.setCapability(CapabilityType.VERSION, System.getenv(SELENIUM_VERSION));
            caps.setVersion(System.getenv("SELENIUM_VERSION"));
        }
        //caps.setCapability(CapabilityType.VERSION, System.getenv(SELENIUM_VERSION));
        caps.setCapability(CapabilityType.PLATFORM, System.getenv("SELENIUM_PLATFORM"));
        
        
        String methodName = name.getMethodName();
        caps.setCapability("name", methodName);
        
        if(buildTag != null){
        	caps.setCapability("build", buildTag);
        }

        //if we have orientation then set it
        if (System.getenv("SELENIUM_DEVICE_ORIENTATION") != null) {
            caps.setCapability("deviceOrientation", System.getenv("SELENIUM_DEVICE_ORIENTATION"));
        }

        //caps.setCapability("name", name.getMethodName();;
        //caps.setCapability("build", name.getMethodName() + "__" + System.getenv("BUILD_NUMBER"));
        caps.setCapability("public", "public");
        //caps.setCapability("tunnelIdentifier", "test-tun");
        this.driver = new RemoteWebDriver(
                new URL("http://" + authentication.getUsername() + ":" + authentication.getAccessKey() + "@ondemand.saucelabs.com:80/wd/hub"),caps);
        this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
        //this.sauceClient = new SauceREST(authentication.getUsername(), authentication.getAccessKey());
        //this.pageSource = driver.getPageSource();
        //System.out.println(pageSource);

    }

    /**
     * Runs a simple test verifying the title of the amazon.com homepage.
     * @throws Exception
     */
    
    @Test
    public void testGoogle() throws Exception {
    	driver.get("https://www.google.com/");
    	assertEquals("Google", driver.getTitle());
    }
    
    @Test
    public void testGoogle2() throws Exception {
    	this.testGoogle();
    }
    
    @Test
    public void testGoogle3() throws Exception {
    	this.testGoogle();
        WebElement query = driver.findElement(By.name("q"));
        query.sendKeys("Sauce Labs");
        query.submit();
    }

    /**
     * Closes the {@link WebDriver} session.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

    /**
     *
     * @return the value of the Sauce Job id.
     */
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
}
