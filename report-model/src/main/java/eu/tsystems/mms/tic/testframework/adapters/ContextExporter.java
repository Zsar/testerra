package eu.tsystems.mms.tic.testframework.adapters;

import com.google.common.net.MediaType;
import com.google.gson.Gson;
import eu.tsystems.mms.tic.testframework.internal.IDUtils;
import eu.tsystems.mms.tic.testframework.report.TestStatusController;
import eu.tsystems.mms.tic.testframework.report.model.BuildInformation;
import eu.tsystems.mms.tic.testframework.report.model.ClassContext;
import eu.tsystems.mms.tic.testframework.report.model.ContextValues;
import eu.tsystems.mms.tic.testframework.report.model.ErrorContext;
import eu.tsystems.mms.tic.testframework.report.model.ExecStatusType;
import eu.tsystems.mms.tic.testframework.report.model.FailureCorridorValue;
import eu.tsystems.mms.tic.testframework.report.model.File;
import eu.tsystems.mms.tic.testframework.report.model.MethodType;
import eu.tsystems.mms.tic.testframework.report.model.PClickPathEvent;
import eu.tsystems.mms.tic.testframework.report.model.PClickPathEventType;
import eu.tsystems.mms.tic.testframework.report.model.PLogMessage;
import eu.tsystems.mms.tic.testframework.report.model.PLogMessageType;
import eu.tsystems.mms.tic.testframework.report.model.PTestStep;
import eu.tsystems.mms.tic.testframework.report.model.PTestStepAction;
import eu.tsystems.mms.tic.testframework.report.model.ResultStatusType;
import eu.tsystems.mms.tic.testframework.report.model.RunConfig;
import eu.tsystems.mms.tic.testframework.report.model.ScriptSource;
import eu.tsystems.mms.tic.testframework.report.model.ScriptSourceLine;
import eu.tsystems.mms.tic.testframework.report.model.SessionContext;
import eu.tsystems.mms.tic.testframework.report.model.StackTraceCause;
import eu.tsystems.mms.tic.testframework.report.model.SuiteContext;
import eu.tsystems.mms.tic.testframework.report.model.TestContext;
import eu.tsystems.mms.tic.testframework.report.model.context.AbstractContext;
import eu.tsystems.mms.tic.testframework.report.model.context.CustomContext;
import eu.tsystems.mms.tic.testframework.report.model.context.ExecutionContext;
import eu.tsystems.mms.tic.testframework.report.model.context.MethodContext;
import eu.tsystems.mms.tic.testframework.report.model.context.report.Report;
import eu.tsystems.mms.tic.testframework.report.model.steps.TestStep;
import eu.tsystems.mms.tic.testframework.report.model.steps.TestStepAction;
import eu.tsystems.mms.tic.testframework.utils.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import static eu.tsystems.mms.tic.testframework.report.model.SessionContext.newBuilder;

public class ContextExporter {
    private static final Map<TestStatusController.Status, ResultStatusType> STATUS_MAPPING = new LinkedHashMap<>();
    private final Report report = new Report();
    private final Gson jsonEncoder = new Gson();
    private final java.io.File targetVideoDir = report.getFinalReportDirectory(Report.VIDEO_FOLDER_NAME);
    private final java.io.File targetScreenshotDir = report.getFinalReportDirectory(Report.SCREENSHOTS_FOLDER_NAME);
    private final java.io.File currentVideoDir = report.getFinalReportDirectory(Report.VIDEO_FOLDER_NAME);
    private final java.io.File currentScreenshotDir = report.getFinalReportDirectory(Report.SCREENSHOTS_FOLDER_NAME);
    private Map<Integer, StackTraceCause.Builder> uniqueStackTraces = new HashMap();

    private static String annotationToString(Annotation annotation) {
        String json = "\"" + annotation.annotationType().getSimpleName() + "\"";
        json += " : { ";

        Method[] methods = annotation.annotationType().getMethods();
        List<String> params = new LinkedList<>();
        for (Method method : methods) {
            if (method.getDeclaringClass() == annotation.annotationType()) { //this filters out built-in methods, like hashCode etc
                try {
                    params.add("\"" + method.getName() + "\" : \"" + method.invoke(annotation) + "\"");
                } catch (Exception e) {
                    params.add("\"" + method.getName() + "\" : \"---error---\"");
                }
            }
        }
        json += String.join(", ", params);

        json += " }";
        return json;
    }

    private String mapArtifactsPath(String absolutePath) {
        String path = absolutePath.replace(report.getFinalReportDirectory().toString(), "");

        // replace all \ with /
        path = path.replaceAll("\\\\", "/");

        // remove leading /
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    public eu.tsystems.mms.tic.testframework.report.model.MethodContext.Builder prepareMethodContext(eu.tsystems.mms.tic.testframework.report.model.context.MethodContext methodContext) {
        eu.tsystems.mms.tic.testframework.report.model.MethodContext.Builder builder = eu.tsystems.mms.tic.testframework.report.model.MethodContext.newBuilder();

        apply(createContextValues(methodContext), builder::setContextValues);
        map(methodContext.getMethodType(), type -> MethodType.valueOf(type.name()), builder::setMethodType);
        forEach(methodContext.parameters, parameter -> builder.addParameters(parameter.toString()));
        forEach(methodContext.methodTags, annotation -> builder.addMethodTags(annotationToString(annotation)));
        apply(methodContext.retryNumber, builder::setRetryNumber);
        apply(methodContext.methodRunIndex, builder::setMethodRunIndex);

        apply(methodContext.priorityMessage, builder::setPriorityMessage);
        apply(methodContext.threadName, builder::setThreadName);

        // test steps
        methodContext.readTestSteps().forEach(testStep -> builder.addTestSteps(prepareTestStep(testStep)));
        //value(methodContext.failedStep, MethodContextExporter::createPTestStep, builder::setFailedStep);

        map(methodContext.failureCorridorValue, value -> FailureCorridorValue.valueOf(value.name()), builder::setFailureCorridorValue);
        builder.setClassContextId(methodContext.getClassContext().getId());

        forEach(methodContext.infos, builder::addInfos);
        methodContext.readRelatedMethodContexts().forEach(m -> builder.addRelatedMethodContextIds(m.getId()));
        methodContext.readDependsOnMethodContexts().forEach(m -> builder.addDependsOnMethodContextIds(m.getId()));

        // build context
        if (methodContext.hasErrorContext()) builder.setErrorContext(this.prepareErrorContext(methodContext.getErrorContext()));
        methodContext.readSessionContexts().forEach(sessionContext -> builder.addSessionContextIds(sessionContext.getId()));

        List<CustomContext> customContexts = methodContext.readCustomContexts().collect(Collectors.toList());
        if (customContexts.size()>0) {
            builder.setCustomContextJson(jsonEncoder.toJson(customContexts));
        }

        methodContext.readVideos().forEach(video -> {
            final java.io.File targetVideoFile = new java.io.File(targetVideoDir, video.filename);
            final java.io.File currentVideoFile = new java.io.File(currentVideoDir, video.filename);

            final String videoId = IDUtils.getB64encXID();

            // link file
            builder.addVideoIds(videoId);

            // add video data
            final File.Builder fileBuilderVideo = File.newBuilder();
            fileBuilderVideo.setId(videoId);
            fileBuilderVideo.setRelativePath(targetVideoFile.getPath());
            fileBuilderVideo.setMimetype(MediaType.WEBM_VIDEO.toString());
            fillFileBasicData(fileBuilderVideo, currentVideoFile);
            this.addFile(fileBuilderVideo);
        });

        return builder;
    }

    protected void addFile(File.Builder fileBuilder) {

    }

    private void fillFileBasicData(File.Builder builder, java.io.File file) {
        // timestamps
        long timestamp = System.currentTimeMillis();
        builder.setCreatedTimestamp(timestamp);
        builder.setLastModified(timestamp);

        // file size
        builder.setSize(file.length());
    }
//
//    public StackTrace.Builder prepareStackTrace(eu.tsystems.mms.tic.testframework.report.model.context.StackTrace stackTrace) {
//        StackTrace.Builder builder = StackTrace.newBuilder();
//
//        //apply(stackTrace.additionalErrorMessage, builder::setAdditionalErrorMessage);
//        map(stackTrace.stackTrace, this::prepareStackTraceCause, builder::setCause);
//
//        return builder;
//    }

    public ScriptSource.Builder prepareScriptSource(eu.tsystems.mms.tic.testframework.report.model.context.ScriptSource scriptSource) {
        ScriptSource.Builder builder = ScriptSource.newBuilder();

        apply(scriptSource.fileName, builder::setFileName);
        apply(scriptSource.methodName, builder::setMethodName);
        forEach(scriptSource.lines, line -> builder.addLines(prepareScriptSourceLine(line)));

        return builder;
    }

    public ScriptSourceLine.Builder prepareScriptSourceLine(eu.tsystems.mms.tic.testframework.report.model.context.ScriptSource.Line line) {
        ScriptSourceLine.Builder builder = ScriptSourceLine.newBuilder();

        apply(line.line, builder::setLine);
        apply(line.lineNumber, builder::setLineNumber);
        apply(line.mark, builder::setMark);

        return builder;
    }

    public ErrorContext.Builder prepareErrorContext(eu.tsystems.mms.tic.testframework.report.model.context.ErrorContext errorContext) {
        ErrorContext.Builder builder = ErrorContext.newBuilder();

//        apply(errorContext.getReadableErrorMessage(), builder::setReadableErrorMessage);
//        apply(errorContext.getAdditionalErrorMessage(), builder::setAdditionalErrorMessage);
        if (errorContext.getThrowable() != null) {
            StackTraceCause.Builder stackTraceCause = this.prepareStackTraceCause(errorContext.getThrowable());
            builder.setCauseId(stackTraceCause.getId());
        }
//        apply(errorContext.errorFingerprint, builder::setErrorFingerprint);
        errorContext.getScriptSource().ifPresent(scriptSource -> builder.setScriptSource(this.prepareScriptSource(scriptSource)));
        errorContext.getExecutionObjectSource().ifPresent(scriptSource -> builder.setExecutionObjectSource(this.prepareScriptSource(scriptSource)));
        if (errorContext.getTicketId() != null) builder.setTicketId(errorContext.getTicketId().toString());
        apply(errorContext.getDescription(), builder::setDescription);

        return builder;
    }

    public PTestStep.Builder prepareTestStep(TestStep testStep) {
        PTestStep.Builder builder = PTestStep.newBuilder();

        apply(testStep.getName(), builder::setName);
        forEach(testStep.getTestStepActions(), testStepAction -> builder.addTestStepActions(prepareTestStepAction(testStepAction)));

        return builder;
    }

    public PTestStepAction.Builder prepareTestStepAction(TestStepAction testStepAction) {
        PTestStepAction.Builder testStepBuilder = PTestStepAction.newBuilder();

        apply(testStepAction.getName(), testStepBuilder::setName);
        apply(testStepAction.getTimestamp(), testStepBuilder::setTimestamp);

        testStepAction.readClickPathEvents().forEach(clickPathEvent -> {
            PClickPathEvent.Builder clickPathBuilder = PClickPathEvent.newBuilder();
            switch (clickPathEvent.getType()) {
                case WINDOW:
                    clickPathBuilder.setType(PClickPathEventType.WINDOW);
                    break;
                case CLICK:
                    clickPathBuilder.setType(PClickPathEventType.CLICK);
                    break;
                case VALUE:
                    clickPathBuilder.setType(PClickPathEventType.VALUE);
                    break;
                case PAGE:
                    clickPathBuilder.setType(PClickPathEventType.PAGE);
                    break;
                case URL:
                    clickPathBuilder.setType(PClickPathEventType.URL);
                    break;
                default:
                    clickPathBuilder.setType(PClickPathEventType.NOT_SET);
            }
            clickPathBuilder.setSubject(clickPathEvent.getSubject());
            clickPathBuilder.setSessionId(clickPathEvent.getSessionId());
            testStepBuilder.addClickpathEvents(clickPathBuilder.build());
        });

        testStepAction.readScreenshots().forEach(screenshot -> {
            // build screenshot and sources files
            final java.io.File targetScreenshotFile = new java.io.File(targetScreenshotDir, screenshot.filename);
            final java.io.File currentScreenshotFile = new java.io.File(currentScreenshotDir, screenshot.filename);

            //final java.io.File realSourceFile = new java.io.File(Report.SCREENSHOTS_DIRECTORY, screenshot.sourceFilename);
            final java.io.File targetSourceFile = new java.io.File(targetScreenshotDir, screenshot.filename);
            final java.io.File currentSourceFile = new java.io.File(currentScreenshotDir, screenshot.filename);
            final String mappedSourcePath = mapArtifactsPath(targetSourceFile.getAbsolutePath());

            final String screenshotId = IDUtils.getB64encXID();
            final String sourcesRefId = IDUtils.getB64encXID();

            // create ref link
            //builder.addScreenshotIds(screenshotId);

            // add screenshot data
            final File.Builder fileBuilderScreenshot = File.newBuilder();
            fileBuilderScreenshot.setId(screenshotId);
            fileBuilderScreenshot.setRelativePath(targetScreenshotFile.getPath());
            fileBuilderScreenshot.setMimetype(MediaType.PNG.toString());
            fileBuilderScreenshot.putAllMeta(screenshot.meta());
            fileBuilderScreenshot.putMeta("sourcesRefId", sourcesRefId);
            fillFileBasicData(fileBuilderScreenshot, currentScreenshotFile);
            this.addFile(fileBuilderScreenshot);

            // add sources data
            final File.Builder fileBuilderSources = File.newBuilder();
            fileBuilderSources.setId(sourcesRefId);
            fileBuilderSources.setRelativePath(mappedSourcePath);
            fileBuilderSources.setMimetype(MediaType.PLAIN_TEXT_UTF_8.toString());
            fillFileBasicData(fileBuilderSources, currentSourceFile);

            this.addFile(fileBuilderSources);

            testStepBuilder.addScreenshotIds(screenshotId);
        });

        testStepAction.readLogEvents().forEach(logEvent -> {
            testStepBuilder.addLogMessages(prepareLogEvent(logEvent));
        });

        testStepAction.readOptionalAssertions().forEach(assertionInfo -> {
            testStepBuilder.addOptionalAssertions(prepareErrorContext(assertionInfo));
        });

        testStepAction.readCollectedAssertions().forEach(assertionInfo -> {
            testStepBuilder.addCollectedAssertions(prepareErrorContext(assertionInfo));
        });

        return testStepBuilder;
    }

    public ContextExporter() {
        for (TestStatusController.Status status : TestStatusController.Status.values()) {
            /*
            Status
             */
            ResultStatusType resultStatusType = ResultStatusType.valueOf(status.name());

            // add to map
            STATUS_MAPPING.put(status, resultStatusType);
        }
    }

    ResultStatusType getMappedStatus(TestStatusController.Status status) {
        return STATUS_MAPPING.get(status);
    }

    /**
     * Applies a value if not null
     */
    protected <T, R> void apply(T value, Function<T, R> function) {
        if (value != null) {
            function.apply(value);
        }
    }

    /**
     * Maps and applies a value if not null
     */
    protected<T, M, R> void map(T value, Function<T, M> map, Function<M, R> function) {
        if (value != null) {
            M m = map.apply(value);
            function.apply(m);
        }
    }

    /**
     * Iterates if not null
     */
    protected <T extends Iterable<C>, C> void forEach(T value, Consumer<C> function) {
        if (value != null) {
            value.forEach(function);
        }
    }

    protected ContextValues createContextValues(AbstractContext context) {
        ContextValues.Builder builder = ContextValues.newBuilder();

        apply(context.getId(), builder::setId);
        //apply(context.swi, builder::setSwi);
        apply(System.currentTimeMillis(), builder::setCreated);
        apply(context.getName(), builder::setName);
        map(context.getStartTime(), Date::getTime, builder::setStartTime);
        map(context.getEndTime(), Date::getTime, builder::setEndTime);

        if (context instanceof MethodContext) {
            MethodContext methodContext = (MethodContext) context;

            // result status
            map(methodContext.status, this::getMappedStatus, builder::setResultStatus);

            // exec status
            if (methodContext.status == TestStatusController.Status.NO_RUN) {
                builder.setExecStatus(ExecStatusType.RUNNING);
            } else {
                builder.setExecStatus(ExecStatusType.FINISHED);
            }
        } else if (context instanceof ExecutionContext) {
            ExecutionContext executionContext = (ExecutionContext) context;
            if (executionContext.crashed) {
                /*
                crashed state
                 */
                builder.setExecStatus(ExecStatusType.CRASHED);
            } else {
                if (TestStatusController.getTestsSkipped() == executionContext.estimatedTestMethodCount) {
                    builder.setResultStatus(ResultStatusType.SKIPPED);
                    builder.setExecStatus(ExecStatusType.VOID);
                } else if (TestStatusController.getTestsFailed() + TestStatusController.getTestsSuccessful() == 0) {
                    builder.setResultStatus(ResultStatusType.NO_RUN);
                    builder.setExecStatus(ExecStatusType.VOID);

                } else {
                    ResultStatusType resultStatusType = STATUS_MAPPING.get(executionContext.getStatus());
                    builder.setResultStatus(resultStatusType);

                    // exec status
                    if (executionContext.getEndTime() != null) {
                        builder.setExecStatus(ExecStatusType.FINISHED);
                    } else {
                        builder.setExecStatus(ExecStatusType.RUNNING);
                    }
                }
            }
        }
        return builder.build();
    }

    protected PLogMessage.Builder prepareLogEvent(LogEvent logEvent) {
        PLogMessage.Builder plogMessageBuilder = PLogMessage.newBuilder();
        plogMessageBuilder.setLoggerName(logEvent.getLoggerName());
        plogMessageBuilder.setMessage(logEvent.getMessage().getFormattedMessage());
        if (logEvent.getLevel() == Level.ERROR) {
            plogMessageBuilder.setType(PLogMessageType.LMT_ERROR);
        } else if (logEvent.getLevel() == Level.WARN) {
            plogMessageBuilder.setType(PLogMessageType.LMT_WARN);
        } else if (logEvent.getLevel() == Level.INFO) {
            plogMessageBuilder.setType(PLogMessageType.LMT_INFO);
        } else if (logEvent.getLevel() == Level.DEBUG) {
            plogMessageBuilder.setType(PLogMessageType.LMT_DEBUG);
        }
        plogMessageBuilder.setTimestamp(logEvent.getTimeMillis());
        plogMessageBuilder.setThreadName(logEvent.getThreadName());

        if (logEvent.getThrown() != null) {
            StackTraceCause.Builder stackTraceCause = prepareStackTraceCause(logEvent.getThrown());
            plogMessageBuilder.setCauseId(stackTraceCause.getId());
        }

        return plogMessageBuilder;
    }

    protected StackTraceCause.Builder prepareStackTraceCause(Throwable throwable) {
//        StackTraceElement stackTraceElement = throwable.getStackTrace()[0];
//        String uniqueKey = throwable.getMessage()+stackTraceElement.getFileName()+stackTraceElement.getLineNumber();
        uniqueStackTraces.putIfAbsent(throwable.hashCode(), this._prepareStackTraceCause(throwable));
        return uniqueStackTraces.get(throwable.hashCode());
    }

    private StackTraceCause.Builder _prepareStackTraceCause(Throwable throwable) {
        StackTraceCause.Builder builder = StackTraceCause.newBuilder();
        builder.setId(Integer.toString(throwable.hashCode()));
        apply(throwable.getClass().getName(), builder::setClassName);
        apply(throwable.getMessage(), builder::setMessage);
        builder.addAllStackTraceElements(Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toList()));

        if ((throwable.getCause() != null) && (throwable.getCause() != throwable)) {
            StackTraceCause.Builder stackTraceCause = this.prepareStackTraceCause(throwable.getCause());
            builder.setCauseId(stackTraceCause.getId());
        }
        return builder;
    }

    public ClassContext.Builder prepareClassContext(eu.tsystems.mms.tic.testframework.report.model.context.ClassContext classContext) {
        ClassContext.Builder builder = ClassContext.newBuilder();

        apply(createContextValues(classContext), builder::setContextValues);
        apply(classContext.getTestClass().getName(), builder::setFullClassName);
        apply(classContext.getTestContext().getId(), builder::setTestContextId);

        if (classContext.getTestClassContext() != null) {
            builder.setTestContextName(classContext.getTestClassContext().name());
        }
        return builder;
    }

    public eu.tsystems.mms.tic.testframework.report.model.ExecutionContext.Builder prepareExecutionContext(eu.tsystems.mms.tic.testframework.report.model.context.ExecutionContext executionContext) {
        eu.tsystems.mms.tic.testframework.report.model.ExecutionContext.Builder builder = eu.tsystems.mms.tic.testframework.report.model.ExecutionContext.newBuilder();

        apply(createContextValues(executionContext), builder::setContextValues);
//        forEach(executionContext.suiteContexts, suiteContext -> builder.addSuiteContextIds(suiteContext.getId()));
//        forEach(executionContext.mergedClassContexts, classContext -> builder.addMergedClassContextIds(classContext.id));
//        map(executionContext.exitPoints, this::createContextClip, builder::addAllExitPoints);
//        map(executionContext.failureAspects, this::createContextClip, builder::addAllFailureAscpects);
        map(executionContext.runConfig, this::prepareRunConfig, builder::setRunConfig);
        executionContext.readExclusiveSessionContexts().forEach(sessionContext -> builder.addExclusiveSessionContextIds(sessionContext.getId()));
        apply(executionContext.estimatedTestMethodCount, builder::setEstimatedTestsCount);
        executionContext.readMethodContextLessLogs().forEach(logEvent -> builder.addLogMessages(prepareLogEvent(logEvent)));

        this.uniqueStackTraces.values().forEach(stackTraceCauseBuilder -> {
            builder.putCauses(stackTraceCauseBuilder.getId(), stackTraceCauseBuilder.build());
        });
        return builder;
    }
//
//    private List<ContextClip> createContextClip(Map<String, List<eu.tsystems.mms.tic.testframework.report.model.context.MethodContext>> values) {
//        List<ContextClip> out = new LinkedList<>();
//        values.forEach((key, list) -> {
//            ContextClip.Builder builder = ContextClip.newBuilder();
//            builder.setKey(key);
//            list.forEach(methodContext -> builder.addMethodContextIds(methodContext.id));
//            ContextClip contextClip = builder.build();
//            out.add(contextClip);
//        });
//        return out;
//    }

    public RunConfig.Builder prepareRunConfig(eu.tsystems.mms.tic.testframework.report.model.context.RunConfig runConfig) {
        RunConfig.Builder builder = RunConfig.newBuilder();

        apply(runConfig.getReportName(), builder::setReportName);
        apply(runConfig.RUNCFG, builder::setRuncfg);

        /*
        add build information
         */
        BuildInformation.Builder bi = BuildInformation.newBuilder();
        apply(runConfig.buildInformation.buildJavaVersion, bi::setBuildJavaVersion);
        //apply(runConfig.buildInformation.buildOsArch, bi::setBuildOsName);
        apply(runConfig.buildInformation.buildOsName, bi::setBuildOsName);
        apply(runConfig.buildInformation.buildOsVersion, bi::setBuildOsVersion);
        apply(runConfig.buildInformation.buildTimestamp, bi::setBuildTimestamp);
        apply(runConfig.buildInformation.buildUserName, bi::setBuildUserName);
        apply(runConfig.buildInformation.buildVersion, bi::setBuildVersion);
        builder.setBuildInformation(bi);

        return builder;
    }

    public SessionContext.Builder prepareSessionContext(eu.tsystems.mms.tic.testframework.report.model.context.SessionContext sessionContext) {
        SessionContext.Builder builder = newBuilder();

        apply(createContextValues(sessionContext), builder::setContextValues);
        apply(sessionContext.getSessionKey(), builder::setSessionKey);
        apply(sessionContext.getProvider(), builder::setProvider);
        apply(sessionContext.getSessionId(), builder::setSessionId);

        // translate object map to string map
        Map<String, String> newMap = new LinkedHashMap<>();
        for (String key : sessionContext.getMetaData().keySet()) {
            Object value = sessionContext.getMetaData().get(key);
            if (StringUtils.isStringEmpty(key) || value == null || StringUtils.isStringEmpty(value.toString())) {
                // ignore
            } else {
                newMap.put(key, value.toString());
            }
        }
        builder.putAllMetadata(newMap);

        return builder;
    }

    public SuiteContext.Builder prepareSuiteContext(eu.tsystems.mms.tic.testframework.report.model.context.SuiteContext suiteContext) {
        SuiteContext.Builder builder = SuiteContext.newBuilder();

        apply(createContextValues(suiteContext), builder::setContextValues);
        builder.setExecutionContextId(suiteContext.getExecutionContext().getId());

        return builder;
    }

    public TestContext.Builder prepareTestContext(eu.tsystems.mms.tic.testframework.report.model.context.TestContext testContext) {
        TestContext.Builder builder = TestContext.newBuilder();

        apply(createContextValues(testContext), builder::setContextValues);
        builder.setSuiteContextId(testContext.getSuiteContext().getId());

        return builder;
    }
}
