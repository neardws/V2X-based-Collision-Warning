package com.linmu.collision_warning_system.services;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.linmu.collision_warning_system.services.work.KeepNCSAliveWork;

import java.util.concurrent.TimeUnit;

public class WorkService {
    private static WorkService INSTANCE;
    public static WorkService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new WorkService();
        }
        return INSTANCE;
    }
    WorkService() {

    }

    public void keepNCSAlive(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                KeepNCSAliveWork.class,
                150,
                TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "keepNCSAlive",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}
