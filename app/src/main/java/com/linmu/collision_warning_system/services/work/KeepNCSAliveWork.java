package com.linmu.collision_warning_system.services.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.linmu.collision_warning_system.services.NcsLocationService;



public class KeepNCSAliveWork extends Worker {
    public KeepNCSAliveWork(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        NcsLocationService.getInstance().keepNcsAlive();
        Log.i("MyLogTag", "execute: 保持NCS激活！");
        return Result.success();
    }
}
