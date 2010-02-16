/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package ning.http.client;

import ning.http.client.providers.NettyAsyncHttpProvider;
import ning.http.client.Cookie;
import ning.http.client.Headers;
import ning.http.client.Part;
import ning.http.client.ProxyServer;
import ning.http.client.Request;
import ning.http.client.Request.EntityWriter;
import ning.http.client.RequestType;
import ning.http.client.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class support asynchronous and synchronous HTTP request.
 *
 * <p>
 * To execute synchronous HTTP request, you just need to do
 * <p><pre><code>
 *    AsyncHttpClient c = new AsyncHttpClient();
 *    Future<Response> f = c.doGet(TARGET_URL).get()
 * </code></pre></p>
 *
 * The code above will block until the response is fully received. To execute asynchronous HTTP request, you
 * create an {@link AsyncHandler}
 *
 * <p><pre><code>
 *       AsyncHttpClient c = new AsyncHttpClient();
 *       Future<Response> f = c.doGet(TARGET_URL, new AsyncHandler() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                 // Do something
                return response;
            }

            @Override
            public void onThrowable(Throwable t){
            }
        });
        Response response = f.get();
 * </code></pre></p><p>
 * The {@link AsyncHandler#onCompleted(ning.http.client.Response)} will be invoked once the http response has been fully read, which include
 * the http headers and the response body. Note that the entire response will be buffered in memory.
 * </p><p>
 * You can also have more control about the how the response is asynchronously processed by using a {@link AsyncStreamingHandler}
 * <p><pre><code>
        AsyncHttpClient c = new AsyncHttpClient();
        Future<Response> f = c.doGet(TARGET_URL, new AsyncStreamingHandler() {

            @Override
            public Response onContentReceived(HttpContent content) throws ResponseComplete {
                if (content instanceof HttpResponseHeaders) {
                    // The headers has been read
                    // If you don't want to read the body, or stop processing the response
                    throw new ResponseComplete();
                } else if (content instanceof HttpResponseBody) {
                    HttpResponseBody b = (HttpResponseBody) content;
                    // Do something with the body. It may not been fully read yet.
                    if (b.isComplete()){
                        // The full response has been read.
                    }
                }
                return content.getResponse();
            }

            @Override
            public void onThrowable(Throwable t){

            }
        });
        Response response = f.get();
 * </code></pre></p><p>
 * From an {@link HttpContent}, you can asynchronously process the response headers and body and decide when to
 * stop the processing the response by throwing {@link AsyncStreamingHandler.ResponseComplete} at any moment. The returned
 * {@link Response} will be incomplete until {@link HttpResponseBody#isComplete()} return true, which means the
 * response has been fully read and buffered in memory.</p><p>
 * This class can also be used with the need of {@link AsyncHandler}</p>
 * <p><pre><code> '
 *      AsyncHttpClient c = new AsyncHttpClient();
 *      Future<Response> f = c.doGet(TARGET_URL);
 *      Response r = f.get();
 * </code></pre></p><p>
 *
 */
public class AsyncHttpClient {
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = Integer.getInteger("xn.httpClient.defaultMaxTotalConnections", 2000);
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = Integer.getInteger("xn.httpClient.defaultMaxConnectionsPerHost", 2000);
    public static final long DEFAULT_CONNECTION_TIMEOUT_MS = Long.getLong("xn.httpClient.defaultConnectionTimeoutInMS", 60 * 1000L);
    public static final long DEFAULT_IDLE_CONNECTION_TIMEOUT_MS = Long.getLong("xn.httpClient.defaultIdleConnectionTimeoutInMS", 15 * 1000L);
    public static final int DEFAULT_REQUEST_TIMEOUT_MS = Integer.getInteger("xn.httpClient.defaultRequestTimeoutInMS", 60 * 1000);
    public static final boolean DEFAULT_REDIRECTS_ENABLED = Boolean.getBoolean("xn.httpClient.defaultRedirectsEnabled");
    public static final int DEFAULT_MAX_REDIRECTS = Integer.getInteger("xn.httpClient.defaultMaxRedirects", 5);

    private final AsyncHttpProvider httpProvider;

    public AsyncHttpClient() {
        this(new NettyAsyncHttpProvider(new ProviderConfig(
                Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()))));
    }

    public AsyncHttpClient(AsyncHttpProvider httpProvider) {
        this.httpProvider = httpProvider;

        setMaximumConnectionsTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        setMaximumConnectionsPerHost(DEFAULT_MAX_CONNECTIONS_PER_HOST);
        setFollowRedirects(DEFAULT_REDIRECTS_ENABLED);
        setRequestTimeout(DEFAULT_REQUEST_TIMEOUT_MS);
        setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MS);
        setIdleConnectionTimeout(DEFAULT_IDLE_CONNECTION_TIMEOUT_MS);
        setMaximumNumberOfRedirects(DEFAULT_MAX_REDIRECTS);
    }

    private final static AsyncHandler<Response> voidHandler = new AsyncHandler<Response>(){

        @Override
        public Response onCompleted(Response response) throws IOException{
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
        }

    };


    public AsyncHttpProvider getProvider() {
        return httpProvider;
    }

    public void close() {
        httpProvider.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void setMaximumConnectionsTotal(int maxConnectionsTotal) {
        httpProvider.setMaximumConnectionsTotal(maxConnectionsTotal);
    }

    public void setMaximumConnectionsPerHost(int maxConnectionsPerHost) {
        httpProvider.setMaximumConnectionsPerHost(maxConnectionsPerHost);
    }

    public void setConnectionTimeout(long timeOutInMS) {
        httpProvider.setConnectionTimeout(timeOutInMS);
    }

    public void setIdleConnectionTimeout(long timeOutInMS) {
        httpProvider.setIdleConnectionTimeout(timeOutInMS);
    }

    public void setRequestTimeout(int timeOutInMS) {
        httpProvider.setRequestTimeout(timeOutInMS);
    }

    public void setFollowRedirects(boolean followRedirects) {
        httpProvider.setFollowRedirects(followRedirects);
    }

    public void setMaximumNumberOfRedirects(int maxNumRedirects) {
        httpProvider.setMaximumNumberOfRedirects(maxNumRedirects);
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        httpProvider.setCompressionEnabled(compressionEnabled);
    }

    public void setUserAgent(String userAgent) {
        httpProvider.setUserAgent(userAgent);
    }

    /**
     * Sets the proxy for this HttpClient.
     *
     * @param proxyServer The proxy server to use. Can be null, which means "no proxy".
     */
    public void setProxy(final ProxyServer proxyServer) {
        httpProvider.setProxyServer(proxyServer);
    }

    public boolean isCompressionEnabled() {
        return httpProvider.isCompressionEnabled();
    }


    public Future<Response> doGet(String url) throws IOException {
        return doGet(url,(Headers) null);
    }

    public Future<Response> doGet(String url, Headers headers) throws IOException {
        return doGet(url, headers,(List<Cookie>) null);
    }

    public Future<Response> doGet(String url, Headers headers, List<Cookie> cookies) throws IOException {
        return performRequest(new Request(RequestType.GET, url, headers, cookies), voidHandler);
    }

    public <T> Future<T> doGet(String url, AsyncHandler<T> handler) throws IOException {
        return doGet(url, null, null, handler);
    }

    public <T> Future<T> doGet(String url, Headers headers, AsyncHandler<T> handler) throws IOException {
        return doGet(url, headers, null, handler);
    }

    public <T> Future<T> doGet(String url, Headers headers, List<Cookie> cookies, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.GET, url, headers, cookies), handler);
    }

    public Future<Response>  doPost(String url, byte[] data) throws IOException {
        return doPost(url, null, null, data);
    }

    public Future<Response>  doPost(String url, Headers headers, byte[] data) throws IOException {
        return doPost(url, headers, null, data);
    }

    public Future<Response>  doPost(String url, Headers headers, List<Cookie> cookies, byte[] data) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, data), voidHandler);
    }

    public Future<Response>  doPost(String url, InputStream data) throws IOException {
        return doPost(url, null, null, data, -1);
    }

    public Future<Response>  doPost(String url, EntityWriter entityWriter) throws IOException {
        return doPost(url, null, null, entityWriter, -1);
    }

    public Future<Response>  doPost(String url, Headers headers, InputStream data) throws IOException {
        return doPost(url, headers, null, data, -1);
    }

    public Future<Response>  doPost(String url, Headers headers, EntityWriter entityWriter) throws IOException {
        return doPost(url, headers, entityWriter, -1);
    }

    public Future<Response>  doPost(String url, Headers headers, EntityWriter entityWriter, long length) throws IOException {
        return doPost(url, headers, null, entityWriter, length);
    }

    public Future<Response>  doPost(String url, Headers headers, List<Cookie> cookies, InputStream data) throws IOException {
        return doPost(url, headers, cookies, data, -1);
    }

    public Future<Response>  doPost(String url, InputStream data, long length) throws IOException {
        return doPost(url, null, null, data, length);
    }

    public Future<Response>  doPost(String url, Headers headers, InputStream data, long length) throws IOException {
        return doPost(url, headers, null, data, length);
    }

    public Future<Response>  doPost(String url, Headers headers, List<Cookie> cookies, InputStream data, long length) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, data, length), voidHandler);
    }

   public Future<Response>  doPost(String url, Headers headers, List<Cookie> cookies, EntityWriter entityWriter, long length) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, entityWriter, length), voidHandler);
    }

    public Future<Response> doPost(String url, Map<String, String> params) throws IOException {
        return doPost(url, (Headers)null, (List<Cookie>)null, params);
    }

    public Future<Response>  doPost(String url, Headers headers, Map<String, String> params) throws IOException {
        return doPost(url, headers, null, params);
    }

    public Future<Response>  doPost(String url, Headers headers, List<Cookie> cookies, Map<String, String> params) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, params), voidHandler);
    }

    public <T> Future<T> doPost(String url, byte[] data, AsyncHandler<T> handler) throws IOException {
        return doPost(url, null, null, data, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, byte[] data, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, null, data, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, List<Cookie> cookies, byte[] data, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, data), handler);
    }

    public <T> Future<T> doPost(String url, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPost(url, null, null, data, -1, handler);
    }

    public <T> Future<T> doPost(String url, EntityWriter entityWriter, AsyncHandler<T> handler) throws IOException {
        return doPost(url, null, null, entityWriter, -1, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, null, data, -1, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, EntityWriter entityWriter, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, entityWriter, handler, -1);
    }

    public <T> Future<T> doPost(String url, Headers headers, EntityWriter entityWriter, AsyncHandler<T> handler, long length) throws IOException {
        return doPost(url, headers, null, entityWriter, length, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, List<Cookie> cookies, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, cookies, data, -1, handler);
    }

    public <T> Future<T> doPost(String url, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return doPost(url, null, null, data, length, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, null, data, length, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, List<Cookie> cookies, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, data, length), handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, List<Cookie> cookies, EntityWriter entityWriter, long length, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, entityWriter, length), handler);
    }

    public <T> Future<T> doPost(String url, Map<String, String> params, AsyncHandler<T> handler) throws IOException {
        return doPost(url, null, null, params, handler);
    }

    public <T> Future<T> doPost(String url, Headers headers, Map<String, String> params, AsyncHandler<T> handler) throws IOException {
        return doPost(url, headers, null, params, handler);
    }
   
    public <T> Future<T> doPost(String url, Headers headers, List<Cookie> cookies, Map<String, String> params, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, params), handler);
    }

    public Future<Response> doMultipartPost(String url, List<Part> params) throws IOException {
        return doMultipartPost(url, null, null, params);
    }

    public Future<Response> doMultipartPost(String url, Headers headers, List<Part> params) throws IOException {
        return doMultipartPost(url, headers, null, params);
    }

    public Future<Response> doMultipartPost(String url, Headers headers, List<Cookie> cookies, List<Part> params) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, params), voidHandler);
    }    

    public <T> Future<T> doMultipartPost(String url, List<Part> params, AsyncHandler<T> handler) throws IOException {
        return doMultipartPost(url, null, null, params, handler);
    }

    public <T> Future<T> doMultipartPost(String url, Headers headers, List<Part> params, AsyncHandler<T> handler) throws IOException {
        return doMultipartPost(url, headers, null, params, handler);
    }

    public <T> Future<T> doMultipartPost(String url, Headers headers, List<Cookie> cookies, List<Part> params, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.POST, url, headers, cookies, params), handler);
    }

    public Future<Response> doPut(String url, byte[] data) throws IOException {
        return doPut(url, null, null, data);
    }

    public Future<Response> doPut(String url, Headers headers, byte[] data) throws IOException {
        return doPut(url, headers, null, data);
    }

    public Future<Response> doPut(String url, Headers headers, List<Cookie> cookies, byte[] data) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, data), voidHandler);
    }

    public Future<Response> doPut(String url, InputStream data) throws IOException {
        return doPut(url, null, null, data, -1);
    }

    public Future<Response> doPut(String url, Headers headers, InputStream data) throws IOException {
        return doPut(url, headers, null, data, -1);
    }

    public Future<Response> doPut(String url, Headers headers, List<Cookie> cookies, InputStream data) throws IOException {
        return doPut(url, headers, cookies, data, -1);
    }

    public Future<Response> doPut(String url, InputStream data, long length) throws IOException {
        return doPut(url, null, null, data, length);
    }

    public Future<Response> doPut(String url, Headers headers, InputStream data, long length) throws IOException {
        return doPut(url, headers, null, data, length);
    }

    public Future<Response> doPut(String url, Headers headers, EntityWriter entityWriter, long length) throws IOException {
        return doPut(url, headers, (List<Cookie>)null, entityWriter, length);
    }

    public Future<Response> doPut(String url, Headers headers, List<Cookie> cookies, InputStream data, long length) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, data, length), voidHandler);
    }

    public Future<Response> doPut(String url, Headers headers, List<Cookie> cookies, EntityWriter entityWriter, long length) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, entityWriter, length), voidHandler);
    }

    public <T> Future<T> doPut(String url, byte[] data, AsyncHandler<T> handler) throws IOException {
        return doPut(url, null, null, data, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, byte[] data, AsyncHandler<T> handler) throws IOException {
        return doPut(url, headers, null, data, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, List<Cookie> cookies, byte[] data, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, data), handler);
    }

    public <T> Future<T> doPut(String url, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPut(url, null, null, data, -1, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPut(url, headers, null, data, -1, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, List<Cookie> cookies, InputStream data, AsyncHandler<T> handler) throws IOException {
        return doPut(url, headers, cookies, data, -1, handler);
    }

    public <T> Future<T> doPut(String url, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return doPut(url, null, null, data, length, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return doPut(url, headers, null, data, length, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, EntityWriter entityWriter, long length, AsyncHandler<T> handler) throws IOException {
        return doPut(url, headers, null, entityWriter, length, handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, List<Cookie> cookies, InputStream data, long length, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, data, length), handler);
    }

    public <T> Future<T> doPut(String url, Headers headers, List<Cookie> cookies, EntityWriter entityWriter, long length, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.PUT, url, headers, cookies, entityWriter, length), handler);
    }

    public Future<Response> doDelete(String url) throws IOException {
        return doDelete(url, (Headers)null);
    }

    public Future<Response> doDelete(String url, Headers headers) throws IOException {
        return doDelete(url, headers, (List<Cookie>)null);
    }

    public Future<Response> doDelete(String url, Headers headers, List<Cookie> cookies) throws IOException {
        return performRequest(new Request(RequestType.DELETE, url, headers, cookies), voidHandler);
    }

    public <T> Future<T> doDelete(String url, AsyncHandler<T> handler) throws IOException {
        return doDelete(url, null, null, handler);
    }

    public <T> Future<T> doDelete(String url, Headers headers, AsyncHandler<T> handler) throws IOException {
        return doDelete(url, headers, null, handler);
    }

    public <T> Future<T> doDelete(String url, Headers headers, List<Cookie> cookies, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.DELETE, url, headers, cookies), handler);
    }

    public Future<Response> doHead(String url) throws IOException {
        return doHead(url, (Headers)null);
    }

    public Future<Response> doHead(String url, Headers headers) throws IOException {
        return doHead(url, headers, (List<Cookie>)null);
    }

    public Future<Response> doHead(String url, Headers headers, List<Cookie> cookies) throws IOException {
        return performRequest(new Request(RequestType.HEAD, url, headers, cookies), voidHandler);
    }
    
    public <T> Future<T> doHead(String url, AsyncHandler<T> handler) throws IOException {
        return doHead(url, null, null, handler);
    }

    public <T> Future<T> doHead(String url, Headers headers, AsyncHandler<T> handler) throws IOException {
        return doHead(url, headers, null, handler);
    }

    public <T> Future<T> doHead(String url, Headers headers, List<Cookie> cookies, AsyncHandler<T> handler) throws IOException {
        return performRequest(new Request(RequestType.HEAD, url, headers, cookies), handler);
    }

    public <T> Future<T> performRequest(Request request,
                                AsyncHandler<T> handler) throws IOException {
        return httpProvider.handle(request, handler);
    }
}