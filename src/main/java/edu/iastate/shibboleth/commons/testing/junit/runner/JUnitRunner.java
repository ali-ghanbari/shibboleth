package edu.iastate.shibboleth.commons.testing.junit.runner;

/*-
 * #%L
 * shibboleth-maven-plugin
 * %%
 * Copyright (C) 2021 - 2022 Iowa State University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.iastate.shibboleth.constants.Params;
import edu.iastate.shibboleth.commons.testing.LexTestComparator;
import edu.iastate.shibboleth.commons.testing.TestComparator;
import edu.iastate.shibboleth.commons.testing.junit.JUnitUtils;
import org.pitest.functional.predicate.Predicate;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A versatile JUnit runner based on PIT!
 *
 * @author Ali Ghanbari (alig@istate.edu)
 */
public class JUnitRunner {
    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    }

    private List<TestUnit> testUnits;

    private final ResultCollector resultCollector;

    public JUnitRunner(final List<TestUnit> testUnits,
                       final ResultCollector resultCollector) {
        this.testUnits = testUnits;
        this.resultCollector = resultCollector;
        testUnits.sort(new LexTestComparator());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final TestComparator testComparator,
                       final ResultCollector resultCollector) {
        final List<TestUnit> testUnits = JUnitUtils.discoverTestUnits(classNames);
        testUnits.sort(testComparator);
        this.testUnits = testUnits;
        this.resultCollector = resultCollector;
    }

    public JUnitRunner(final Collection<String> classNames,
                       final ResultCollector resultCollector) {
        this(classNames, new LexTestComparator(), resultCollector);
    }

    public JUnitRunner(final Collection<String> classNames) {
        this(classNames, new LexTestComparator());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final TestComparator testComparator) {
        this(classNames, testComparator, new DefaultResultCollector());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final Predicate<String> failingTestFilter) {
        this(classNames, failingTestFilter, new LexTestComparator());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final Predicate<String> failingTestFilter,
                       final TestComparator testComparator) {
        final List<TestUnit> testUnits = JUnitUtils.discoverTestUnits(classNames);
        testUnits.sort(testComparator);
        this.testUnits = testUnits;
        this.resultCollector = new EarlyExitResultCollector(new DefaultResultCollector(), failingTestFilter);
    }

    public List<TestUnit> getTestUnits() {
        return this.testUnits;
    }

    public void setTestUnits(List<TestUnit> testUnits) {
        this.testUnits = testUnits;
    }

    public boolean run() {
        return run(TestUnitFilter.all());
    }

    public boolean run(final Predicate<TestUnit> shouldRun) {
        for (final TestUnit testUnit : this.testUnits) {
            if (!shouldRun.apply(testUnit)) {
                continue;
            }
            final Runnable task = () -> testUnit.execute(JUnitRunner.this.resultCollector);
            try {
                EXECUTOR_SERVICE.submit(task).get(Params.TEST_UNIT_TIME_OUT, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                System.out.println("WARNING: Running test case is terminated due to TIME_OUT.");
                return false;
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("WARNING: Running test case is terminated.");
                return false;
            }
            if (this.resultCollector.shouldExit()) {
                System.out.println("WARNING: Running test cases is terminated.");
                return false;
            }
        }
        return true;
    }
}
