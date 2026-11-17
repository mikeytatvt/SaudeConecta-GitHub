package br.edu.unicid.saudeconecta.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Threads do app: o banco nunca é acessado na thread principal. */
public final class AppExecutors {
    private static final ExecutorService DISCO = Executors.newSingleThreadExecutor();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private AppExecutors() { }

    public static ExecutorService disco() {
        return DISCO;
    }

    public static void principal(Runnable tarefa) {
        PRINCIPAL.post(tarefa);
    }
}
