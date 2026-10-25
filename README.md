# Fractal Tree 🌳

A Java program that draws a recursive fractal tree. The branch calculations run **in parallel on a thread pool**, and a single drawing thread puts the results on screen.

The trunk starts black, and the last few levels turn green like leaves.

## How it works

- **Recursion.** Each branch splits into two child branches, at −20° and +20° from the parent's angle, and each child is one step shorter. This continues for 10 levels.
- **Producer–consumer pattern.** The worker threads only *calculate* branch coordinates. They put them into a thread-safe `LinkedBlockingQueue`, and the `paint` method takes branches off the queue one by one and draws them.
- **Thread pool.** One of the two recursive calls is submitted to an `ExecutorService` as a new task, and the other runs directly on the current thread. The pool size is twice the number of CPU cores, capped at 128 threads.
- **Synchronization.** A task counter guarded by a shared lock object tracks how many branches are still unfinished. The main thread `wait()`s on the lock until the counter reaches zero, then shuts the executor down cleanly.

## Running it

You need Java 8 or newer.

```
javac FractalTree.java
java FractalTree
```

**Slow mode** pauses 0.1 seconds at every level, so you can watch the tree being built branch by branch:

```
java FractalTree true
```

## Concepts practised

`ExecutorService` · `BlockingQueue` · `synchronized` · `wait` / `notify` · recursion · Java AWT/Swing drawing

## About

Made as a university assignment on concurrent programming in Java.
