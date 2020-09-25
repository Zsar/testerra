/*
 * Testerra
 *
 * (C) 2020, Mike Reiche, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
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
 */

package eu.tsystems.mms.tic.testframework.test.pageobjects;

import eu.tsystems.mms.tic.testframework.common.Testerra;
import eu.tsystems.mms.tic.testframework.logging.Loggable;
import eu.tsystems.mms.tic.testframework.pageobjects.DefaultElementLabelProvider;
import eu.tsystems.mms.tic.testframework.pageobjects.ElementLabelProvider;
import eu.tsystems.mms.tic.testframework.testing.TesterraTest;
import eu.tsystems.mms.tic.testframework.utils.Formatter;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

public class DefaultElementLabelProviderTest extends TesterraTest implements Loggable {

    private ElementLabelProvider locator = new DefaultElementLabelProvider();
    private Formatter formatter = Testerra.injector.getInstance(Formatter.class);

    @Test
    public void test_button() {
        By[] bys = locator.createBy("button", "Ich stimme zu");
        for (By by : bys) {
            log().info(by.toString());
        }
    }

}
