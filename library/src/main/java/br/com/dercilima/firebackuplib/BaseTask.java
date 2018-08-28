package br.com.dercilima.firebackuplib;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class BaseTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private WeakReference<Context> context;

    public BaseTask(Context context) {
        this.context = new WeakReference<>(context);
    }

    protected Context getContext() {
        return context.get();
    }

}
