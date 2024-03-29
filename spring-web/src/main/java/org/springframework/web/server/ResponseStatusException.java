/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.server;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 指定 HTTP 响应码的异常
 * <p>
 * Base class for exceptions associated with specific HTTP response status codes.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ResponseStatusException extends NestedRuntimeException {

	private final HttpStatus status;

	@Nullable
	private final String reason;


	/**
	 * Constructor with a response status.
	 *
	 * @param status the HTTP status (required)
	 */
	public ResponseStatusException(HttpStatus status) {
		this(status, null, null);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation.
	 *
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason) {
		this(status, reason, null);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation, as well as a nested exception.
	 *
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 * @param cause  a nested exception (optional)
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		Assert.notNull(status, "HttpStatus is required");
		this.status = status;
		this.reason = reason;
	}


	/**
	 * Return the HTTP status associated with this exception.
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * Return headers associated with the exception that should be added to the
	 * error response, e.g. "Allow", "Accept", etc.
	 * <p>The default implementation in this class returns an empty map.
	 *
	 * @since 5.1.11
	 * @deprecated as of 5.1.13 in favor of {@link #getResponseHeaders()}
	 */
	@Deprecated
	public Map<String, String> getHeaders() {
		return Collections.emptyMap();
	}

	/**
	 * Return headers associated with the exception that should be added to the
	 * error response, e.g. "Allow", "Accept", etc.
	 * <p>The default implementation in this class returns empty headers.
	 *
	 * @since 5.1.13
	 */
	public HttpHeaders getResponseHeaders() {
		Map<String, String> headers = getHeaders();
		if (headers.isEmpty()) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders result = new HttpHeaders();
		getHeaders().forEach(result::add);
		return result;
	}

	/**
	 * The reason explaining the exception (potentially {@code null} or empty).
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}


	@Override
	public String getMessage() {
		String msg = this.status + (this.reason != null ? " \"" + this.reason + "\"" : "");
		return NestedExceptionUtils.buildMessage(msg, getCause());
	}

}
