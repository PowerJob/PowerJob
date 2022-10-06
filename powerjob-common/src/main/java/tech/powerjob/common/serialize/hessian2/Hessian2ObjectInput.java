/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.powerjob.common.serialize.hessian2;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Hessian2 object input implementation
 * @author zdyu
 * @since 2022/10/6
 */
public class Hessian2ObjectInput implements Closeable {
    private final Hessian2Input mH2i;
    private final DefaultHessian2FactoryInitializer hessian2FactoryInitializer;

    public Hessian2ObjectInput(InputStream is) {
        mH2i = new Hessian2Input(is);
        hessian2FactoryInitializer = DefaultHessian2FactoryInitializer.getInstance();
        mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
    }

    public Object readObject() throws IOException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return mH2i.readObject();
    }

    public <T> T readObject(Class<T> cls) throws IOException,
            ClassNotFoundException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return (T) mH2i.readObject(cls);
    }

    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        if (!mH2i.getSerializerFactory().getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryInitializer.getSerializerFactory());
        }
        return readObject(cls);
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        mH2i.reset();
    }
}
