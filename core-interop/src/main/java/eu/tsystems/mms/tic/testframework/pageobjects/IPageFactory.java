package eu.tsystems.mms.tic.testframework.pageobjects;

import org.openqa.selenium.WebDriver;

public interface IPageFactory {
    <T extends IPage> T create(Class<T> pageClass);
    <T extends IPage> T create(Class<T> pageClass, WebDriver webDriver);
    <T extends IPage> T create(Class<T> pageClass, IGuiElement guiElement);
}
