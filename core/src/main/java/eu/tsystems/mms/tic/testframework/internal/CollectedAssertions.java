/*
 * Testerra
 *
 * (C) 2020,  Peter Lehmann, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
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
 package eu.tsystems.mms.tic.testframework.internal;

import eu.tsystems.mms.tic.testframework.interop.TestEvidenceCollector;
import eu.tsystems.mms.tic.testframework.report.model.AssertionInfo;
import eu.tsystems.mms.tic.testframework.report.model.context.CustomContext;
import eu.tsystems.mms.tic.testframework.report.model.context.MethodContext;
import eu.tsystems.mms.tic.testframework.report.model.context.Screenshot;
import eu.tsystems.mms.tic.testframework.report.utils.ExecutionContextController;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CollectedAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger("AssertCollector");
    private static final ThreadLocal<List<AssertionInfo>> ASSERTION_INFOS = new ThreadLocal<>();

    private CollectedAssertions() {

    }

    public synchronized static void store(Throwable throwable) {
        if (ASSERTION_INFOS.get() == null) {
            ASSERTION_INFOS.set(new LinkedList<>());
        }

        List<AssertionInfo> assertionInfos = ASSERTION_INFOS.get();

        /*
        add info
         */
        AssertionInfo assertionInfo = new AssertionInfo(throwable);

        MethodContext currentMethodContext = ExecutionContextController.getCurrentMethodContext();

        // take scrennshots
        List<Screenshot> screenshots = TestEvidenceCollector.collectScreenshots();
        if (screenshots != null) {
            currentMethodContext.addScreenshots(screenshots.stream());
        }

        // get custom error contexts in queue
        List<CustomContext> customContexts = ExecutionContextController.getCurrentMethodContext().customContexts;
        currentMethodContext.customContexts.addAll(customContexts);
        customContexts.clear();

        // and store
        assertionInfos.add(assertionInfo);
    }

    public static void clear() {
        ASSERTION_INFOS.remove();
    }

    public static boolean hasEntries() {
        if (ASSERTION_INFOS.get() == null) {
            return false;
        }
        if (ASSERTION_INFOS.get().size() > 0) {
            return true;
        }
        return false;
    }

    public static List<AssertionInfo> getEntries() {
        return ASSERTION_INFOS.get();
    }

}
