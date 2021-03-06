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
package org.apache.mina.common.support;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.IdentityHashSet;

/**
 * A helper which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupport {
    /**
     * A list of {@link IoServiceListener}s.
     */
    private final List listeners = new ArrayList();

    /**
     * Tracks managed <tt>serviceAddress</tt>es.
     */
    private final Set managedServiceAddresses = new HashSet();

    /**
     * Tracks managed sesssions with <tt>serviceAddress</tt> as a key.
     */
    private final Map managedSessions = new HashMap();

    /**
     * Creates a new instance.
     */
    public IoServiceListenerSupport() {
    }

    /**
     * Adds a new listener.
     */
    public void add(IoServiceListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an existing listener.
     */
    public void remove(IoServiceListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public Set getManagedServiceAddresses() {
        return Collections.unmodifiableSet(managedServiceAddresses);
    }

    public boolean isManaged(SocketAddress serviceAddress) {
        synchronized (managedServiceAddresses) {
            return managedServiceAddresses.contains(serviceAddress);
        }
    }

    public Set getManagedSessions(SocketAddress serviceAddress) {
        Set sessions;
        synchronized (managedSessions) {
            sessions = (Set) managedSessions.get(serviceAddress);
            if (sessions == null) {
                sessions = new IdentityHashSet();
            }
        }

        synchronized (sessions) {
            return new IdentityHashSet(sessions);
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService, SocketAddress, IoHandler, IoServiceConfig)}
     * for all registered listeners.
     */
    public void fireServiceActivated(IoService service,
            SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config) {
        synchronized (managedServiceAddresses) {
            if (!managedServiceAddresses.add(serviceAddress)) {
                return;
            }
        }

        synchronized (listeners) {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                try {
                    ((IoServiceListener) i.next()).serviceActivated(service,
                            serviceAddress, handler, config);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService, SocketAddress, IoHandler, IoServiceConfig)}
     * for all registered listeners.
     */
    public synchronized void fireServiceDeactivated(IoService service,
            SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config) {
        synchronized (managedServiceAddresses) {
            if (!managedServiceAddresses.remove(serviceAddress)) {
                return;
            }
        }

        try {
            synchronized (listeners) {
                for (Iterator i = listeners.iterator(); i.hasNext();) {
                    try {
                        ((IoServiceListener) i.next()).serviceDeactivated(service,
                                serviceAddress, handler, config);
                    } catch (Throwable e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        } finally {
            disconnectSessions(serviceAddress, config);
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     */
    public void fireSessionCreated(IoSession session) {
        SocketAddress serviceAddress = session.getServiceAddress();

        // Get the session set.
        boolean firstSession = false;
        Set sessions;
        synchronized (managedSessions) {
            sessions = (Set) managedSessions.get(serviceAddress);
            if (sessions == null) {
                sessions = new IdentityHashSet();
                managedSessions.put(serviceAddress, sessions);
                firstSession = true;
            }

            // If already registered, ignore.
            synchronized (sessions) {
                if (!sessions.add(session)) {
                    return;
                }
            }
        }

        // If the first connector session, fire a virtual service activation event.
        if (session.getService() instanceof IoConnector && firstSession) {
            fireServiceActivated(session.getService(), session
                    .getServiceAddress(), session.getHandler(), session
                    .getServiceConfig());
        }

        // Fire session events.
        session.getFilterChain().fireSessionCreated(session);
        session.getFilterChain().fireSessionOpened(session);

        // Fire listener events.
        synchronized (listeners) {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                try {
                    ((IoServiceListener) i.next()).sessionCreated(session);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     */
    public void fireSessionDestroyed(IoSession session) {
        SocketAddress serviceAddress = session.getServiceAddress();

        // Get the session set.
        Set sessions;
        boolean lastSession = false;
        synchronized (managedSessions) {
            sessions = (Set) managedSessions.get(serviceAddress);
            // Ignore if unknown.
            if (sessions == null) {
                return;
            }

            // Try to remove the remaining empty session set after removal.
            synchronized (sessions) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    managedSessions.remove(serviceAddress);
                    lastSession = true;
                }
            }
        }

        // Fire session events.
        session.getFilterChain().fireSessionClosed(session);

        // Fire listener events.
        try {
            synchronized (listeners) {
                for (Iterator i = listeners.iterator(); i.hasNext();) {
                    try {
                        ((IoServiceListener) i.next()).sessionDestroyed(session);
                    } catch (Throwable e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        } finally {
            // Fire a virtual service deactivation event for the last session of the connector.
            //TODO double-check that this is *STILL* the last session. May not be the case
            if (session.getService() instanceof IoConnector && lastSession) {
                fireServiceDeactivated(session.getService(), session
                        .getServiceAddress(), session.getHandler(), session
                        .getServiceConfig());
            }
        }
    }

    private void disconnectSessions(SocketAddress serviceAddress,
            IoServiceConfig config) {
        if (!(config instanceof IoAcceptorConfig)) {
            return;
        }

        if (!((IoAcceptorConfig) config).isDisconnectOnUnbind()) {
            return;
        }

        Set sessions;
        synchronized (managedSessions) {
            sessions = (Set) managedSessions.get(serviceAddress);
        }

        if (sessions == null) {
            return;
        }

        final Object lock = new Object();
        Set sessionsCopy;

        // Create a copy to avoid ConcurrentModificationException
        synchronized (sessions) {
            sessionsCopy = new IdentityHashSet(sessions);
        }

        for (Iterator i = sessionsCopy.iterator(); i.hasNext();) {
            ((IoSession) i.next()).close().addListener(new IoFutureListener() {
                public void operationComplete(IoFuture future) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
        }

        try {
            synchronized (lock) {
                while (!managedSessions.isEmpty()) {
                    lock.wait(500);
                }
            }
        } catch (InterruptedException ie) {
            // Ignored
        }
    }
}
