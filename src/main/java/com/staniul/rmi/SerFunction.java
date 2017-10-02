package com.staniul.rmi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Function;

interface SerFunction<T, R> extends Function<T, R>, Serializable { }