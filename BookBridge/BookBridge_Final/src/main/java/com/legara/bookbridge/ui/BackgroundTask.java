package com.legara.bookbridge.ui;

import com.legara.bookbridge.exception.LibraryException;

@FunctionalInterface
public interface BackgroundTask<T> {
    T run() throws LibraryException;
}
