/*******************************************************************************
 * Copyright (c) 2009 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.osgi.services.discovery;

import org.eclipse.ecf.discovery.IServiceInfo;
import org.osgi.framework.ServiceReference;

public interface IHostDiscoveryListener {

	public void publish(ServiceReference publicationServiceReference,
			IServiceInfo serviceInfo);

	public void unpublish(ServiceReference publicationServiceReference,
			IServiceInfo serviceInfo);

}
