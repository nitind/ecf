/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.xmpp.container;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.callback.Callback;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.ContainerInstantiationException;
import org.eclipse.ecf.core.SharedObjectAddException;
import org.eclipse.ecf.core.comm.AsynchConnectionEvent;
import org.eclipse.ecf.core.comm.ConnectionInstantiationException;
import org.eclipse.ecf.core.comm.ISynchAsynchConnection;
import org.eclipse.ecf.core.events.ContainerConnectedEvent;
import org.eclipse.ecf.core.events.ContainerDisconnectedEvent;
import org.eclipse.ecf.core.events.ContainerDisconnectingEvent;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.IDInstantiationException;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.security.ObjectCallback;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.core.util.Event;
import org.eclipse.ecf.core.util.IQueueEnqueue;
import org.eclipse.ecf.filetransfer.FileTransferInfo;
import org.eclipse.ecf.filetransfer.IFileTransferInfo;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IIncomingFileTransferRequestListener;
import org.eclipse.ecf.filetransfer.IOutgoingFileTransferContainer;
import org.eclipse.ecf.filetransfer.OutgoingFileTransferException;
import org.eclipse.ecf.filetransfer.events.IFileTransferEvent;
import org.eclipse.ecf.presence.IAccountManager;
import org.eclipse.ecf.presence.IMessageListener;
import org.eclipse.ecf.presence.IMessageSender;
import org.eclipse.ecf.presence.IPresence;
import org.eclipse.ecf.presence.IPresenceContainer;
import org.eclipse.ecf.presence.IPresenceListener;
import org.eclipse.ecf.presence.IPresenceSender;
import org.eclipse.ecf.presence.ISubscribeListener;
import org.eclipse.ecf.presence.IMessageListener.Type;
import org.eclipse.ecf.presence.chat.IChatRoomContainer;
import org.eclipse.ecf.presence.chat.IChatRoomManager;
import org.eclipse.ecf.presence.chat.IRoomInfo;
import org.eclipse.ecf.provider.generic.ClientSOContainer;
import org.eclipse.ecf.provider.generic.ContainerMessage;
import org.eclipse.ecf.provider.generic.SOConfig;
import org.eclipse.ecf.provider.generic.SOContainerConfig;
import org.eclipse.ecf.provider.generic.SOContext;
import org.eclipse.ecf.provider.generic.SOWrapper;
import org.eclipse.ecf.provider.xmpp.Trace;
import org.eclipse.ecf.provider.xmpp.XmppPlugin;
import org.eclipse.ecf.provider.xmpp.events.IQEvent;
import org.eclipse.ecf.provider.xmpp.events.MessageEvent;
import org.eclipse.ecf.provider.xmpp.events.PresenceEvent;
import org.eclipse.ecf.provider.xmpp.filetransfer.XMPPOutgoingFileTransfer;
import org.eclipse.ecf.provider.xmpp.identity.XMPPID;
import org.eclipse.ecf.provider.xmpp.identity.XMPPRoomID;
import org.eclipse.ecf.provider.xmpp.smack.ECFConnection;
import org.eclipse.ecf.provider.xmpp.smack.ECFConnectionObjectPacketEvent;
import org.eclipse.ecf.provider.xmpp.smack.ECFConnectionPacketEvent;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.MUCUser;

public class XMPPClientSOContainer extends ClientSOContainer implements IOutgoingFileTransferContainer {

	public static final int DEFAULT_KEEPALIVE = 30000;
	Trace trace = Trace.create("XMPPClientSOContainer");
	
	public static final String NAMESPACE_IDENTIFIER = XmppPlugin.getDefault().getNamespaceIdentifier();
	public static final String XMPP_SHARED_OBJECT_ID = XMPPClientSOContainer.class
			.getName()
			+ ".xmpphandler";
	
	protected static final String GOOGLE_SERVICENAME = "gmail.com";
	
	int keepAlive = 0;
	protected IIMMessageSender messageSender = null;
	protected XMPPPresenceSharedObject sharedObject = null;
	protected ID sharedObjectID = null;
	Vector chats = new Vector();
	
	protected void addChat(IChatRoomContainer container) {
		chats.add(container);
	}
	protected void removeChat(IChatRoomContainer container) {
		chats.remove(container);
	}
	protected void trace(String msg) {
		if (trace != null) {
			trace.msg(msg);
		}
	}
	protected void dumpStack(Throwable t, String msg) {
		if (trace != null) {
			trace.dumpStack(t,msg);
		}
	}
	protected XMPPClientSOContainer(SOContainerConfig config, int keepAlive) throws Exception {
		super(config);
		this.keepAlive = keepAlive;
		this.sharedObjectID = IDFactory.getDefault().createStringID(XMPP_SHARED_OBJECT_ID);
		sharedObject = new XMPPPresenceSharedObject();
	}
	public XMPPClientSOContainer() throws Exception {
		this(DEFAULT_KEEPALIVE);
	}
	public XMPPClientSOContainer(int ka) throws Exception {
		this(new SOContainerConfig(IDFactory.getDefault().createGUID()),ka);
	}

	public XMPPClientSOContainer(String userhost, int ka) throws Exception {
		this(new SOContainerConfig(IDFactory.getDefault().createStringID(userhost)),ka);
	}
	protected void disposeChats() {
		for(Iterator i=chats.iterator(); i.hasNext(); ) {
			IChatRoomContainer cc = (IChatRoomContainer) i.next();
			cc.dispose();
		}
		chats.clear();
	}
	protected IChatRoomContainer findChat(ID toID) {
		if (toID == null) return null;
		XMPPRoomID roomID = null;
		if (toID instanceof XMPPRoomID) {
			roomID = (XMPPRoomID) toID;
			String mucname = roomID.getMucString();
			for(Iterator i=chats.iterator(); i.hasNext(); ) {
				IChatRoomContainer cont = (IChatRoomContainer) i.next();
				if (cont == null) continue;
				ID tid = cont.getConnectedID();
				if (tid != null && tid instanceof XMPPRoomID) {
					XMPPRoomID targetID = (XMPPRoomID) tid;
					String tmuc = targetID.getMucString();
					if (tmuc.equals(mucname)) {
						return cont;
					}
				}
			}
		}
		return null;
	}
	protected ID handleConnectResponse(ID originalTarget, Object serverData)
			throws Exception {
		if (originalTarget != null && !originalTarget.equals(getID())) {
			addNewRemoteMember(originalTarget, null);
			// notify listeners
			fireContainerEvent(new ContainerConnectedEvent(this
					.getID(), originalTarget));
		}
		// If we've got the connection then pass it onto shared object also
		ECFConnection conn = (ECFConnection) getConnection();
		if (conn != null && sharedObject != null) {
			sharedObject.setConnection(conn.getXMPPConnection());
		}
		return originalTarget;
	}

	protected void addSharedObjectToContainer(ID remote)
			throws SharedObjectAddException {
		getSharedObjectManager().addSharedObject(sharedObjectID, sharedObject,
				new HashMap());
	}

	public void dispose() {
		if (sharedObject != null) {
			getSharedObjectManager().removeSharedObject(sharedObjectID);
		}
		sharedObjectID = null;
		sharedObject = null;
		messageSender = null;
		disposeChats();
		super.dispose();
	}

	protected ISynchAsynchConnection createConnection(ID remoteSpace,
			Object data) throws ConnectionInstantiationException {
		ISynchAsynchConnection conn = null;
		boolean google = false;
		if (remoteSpace instanceof XMPPID) {
			XMPPID theID = (XMPPID) remoteSpace;
			String host = theID.getHostname();
			if (host.toLowerCase().equals(GOOGLE_SERVICENAME)) {
				google = true;
			}
		}
		conn = new ECFConnection(google,getConnectNamespace(),receiver);
		Object res = conn.getAdapter(IIMMessageSender.class);
		if (res != null) {
			// got it
			messageSender = (IIMMessageSender) res;
		}
		return conn;
	}

	protected Object createConnectData(ID target, Callback [] cbs, Object data) {
		// first one is password callback
		if (cbs.length > 0) {
			if (cbs[0] instanceof ObjectCallback) {
				ObjectCallback cb = (ObjectCallback) cbs[0];
				return cb.getObject();
			}
		}
		return data;
	}

	protected Callback[] createAuthorizationCallbacks() {
		Callback [] cbs = new Callback[1];
		cbs[0] = new ObjectCallback();
		return cbs;
	}


	protected int getConnectTimeout() {
		return keepAlive;
	}

	public Roster getRoster() throws IOException {
		if (messageSender != null) {
			return messageSender.getRoster();
		} else
			return null;
	}
	
	public Namespace getConnectNamespace() {
		return IDFactory.getDefault().getNamespaceByName(XmppPlugin.getDefault().getNamespaceIdentifier());
	}
	
	protected void deliverEvent(Event evt) {
		SOWrapper wrap = getSharedObjectWrapper(sharedObjectID);
		if (wrap != null) wrap.deliverEvent(evt);
		else trace("deliverEvent("+evt+") wrapper object is unavailable");
	}
	
	protected void handleXMPPMessage(Packet aPacket) throws IOException {
		if (!handleAsExtension(aPacket)) {
			if (aPacket instanceof IQ) {
				deliverEvent(new IQEvent((IQ) aPacket));
			} else if (aPacket instanceof Message) {
				deliverEvent(new MessageEvent((Message) aPacket));
			} else if (aPacket instanceof Presence) {
				deliverEvent(new PresenceEvent((Presence) aPacket));
			} else {
				// unexpected message
				debug("got unexpected packet " + aPacket.toXML());
			}
		}
	}

	protected boolean handleAsExtension(Packet packet) {
		// XXX this is where extension mechanism needs to be added
        Iterator i = packet.getExtensions();
        for(; i.hasNext(); ) {
        	Object extension = i.next();
        	trace("XMPPClientSOContainer.handleAsExtension(ext="+extension+",packet="+packet.toXML()+")");
            if (packet instanceof Presence && extension instanceof MUCUser) {
            	trace("XMPPClientSOContainer.handleAsExtension: received presence for MUCUser");
            	return true;
            }
        }
		return false;
	}

	public void connect(ID remote, IConnectContext joinContext)
			throws ContainerConnectException {
		try {
			addSharedObjectToContainer(remote);
			super.connect(remote, joinContext);
		} catch (ContainerConnectException e) {
			dispose();
			throw e;
		} catch (SharedObjectAddException e1) {
			dispose();
			throw new ContainerConnectException(
					"Exception adding shared object " + sharedObjectID, e1);
		}
	}

	public void disconnect() {
		ID groupID = getConnectedID();
		fireContainerEvent(new ContainerDisconnectingEvent(this
				.getID(), groupID));
		synchronized (getConnectLock()) {
			// If we are currently connected
			if (isConnected()) {
				ISynchAsynchConnection conn = getConnection();
				synchronized (conn) {
					synchronized (getGroupMembershipLock()) {
						memberLeave(groupID, null);
					}
					try {
						conn.disconnect();
					} catch (IOException e) {
						dumpStack("Exception disconnecting", e);
					}
				}
			}
			connectionState = DISCONNECTED;
			this.connection = null;
			remoteServerID = null;
		}
		// notify listeners
		fireContainerEvent(new ContainerDisconnectedEvent(this.getID(),
				groupID));
		dispose();
	}

	protected SOContext createSharedObjectContext(SOConfig soconfig,
			IQueueEnqueue queue) {
		return new XMPPContainerContext(soconfig.getSharedObjectID(), soconfig
				.getHomeContainerID(), this, soconfig.getProperties(), queue);
	}

	protected void processAsynch(AsynchConnectionEvent e) {
		try {
			if (e instanceof ECFConnectionPacketEvent) {
				// It's a regular xmpp message
				handleXMPPMessage((Packet) e.getData());
				return;
			} else if (e instanceof ECFConnectionObjectPacketEvent) {
				// It's an ECF object message
				ECFConnectionObjectPacketEvent evt = (ECFConnectionObjectPacketEvent) e;
				Object obj = evt.getObjectValue();
				// this should be a ContainerMessage
				Object cm = deserializeContainerMessage((byte[]) obj);
                if (cm == null) throw new IOException("deserialized object is null");
				ContainerMessage contMessage = (ContainerMessage) cm;
				IChatRoomContainer chat = findChat(contMessage.getToContainerID());
				if (chat != null && chat instanceof XMPPGroupChatSOContainer) {
					XMPPGroupChatSOContainer cont = (XMPPGroupChatSOContainer) chat;
					cont.handleContainerMessage(contMessage);
					return;
				}
				Object data = contMessage.getData();
				if (data instanceof ContainerMessage.CreateMessage) {
					handleCreateMessage(contMessage);
				} else if (data instanceof ContainerMessage.CreateResponseMessage) {
					handleCreateResponseMessage(contMessage);
				} else if (data instanceof ContainerMessage.SharedObjectMessage) {
					handleSharedObjectMessage(contMessage);
				} else if (data instanceof ContainerMessage.SharedObjectDisposeMessage) {
					handleSharedObjectDisposeMessage(contMessage);
				} else {
					debug("got unrecognized container message...ignoring: " + contMessage);
				}
			} else {
				// Unexpected type...
				debug("got unexpected event: " + e);
			}
		} catch (Exception except) {
            System.err.println("Exception in processAsynch");
            except.printStackTrace(System.err);
			dumpStack("Exception processing event " + e, except);
		}
	}

	public void sendMessage(ID target, String message) throws IOException {
		if (messageSender != null) {
			messageSender.sendMessage(target, message);
		}
	}
    
	protected Presence createPresenceFromIPresence(IPresence presence) {
		return sharedObject.createPresence(presence);
	}
	protected void sendPresenceUpdate(ID target, Presence presence) throws IOException {
		if (messageSender != null) {
			if (presence == null) throw new NullPointerException("presence cannot be null");
			messageSender.sendPresenceUpdate(target, presence);
		}
	}

	protected void sendRosterAdd(String user, String name, String [] groups) throws IOException {
		if (messageSender != null) {
			messageSender.sendRosterAdd(user,name,groups);
		}
	}
	protected void sendRosterRemove(String user) throws IOException {
		if (messageSender != null) {
			messageSender.sendRosterRemove(user);
		}
	}

    public Object getAdapter(Class clazz) {
		if (clazz.equals(IPresenceContainer.class)) {
            return new IPresenceContainer() {
            	
            	public Object getAdapter(Class clazz) {
            		return null;
            	}
                public void addPresenceListener(IPresenceListener listener) {
                    sharedObject.addPresenceListener(listener);
                }

                public void addMessageListener(IMessageListener listener) {
                    sharedObject.addMessageListener(listener);
                }
                public IMessageSender getMessageSender() {
                    return new IMessageSender() {

                        public void sendMessage(ID fromID, ID toID, Type type, String subject, String message) {
                            try {
                                XMPPClientSOContainer.this.sendMessage(toID,message);
                            } catch (IOException e) {
                                dumpStack("Exception in sendmessage to "+toID+" with message "+message,e);
                            }
                            
                        }

                    };
                }
				public IPresenceSender getPresenceSender() {
					return new IPresenceSender() {
						public void sendPresenceUpdate(ID fromID, ID toID, IPresence presence) {
                            try {
								Presence newPresence = createPresenceFromIPresence(presence);
                                XMPPClientSOContainer.this.sendPresenceUpdate(toID,newPresence);
                            } catch (IOException e) {
                                dumpStack("Exception in sendPresenceUpdate to "+toID+" with presence "+presence,e);
                            }
						}
						public void sendRosterAdd(ID fromID, String user, String name, String[] groups) {
                            try {
                                XMPPClientSOContainer.this.sendRosterAdd(user,name,groups);
                            } catch (IOException e) {
                                dumpStack("Exception in sendRosterAdd",e);
                            }
						}
						public void sendRosterRemove(ID fromID, ID userID) {
                            try {
								if (userID == null) return;
                                XMPPClientSOContainer.this.sendRosterRemove(userID.getName());
                            } catch (IOException e) {
                                dumpStack("Exception in sendRosterAdd",e);
                            }
						}

					};
				}
				
				public IAccountManager getAccountManager() {
					return new IAccountManager() {
						public void changePassword(String newpassword) throws ECFException {
							sharedObject.changePassword(newpassword);
						}
						public void createAccount(String username, String password, Map attributes) throws ECFException {
							sharedObject.createAccount(username,password,attributes);
						}
						public void deleteAccount() throws ECFException {
							sharedObject.deleteAccount();
						}
						public String getAccountInstructions() {
							return sharedObject.getAccountInstructions();
						}
						public String[] getAccountAttributeNames() {
							return sharedObject.getAccountAttributeNames();
						}
						public Object getAccountAttribute(String name) {
							return sharedObject.getAccountAttribute(name);
						}
						public boolean supportsCreation() {
							return sharedObject.supportsCreation();
						}
					};
				}
				public void addSubscribeListener(ISubscribeListener listener) {
					sharedObject.addSubscribeListener(listener);
				}
				public IChatRoomManager getChatRoomManager() {
					return new IChatRoomManager() {
						public IChatRoomManager getParent() {
							return null;
						}
						public IChatRoomManager[] getChildren() {
							return new IChatRoomManager[0];
						}
						public ID[] getChatRooms() {
							return XMPPClientSOContainer.this.getChatRooms();
						}
						public IRoomInfo getChatRoomInfo(String roomname) {
							return XMPPClientSOContainer.this.getChatRoomInfo(roomname);
						}
						public IRoomInfo[] getChatRoomsInfo() {
							ID [] chatRooms = getChatRooms();
							if (chatRooms == null) return null;
							IRoomInfo [] res = new IRoomInfo[chatRooms.length];
							int count = 0;
							for(int i=0; i < chatRooms.length; i++) {
								IRoomInfo infoResult = XMPPClientSOContainer.this.getChatRoomInfo(chatRooms[i]);
								if (infoResult != null) {
									res[count++] = infoResult;
								}
							}
							IRoomInfo [] results = new IRoomInfo[count];
							for(int i=0; i < count; i++) {
								results[i] = res[i];
							}
							return results;
						}
						public Object getAdapter(Class adapter) {
							return null;
						}};
				}
            };
        } else {
			return super.getAdapter(clazz);
		}
    }
    protected Collection getHostedRoomForService(String svc) throws XMPPException {
		return MultiUserChat.getHostedRooms(sharedObject.getConnection(),svc);
    }
    protected ID createIDFromHostedRoom(HostedRoom room) {
    	try {
    		return new XMPPRoomID(getConnectNamespace(),sharedObject.getConnection(),room.getJid(),room.getName());
    	} catch (URISyntaxException e) {
    		// debug output
    		dumpStack("Exception in createIDFromHostedRoom("+room+")",e);
    		return null;
    	}
    }
    protected ID[] getChatRooms() {
    	if (sharedObject == null) return null;
    	XMPPConnection conn = sharedObject.getConnection();
    	if (conn == null) return null;
    	Collection result = new ArrayList();
    	try {
    		Collection svcs = MultiUserChat.getServiceNames(sharedObject.getConnection());
    		for(Iterator svcsi = svcs.iterator(); svcsi.hasNext(); ) {
    			String svc = (String) svcsi.next();
    			Collection rooms = getHostedRoomForService(svc);
    			for(Iterator roomsi = rooms.iterator(); roomsi.hasNext(); ) {
    				HostedRoom room = (HostedRoom) roomsi.next();
    				ID roomID = createIDFromHostedRoom(room);
    				if (roomID != null) result.add(roomID);
    			}
    		}
    	} catch (XMPPException e) {
    		dumpStack("Exception in getChatRooms()",e);
    		return null;
    	}
    	return (ID []) result.toArray(new ID[] {});
    }
    class ECFRoomInfo implements IRoomInfo {

    	RoomInfo info;
    	XMPPRoomID roomID;
    	ID connectedID;
    	public ECFRoomInfo(XMPPRoomID roomID, RoomInfo info, ID connectedID) {
    		this.roomID = roomID;
    		this.info = info;
    		this.connectedID = connectedID;
    	}
		public String getDescription() {
			return info.getDescription();
		}
		public String getSubject() {
			return info.getSubject();
		}
		public ID getRoomID() {
			return roomID;
		}
		public int getParticipantsCount() {
			return info.getOccupantsCount();
		}
		public String getName() {
			return roomID.getLongName();
		}
		public boolean isPersistent() {
			return info.isPersistent();
		}
		public boolean requiresPassword() {
			return info.isPasswordProtected();
		}
		public boolean isModerated() {
			return info.isModerated();
		}
		public ID getConnectedID() {
			return roomID;
		}
		public Object getAdapter(Class clazz) {
			return null;
		}
		public IChatRoomContainer createChatRoomContainer() throws ContainerInstantiationException {
			IChatRoomContainer chatContainer = null;
			try {
				chatContainer = new XMPPGroupChatSOContainer(XMPPClientSOContainer.this.getConnection(),sharedObject.getConnection(),getConnectNamespace());
			} catch (IDInstantiationException e) {
				throw new ContainerInstantiationException("Exception creating chat container for presence container "+getID(),e);
			}
			chats.add(chatContainer);
			return chatContainer;
		}
    	public String toString() {
    		StringBuffer buf = new StringBuffer("ECFRoomInfo[");
    		buf.append("id=").append(getID()).append(";name="+getName());
    		buf.append(";service="+getConnectedID());
    		buf.append(";count="+getParticipantsCount());
    		buf.append(";subject="+getSubject()).append(";desc="+getDescription());
    		buf.append(";pers="+isPersistent()).append(";pw="+requiresPassword());
    		buf.append(";mod="+isModerated()).append("]");
    		return buf.toString();
    	}
    }
    protected IRoomInfo getChatRoomInfo(ID roomID) {
    	if (!(roomID instanceof XMPPRoomID)) return null;
    	XMPPRoomID cRoomID = (XMPPRoomID) roomID;
    	try {
    		RoomInfo info = MultiUserChat.getRoomInfo(sharedObject.getConnection(),cRoomID.getMucString());
    		if (info != null) {
    			return new ECFRoomInfo(cRoomID,info,getConnectedID());
    		}
    	} catch (XMPPException e) {
    		dumpStack("Exception in getChatRoomInfo("+roomID+")",e);
    		return null;
    	}
    	return null;
    }
    protected IRoomInfo getChatRoomInfo(String roomname) {
    	try {
			// Create roomid
			XMPPRoomID roomID = new XMPPRoomID(getConnectNamespace(),sharedObject.getConnection(),roomname);
			String mucName = roomID.getMucString();
    		RoomInfo info = MultiUserChat.getRoomInfo(sharedObject.getConnection(),mucName);
    		if (info != null) {
    			return new ECFRoomInfo(roomID,info,getConnectedID());
    		}
    	} catch (Exception e) {
    		dumpStack("Exception in getChatRoomInfo("+roomname+")",e);
    		return null;
    	}
    	return null;
    }
    
    // IFileTransferContainer implementation
    
    List transferListeners = new ArrayList();
    
    List incomingTransferListeners = new ArrayList();
    
    protected void addFileTransferListener(IFileTransferListener listener) {
    	transferListeners.add(listener);
    }
    protected void removeFileTransferListener(IFileTransferListener listener) {
    	transferListeners.remove(listener);
    }
	public void addIncomingFileTransferRequestListener(IIncomingFileTransferRequestListener listener) {
		incomingTransferListeners.add(listener);
	}
	public void sendOutgoingRequest(ID targetReceiver, IFileTransferInfo localFileToSend, IFileTransferListener progressListener) throws OutgoingFileTransferException {
		XMPPConnection xmppConnection = sharedObject.getConnection();
		if (xmppConnection == null || !xmppConnection.isConnected()) throw new OutgoingFileTransferException("not connected");
		FileTransferManager manager = new FileTransferManager(xmppConnection);
		
		XMPPOutgoingFileTransfer fileTransfer = new XMPPOutgoingFileTransfer(manager, (XMPPID) targetReceiver, localFileToSend, progressListener);
		
		try {
			fileTransfer.startSend(localFileToSend.getFile(), localFileToSend.getDescription());
		} catch (XMPPException e) {
			throw new OutgoingFileTransferException("Exception sending start request",e);
		}
	}
	protected void fireFileTransferEvent(IFileTransferEvent event) {
		for(Iterator i=transferListeners.iterator(); i.hasNext(); ) {
			IFileTransferListener l = (IFileTransferListener) i.next();
			l.handleTransferEvent(event);
		}
	}
	public Namespace getOutgoingFileTransferNamespace() {
		return getConnectNamespace();
	}
	public boolean removeIncomingFileTransferRequestListener(IIncomingFileTransferRequestListener listener) {
		return incomingTransferListeners.remove(listener);
	}
	public void sendOutgoingRequest(ID targetReceiver, File localFileToSend, IFileTransferListener transferListener) throws OutgoingFileTransferException {
		sendOutgoingRequest(targetReceiver, new FileTransferInfo(localFileToSend), transferListener);
	}

}