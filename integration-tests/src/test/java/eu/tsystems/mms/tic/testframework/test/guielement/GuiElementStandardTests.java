/*
 * Testerra
 *
 * (C) 2020, Eric Kubenka, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package eu.tsystems.mms.tic.testframework.test.guielement;

import com.google.inject.Inject;
import eu.tsystems.mms.tic.testframework.ioc.DriverUi_Desktop;
import eu.tsystems.mms.tic.testframework.pageobjects.GuiElement;
import eu.tsystems.mms.tic.testframework.pageobjects.Locator;
import eu.tsystems.mms.tic.testframework.webdrivermanager.DesktopWebDriverUtils;
import org.testng.annotations.Guice;

@Guice(modules = DriverUi_Desktop.class)
public class GuiElementStandardTests extends AbstractGuiElementNonFunctionalAssertionTest {
    @Inject
    public GuiElementStandardTests(final DesktopWebDriverUtils desktopWebDriverUtils) {
        super(desktopWebDriverUtils);
    }

    @Override
    @Deprecated
    public GuiElement getGuiElementBy(Locator locator) {
        return (GuiElement) UI_ELEMENT_FINDER_FACTORY.create(getWebDriver()).find(locator);
    }
}
