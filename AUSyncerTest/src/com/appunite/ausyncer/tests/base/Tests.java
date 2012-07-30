package com.appunite.ausyncer.tests.base;

import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matcher;

public class Tests {

	public static interface ValueRunnable<T> {
		public T getValue() throws Exception;
	};

	public static <T> void assertThatWithTimeout(ValueRunnable<T> runnable,
			Matcher<T> matcher, int timeoutMs, int timeSpanMs)
			throws Exception {
		long endTime = System.currentTimeMillis() + timeoutMs;
		for (;;) {
			T value = runnable.getValue();
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis - endTime >= timeoutMs) {
				assertThat(value, matcher);
				return;
			}
			if (matcher.matches(value))
				return;
			Thread.sleep(timeSpanMs);
		}
	}
	
	public static <T> void assertThatWithTimeout(ValueRunnable<T> runnable,
			Matcher<T> matcher, int timeoutMs) throws Exception
	{
		assertThatWithTimeout(runnable, matcher, timeoutMs, 10);
	}
}
