package cn.hmz.mypassword.model;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

/**
 * 单任务线程，先进先出执行任务
 */
public abstract class AsyncSingleTask<D> {
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static final Handler handler = new Handler(Looper.getMainLooper());

	private AsyncResult<D> asyncResult;
	private boolean isRunned = false;
	private int delay = 0;

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public synchronized void execute() {
		if (isRunned)
			throw new RuntimeException("该任务已经运行过，不能再次调用");

		isRunned = true;
		executorService.execute(backgroundRunable);
	}

	private Runnable backgroundRunable = new Runnable() {
		@Override
		public void run() {
			asyncResult = doInBackground(new AsyncResult<D>());
			handler.postDelayed(mainThreadRunable, delay);
		}
	};

	private Runnable mainThreadRunable = new Runnable() {
		@Override
		public void run() {
			runOnUIThread(asyncResult);
		}
	};

	/**
	 * 在后台执行
	 */
	protected abstract AsyncResult<D> doInBackground(AsyncResult<D> asyncResult);

	/**
	 * 在UI线程执行
	 * 
	 * @param asyncResult
	 */
	protected void runOnUIThread(AsyncResult<D> asyncResult){}
}
