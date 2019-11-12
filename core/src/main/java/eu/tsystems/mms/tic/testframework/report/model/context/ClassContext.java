/*
 * (C) Copyright T-Systems Multimedia Solutions GmbH 2018, ..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Peter Lehmann
 *     erku
 *     pele
 */
/*
 * Created on 25.01.2011
 *
 * Copyright(c) 2011 - 2011 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package eu.tsystems.mms.tic.testframework.report.model.context;

import eu.tsystems.mms.tic.testframework.annotations.TesterraClassContext;
import eu.tsystems.mms.tic.testframework.events.TesterraEvent;
import eu.tsystems.mms.tic.testframework.events.TesterraEventDataType;
import eu.tsystems.mms.tic.testframework.events.TesterraEventService;
import eu.tsystems.mms.tic.testframework.events.TesterraEventType;
import eu.tsystems.mms.tic.testframework.exceptions.TesterraSystemException;
import eu.tsystems.mms.tic.testframework.report.FailureCorridor;
import eu.tsystems.mms.tic.testframework.report.TestStatusController;
import eu.tsystems.mms.tic.testframework.report.model.MethodType;
import eu.tsystems.mms.tic.testframework.report.utils.TestNGHelper;
import eu.tsystems.mms.tic.testframework.utils.ArrayUtils;
import org.testng.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds the informations of a test class.
 *
 * @author pele
 */
public class ClassContext extends Context implements SynchronizableContext {

    public final List<MethodContext> methodContexts = new LinkedList<>();
    public String fullClassName;
    public String simpleClassName;
    public final TestContext testContext;
    public final ExecutionContext executionContext;
    public TesterraClassContext testerraClassContext = null;
    public boolean merged = false;
    public ClassContext mergedIntoClassContext = null;

    public ClassContext(TestContext testContext, ExecutionContext executionContext) {
        this.parentContext = this.testContext = testContext;
        this.executionContext = executionContext;
    }

    public MethodContext findTestMethodContainer(String methodName) {
        return getContext(MethodContext.class, methodContexts, methodName, false, null);
    }

    public MethodContext getMethodContext(ITestResult testResult, ITestContext iTestContext, IInvokedMethod invokedMethod) {
        final Object[] parameters = testResult.getParameters();
        final ITestNGMethod testMethod = TestNGHelper.getTestMethod(testResult, iTestContext, invokedMethod);
        return this.getMethodContext(testResult, iTestContext, testMethod, parameters);
    }

    public MethodContext getMethodContext(ITestResult testResult, ITestContext iTestContext, ITestNGMethod iTestNGMethod, Object[] parameters) {
        final String name = iTestNGMethod.getMethodName();

        final List<Object> parametersList = Arrays.stream(parameters).collect(Collectors.toList());

        List<MethodContext> collect;

        synchronized (methodContexts) {
            if (testResult != null) {
                collect = methodContexts.stream()
                        .filter(mc -> testResult == mc.testResult)
                        .collect(Collectors.toList());
            } else {
                // TODO: (!!!!) this is not eindeutig
                collect = methodContexts.stream()
                        .filter(mc -> iTestContext == mc.iTestContext)
                        .filter(mc -> iTestNGMethod == mc.iTestNgMethod)
                        .filter(mc -> mc.parameters.containsAll(parametersList))
                        .collect(Collectors.toList());
            }
        }

        MethodContext methodContext;
        if (collect.isEmpty()) {
                /*
                create new one
                 */
            MethodType methodType;

            final boolean isTest;
            if (iTestNGMethod != null) {
                isTest = iTestNGMethod.isTest();
            } else {
                throw new TesterraSystemException("Error getting method infos, seems like a TestNG bug.\n" + ArrayUtils.join(new Object[]{iTestNGMethod, iTestContext}, "\n"));
            }

            if (isTest) {
                methodType = MethodType.TEST_METHOD;
            } else {
                methodType = MethodType.CONFIGURATION_METHOD;
            }

            TestContext correctTestContext = testContext;
            SuiteContext correctSuiteContext = testContext.suiteContext;
            if (merged) {
                correctSuiteContext = executionContext.getSuiteContext(iTestContext);
                correctTestContext = correctSuiteContext.getTestContext(iTestContext);
            }

            methodContext = new MethodContext(name, methodType, this, correctTestContext, correctSuiteContext, executionContext);
            fillBasicContextValues(methodContext, this, name);

            methodContext.testResult = testResult;
            methodContext.iTestContext = iTestContext;
            methodContext.iTestNgMethod = iTestNGMethod;

                /*
                enhance swi with parameters, set parameters into context
                 */
            if (parameters.length > 0) {
                methodContext.parameters = Arrays.stream(parameters).map(Object::toString).collect(Collectors.toList());
                String swiSuffix = methodContext.parameters.stream().map(Object::toString).collect(Collectors.joining("_"));
                methodContext.swi += "_" + swiSuffix;
            }

                /*
                link to merged context
                 */
            if (merged) {
                synchronized (mergedIntoClassContext.methodContexts) {
                    mergedIntoClassContext.methodContexts.add(methodContext);
                }
            }

                /*
                also check for annotations
                 */
            Method method = iTestNGMethod.getConstructorOrMethod().getMethod();
            if (method.isAnnotationPresent(FailureCorridor.High.class)) {
                methodContext.failureCorridorValue = FailureCorridor.Value.HIGH;
            } else if (method.isAnnotationPresent(FailureCorridor.Mid.class)) {
                methodContext.failureCorridorValue = FailureCorridor.Value.MID;
            } else if (method.isAnnotationPresent(FailureCorridor.Low.class)) {
                methodContext.failureCorridorValue = FailureCorridor.Value.LOW;
            }

            /*
            add to method contexts
             */
            synchronized (methodContexts) {
                methodContexts.add(methodContext);
            }

            // fire context update event: create method context
            TesterraEventService.getInstance().fireEvent(new TesterraEvent(TesterraEventType.CONTEXT_UPDATE)
                    .addData(TesterraEventDataType.CONTEXT, methodContext)
                    .addData(TesterraEventDataType.WITH_PARENT, true));
        } else {
            if (collect.size() > 1) {
                LOGGER.error("INTERNAL ERROR: Found " + collect.size() + " " + MethodContext.class.getSimpleName() + "s with name " + name + ", picking first one");
            }
            methodContext = collect.get(0);
        }
        return methodContext;
    }

    public List<MethodContext> copyOfMethodContexts() {
        synchronized (methodContexts) {
            return new LinkedList<>(methodContexts);
        }
    }

    public MethodContext safeAddSkipMethod(ITestResult testResult, IInvokedMethod invokedMethod) {
        MethodContext methodContext = getMethodContext(testResult, testResult.getTestContext(), invokedMethod);
        methodContext.errorContext().setThrowable(null, new SkipException("Skipped"));
        methodContext.status = TestStatusController.Status.SKIPPED;
        return methodContext;
    }

    @Override
    public TestStatusController.Status getStatus() {
        return getStatusFromContexts(getRepresentationalMethods());
    }

    public Context[] getRepresentationalMethods() {
        List<MethodContext> methodContexts = copyOfMethodContexts();
        Context[] contexts = methodContexts.stream().filter(MethodContext::isRepresentationalTestMethod).toArray(Context[]::new);
        return contexts;
    }

    public Map<TestStatusController.Status, Integer> getMethodStats(boolean includeTestMethods, boolean includeConfigMethods) {
        Map<TestStatusController.Status, Integer> counts = new LinkedHashMap<>();

        // initialize with 0
        Arrays.stream(TestStatusController.Status.values()).forEach(status -> counts.put(status, 0));

        List<MethodContext> methodContexts = copyOfMethodContexts();
        methodContexts.stream().filter(mc -> (includeTestMethods && mc.isTestMethod()) || (includeConfigMethods && mc.isConfigMethod())).forEach(methodContext -> {
            TestStatusController.Status status = methodContext.getStatus();
            int value = 0;
            if (counts.containsKey(status)) {
                value = counts.get(status);
            }

            counts.put(status, value + 1);
        });

        return counts;
    }

    public List<MethodContext> getTestMethodsWithStatus(TestStatusController.Status status) {
        List<MethodContext> methodContexts = new LinkedList<>();
        copyOfMethodContexts().forEach(methodContext -> {
            if (methodContext.isTestMethod() && status == methodContext.status) {
                methodContexts.add(methodContext);
            }
        });
        return methodContexts;
    }

    protected void setExplicitName() {
        name = simpleClassName + "_" + testContext.suiteContext.name + "_" + testContext.name;
    }

}
