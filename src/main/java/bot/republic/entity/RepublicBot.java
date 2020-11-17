package bot.republic.entity;

import bot.republic.RepublicApplication;
import org.openqa.selenium.*;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RepublicBot {

    private static String botUsername;

    @Value("${bot.username}")
    public void setBotUsername(String value) {
        this.botUsername = value;
    }

    private static String botPassword;

    @Value("${bot.password}")
    public void set(String value) {
        this.botPassword = value;
    }

    private static String botBrowser;

    @Value("${bot.browser}")
    public void setBotBrowser(String value) {
        this.botBrowser = value;
    }

    private static String botSession;

    @Value("${bot.session}")
    public void setBotSession(String value) {
        this.botSession = value;
    }

    private static String chromeProfiler;

    @Value("${bot.profile.chrome}")
    public void setChromeProfiler(String value) {
        this.chromeProfiler = value;
    }

    private static String firefoxProfiler;

    @Value("${bot.profile.firefox}")
    public void setFirefoxProfiler(String value) {
        this.firefoxProfiler = value;
    }

    //Fields
    private static final String URL_BASE_PURCHASE = "https://republic.gg/purchase/purchase-form";
    private static final String URL_BASE_PURCHASE_DIAMONDS = "https://republic.gg/purchase/purchase-form/mobile-legends-diamonds";

    private static final String VOUCHER_TYPE_DIAMOND = "diamond";
    private static final String VOUCHER_TYPE_MEMBER = "member";

    private static final String BOT_BROWSER_CHROME = "chrome";
    private static final String BOT_BROWSER_FIREFOX = "firefox";

    private static final String BOT_SESSION_MODE_PRIVATE = "private";
    private static final String BOT_SESSION_MODE_PROFILED = "profiled";

    private static final String BOT_FIREFOX_PRIVATE_PROPERTY = "browser.privatebrowsing.autostart";
    private static final String BOT_CHROME_PRIVATE_PROPERTY = "--incognito";

    //ELEMENT LOCATORS
    private static final String ELEMENT_FORM_USER_ID = "purchaseform-custom_fields-userid";
    private static final String ELEMENT_FORM_ZONE_ID = "purchaseform-custom_fields-zoneid";

    private static final String ELEMENT_BUTTON_BUY_NOW = ".buy-now-button";

    private static final String ELEMENT_ERROR_USER_ID = ".field-purchaseform-custom_fields-userid > .help-block";
    private static final String ELEMENT_ERROR_ZONE_ID = ".field-purchaseform-custom_fields-zoneid > .help-block";
    private static final String ELEMENT_ERROR_BALANCE = ".field-purchaseform-package_id > .help-block";

    private static final String ELEMENT_STATUS_PENDING_SUCCESS = ".cr-green";

    //SCRIPT COMMAND
    private static final String JS_SCROLL_TO_INPUT = "window.scrollTo(0,113)";

    private static final String MESSAGE_ERROR_PLAYER_DOES_NOT_EXIST = "Invalid User ID or Zone ID";
    private static final String MESSAGE_SUCCESS_TRANSACTION = "Thank You For Purchase!";

    private Voucher voucher;
    private String url;
    String message = "";

    private Map<Boolean, String> transactionStatus = new HashMap<>();

    private static WebDriver driver;
    private static Map<String, Object> vars;
//    private static JavascriptExecutor js;

    @Autowired
    public RepublicBot() {

    }

    private void setUpBot() {
        if (botBrowser.equalsIgnoreCase(BOT_BROWSER_CHROME)) {
            ChromeOptions options = getChromeOption();
            driver = new ChromeDriver(options);
        } else if (botBrowser.equalsIgnoreCase(BOT_BROWSER_FIREFOX)) {
            FirefoxOptions firefoxOptions = getFirefoxOption();
            driver = new FirefoxDriver(firefoxOptions);
        }
//        js = (JavascriptExecutor) driver;
        vars = new HashMap<>();
    }

    private FirefoxOptions getFirefoxOption() {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        ProfilesIni profile = new ProfilesIni();
        if (botSession.equalsIgnoreCase(BOT_SESSION_MODE_PRIVATE)) {
            firefoxOptions.setCapability(BOT_FIREFOX_PRIVATE_PROPERTY, true);
            return firefoxOptions;
        } else if (botSession.equalsIgnoreCase(BOT_SESSION_MODE_PROFILED)) {
            FirefoxProfile myprofile = profile.getProfile(firefoxProfiler);
            firefoxOptions.setProfile(myprofile);
            return firefoxOptions;
        } else return firefoxOptions;
    }

    private ChromeOptions getChromeOption() {
        ChromeOptions options = new ChromeOptions();
        if (botSession.equalsIgnoreCase(BOT_SESSION_MODE_PRIVATE)) {
            options.addArguments(BOT_CHROME_PRIVATE_PROPERTY);
            return options;
        } else if (botSession.equalsIgnoreCase(BOT_SESSION_MODE_PROFILED)) {
            options.addArguments(chromeProfiler);
            return options;
        } else
            return options;
    }

    public RepublicBot(Voucher voucher) {
        this.voucher = voucher;
    }

    public static RepublicBot withVoucher(Voucher voucher) {
        return new RepublicBot(voucher);
    }

    private static void initSystemDriverProperties() {
        System.setProperty("webdriver.chrome.driver", "webdrivers\\chromedriver86.0.424.exe");
        System.setProperty("webdriver.gecko.driver", "webdrivers\\geckodriver.exe");
        System.setProperty("webdriver.ie.driver", "webdrivers\\IEDriverServer.exe");
    }

    public Map<Boolean, String> processTopTupAndGetMessage() {
        initSystemDriverProperties();
        try {

            setUpBot();
        } catch (Exception e) {
            e.printStackTrace();
            updateMessage("Start Browser Failed");
        }
        setUrl(voucher.getDenomUri(), voucher.getType());

        processTopUp();

        return transactionStatus;
    }

    private void setUrl(String denomUri, String type) {
        url = buildUrl(denomUri, type);
    }

    private String buildUrl(String denomUri, String type) {
        if (type.equalsIgnoreCase(VOUCHER_TYPE_DIAMOND)) {
            String params = "?price=" + denomUri;
            return URL_BASE_PURCHASE_DIAMONDS + params;
        } else if (type.equalsIgnoreCase(VOUCHER_TYPE_MEMBER)) {
            return URL_BASE_PURCHASE + denomUri;
        } else
            return null;
    }

    private void updateMessage(String message) {
        this.message = message;
    }

    private void processTopUp() {

        startBrowserAndNavigateToPage();

        inputPlayerIdAndSelectDenom();
        sleep(3);

        processPayment();

        sleep(3);

        try {
//            checkForPaymentResult();
            boolean status = false;

            if (areBalanceSufficientAndPlayerExist()) {
                if (isTransactionPending()) {
                    status = true;
                    message = MESSAGE_SUCCESS_TRANSACTION;
                }
            }

            transactionStatus.put(status, message);
        } catch (Exception e) {
            e.printStackTrace();
            updateMessage("WebDriver Failure During Transaction");
        } finally {
            driver.quit();
        }
    }

    private boolean isTransactionPending() {
        return driver.findElements(By.cssSelector(ELEMENT_STATUS_PENDING_SUCCESS)).size() != 0;
    }

    private boolean areBalanceSufficientAndPlayerExist() {
        return (!isElementExists(ELEMENT_ERROR_BALANCE) || !isPlayerExists());
    }

    private boolean isPlayerExists() {
        return (isElementExists(ELEMENT_ERROR_USER_ID));
    }

    private boolean isElementExists(String locator) {
        boolean isElementExists = false;
        try {
            RepublicApplication.logger.info("Checking ELements After Payment : " + driver.findElement(By.cssSelector(locator)).getText());
            isElementExists = driver.findElements(By.cssSelector(locator)).size() != 0;
            WebElement element = driver.findElement(By.cssSelector(locator));
            message += element.getText() + " ";
        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Failed Check Element : " + locator);
        }
        return isElementExists;
    }

    private void processPayment() {
        try {
            RepublicApplication.logger.info("Clicking 'Buy Now' Button");
            driver.findElement(By.cssSelector(ELEMENT_BUTTON_BUY_NOW)).click();
        } catch (Exception e) {
            e.printStackTrace();
            updateMessage("Failed Click Button Buy Now");
            RepublicApplication.logger.info("Error Clicking 'Buy Now' Button");
        }
    }

    private void startBrowserAndNavigateToPage() {
        try {
            RepublicApplication.logger.info("Navigating to : " + url);
            driver.get(url);
//            driver.manage().window().setSize(new Dimension(1463, 816));
        } catch (Exception e) {
            e.printStackTrace();
            updateMessage("Failure During Navigating to Url");
            RepublicApplication.logger.info("Error Navigating to URL");
        }
    }

    private void inputPlayerIdAndSelectDenom() {
//        js.executeScript(JS_SCROLL_TO_INPUT);
        try {
            RepublicApplication.logger.info("Input Player ID And Zone ID");
            driver.findElement(By.id(ELEMENT_FORM_USER_ID)).click();
            driver.findElement(By.id(ELEMENT_FORM_USER_ID)).clear();
            driver.findElement(By.id(ELEMENT_FORM_USER_ID)).sendKeys(voucher.getPlayer().getPlayerId());

            sleep(3);

            driver.findElement(By.id(ELEMENT_FORM_ZONE_ID)).click();
            driver.findElement(By.id(ELEMENT_FORM_ZONE_ID)).clear();
            driver.findElement(By.id(ELEMENT_FORM_ZONE_ID)).sendKeys(voucher.getPlayer().getZoneId());

            driver.findElement(By.id(ELEMENT_FORM_ZONE_ID)).sendKeys(Keys.ENTER);
        } catch (Exception e) {
            e.printStackTrace();
            updateMessage("Failure During Input PlayerId");
            RepublicApplication.logger.info("Error Input Player ID And Zone ID");
        }
    }

    private void sleep(Integer time) {
        try {
            Thread.sleep(time * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

