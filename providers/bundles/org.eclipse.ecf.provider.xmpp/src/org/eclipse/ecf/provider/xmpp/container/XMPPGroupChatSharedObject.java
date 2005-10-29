/*
 * Created on Mar 21, 2005
 *
 */
package org.eclipse.ecf.provider.xmpp.container;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Vector;
import org.eclipse.ecf.core.ISharedObject;
import org.eclipse.ecf.core.ISharedObjectConfig;
import org.eclipse.ecf.core.ISharedObjectContext;
import org.eclipse.ecf.core.SharedObjectInitException;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.util.Event;
import org.eclipse.ecf.presence.IInvitationListener;
import org.eclipse.ecf.presence.IMessageListener;
import org.eclipse.ecf.presence.IPresence;
import org.eclipse.ecf.presence.chat.IChatParticipantListener;
import org.eclipse.ecf.provider.xmpp.Trace;
import org.eclipse.ecf.provider.xmpp.events.ChatMembershipEvent;
import org.eclipse.ecf.provider.xmpp.events.InvitationReceivedEvent;
import org.eclipse.ecf.provider.xmpp.events.MessageEvent;
import org.eclipse.ecf.provider.xmpp.events.PresenceEvent;
import org.eclipse.ecf.provider.xmpp.identity.XMPPID;
import org.eclipse.ecf.provider.xmpp.identity.XMPPRoomID;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;

public class XMPPGroupChatSharedObject implements ISharedObject {

    public static Trace trace = Trace.create("xmppgroupchatsharedobject");

    ISharedObjectConfig config = null;
    
    Vector messageListeners = new Vector();
	Namespace usernamespace = null;
	XMPPConnection connection = null;
    Vector participantListeners = new Vector();
    Vector invitationListeners = new Vector();
    
    protected void debug(String msg) {
        if (Trace.ON && trace != null) {
            trace.msg(config.getSharedObjectID() + ":" + msg);
        }
    }
    protected void dumpStack(String msg, Throwable e) {
        if (Trace.ON && trace != null) {
            trace.dumpStack(e, config.getSharedObjectID() + ":" + msg);
        }
    }

    protected void addChatParticipantListener(IChatParticipantListener listener) {
        participantListeners.add(listener);
    }
    protected void removeChatParticipantListener(IChatParticipantListener listener) {
        participantListeners.remove(listener);
    }
    protected void addInvitationListener(IInvitationListener listener) {
        invitationListeners.add(listener);
    }
    protected void removeInvitationListener(IInvitationListener listener) {
    	invitationListeners.remove(listener);
    }
    protected void addMessageListener(IMessageListener listener) {
        messageListeners.add(listener);
    }
    protected void removeMessageListener(IMessageListener listener) {
        messageListeners.add(listener);
    }

    public XMPPGroupChatSharedObject(Namespace usernamespace, XMPPConnection conn) {
        super();
        this.usernamespace = usernamespace;
        this.connection = conn;
    }
    
    protected ISharedObjectContext getContext() {
        return config.getContext();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ecf.core.ISharedObject#init(org.eclipse.ecf.core.ISharedObjectConfig)
     */
    public void init(ISharedObjectConfig initData)
            throws SharedObjectInitException {
        this.config = initData;
    }

    protected ID makeUserIDFromName(String name) {
        ID result = null;
        try {
            result = new XMPPID(usernamespace,name);
            return result;
        } catch (Exception e) {
            dumpStack("Exception in makeIDFromName", e);
            return null;
        }
    }

    protected Message.Type [] ALLOWED_MESSAGES = { Message.Type.GROUP_CHAT };
    protected Message filterMessageType(Message msg) {
    	for(int i=0; i < ALLOWED_MESSAGES.length; i++) {
    		if (ALLOWED_MESSAGES[i].equals(msg.getType())) {
    			return msg;
    		}
    	}
    	return null;
    }
    protected String canonicalizeRoomFrom(String from) {
        if (from == null)
            return null;
        int atIndex = from.indexOf('@');
        String hostname = null;
        String username = null;
        int index = from.indexOf("/");
        if (atIndex > 0 && index > 0) {
        	hostname = from.substring(atIndex+1,index);
            username = from.substring(index+1);
            return username+"@"+hostname;
        }
        return from;
    }
    protected IMessageListener.Type makeMessageType(Message.Type type) {
        if (type == null)
            return IMessageListener.Type.NORMAL;
        if (type == Message.Type.CHAT) {
            return IMessageListener.Type.CHAT;
        } else if (type == Message.Type.NORMAL) {
            return IMessageListener.Type.NORMAL;
        } else if (type == Message.Type.GROUP_CHAT) {
            return IMessageListener.Type.GROUP_CHAT;
        } else if (type == Message.Type.HEADLINE) {
            return IMessageListener.Type.HEADLINE;
        } else if (type == Message.Type.HEADLINE) {
            return IMessageListener.Type.HEADLINE;
        } else
            return IMessageListener.Type.NORMAL;
    }
    protected void fireMessage(ID from, ID to, IMessageListener.Type type,
            String subject, String body) {
        for (Iterator i = messageListeners.iterator(); i.hasNext();) {
            IMessageListener l = (IMessageListener) i.next();
            l.handleMessage(from, to, type, subject, body);
        }
    }
    protected String canonicalizeRoomTo(String to) {
        if (to == null)
            return null;
        int index = to.indexOf("/");
        if (index > 0) {
            return to.substring(0, index);
        } else
            return to;
    }

    protected ID makeRoomIDFromName(String from) {
    	try {
    		return new XMPPRoomID(usernamespace,connection,from);
    	} catch (URISyntaxException e) {
            dumpStack("Exception in makeRoomIDFromName", e);
            return null;
    	}
    }

    protected void handleMessageEvent(MessageEvent evt) {
        Message msg = evt.getMessage();
        String from = msg.getFrom();
        String to = msg.getTo();
        String body = msg.getBody();
        String subject = msg.getSubject();
        ID fromID = makeUserIDFromName(canonicalizeRoomFrom(from));
        ID toID = makeUserIDFromName(canonicalizeRoomTo(to));
        msg = filterMessageType(msg);
        if (msg != null) fireMessage(fromID, toID, makeMessageType(msg.getType()), subject, body);
    }

    protected IPresence.Type makeIPresenceType(Presence xmppPresence) {
        if (xmppPresence == null)
            return IPresence.Type.AVAILABLE;
        Type type = xmppPresence.getType();
        if (type == Presence.Type.AVAILABLE) {
            return IPresence.Type.AVAILABLE;
        } else if (type == Presence.Type.ERROR) {
            return IPresence.Type.ERROR;
        } else if (type == Presence.Type.SUBSCRIBE) {
            return IPresence.Type.SUBSCRIBE;
        } else if (type == Presence.Type.SUBSCRIBED) {
            return IPresence.Type.SUBSCRIBED;
        } else if (type == Presence.Type.UNSUBSCRIBE) {
            return IPresence.Type.UNSUBSCRIBE;
        } else if (type == Presence.Type.UNSUBSCRIBED) {
            return IPresence.Type.UNSUBSCRIBED;
        } else if (type == Presence.Type.UNAVAILABLE) {
            return IPresence.Type.UNAVAILABLE;
        }
        return IPresence.Type.AVAILABLE;
    }

    protected IPresence.Mode makeIPresenceMode(Presence xmppPresence) {
        if (xmppPresence == null)
            return IPresence.Mode.AVAILABLE;
        Mode mode = xmppPresence.getMode();
        if (mode == Presence.Mode.AVAILABLE) {
            return IPresence.Mode.AVAILABLE;
        } else if (mode == Presence.Mode.AWAY) {
            return IPresence.Mode.AWAY;
        } else if (mode == Presence.Mode.CHAT) {
            return IPresence.Mode.CHAT;
        } else if (mode == Presence.Mode.DO_NOT_DISTURB) {
            return IPresence.Mode.DND;
        } else if (mode == Presence.Mode.EXTENDED_AWAY) {
            return IPresence.Mode.EXTENDED_AWAY;
        } else if (mode == Presence.Mode.INVISIBLE) {
            return IPresence.Mode.INVISIBLE;
        }
        return IPresence.Mode.AVAILABLE;
    }
    protected IPresence makeIPresence(Presence xmppPresence) {
        int priority = xmppPresence.getPriority();
        String status = xmppPresence.getStatus();
        IPresence newPresence = new org.eclipse.ecf.presence.impl.Presence(
                makeIPresenceType(xmppPresence), priority, status,
                makeIPresenceMode(xmppPresence));
        return newPresence;
    }

    protected void handlePresenceEvent(PresenceEvent evt) {
        Presence xmppPresence = evt.getPresence();
        String from = canonicalizeRoomFrom(xmppPresence.getFrom());
        IPresence newPresence = makeIPresence(xmppPresence);
        ID fromID = makeUserIDFromName(from);
		fireParticipant(fromID, newPresence);
    }

    protected void handleChatMembershipEvent(ChatMembershipEvent evt) {
    	String from = canonicalizeRoomFrom(evt.getFrom());
        ID fromID = makeUserIDFromName(from);
        fireChatParticipant(fromID,evt.isAdd());
    }
    protected void fireParticipant(ID fromID, IPresence presence) {
        for (Iterator i = participantListeners.iterator(); i.hasNext();) {
            IChatParticipantListener l = (IChatParticipantListener) i.next();
            l.handlePresence(fromID, presence);
        }
    }
    protected void fireChatParticipant(ID fromID, boolean join) {
        for (Iterator i = participantListeners.iterator(); i.hasNext();) {
            IChatParticipantListener l = (IChatParticipantListener) i.next();
            if (join) {
            	l.joined(fromID);
            } else {
            	l.left(fromID);
            }
        }
    }
    protected void fireInvitationReceived(ID roomID, ID fromID, ID toID, String subject, String body) {
        for (Iterator i = invitationListeners.iterator(); i.hasNext();) {
            IInvitationListener l = (IInvitationListener) i.next();
            l.handleInvitationReceived(roomID,fromID,toID,subject,body);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ecf.core.ISharedObject#handleEvent(org.eclipse.ecf.core.util.Event)
     */
    public void handleEvent(Event event) {
        debug("handleEvent(" + event + ")");
        if (event instanceof MessageEvent) {
            handleMessageEvent((MessageEvent) event);
        } else if (event instanceof PresenceEvent) {
        	handlePresenceEvent((PresenceEvent) event);
        } else if (event instanceof InvitationReceivedEvent) {
        	handleInvitationEvent((InvitationReceivedEvent) event);
        } else if (event instanceof ChatMembershipEvent) {
        	handleChatMembershipEvent((ChatMembershipEvent) event);
        } else {	
        	debug("unrecognized event " + event);
        }
    }

    protected void handleInvitationEvent(InvitationReceivedEvent event) {
    	XMPPConnection conn = event.getConnection();
    	if (conn == connection) {
	    	ID roomID = makeRoomIDFromName(event.getRoom());
	    	ID fromID = makeUserIDFromName(event.getInviter());
	    	Message mess = event.getMessage();
	    	ID toID = makeUserIDFromName(mess.getTo());
	    	String subject = mess.getSubject();
	    	String body = event.getReason();
	    	fireInvitationReceived(roomID,fromID,toID,subject,body);
    	} else {
    		debug("got invitation event for other connection "+event);
    	}
    }
    /* (non-Javadoc)
     * @see org.eclipse.ecf.core.ISharedObject#handleEvents(org.eclipse.ecf.core.util.Event[])
     */
    public void handleEvents(Event[] events) {
        for(int i=0; i < events.length; i++) {
            this.handleEvent(events[i]);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ecf.core.ISharedObject#dispose(org.eclipse.ecf.core.identity.ID)
     */
    public void dispose(ID containerID) {
        messageListeners.clear();
        participantListeners.clear();
        invitationListeners.clear();
        this.config = null;
        this.connection = null;
        this.usernamespace = null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ecf.core.ISharedObject#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class clazz) {
        return null;
    }
}
