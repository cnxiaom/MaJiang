/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;
import org.apache.mina.util.Queue;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketAcceptor extends BaseIoAcceptor {
    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private final Executor executor;

    private final Object lock = new Object();

    private final int id = nextId++;

    private final String threadName = "SocketAcceptor-" + id;

    private SocketAcceptorConfig defaultConfig = new SocketAcceptorConfig();

    private final Map channels = new HashMap();

    private final Queue registerQueue = new Queue();

    private final Queue cancelQueue = new Queue();

    private final SocketIoProcessor[] ioProcessors;

    private final int processorCount;

    /**
     * @noinspection FieldAccessedSynchronizedAndUnsynchronized
     */
    private Selector selector;

    private Worker worker;

    private int processorDistributor = 0;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    public SocketAcceptor() {
        this(1, new NewThreadExecutor());
    }

    /**
     * Create an acceptor with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor Executor to use for launching threads
     */
    public SocketAcceptor(int processorCount, Executor executor) {
        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        // The default reuseAddress of an accepted socket should be 'true'.
        ((SocketSessionConfig) defaultConfig.getSessionConfig())
                .setReuseAddress(true);

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new SocketIoProcessor(
                    "SocketAcceptorIoProcessor-" + id + "." + i, executor);
        }
    }

    /**
     * Binds to the specified <code>address</code> and handles incoming connections with the specified
     * <code>handler</code>.  Backlog value is configured to the value of <code>backlog</code> property.
     *
     * @throws IOException if failed to bind
     */
    public void bind(SocketAddress address, IoHandler handler,
            IoServiceConfig config) throws IOException {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (address != null && !(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unexpected address type: "
                    + address.getClass());
        }

        if (config == null) {
            config = getDefaultConfig();
        }

        RegistrationRequest request = new RegistrationRequest(address, handler,
                config);

        synchronized (lock) {
            startupWorker();
    
            synchronized (registerQueue) {
                registerQueue.push(request);
            }
    
            selector.wakeup();
        }

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        if (request.exception != null) {
            throw request.exception;
        }
    }
    
    private Selector getSelector() {
        synchronized (lock) {
            return this.selector;
        }
    }

    private void startupWorker() throws IOException {
        synchronized (lock) {
            if (worker == null) {
                selector = Selector.open();
                worker = new Worker();

                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    public void unbind(SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        CancellationRequest request = new CancellationRequest(address);

        try {
            startupWorker();
        } catch (IOException e) {
            // IOException is thrown only when Worker thread is not
            // running and failed to open a selector.  We simply throw
            // IllegalArgumentException here because we can simply
            // conclude that nothing is bound to the selector.
            throw new IllegalArgumentException("Address not bound: " + address);
        }

        synchronized (cancelQueue) {
            cancelQueue.push(request);
        }

        selector.wakeup();

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        if (request.exception != null) {
            request.exception.fillInStackTrace();

            throw request.exception;
        }
    }

    public void unbindAll() {
        List addresses;
        synchronized (channels) {
            addresses = new ArrayList(channels.keySet());
        }

        for (Iterator i = addresses.iterator(); i.hasNext();) {
            unbind((SocketAddress) i.next());
        }
    }

    private class Worker implements Runnable {
        public void run() {
            Selector selector = getSelector();
            for (;;) {
                try {
                    int nKeys = selector.select();

                    registerNew();

                    if (nKeys > 0) {
                        processSessions(selector.selectedKeys());
                    }

                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        synchronized (lock) {
                            if (selector.keys().isEmpty()
                                    && registerQueue.isEmpty()
                                    && cancelQueue.isEmpty()) {
                                worker = null;
                                try {
                                    selector.close();
                                } catch (IOException e) {
                                    ExceptionMonitor.getInstance()
                                            .exceptionCaught(e);
                                } finally {
                                    SocketAcceptor.this.selector = null;
                                }
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }

        private void processSessions(Set keys) throws IOException {
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();

                it.remove();

                if (!key.isAcceptable()) {
                    continue;
                }

                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();

                SocketChannel ch = ssc.accept();

                if (ch == null) {
                    continue;
                }

                boolean success = false;
                try {
                    RegistrationRequest req = (RegistrationRequest) key
                            .attachment();
                    SocketSessionImpl session = new SocketSessionImpl(
                            SocketAcceptor.this, nextProcessor(),
                            getListeners(), req.config, ch, req.handler,
                            req.address);
                    getFilterChainBuilder().buildFilterChain(
                            session.getFilterChain());
                    req.config.getFilterChainBuilder().buildFilterChain(
                            session.getFilterChain());
                    req.config.getThreadModel().buildFilterChain(
                            session.getFilterChain());
                    session.getIoProcessor().addNew(session);
                    success = true;
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);
                } finally {
                    if (!success) {
                        ch.close();
                    }
                }
            }
        }
    }

    private SocketIoProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    public IoServiceConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Sets the config this acceptor will use by default.
     * 
     * @param defaultConfig the default config.
     * @throws NullPointerException if the specified value is <code>null</code>.
     */
    public void setDefaultConfig(SocketAcceptorConfig defaultConfig) {
        if (defaultConfig == null) {
            throw new NullPointerException("defaultConfig");
        }
        this.defaultConfig = defaultConfig;
    }

    private void registerNew() {
        if (registerQueue.isEmpty()) {
            return;
        }

        Selector selector = getSelector();
        for (;;) {
            RegistrationRequest req;

            synchronized (registerQueue) {
                req = (RegistrationRequest) registerQueue.pop();
            }

            if (req == null) {
                break;
            }

            ServerSocketChannel ssc = null;

            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);

                // Configure the server socket,
                SocketAcceptorConfig cfg;
                if (req.config instanceof SocketAcceptorConfig) {
                    cfg = (SocketAcceptorConfig) req.config;
                } else {
                    cfg = (SocketAcceptorConfig) getDefaultConfig();
                }

                ssc.socket().setReuseAddress(cfg.isReuseAddress());
                ssc.socket().setReceiveBufferSize(
                        ((SocketSessionConfig) cfg.getSessionConfig())
                                .getReceiveBufferSize());

                // and bind.
                ssc.socket().bind(req.address, cfg.getBacklog());
                if (req.address == null || req.address.getPort() == 0) {
                    req.address = (InetSocketAddress) ssc.socket()
                            .getLocalSocketAddress();
                }
                ssc.register(selector, SelectionKey.OP_ACCEPT, req);

                synchronized (channels) {
                    channels.put(req.address, ssc);
                }

                getListeners().fireServiceActivated(this, req.address,
                        req.handler, req.config);
            } catch (IOException e) {
                req.exception = e;
            } finally {
                synchronized (req) {
                    req.done = true;

                    req.notifyAll();
                }

                if (ssc != null && req.exception != null) {
                    try {
                        ssc.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }
    }

    private void cancelKeys() {
        if (cancelQueue.isEmpty()) {
            return;
        }

        Selector selector = getSelector();
        for (;;) {
            CancellationRequest request;

            synchronized (cancelQueue) {
                request = (CancellationRequest) cancelQueue.pop();
            }

            if (request == null) {
                break;
            }

            ServerSocketChannel ssc;
            synchronized (channels) {
                ssc = (ServerSocketChannel) channels.remove(request.address);
            }

            // close the channel
            try {
                if (ssc == null) {
                    request.exception = new IllegalArgumentException(
                            "Address not bound: " + request.address);
                } else {
                    SelectionKey key = ssc.keyFor(selector);
                    request.registrationRequest = (RegistrationRequest) key
                            .attachment();
                    key.cancel();

                    selector.wakeup(); // wake up again to trigger thread death

                    ssc.close();
                }
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            } finally {
                synchronized (request) {
                    request.done = true;
                    request.notifyAll();
                }

                if (request.exception == null) {
                    getListeners().fireServiceDeactivated(this,
                            request.address,
                            request.registrationRequest.handler,
                            request.registrationRequest.config);
                }
            }
        }
    }

    private static class RegistrationRequest {
        private InetSocketAddress address;

        private final IoHandler handler;

        private final IoServiceConfig config;

        private IOException exception;

        private boolean done;

        private RegistrationRequest(SocketAddress address, IoHandler handler,
                IoServiceConfig config) {
            this.address = (InetSocketAddress) address;
            this.handler = handler;
            this.config = config;
        }
    }

    private static class CancellationRequest {
        private final SocketAddress address;

        private boolean done;

        private RegistrationRequest registrationRequest;

        private RuntimeException exception;

        private CancellationRequest(SocketAddress address) {
            this.address = address;
        }
    }
}
