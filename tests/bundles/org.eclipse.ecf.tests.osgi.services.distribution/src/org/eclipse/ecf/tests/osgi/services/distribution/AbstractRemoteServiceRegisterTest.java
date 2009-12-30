/*******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.tests.osgi.services.distribution;

import java.util.Properties;

import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.remoteservice.Constants;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceReference;
import org.osgi.framework.ServiceRegistration;

public abstract class AbstractRemoteServiceRegisterTest extends
		AbstractDistributionTest {

	protected static final int REGISTER_WAIT = 2000;

	protected abstract String getServerContainerTypeName();
	
	protected void tearDown() throws Exception {
		super.tearDown();
		IContainer [] containers = getContainerManager().getAllContainers();
		for(int i=0; i < containers.length; i++) {
			containers[i].dispose();
		}
		getContainerManager().removeAllContainers();
		
	}
	
	protected void registerWaitAndUnregister(Properties props, boolean verifyRegistration) throws Exception {
		// Actually register with default service (IConcatService)
		ServiceRegistration registration = registerDefaultService(props);
		// Wait a while
		Thread.sleep(REGISTER_WAIT);
		// Verify
		if (verifyRegistration) verifyRemoteServiceRegisteredWithServer();
		// Then unregister
		registration.unregister();
		Thread.sleep(REGISTER_WAIT);
	}

	private void verifyRemoteServiceRegisteredWithServer() throws Exception {
		verifyRemoteServiceRegistered(getServerContainerAdapter(), getDefaultServiceClasses()[0]);
	}

	protected void verifyRemoteServiceRegistered(IRemoteServiceContainerAdapter adapter, String className) throws Exception {
		IRemoteServiceReference [] refs = adapter.getRemoteServiceReferences((ID[]) null, className, null);
		assertNotNull(refs);
		assertTrue(refs.length > 0);
		String[] objectClasses = (String[]) refs[0].getProperty(Constants.OBJECTCLASS);
		assertTrue(objectClasses != null);
		assertTrue(objectClasses.length > 0);
		assertTrue(objectClasses[0].equals(className));
	}
	
	private IRemoteServiceContainerAdapter getServerContainerAdapter() {
		IContainer [] containers = getContainerManager().getAllContainers();
		String containerType = getServerContainerTypeName();
		for(int i=0; i < containers.length; i++) {
			ContainerTypeDescription ctd = getContainerManager().getContainerTypeDescription(containers[i].getID());
			if (ctd != null && ctd.getName().equals(containerType)) return (IRemoteServiceContainerAdapter) containers[i].getAdapter(IRemoteServiceContainerAdapter.class);
		}
		return null;
	}

	public void testRegisterOnCreatedServer() throws Exception {
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnCreatedServerWithIdentity() throws Exception {
		Properties props = new Properties();
		// Set config to the server container name/provider config name (e.g. ecf.generic.server)
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		// set the container factory arguments to the server identity (e.g. ecftcp://localhost:3282/server)
		props.put(SERVICE_EXPORTED_CONTAINER_FACTORY_ARGUMENTS, new String[] { getServerIdentity() } );
		// Set the service exported interfaces to all
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServer() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServerWithIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intents
		props.put(SERVICE_INTENTS, "passByValue");
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServerWithMissingIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intent that no one actually exposes
		props.put(SERVICE_INTENTS, "foobar");
		registerWaitAndUnregister(props, false);
	}

	public void testRegisterOnExistingServerWithExportedIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intents
		props.put(SERVICE_EXPORTED_INTENTS, "passByValue");
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServerWithMissingExportedIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intent that no one actually exposes
		props.put(SERVICE_EXPORTED_INTENTS, "foobar");
		registerWaitAndUnregister(props, false);
	}

	public void testRegisterOnExistingServerWithExportedExtraIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intents
		props.put(SERVICE_EXPORTED_INTENTS_EXTRA, "passByValue");
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServerWithMissingExportedExtraIntents() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		// Add intent that no one actually exposes
		props.put(SERVICE_EXPORTED_INTENTS_EXTRA, "foobar");
		registerWaitAndUnregister(props, false);
	}

	public void testRegisterOnExistingServerWithContainerID() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),new Object[] {createServerID()});
		
		Properties props = new Properties();
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		props.put(SERVICE_EXPORTED_CONTAINER_ID, createServerID());
		registerWaitAndUnregister(props, true);
	}

	public void testRegisterOnExistingServerWithIdentity() throws Exception {
		// Create server container
		this.server = ContainerFactory.getDefault().createContainer(getServerContainerTypeName(),getServerIdentity());
		
		Properties props = new Properties();
		// Set config to the server container name/provider config name (e.g. ecf.generic.server)
		props.put(SERVICE_EXPORTED_CONFIGS, getServerContainerTypeName());
		// Set the service exported interfaces to all
		props.put(SERVICE_EXPORTED_INTERFACES, new String[] {SERVICE_EXPORTED_INTERFACES_WILDCARD});
		
		registerWaitAndUnregister(props, true);
	}

}