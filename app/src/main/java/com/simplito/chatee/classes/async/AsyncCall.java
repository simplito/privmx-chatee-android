//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.simplito.chatee.classes.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class AsyncCall<T> implements Promise<T> {
    private final CompletableFuture<T> future;
    private Exception exception;

    private final List<Pair<Executor, Callback<T>>> successCallbacks = new ArrayList<>();
    private final List<Pair<Executor, Callback<Exception>>> errorCallbacks = new ArrayList<>();

    public AsyncCall(Callable<T> callable) {
        this(PrivmxThreadPoolExecutor.currentPool(), callable);
    }

    public AsyncCall(
            Executor executor,
            Callable<T> callable
    ) {
        this.future = CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, executor);

        this.future.whenComplete((result, exception) -> {
            if (exception == null) {
                for (Pair<Executor, Callback<T>> pair : this.successCallbacks) {
                    if (pair.first != null) {
                        pair.first.execute(() -> pair.second.call(result));
                    } else {
                        pair.second.call(result);
                    }
                }
            } else {
                this.exception = (Exception) exception.getCause();
                for (Pair<Executor, Callback<Exception>> pair : this.errorCallbacks) {
                    if (pair.first != null) {
                        pair.first.execute(() -> pair.second.call(this.exception));
                    } else {
                        pair.second.call(this.exception);
                    }
                }
            }
        });
    }

    public Promise<T> then(Callback<T> callback) {
        return then(null, callback);
    }

    @Override
    public Promise<T> then(Executor executor, Callback<T> callback) {
        if (callback == null) return this;
        this.successCallbacks.add(new Pair<>(executor, callback));
        if (this.future.isDone()) {
            try {
                callback.call(future.get());
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public Promise<T> fail(Callback<Exception> callback) {
        return fail(null, callback);
    }

    @Override
    public Promise<T> fail(Executor executor, Callback<Exception> callback) {
        if (callback == null) return this;
        this.errorCallbacks.add(new Pair<>(executor, callback));
        if (this.future.isDone() && this.exception != null) {
            callback.call(this.exception);
        }
        return this;
    }

    private static class Pair<T, U> {
        public T first;
        public U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }
}