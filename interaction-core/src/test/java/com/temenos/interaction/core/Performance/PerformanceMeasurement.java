package com.temenos.interaction.core.Performance;

import java.util.concurrent.TimeUnit;

/**
 * @author kwieconkowski
 */

public class PerformanceMeasurement {
    public static String getExecutionTimeSummary(Runnable method, boolean printToScreen) {
        long execution_time_method = getExecutionTime(method);
        String timeOfExecution = String.format("Time of execution: %02d min, %02d sec",
                TimeUnit.MILLISECONDS.toMinutes(execution_time_method),
                TimeUnit.MILLISECONDS.toSeconds(execution_time_method)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(execution_time_method)));
        if (printToScreen) {
            System.out.println(timeOfExecution);
        }
        return timeOfExecution;
    }

    public static long getExecutionTimeInSeconds(Runnable method, boolean printToScreen) {
        long execution_time_method = TimeUnit.MILLISECONDS.toSeconds(getExecutionTime(method));
        if (printToScreen) {
            System.out.println(String.format("Time of execution: %04d sec", execution_time_method));
        }
        return execution_time_method;
    }

    public static int comparePerformance(Runnable method_1, Runnable method_2, boolean printToScreen) {
        long execution_time_method_1 = getExecutionTime(method_1);
        long execution_time_method_2 = getExecutionTime(method_2);
        int compareResult = Long.compare(execution_time_method_1, execution_time_method_2);
        if (printToScreen) {
            String msg;
            if (compareResult == 0) {
                msg = "Both methods have similar performance.";
            } else if (compareResult == -1) {
                msg = "Method_1 is faster.";
            } else {
                msg = "Method_2 is faster.";
            }
            System.out.println(String.format(msg + " Method_1: %02d sec, Method_2: %02d sec",
                    TimeUnit.MILLISECONDS.toSeconds(execution_time_method_1),
                    TimeUnit.MILLISECONDS.toSeconds(execution_time_method_2)));
        }
        return compareResult;
    }

    private static long getExecutionTime(Runnable method) {
        long prev_time = System.nanoTime();
        method.run();
        return convertFromNanoToMilli(System.nanoTime()) - convertFromNanoToMilli(prev_time);
    }

    private static long convertFromNanoToMilli(long nanoTime) {
        return nanoTime / 1000000;
    }
}
