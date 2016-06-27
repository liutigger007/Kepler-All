package com.kepler.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

import com.kepler.config.PropertiesUtils;

/**
 * @author kim 2015年7月16日
 */
public class ThreadFactory implements FactoryBean<ThreadPoolExecutor> {

	private static final Log LOGGER = LogFactory.getLog(ThreadFactory.class);

	// 最小8个线程
	private static final int THREAD_CORE = Math.min(PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".core", Runtime.getRuntime().availableProcessors() * 2), 8);

	private static final int THREAD_MAX = PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".max", ThreadFactory.THREAD_CORE * 2);

	private static final int THREAD_KEEPALIVE = PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".keepalive", 60000);

	private static final int THREAD_QUEUE = PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".queue", 50);

	/**
	 * 是否使用ShutdownNow
	 */
	private static final boolean SHUTDOWN_WAIT = PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".shutdown_wait", false);

	/**
	 * 扫描线程池是否完毕间隔
	 */
	private static final int SHUTDOWN_INTERVAL = PropertiesUtils.get(ThreadFactory.class.getName().toLowerCase() + ".shutdown_interval", 1000);

	private ThreadPoolExecutor threads;

	@Override
	public ThreadPoolExecutor getObject() throws Exception {
		return this.threads;
	}

	@Override
	public Class<ThreadPoolExecutor> getObjectType() {
		return ThreadPoolExecutor.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * For Spring
	 */
	public void init() {
		this.threads = new ThreadPoolExecutor(ThreadFactory.THREAD_CORE, ThreadFactory.THREAD_MAX, ThreadFactory.THREAD_KEEPALIVE, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(ThreadFactory.THREAD_QUEUE), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * For Spring
	 * 
	 * @throws Exception
	 */
	public void destroy() throws Exception {
		if (ThreadFactory.SHUTDOWN_WAIT) {
			this.shutdown4waiting();
		} else {
			this.shutdown4immediately();
		}
	}

	private void shutdown4waiting() throws Exception {
		this.threads.shutdown();
		this.waitingRunnable(System.currentTimeMillis());
	}

	private void shutdown4immediately() throws Exception {
		for (Runnable each : this.threads.shutdownNow()) {
			ThreadFactory.LOGGER.warn("Shutdown threads, lossing " + each.getClass() + " ... ");
		}
		this.waitingRunnable(System.currentTimeMillis());
	}

	private void waitingRunnable(long timestamp) throws InterruptedException {
		while (!this.threads.awaitTermination(ThreadFactory.SHUTDOWN_INTERVAL, TimeUnit.MILLISECONDS)) {
			ThreadFactory.LOGGER.info("Shutdown threads using " + (TimeUnit.SECONDS.convert(System.currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS)) + "s ...");
		}
	}
}
