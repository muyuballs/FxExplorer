/*
 * Copyright 2015. Qiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gescobar.httpserver;

import android.text.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Response {

    public enum HttpStatus {

        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        PARTIAL_INFO(203, "Partial Info"),
        NO_CONTENT(204, "No Content"),
        MOVED(301, "Moved Permanently"),
        FOUND(302, "Found"),
        SEE_OTHER(303, "See Other"),
        NOT_MODIFIED(304, "Not Modified"),
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        CONFLICT(409, "Conflict"),
        INTERNAL_ERROR(500, "Internal Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        OVERLOADED(502, "Overloaded"),
        GATEWAY_TIMEOUT(503, "Gateway Timeout");

        private int code;

        private String reason;

        private HttpStatus(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        public int getCode() {
            return code;
        }

        public String getReason() {
            return reason;
        }

    }

    private String responseBody;
    private HttpStatus status;
    private String contentType;
    private boolean finished;
    private OutputStream outputStream;
    private HashMap<String, String> headers;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.headers = new HashMap<>();
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void finish() throws IOException {
        if (!finished) {
            outputStream.flush();
            outputStream.close();
            this.finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public Response status(HttpStatus status) {
        this.status = status;
        return this;
    }

    public Response ok() {
        this.status = HttpStatus.OK;
        return this;
    }

    public Response notFound() {
        this.status = HttpStatus.NOT_FOUND;
        return this;
    }

    public Response contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public void writeHeader() throws IOException {
        outputStream.write(headString().getBytes(Charset.forName("UTF-8")));
    }

    public void writeBody() throws IOException {
        if (!TextUtils.isEmpty(responseBody)) {
            outputStream.write(responseBody.getBytes(Charset.forName("UTF-8")));
        }
    }

    public void setBody(String body) {
        this.responseBody = body;
    }

    public String headString() {
        String ret = "HTTP/1.1 " + status.getCode() + " " + status.getReason() + "\r\n";
        if (status.getCode() != 200 || TextUtils.isEmpty(contentType)) {
            ret += "Content-Type: text/html;charset=utf8\r\n";
        } else {
            ret += "Content-Type: " + contentType + "\r\n";
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            ret += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        return ret + "\r\n";
    }
}
