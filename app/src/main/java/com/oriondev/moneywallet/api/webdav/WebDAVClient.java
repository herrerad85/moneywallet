/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.moneywallet.api.webdav;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oriondev.moneywallet.model.WebDAVFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The whole WebDAV surface this app needs: list one folder, create one folder, upload a file,
 * download a file. That is four HTTP verbs, which is why there is no client library here.
 *
 * The methods that do not touch the network are static and package visible so they can be unit
 * tested on the JVM. The verbs themselves cannot be: PROPFIND and MKCOL are rejected by
 * {@link HttpURLConnection#setRequestMethod} on a desktop JVM and only work on Android, where the
 * implementation accepts arbitrary methods.
 */
public class WebDAVClient {

    static final String DAV_NAMESPACE = "DAV:";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    private final String mBaseUrl;
    private final String mAuthorization;

    public WebDAVClient(String baseUrl, String username, String password) {
        mBaseUrl = normalizeBaseUrl(baseUrl);
        mAuthorization = basicAuthHeader(username, password);
    }

    /**
     * Trims the URL and guarantees exactly one trailing slash, so joining a path is always a plain
     * concatenation. A base URL saved with or without the slash has to behave identically; users
     * type it both ways.
     */
    static String normalizeBaseUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "/";
    }

    static String basicAuthHeader(String username, String password) {
        String credentials = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        byte[] raw = credentials.getBytes(Charset.forName("UTF-8"));
        return "Basic " + Base64.encodeToString(raw, Base64.NO_WRAP);
    }

    /**
     * Percent encodes each path segment, leaving the separators alone. A backup file named with a
     * space or an accented character is ordinary, and an unencoded one produces a 400 from most
     * servers, so this is not a corner case.
     */
    static String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(encodeSegment(segments[i]));
        }
        return builder.toString();
    }

    private static String encodeSegment(String segment) {
        if (segment.isEmpty()) {
            return segment;
        }
        try {
            // URLEncoder targets form bodies, not paths: it maps a space to '+' and escapes a few
            // characters that are legal in a path. Undo those three rather than hand rolling a
            // second encoder.
            return URLEncoder.encode(segment, "UTF-8")
                    .replace("+", "%20")
                    .replace("%7E", "~")
                    .replace("*", "%2A");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is always available");
        }
    }

    static String decodePath(String encoded) {
        try {
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is always available");
        } catch (IllegalArgumentException e) {
            // A malformed escape is the server's problem, not a reason to lose the whole listing.
            return encoded;
        }
    }

    /**
     * Reduces an href from a PROPFIND response to a path relative to the base URL. Servers are
     * inconsistent here: some return an absolute URL, some return an absolute path including the
     * base URL's own prefix, so both shapes are handled.
     */
    static String hrefToRelativePath(String href, String basePath) {
        String path = href;
        int schemeEnd = path.indexOf("://");
        if (schemeEnd >= 0) {
            int hostEnd = path.indexOf('/', schemeEnd + 3);
            path = hostEnd >= 0 ? path.substring(hostEnd) : "/";
        }
        path = decodePath(path);
        if (!basePath.isEmpty() && path.startsWith(basePath)) {
            path = path.substring(basePath.length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    static String nameOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /**
     * Parses a PROPFIND multistatus body. Namespace aware on purpose: servers disagree about the
     * prefix (D:, d:, lp1:), so matching on the DAV: namespace rather than the tag text is what
     * makes this work across Nextcloud, ownCloud and a plain Apache mod_dav.
     *
     * The entry describing the requested folder itself is dropped: a Depth 1 PROPFIND always
     * returns it alongside its children.
     */
    static List<WebDAVFile> parsePropfind(InputStream in, String basePath, String requestedPath)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // The response is data, not a document that gets to reference the local filesystem.
        setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(in);

        List<WebDAVFile> files = new ArrayList<>();
        NodeList responses = document.getElementsByTagNameNS(DAV_NAMESPACE, "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            String href = textOf(response, "href");
            if (href == null) {
                continue;
            }
            String path = hrefToRelativePath(href, basePath);
            if (path.equals(requestedPath)) {
                continue;
            }
            boolean isDirectory = firstElement(response, "collection") != null;
            long size = 0L;
            String contentLength = textOf(response, "getcontentlength");
            if (contentLength != null) {
                try {
                    size = Long.parseLong(contentLength.trim());
                } catch (NumberFormatException ignored) {
                    // A folder, or a server that omits the length. Zero is the right answer.
                }
            }
            files.add(new WebDAVFile(nameOf(path), path, size, isDirectory));
        }
        return files;
    }

    private static void setFeatureQuietly(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // Not every parser knows this feature. Namespace awareness is the part we depend on.
        }
    }

    @Nullable
    private static Element firstElement(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(DAV_NAMESPACE, localName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    @Nullable
    private static String textOf(Element parent, String localName) {
        Element element = firstElement(parent, localName);
        if (element == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(child.getNodeValue());
            }
        }
        return text.toString();
    }

    String urlFor(String path) {
        return mBaseUrl + encodePath(path == null ? "" : path);
    }

    /**
     * The path component of the base URL, used to strip a server's absolute href back down to a
     * path relative to the configured root.
     */
    String basePath() {
        try {
            String path = new URL(mBaseUrl).getPath();
            return path == null ? "" : path;
        } catch (MalformedURLException e) {
            return "";
        }
    }

    private HttpURLConnection open(String path, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlFor(path)).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Authorization", mAuthorization);
        return connection;
    }

    private static boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    @NonNull
    public List<WebDAVFile> list(String path) throws WebDAVException {
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<d:propfind xmlns:d=\"DAV:\"><d:prop>"
                + "<d:resourcetype/><d:getcontentlength/>"
                + "</d:prop></d:propfind>";
        HttpURLConnection connection = null;
        try {
            connection = open(directoryPath(path), "PROPFIND");
            connection.setRequestProperty("Depth", "1");
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
            connection.setDoOutput(true);
            byte[] payload = body.getBytes(Charset.forName("UTF-8"));
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }
            int code = connection.getResponseCode();
            if (!isSuccess(code)) {
                throw WebDAVException.forStatus(code, "list " + path);
            }
            byte[] responseBody = readAll(connection.getInputStream());
            return parsePropfind(new ByteArrayInputStream(responseBody), basePath(), trimSlashes(path));
        } catch (IOException e) {
            throw new WebDAVException("Could not list " + path, e, true);
        } catch (SAXException | ParserConfigurationException e) {
            throw new WebDAVException("Could not read the server's folder listing", e, true);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void makeCollection(String path) throws WebDAVException {
        HttpURLConnection connection = null;
        try {
            connection = open(directoryPath(path), "MKCOL");
            int code = connection.getResponseCode();
            if (!isSuccess(code)) {
                throw WebDAVException.forStatus(code, "create folder " + path);
            }
        } catch (IOException e) {
            throw new WebDAVException("Could not create folder " + path, e, true);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void upload(String path, InputStream in, long size) throws WebDAVException {
        HttpURLConnection connection = null;
        try {
            connection = open(path, "PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            if (size > 0) {
                connection.setFixedLengthStreamingMode(size);
            } else {
                connection.setChunkedStreamingMode(0);
            }
            try (OutputStream out = connection.getOutputStream()) {
                copy(in, out);
            }
            int code = connection.getResponseCode();
            if (!isSuccess(code)) {
                throw WebDAVException.forStatus(code, "upload " + path);
            }
        } catch (IOException e) {
            throw new WebDAVException("Could not upload " + path, e, true);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Opens the download stream. The caller owns it and must close it, which also releases the
     * connection.
     */
    public InputStream openDownload(String path) throws WebDAVException {
        try {
            HttpURLConnection connection = open(path, "GET");
            int code = connection.getResponseCode();
            if (!isSuccess(code)) {
                connection.disconnect();
                throw WebDAVException.forStatus(code, "download " + path);
            }
            return connection.getInputStream();
        } catch (IOException e) {
            throw new WebDAVException("Could not download " + path, e, true);
        }
    }

    /**
     * A cheap PROPFIND against the root, used by the setup dialog so a typo or a wrong password is
     * reported while the user is still looking at the form.
     */
    public void checkConnection() throws WebDAVException {
        list("");
    }

    private static String directoryPath(String path) {
        String trimmed = trimSlashes(path);
        return trimmed.isEmpty() ? "" : trimmed + "/";
    }

    static String trimSlashes(String path) {
        String result = path == null ? "" : path;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        copy(in, buffer);
        return buffer.toByteArray();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            out.write(chunk, 0, read);
        }
    }
}
