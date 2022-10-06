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

import com.alibaba.com.caucho.hessian.io.Hessian2Output;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Hessian2 object output implementation
 * @author zdyu
 * @since 2022/10/6
 */
public class Hessian2ObjectOutput  implements Closeable {

    private final Hessian2Output mH2o;

    public Hessian2ObjectOutput(OutputStream os) {
        mH2o = new Hessian2Output(os);
        mH2o.setSerializerFactory(DefaultHessian2FactoryInitializer.getInstance().getSerializerFactory());
    }

    public void writeObject(Object obj) throws IOException {
        mH2o.writeObject(obj);
    }
    public void flushBuffer() throws IOException {
        mH2o.flushBuffer();
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
        mH2o.reset();
    }
}
