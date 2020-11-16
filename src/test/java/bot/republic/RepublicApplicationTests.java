package bot.republic;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import java.util.HashMap;
import java.util.Map;

//@SpringBootTest
class RepublicApplicationTests {

	private WebDriver driver;
	private Map<String, Object> vars;
	JavascriptExecutor js;
	@Before
	public void setUp() {
		driver = new ChromeDriver();
		js = (JavascriptExecutor) driver;
		vars = new HashMap<String, Object>();
	}
	@After
	public void tearDown() {
		driver.quit();
	}
	@Test
	public void untitled() {
		driver.get("https://republic.gg/purchase/purchase-form/mobile-legends-diamonds?price=500");
		driver.manage().window().setSize(new Dimension(1463, 816));
		driver.findElement(By.id("purchaseform-custom_fields-userid")).click();
		driver.findElement(By.id("purchaseform-custom_fields-userid")).click();
		{
			WebElement element = driver.findElement(By.id("purchaseform-custom_fields-userid"));
			Actions builder = new Actions(driver);
			builder.doubleClick(element).perform();
		}
		driver.findElement(By.id("purchaseform-custom_fields-userid")).sendKeys("126606687");
		driver.findElement(By.id("purchaseform-custom_fields-zoneid")).sendKeys("2632");
		driver.findElement(By.cssSelector(".buy-now-button")).click();
		driver.findElement(By.cssSelector(".col-8 > div:nth-child(1)")).click();
		driver.findElement(By.cssSelector(".col-8 > div:nth-child(1)")).click();
		driver.findElement(By.cssSelector(".cr-orange")).click();
		driver.findElement(By.cssSelector(".item-price")).click();
		driver.findElement(By.cssSelector(".col-8 > div:nth-child(1)")).click();
		driver.findElement(By.cssSelector(".mb-1")).click();
		driver.findElement(By.cssSelector(".item-price")).click();
		driver.findElement(By.cssSelector(".mb-1")).click();
		driver.findElement(By.cssSelector(".mb-1")).click();
		driver.findElement(By.cssSelector(".mb-1")).click();
	}

}
