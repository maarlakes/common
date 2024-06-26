package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public final class Futures {
    private Futures() {
    }

    public static <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getMessage(), e.getCause());
        } catch (CancellationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> toCompletableFuture(@Nonnull Future<T> future) {
        if (future.isDone()) {
            return transformDoneFuture(future);
        }
        if (future instanceof CompletableFuture) {
            return (CompletableFuture<T>) future;
        }
        if (future instanceof CompletionStage) {
            return ((CompletionStage<T>) future).toCompletableFuture();
        }
        final CompletableFuture<T> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                if (!future.isDone()) {
                    awaitFuture(future);
                }
                return future.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
        completableFuture.whenComplete((r, e) -> {
            if (completableFuture.isCancelled()) {
                future.cancel(true);
            }
        });
        return completableFuture;
    }

    private static <T> void awaitFuture(@Nonnull Future<T> future) throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            @Override
            public boolean block() throws InterruptedException {
                if (!this.isReleasable()) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true;
            }

            @Override
            public boolean isReleasable() {
                return future.isDone();
            }
        });
    }

    private static <T> CompletableFuture<T> transformDoneFuture(@Nonnull Future<T> future) {
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        if (future.isCancelled()) {
            completableFuture.cancel(true);
        } else {
            try {
                completableFuture.complete(future.get());
            } catch (Throwable error) {
                completableFuture.completeExceptionally(error);
            }
        }

        return completableFuture;
    }
}
