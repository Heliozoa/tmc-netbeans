package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;
import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.RunResult.Status;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.ui.TestResultWindow;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * Waits for test and validation results and shows the result view only when
 * both have become available.
 */
public final class ResultCollector {

    private final Exercise exercise;
    private ImmutableList<TestResult> testCaseResults;
    private ValidationResult validationResults;

    private boolean testCaseResultsSet = false;
    private boolean validationResultsSet = false;
    private boolean dontWaitForValidations = false; // For e.g showing compilation errors.

    private boolean isReturnable;
    private Runnable submissionCallback;

    public ResultCollector(Exercise exercise) {
        this.exercise = exercise;
    }

    public void setValidationResult(final ValidationResult result) {

        this.validationResults = result;
        this.validationResultsSet = true;

        showResultsIfReady();
    }

    public void setTestCaseResults(final ImmutableList<TestResult> results) {

        this.testCaseResults = results;
        this.testCaseResultsSet = true;

        showResultsIfReady();
    }

    public void setLocalTestResults(RunResult runResult) {

        if (runResult.status == Status.COMPILE_FAILED || runResult.status == Status.TESTRUN_INTERRUPTED) {

            String STDOUT = "stdout";
            String STDERR = "stderr";
            List<String> log = new ArrayList<String>();
            Map<String,byte[]> logs = runResult.logs;

            if (logs.containsKey(STDOUT)) {
                final String str1 = new String(logs.get(STDOUT), Charset.forName("utf-8"));
                log.addAll(Arrays.asList(str1.split("\\r?\\n")));
            }
            if (logs.containsKey(STDERR)) {
                final String str2 = new String(logs.get(STDERR), Charset.forName("utf-8"));
                log.addAll(Arrays.asList(str2.split("\\r?\\n")));
            }

            log = tryToCleanLog(log);

            String errorType;
            if (runResult.status == Status.COMPILE_FAILED) {
                errorType = "Compilation failed";
            } else {
                errorType = "Testrun interrupted";
            }

            TestResult buildFailed = new TestResult(errorType, false, ImmutableList.<String>of(), errorType, ImmutableList.copyOf(log));

            setTestCaseResults(ImmutableList.of(buildFailed));
            dontWaitForValidations = true;
        } else {
            setTestCaseResults(runResult.testResults);
        }
    }
    
    private synchronized void showResultsIfReady() {

        boolean ready = dontWaitForValidations || (testCaseResultsSet && validationResultsSet);
        if (ready) {
            final boolean submittable = isSubmittable();
            SwingUtilities.invokeLater(() -> {
                TestResultWindow.get().showResults(exercise, testCaseResults, validationResults, submissionCallback, submittable);
            });
        }
    }

    public void setSubmissionCallback(final Runnable submissionCallback) {

        this.submissionCallback = submissionCallback;
    }

    public void setReturnable(final boolean returnable) {

        isReturnable = returnable;
    }

    private boolean isSubmittable() {
        if (exercise.hasDeadlinePassed()) {
            return false;
        }

        for (TestResult result : testCaseResults) {
            if (!result.isSuccessful()) {
                return false;
            }
        }

        if (validationResults == null || validationResults.getStrategy() != Strategy.FAIL) {
            return isReturnable;
        }

        return validationResults.getValidationErrors().isEmpty() && isReturnable;

    }

    private List<String> tryToCleanLog(List<String> log) {
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).contains("-do-compile:")) {
                return log.subList(i, log.size());
            }
        }
        return log;
    }

    
}
