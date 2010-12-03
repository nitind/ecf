/*******************************************************************************
 * Copyright (c) 2007 Versant Corp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Kuppe (mkuppe <at> versant <dot> com) - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.provider.discovery;

import java.util.HashSet;
import java.util.Properties;
import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.discovery.IDiscoveryAdvertiser;
import org.eclipse.ecf.discovery.IDiscoveryLocator;
import org.eclipse.ecf.discovery.service.IDiscoveryService;
import org.eclipse.ecf.provider.discovery.CompositeDiscoveryContainer;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	// The shared instance
	private static Activator plugin;
	public static final String PLUGIN_ID = "org.eclipse.ecf.provider.discovery"; //$NON-NLS-1$

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
		final Properties props = new Properties();
		props.put(IDiscoveryLocator.CONTAINER_NAME, CompositeDiscoveryContainer.NAME);
		props.put(Constants.SERVICE_RANKING, new Integer(1000));
		String[] clazzes = new String[] {IDiscoveryService.class.getName(), IDiscoveryLocator.class.getName(), IDiscoveryAdvertiser.class.getName()};
		context.registerService(clazzes, new ServiceFactory() {

			/* (non-Javadoc)
			 * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
			 */
			public Object getService(final Bundle bundle, final ServiceRegistration registration) {

				// register the composite discovery service)
				final CompositeDiscoveryContainer cdc;
				cdc = new CompositeDiscoveryContainer(new HashSet());
				try {
					cdc.connect(null, null);
				} catch (final ContainerConnectException e) {
					Trace.catching(Activator.PLUGIN_ID, Activator.PLUGIN_ID + "/debug/methods/catching", this.getClass(), "getService(Bundle, ServiceRegistration)", e); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}

				Filter filter = null;
				// add a service listener to add/remove IDS dynamically 
				try {
					final String filter2 = "(&(" + Constants.OBJECTCLASS + "=" + IDiscoveryAdvertiser.class.getName() + ")(!(" + IDiscoveryLocator.CONTAINER_NAME + "=" + CompositeDiscoveryContainer.NAME + ")))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					filter = context.createFilter(filter2);
					context.addServiceListener(new ServiceListener() {
						/* (non-Javadoc)
						 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
						 */
						public void serviceChanged(final ServiceEvent arg0) {
							final IDiscoveryService anIDS = (IDiscoveryService) context.getService(arg0.getServiceReference());
							switch (arg0.getType()) {
								case ServiceEvent.REGISTERED :
									cdc.addContainer(anIDS);
									break;
								case ServiceEvent.UNREGISTERING :
									cdc.removeContainer(anIDS);
									break;
								default :
									break;
							}
						}

					}, filter2);
				} catch (final InvalidSyntaxException e) {
					// nop
				}

				// get all previously registered IDS from OSGi (but not this one)
				final ServiceTracker tracker = new ServiceTracker(context, filter, null);
				tracker.open();
				final Object[] services = tracker.getServices();
				tracker.close();
				if (services != null) {
					for (int i = 0; i < services.length; i++) {
						final Object obj = services[i];
						if (obj != cdc)
							cdc.addContainer(obj);
					}
				}

				return cdc;
			}

			/* (non-Javadoc)
			 * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
			 */
			public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service) {
				((CompositeDiscoveryContainer) service).dispose();
			}
		}, props);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
	}
}
