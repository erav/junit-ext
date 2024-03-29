package com.googlecode.junit.ext;

import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.internal.runners.TestMethod;
import org.junit.internal.runners.MethodRoadie;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.Failure;
import org.junit.runner.Description;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;

import com.googlecode.junit.ext.checkers.Checker;

public class JunitExtRunner extends JUnit4ClassRunner {
    private boolean shouldRunTest4Class;

    public JunitExtRunner(Class<?> klass) throws InitializationError {
        super(klass);
        shouldRunTest4Class = isPrerequisiteSatisfiedForClass(klass);
    }

    protected void invokeTestMethod(Method method, RunNotifier notifier) {
        if (shouldRunTest4Class && isPrereuisitSatisfied(method)) {
            Description description = methodDescription(method);
            Object test = createTest(notifier, description);
            if (test == null) {
                return;
            }
            List<Precondition> list = createPrecondtions(method, test);
            List<Exception> possibleExceptions = new ArrayList<Exception>();
            int failedAt = invokeSetupForPreconditions(list, possibleExceptions);
            try {
                if (arePreconditionsSetUpSucceed(failedAt)) {
                    TestMethod testMethod = wrapMethod(method);
                    new MethodRoadie(test, testMethod, notifier, description).run();
                } else {
                    notifier.fireTestStarted(description);
                    notifier.fireTestStarted(description);
                }
            } finally {
                failedAt = arePreconditionsSetUpSucceed(failedAt) ? list.size() : failedAt;
                for (int i = 0; i < failedAt; i++) {
                    try {
                        Precondition precondition = list.get(i);
                        precondition.teardown();
                    } catch (Exception e) {
                        possibleExceptions.add(e);
                    }
                }
            }
            if (!possibleExceptions.isEmpty()) {
                for (Exception e : possibleExceptions) {
                    notifier.fireTestFailure(new Failure(description, e));
                }
            }
        } else {
            Description testDescription = Description.createTestDescription(this.getTestClass().getJavaClass(),
                    method.getName());
            notifier.fireTestIgnored(testDescription);
        }
    }

    private boolean arePreconditionsSetUpSucceed(int failedAt) {
        return failedAt == -1;
    }

    private int invokeSetupForPreconditions(List<Precondition> list, List<Exception> possibleExceptions) {
        int failedAt = -1;
        int currentIndex = 0;
        for (Precondition precondition : list) {
            currentIndex++;
            try {
                precondition.setup();
            } catch (Exception e) {
                possibleExceptions.add(e);
                failedAt = currentIndex;
                break;
            }
        }
        return failedAt;
    }

    private List<Precondition> createPrecondtions(Method method, Object test) {
        Class<?> declaringClass = method.getDeclaringClass();
        Field[] declaredFields = declaringClass.getDeclaredFields();
        Object context = null;
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(Context.class)) {
                try {
                    declaredField.setAccessible(true);
                    context = declaredField.get(test);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Preconditions preconditions = method.getAnnotation(Preconditions.class);
        ArrayList<Precondition> preconditionsAsList = new ArrayList<Precondition>();
        if (preconditions == null) {
            return preconditionsAsList;
        }
        Class<? extends Precondition>[] classes = preconditions.value();
        for (Class<? extends Precondition> aClass : classes) {
            Precondition precondition;
            try {
                if (context != null) {
                    Constructor<? extends Precondition> constructor = aClass.getConstructor(Object.class);
                    precondition = constructor.newInstance(context);
                } else {
                    precondition = aClass.newInstance();
                }
                preconditionsAsList.add(precondition);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return preconditionsAsList;
    }

    public boolean isPrereuisitSatisfied(Method method) {
        RunIf resource = method.getAnnotation(RunIf.class);
        if (resource == null) {
            return true;
        }
        Class<? extends Checker> prerequisiteChecker = resource.value();
        try {
            Checker checker = instantiateChecker(resource, prerequisiteChecker);
            return checker.satisfy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isArgumentNotProvided(String[] argument) {
        return argument == null || argument.length == 0;
    }

    public Object createTest(RunNotifier notifier, Description description) {
        Object test = null;
        try {
            try {
                test = createTest();
            } catch (InvocationTargetException e) {
                testAborted(notifier, description, e.getCause());
                return null;
            } catch (Exception e) {
                testAborted(notifier, description, e);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return test;
    }


    private void testAborted(RunNotifier notifier, Description description,
                             Throwable e) {
        notifier.fireTestStarted(description);
        notifier.fireTestFailure(new Failure(description, e));
        notifier.fireTestFinished(description);
    }

    public boolean isPrerequisiteSatisfiedForClass(Class<?> klass) {
        RunIf resource = klass.getAnnotation(RunIf.class);
        if (resource == null) {
            return true;

        }
        Class<? extends Checker> prerequisiteChecker = resource.value();
        try {
            Checker checker = instantiateChecker(resource, prerequisiteChecker);
            return checker.satisfy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Checker instantiateChecker(RunIf resource, Class<? extends Checker> prerequisiteChecker) throws Exception {
        String[] arguments = resource.arguments();
        Checker checker;
        if (isArgumentNotProvided(arguments)) {
            checker = prerequisiteChecker.newInstance();
        } else {
            if (arguments.length == 1) {
                Constructor<? extends Checker> constructor = prerequisiteChecker.getConstructor(String.class);
                checker = constructor.newInstance(arguments[0]);
            } else {
                Constructor<? extends Checker> constructor = prerequisiteChecker.getConstructor(String[].class);
                checker = constructor.newInstance(new Object[]{arguments});
            }
        }
        return checker;
    }

}