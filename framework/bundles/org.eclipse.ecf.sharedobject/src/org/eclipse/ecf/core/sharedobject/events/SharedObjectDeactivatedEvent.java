/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.core.sharedobject.events;

import org.eclipse.ecf.core.identity.ID;

public class SharedObjectDeactivatedEvent implements ISharedObjectDeactivatedEvent {
	private ID deactivatedID;

	private ID localContainerID;

	/**
	 * @since 2.6
	 */
	public SharedObjectDeactivatedEvent() {

	}

	public SharedObjectDeactivatedEvent(ID container, ID deact) {
		super();
		this.localContainerID = container;
		this.deactivatedID = deact;
	}

	public ID getDeactivatedID() {
		return deactivatedID;
	}

	public ID getLocalContainerID() {
		return localContainerID;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("SharedObjectDeactivatedEvent["); //$NON-NLS-1$
		sb.append(getLocalContainerID()).append(";"); //$NON-NLS-1$
		sb.append(getDeactivatedID()).append("]"); //$NON-NLS-1$
		return sb.toString();
	}
}