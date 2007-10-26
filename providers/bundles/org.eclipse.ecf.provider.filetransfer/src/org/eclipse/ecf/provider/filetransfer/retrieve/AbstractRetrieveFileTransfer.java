/*******************************************************************************
 * Copyright (c) 2004, 2007 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.identity.FileTransferNamespace;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractRetrieveFileTransfer implements IIncomingFileTransfer, IRetrieveFileTransfer, IFileTransferPausable {

	public static final int DEFAULT_BUF_LENGTH = 4096;

	private static final int FILETRANSFER_ERRORCODE = 1001;

	protected Job job;

	protected URL remoteFileURL;

	protected IFileID remoteFileID;

	protected IFileTransferListener listener;

	protected int buff_length = DEFAULT_BUF_LENGTH;

	protected boolean done = false;

	protected long bytesReceived = 0;

	protected InputStream remoteFileContents;

	protected OutputStream localFileContents;

	protected boolean closeOutputStream = true;

	protected Exception exception;

	protected long fileLength = -1;

	protected Map options = null;

	protected boolean paused = false;

	protected IFileRangeSpecification rangeSpecification = null;

	protected URL getRemoteFileURL() {
		return remoteFileURL;
	}

	protected void setInputStream(InputStream ins) {
		remoteFileContents = ins;
	}

	protected void setOutputStream(OutputStream outs) {
		localFileContents = outs;
	}

	protected void setCloseOutputStream(boolean close) {
		closeOutputStream = close;
	}

	protected void setFileLength(long length) {
		fileLength = length;
	}

	protected Map getOptions() {
		return options;
	}

	public AbstractRetrieveFileTransfer() {
		//
	}

	public class FileTransferJob extends Job {

		public FileTransferJob(String name) {
			super(name);
		}

		protected IStatus run(IProgressMonitor monitor) {
			final byte[] buf = new byte[buff_length];
			final int totalWork = ((fileLength == -1) ? 100 : (int) fileLength);
			monitor.beginTask(getRemoteFileURL().toString() + Messages.AbstractRetrieveFileTransfer_Progress_Data, totalWork);
			try {
				while (!isDone() && !isPaused()) {
					if (monitor.isCanceled())
						throw new UserCancelledException(Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled);
					final int bytes = remoteFileContents.read(buf);
					if (bytes != -1) {
						bytesReceived += bytes;
						localFileContents.write(buf, 0, bytes);
						fireTransferReceiveDataEvent();
						monitor.worked(bytes);
					} else
						done = true;
				}
			} catch (final Exception e) {
				exception = e;
				done = true;
			} finally {
				hardClose();
				monitor.done();
				if (isPaused())
					fireTransferReceivePausedEvent();
				else
					fireTransferReceiveDoneEvent();
			}
			return getFinalStatus(exception);
		}

	}

	protected IStatus getFinalStatus(Throwable exception1) {
		if (exception1 == null)
			return new Status(IStatus.OK, Activator.getDefault().getBundle().getSymbolicName(), 0, Messages.AbstractRetrieveFileTransfer_Status_Transfer_Completed_OK, null);
		else if (exception1 instanceof UserCancelledException)
			return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, FILETRANSFER_ERRORCODE, Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled, exception1);
		else
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, FILETRANSFER_ERRORCODE, Messages.AbstractRetrieveFileTransfer_Status_Transfer_Exception, exception1);
	}

	protected void hardClose() {
		try {
			if (remoteFileContents != null)
				remoteFileContents.close();
		} catch (final IOException e) {
			Activator.getDefault().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "hardClose", e)); //$NON-NLS-1$
		}
		try {
			if (localFileContents != null && closeOutputStream)
				localFileContents.close();
		} catch (final IOException e) {
			Activator.getDefault().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "hardClose", e)); //$NON-NLS-1$
		}
		job = null;
		remoteFileContents = null;
		localFileContents = null;
	}

	protected void fireTransferReceivePausedEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceivePausedEvent() {

			private static final long serialVersionUID = -1317411290525985140L;

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceivePausedEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
				return sb.toString();
			}
		});
	}

	protected void fireTransferReceiveDoneEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveDoneEvent() {

			private static final long serialVersionUID = 6925524078226825710L;

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public Exception getException() {
				return AbstractRetrieveFileTransfer.this.getException();
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveDoneEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength).append(";exception=").append(getException()) //$NON-NLS-1$ //$NON-NLS-2$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}
		});
	}

	protected void fireTransferReceiveDataEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveDataEvent() {
			private static final long serialVersionUID = -5656328374614130161L;

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveDataEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength) //$NON-NLS-1$ 
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}
		});
	}

	public long getBytesReceived() {
		return bytesReceived;
	}

	public void cancel() {
		if (isPaused()) {
			done = true;
			this.exception = new UserCancelledException(Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled);
			fireTransferReceiveDoneEvent();
		} else if (job != null)
			job.cancel();
	}

	public Exception getException() {
		return exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#getPercentComplete()
	 */
	public double getPercentComplete() {
		if (fileLength == -1 || fileLength == 0)
			return fileLength;
		return ((double) bytesReceived / (double) fileLength);
	}

	public boolean isDone() {
		return done;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == null)
			return null;
		if (adapter.isInstance(this)) {
			return this;
		}
		final IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		return (adapterManager == null) ? null : adapterManager.loadAdapter(this, adapter.getName());
	}

	/**
	 * Open incoming and outgoing streams associated with this file transfer.
	 * Subclasses must implement this method to open input and output streams.
	 * The <code>remoteFileContents</code> and <code>localFileContent</code>
	 * must be non-<code>null</code> after successful completion of the
	 * implementation of this method.
	 * 
	 * @throws IncomingFileTransferException
	 */
	protected abstract void openStreams() throws IncomingFileTransferException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID,
	 *      org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(final IFileID remoteFileID1, IFileTransferListener transferListener, Map options1) throws IncomingFileTransferException {
		sendRetrieveRequest(remoteFileID1, null, transferListener, options1);
	}

	public Namespace getRetrieveNamespace() {
		return IDFactory.getDefault().getNamespaceByName(FileTransferNamespace.PROTOCOL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#isPaused()
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Subclass overridable version of {@link #pause()}. Subclasses must
	 * provide an implementation of this method to support
	 * {@link IFileTransferPausable}.
	 * 
	 * @return true if the pause is successful. <code>false</code> otherwise.
	 */
	protected abstract boolean doPause();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#pause()
	 */
	public boolean pause() {
		return doPause();
	}

	/**
	 * Subclass overridable version of {@link #resume()}. Subclasses must
	 * provide an implementation of this method to support
	 * {@link IFileTransferPausable}.
	 * 
	 * @return true if the resume is successful. <code>false</code> otherwise.
	 */
	protected abstract boolean doResume();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#resume()
	 */
	public boolean resume() {
		return doResume();
	}

	public IFileTransferListener getListener() {
		return listener;
	}

	protected void setupAndScheduleJob() {
		job = new FileTransferJob(getRemoteFileURL().toString());
		job.schedule();
	}

	protected void fireReceiveStartEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveStartEvent() {
			private static final long serialVersionUID = -59096575294481755L;

			public IFileID getFileID() {
				return remoteFileID;
			}

			public IIncomingFileTransfer receive(File localFileToSave) throws IOException {
				setOutputStream(new BufferedOutputStream(new FileOutputStream(localFileToSave)));
				setupAndScheduleJob();
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveStartEvent["); //$NON-NLS-1$
				sb.append("isdone=").append(done).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}

			public void cancel() {
				hardClose();
			}

			/**
			 * @param streamToStore
			 * @return incoming file transfer instance.
			 * @throws IOException not thrown in this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore) throws IOException {
				setOutputStream(streamToStore);
				setCloseOutputStream(false);
				setupAndScheduleJob();
				return AbstractRetrieveFileTransfer.this;
			}

		});
	}

	protected void fireReceiveResumedEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveResumedEvent() {

			private static final long serialVersionUID = 7111739642849612839L;

			public IFileID getFileID() {
				return remoteFileID;
			}

			public IIncomingFileTransfer receive(File localFileToSave) throws IOException {
				setOutputStream(new BufferedOutputStream(new FileOutputStream(localFileToSave)));
				setupAndScheduleJob();
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveResumedEvent["); //$NON-NLS-1$
				sb.append("isdone=").append(done).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}

			public void cancel() {
				hardClose();
			}

			/**
			 * @param streamToStore
			 * @return incoming file transfer instance.
			 * @throws IOException not thrown in this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore) throws IOException {
				setOutputStream(streamToStore);
				setCloseOutputStream(false);
				setupAndScheduleJob();
				return AbstractRetrieveFileTransfer.this;
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IIncomingFileTransfer#getFileRangeSpecification()
	 */
	public IFileRangeSpecification getFileRangeSpecification() {
		return rangeSpecification;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, org.eclipse.ecf.filetransfer.IFileRangeSpecification, org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(IFileID rFileID, IFileRangeSpecification rangeSpec, IFileTransferListener transferListener, Map ops) throws IncomingFileTransferException {
		Assert.isNotNull(rFileID, Messages.AbstractRetrieveFileTransfer_RemoteFileID_Not_Null);
		Assert.isNotNull(transferListener, Messages.AbstractRetrieveFileTransfer_TransferListener_Not_Null);
		this.done = false;
		this.bytesReceived = 0;
		this.exception = null;
		this.fileLength = 0;
		this.remoteFileID = rFileID;
		this.rangeSpecification = rangeSpec;
		this.options = ops;

		try {
			this.remoteFileURL = rFileID.getURL();
		} catch (final MalformedURLException e) {
			throw new IncomingFileTransferException(NLS.bind(Messages.AbstractRetrieveFileTransfer_MalformedURLException, rFileID), e);
		}
		this.listener = transferListener;
		openStreams();
	}
}
