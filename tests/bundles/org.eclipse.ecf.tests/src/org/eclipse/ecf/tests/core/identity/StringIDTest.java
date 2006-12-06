/****************************************************************************
* Copyright (c) 2004 Composent, Inc. and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Composent, Inc. - initial API and implementation
*****************************************************************************/
package org.eclipse.ecf.tests.core.identity;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;

public class StringIDTest extends IDAbstractTestCase {

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.tests.IDTestCase#createID()
	 */
	protected ID createID() throws IDCreateException {
		return createStringID(this.getClass().getName());
	}
	
	protected ID createStringID(String val) throws IDCreateException {
		return IDFactory.getDefault().createStringID(val);
	}
	
	public void testCreate() throws Exception {
		ID newID = createID();
		assertNotNull(newID);
	}

	public void testNullCreate() throws Exception {
		try {
			createStringID(null);
			fail();
		} catch (IDCreateException e) {
			// success
		}
	}
	public void testGetName() throws Exception {
		ID id = createStringID(this.getClass().getName());
		assertTrue(id.getName().equals(this.getClass().getName()));
	}
	
	public void testToExternalForm() throws Exception {
		ID id = createStringID(this.getClass().getName());
		assertNotNull(id.toExternalForm());
	}
	
	public void testToString() throws Exception {
		ID id = createStringID(this.getClass().getName());
		assertNotNull(id.toString());
	}
	
	public void testIsEqual() throws Exception {
		ID id1 = createID();
		ID id2 = createID();
		assertTrue(id1.equals(id2));
	}
	
	public void testHashCode() throws Exception {
		ID id1 = createID();
		ID id2 = createID();
		assertTrue(id1.hashCode() == id2.hashCode());
	}
	
	public void testCompareToEqual() throws Exception {
		ID id1 = createID();
		ID id2 = createID();
		assertTrue(id1.compareTo(id2) == 0);		
		assertTrue(id2.compareTo(id1) == 0);		
	}

	public void testCompareToNotEqual() throws Exception {
		ID id1 = createStringID("abcdefghijkl");
		ID id2 = createStringID("abcdefghijklm");
		assertTrue(id1.compareTo(id2) < 0);
		assertTrue(id2.compareTo(id1) > 0);
	}
	
	public void testGetNamespace() throws Exception {
		ID id = createID();
		Namespace ns = id.getNamespace();
		assertNotNull(ns);
	}
	
	public void testEqualNamespaces() throws Exception {
		ID id1 = createID();
		ID id2 = createID();
		Namespace ns1 = id1.getNamespace();
		Namespace ns2 = id2.getNamespace();
		assertTrue(ns1.equals(ns2));
		assertTrue(ns2.equals(ns2));
	}
	
	public void testSerializable() throws Exception {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(buf);
		try {
			out.writeObject(createID());
		} catch (NotSerializableException ex) {
			fail(ex.getLocalizedMessage());
		} finally {
			out.close();
		}
	}
}
