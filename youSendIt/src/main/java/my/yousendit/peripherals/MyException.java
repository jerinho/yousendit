package my.yousendit.peripherals;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.Context;

public class MyException implements java.lang.Thread.UncaughtExceptionHandler {
	
    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}