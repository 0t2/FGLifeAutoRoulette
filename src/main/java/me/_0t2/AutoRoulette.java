package me._0t2;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class AutoRoulette {

    private final String FGLIFE_ROULETTE_URL = "https://member.fglife.com.tw/roulette.do?actionType=list";
    private final String XPATH_CAPTCHA_IMG = "//img[@class=\"myImg\"]";
    private final String XPATH_INPUT_IDNO = "//input[@id=\"idno\"]";
    private final String XPATH_INPUT_CAPTCHA = "//input[@id=\"captcha\"]";
    private final String XPATH_BUTTON_PLAY = "//button[@id=\"btn-play\"]";
    private final String PHANTOMJS_SSL_PROTOCOL_ANY = "--ssl-protocol=any";
    private final String PHANTOMJS_IGNORE_SSL_ERRORS_TRUE = "--ignore-ssl-errors=true";
    private final String PHANTOMJS_PATH = "/opt/phantomjs/bin/phantomjs";
    private final String XPATH_BUTTON_GO = "//button[@class=\"button icon-go\"]";
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final String[] ids = { "YOUR_ID_HERE" };
    private WebDriver driver = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        AutoRoulette autoRoulette = new AutoRoulette();
        autoRoulette.startScheduleTask();
    }

    private void startScheduleTask() {
        final ScheduledFuture<?> taskHandle = scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    for (String id : ids) {
                        for (int i = 0; i < 5; i++) {
                            if (playRoulette(id))
                                break;
                        }
                    }
                }
                , 0, 24, TimeUnit.HOURS);
    }

    private boolean playRoulette(String id) {
        File file = new File(PHANTOMJS_PATH);
        DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
        String[] phantomJsArgs = {PHANTOMJS_SSL_PROTOCOL_ANY, PHANTOMJS_IGNORE_SSL_ERRORS_TRUE};
        desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, phantomJsArgs);
        desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, file.getAbsolutePath());
        driver = new PhantomJSDriver(desiredCapabilities);
        try {
            driver.manage().timeouts().pageLoadTimeout(2000, TimeUnit.SECONDS);
            driver.get(FGLIFE_ROULETTE_URL);
            File screenshotLocation = getCaptchaImage();
            ITesseract instance = new Tesseract();
            String result;
            try {
                result = instance.doOCR(screenshotLocation).split(System.lineSeparator())[0].trim().replaceAll(" ", "");
                if (result.length() != 4)
                    return false;
            } catch (TesseractException e) {
                e.printStackTrace();
                return false;
            }
            driver.findElement(By.xpath(XPATH_INPUT_IDNO)).sendKeys(id);
            driver.findElement(By.xpath(XPATH_INPUT_CAPTCHA)).sendKeys(result);
            driver.findElement(By.xpath(XPATH_BUTTON_GO)).click();
            driver.findElement(By.xpath(XPATH_BUTTON_PLAY));
            Thread.sleep(5000);
            driver.findElement(By.xpath(XPATH_BUTTON_PLAY)).click();
            System.out.println("****** Successfully click play button!");
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            driver.quit();
        }
        return true;
    }

    private File getCaptchaImage() throws IOException {
        WebElement element = driver.findElement(By.xpath(XPATH_CAPTCHA_IMG));
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        BufferedImage fullImage = ImageIO.read(screenshot);
        Point point = element.getLocation();
        int elementWidth = element.getSize().getWidth();
        int elementHeight = element.getSize().getHeight();
        BufferedImage elementScreenshot = fullImage.getSubimage(point.getX(), point.getY(), elementWidth, elementHeight);
        ImageIO.write(elementScreenshot, "png", screenshot);
        File screenshotLocation = new File("captcha.png");
        FileUtils.copyFile(screenshot, screenshotLocation);
        return screenshotLocation;
    }
}
