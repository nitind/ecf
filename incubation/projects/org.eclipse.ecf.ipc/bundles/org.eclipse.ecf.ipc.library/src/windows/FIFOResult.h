/*******************************************************************************
 * Copyright (c) 2009  Clark N. Hobbie
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Clark N. Hobbie - initial API and implementation
 *******************************************************************************/
/**
 * Utility routines for setting the fields in FIFOResult
 */
#ifndef _Included_org_eclipse_ecf_ipc_fifo_FIFOResult
#define _Included_org_eclipse_ecf_ipc_fifo_FIFOResult
#ifdef __cplusplus
extern "C" {
#endif


#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_UNKNOWN
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_UNKNOWN -1L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_SUCCESS
#define org_eclipse_ecf_ipc_fifo_FIFOResult_SUCCESS 0L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_ACCESS_DENIED
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_ACCESS_DENIED 1L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_INVALID_HANDLE
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_INVALID_HANDLE 2L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_PIPE_BUSY
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_PIPE_BUSY 3L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_NOT_FOUND
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_NOT_FOUND 4L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_PIPE_CLOSED
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_PIPE_CLOSED 5L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_CONNECT
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_CONNECT 6L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_READ
#define org_eclipse_ecf_ipc_fifo_FIFOResult_ERROR_READ 7L
#undef org_eclipse_ecf_ipc_fifo_FIFOResult_TIMEOUT
#define org_eclipse_ecf_ipc_fifo_FIFOResult_TIMEOUT 8L



extern void
FIFOResult_SetHandle(JNIEnv *env, jobject result, HANDLE handle);

extern void
FIFOResult_SetResultCode(JNIEnv *env, jobject result, jint value);

extern void
FIFOResult_SetErrorCode(JNIEnv *env, jobject result, DWORD errorCode);

extern void
FIFOResult_SetByteCount(JNIEnv *env, jobject result, DWORD byteCount);

extern int
FIFOResult_isServer(JNIEnv * env, jobject result);

extern void
FIFOResult_initialize(JNIEnv *env);

extern jlong
FIFOResult_GetSyncObject(JNIEnv *env, jobject result);

extern void
FIFOResult_SetSyncObject(JNIEnv *env, jobject result, jlong value);

#ifdef __cplusplus
}
#endif
#endif
