package tech.powerjob.client.service.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.client.common.Protocol;
import tech.powerjob.common.exception.PowerJobException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ClientTransportSafetyTest {
    private static final String STORE_PASSWORD = "local-fixture-only";
    private static final byte[] AUTH = bytes("{\"success\":true,\"data\":{\"appId\":17,\"token\":\"local-fixture-token\"}}");
    private static final byte[] ID = bytes("{\"success\":true,\"data\":9007199254740993}");
    @TempDir static Path directory;
    private static String previousTrustStore;
    private static String previousTrustStorePassword;

    @BeforeAll
    static void createLocalCertificates() throws Exception {
        // All keys are generated per test run and remain inside JUnit's temporary directory.
        keytool("-genkeypair", "-alias", "ca", "-dname", "CN=Local-Test-CA", "-keyalg", "RSA", "-keysize", "2048", "-validity", "3650", "-ext", "bc:c", "-keystore", file("ca.jks"), "-storetype", "JKS");
        keytool("-exportcert", "-alias", "ca", "-keystore", file("ca.jks"), "-rfc", "-file", file("ca.pem"));
        keytool("-importcert", "-alias", "ca", "-keystore", file("trust.jks"), "-storetype", "JKS", "-file", file("ca.pem"), "-noprompt");
        for (String name : Arrays.asList("valid", "wrong-host", "expired")) {
            String hostname = name.equals("wrong-host") ? "wrong.invalid" : "localhost";
            keytool("-genkeypair", "-alias", "server", "-dname", "CN=" + hostname, "-keyalg", "RSA", "-keysize", "2048", "-validity", "365", "-ext", "SAN=dns:" + hostname, "-keystore", file(name + ".jks"), "-storetype", "JKS");
            keytool("-certreq", "-alias", "server", "-keystore", file(name + ".jks"), "-file", file(name + ".csr"));
            List<String> signing = new ArrayList<>(Arrays.asList("-gencert", "-alias", "ca", "-keystore", file("ca.jks"), "-infile", file(name + ".csr"), "-outfile", file(name + ".pem"), "-rfc", "-ext", "SAN=dns:" + hostname, "-ext", "KU=digitalSignature,keyEncipherment", "-ext", "EKU=serverAuth", "-validity", name.equals("expired") ? "1" : "365"));
            if (name.equals("expired")) signing.addAll(Arrays.asList("-startdate", "2020/01/01 00:00:00"));
            keytool(signing.toArray(new String[0]));
            keytool("-importcert", "-alias", "ca", "-keystore", file(name + ".jks"), "-file", file("ca.pem"), "-noprompt");
            keytool("-importcert", "-alias", "server", "-keystore", file(name + ".jks"), "-file", file(name + ".pem"), "-noprompt");
        }
        keytool("-genkeypair", "-alias", "server", "-dname", "CN=localhost", "-keyalg", "RSA", "-keysize", "2048", "-validity", "365", "-ext", "SAN=dns:localhost", "-keystore", file("unknown-ca.jks"), "-storetype", "JKS");
        previousTrustStore = System.getProperty("javax.net.ssl.trustStore");
        previousTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        System.setProperty("javax.net.ssl.trustStore", file("trust.jks"));
        System.setProperty("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
    }

    @AfterAll static void restoreTrustStore() {
        restore("javax.net.ssl.trustStore", previousTrustStore);
        restore("javax.net.ssl.trustStorePassword", previousTrustStorePassword);
    }
    private static void restore(String key, String value) { if (value == null) System.clearProperty(key); else System.setProperty(key, value); }
    private static String file(String name) { return directory.resolve(name).toString(); }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void keytool(String... args) throws Exception {
        Path executable = Paths.get(System.getProperty("java.home"), "bin", "keytool");
        if (!Files.isExecutable(executable)) executable = executable.getParent().getParent().getParent().resolve("bin/keytool");
        List<String> command = new ArrayList<>(); command.add(executable.toString()); command.addAll(Arrays.asList(args));
        command.addAll(Arrays.asList("-storepass", STORE_PASSWORD, "-keypass", STORE_PASSWORD));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(directory.resolve("keytool.log").toFile()).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "keytool deadline exceeded");
        assertEquals(0, process.exitValue(), "keytool failed; inspect local temporary log");
    }

    @Test void acceptsTrustedCertificateAndHostname() throws Exception {
        try (Endpoint endpoint = new Endpoint("valid"); PowerJobClient client = new PowerJobClient(config(true, endpoint.address()))) {
            assertEquals(Long.valueOf(9007199254740993L), client.runJob(1L).getData());
        }
    }
    @Test void rejectsUnknownCertificateAuthority() throws Exception { rejectsCertificate("unknown-ca"); }
    @Test void rejectsWrongHostname() throws Exception { rejectsCertificate("wrong-host"); }
    @Test void rejectsExpiredCertificate() throws Exception { rejectsCertificate("expired"); }
    private void rejectsCertificate(String name) throws Exception {
        try (Endpoint endpoint = new Endpoint(name)) {
            assertThrows(PowerJobException.class, () -> new PowerJobClient(config(true, endpoint.address())));
            assertEquals(0, endpoint.authCalls.get(), "TLS must reject before credential payload reaches the endpoint");
        }
    }
    @Test void acceptsCaseInsensitiveAuthHeaderWithoutReplayingWrite() throws Exception {
        try (Endpoint endpoint = new Endpoint(null); PowerJobClient client = new PowerJobClient(config(false, endpoint.address()))) {
            assertTrue(client.runJob(1L).isSuccess());
            assertEquals(1, endpoint.calls.get());
        }
    }
    @Test void missingAuthHeaderDoesNotReplayCommittedWrite() throws Exception {
        try (Endpoint endpoint = new Endpoint(null); PowerJobClient client = new PowerJobClient(config(false, endpoint.address()))) {
            endpoint.mode = "missing-header";
            PowerJobException error = assertThrows(PowerJobException.class, () -> client.runJob(1L));
            assertTrue(error.getMessage().contains("not retried"));
            assertEquals(1, endpoint.calls.get());
        }
    }
    @Test void explicitAuthRejectionCanRefreshAndRetry() throws Exception {
        try (Endpoint endpoint = new Endpoint(null); PowerJobClient client = new PowerJobClient(config(false, endpoint.address()))) {
            endpoint.mode = "reject-once";
            assertTrue(client.runJob(1L).isSuccess());
            assertEquals(2, endpoint.calls.get());
            assertEquals(1, endpoint.commits.get(), "rejected request has no write side effect");
        }
    }
    @Test void lostResponseDoesNotReplayCommittedWriteOnEitherNode() throws Exception {
        try (Endpoint first = new Endpoint(null); Endpoint second = new Endpoint(null);
             PowerJobClient client = new PowerJobClient(config(false, first.address(), second.address()))) {
            first.mode = "drop";
            PowerJobException error = assertThrows(PowerJobException.class, () -> client.runJob(1L));
            assertTrue(error.getMessage().contains("not retried"));
            assertEquals(1, first.commits.get()); assertEquals(0, second.commits.get());
        }
    }
    @Test void readRequestCanFailOverAfterLostResponse() throws Exception {
        try (Endpoint first = new Endpoint(null); Endpoint second = new Endpoint(null);
             PowerJobClient client = new PowerJobClient(config(false, first.address(), second.address()))) {
            first.mode = "drop";
            assertTrue(client.fetchInstanceStatus(1L).isSuccess());
            assertEquals(1, first.calls.get()); assertEquals(1, second.calls.get());
        }
    }
    @Test void writeCanFailOverWhenConnectionWasNeverEstablished() throws Exception {
        try (Endpoint first = new Endpoint(null); Endpoint second = new Endpoint(null);
             PowerJobClient client = new PowerJobClient(config(false, first.address(), second.address()))) {
            first.close();
            assertTrue(client.runJob(1L).isSuccess());
            assertEquals(0, first.calls.get()); assertEquals(1, second.commits.get());
        }
    }
    @Test void httpServerFailureDoesNotFailOver() throws Exception {
        try (Endpoint first = new Endpoint(null); Endpoint second = new Endpoint(null);
             PowerJobClient client = new PowerJobClient(config(false, first.address(), second.address()))) {
            first.mode = "503";
            assertThrows(PowerJobException.class, () -> client.runJob(1L));
            assertEquals(1, first.calls.get()); assertEquals(0, second.calls.get());
        }
    }
    @Test void retryAfterZeroDoesNotReplayCommittedWrite() throws Exception {
        try (Endpoint endpoint = new Endpoint(null); PowerJobClient client = new PowerJobClient(config(false, endpoint.address()))) {
            endpoint.mode = "503-after-commit";
            assertThrows(PowerJobException.class, () -> client.runJob(1L));
            assertEquals(1, endpoint.calls.get(), "Retry-After: 0 must not bypass the write retry policy");
            assertEquals(1, endpoint.commits.get());
        }
    }
    @Test void crossHostRedirectDoesNotForwardCredentials() throws Exception {
        try (Endpoint first = new Endpoint(null); Endpoint second = new Endpoint(null);
             PowerJobClient client = new PowerJobClient(config(false, first.address()))) {
            first.mode = "redirect";
            first.redirect = "http://127.0.0.1:" + second.server.getAddress().getPort() + "/openApi/runJob2";
            assertThrows(PowerJobException.class, () -> client.runJob(1L));
            assertEquals(1, first.calls.get());
            assertEquals(0, second.calls.get(), "OpenAPI must not follow a response to another configured or unconfigured host");
            assertEquals(0, second.credentialCalls.get(), "app auth headers must never reach the redirected host");
        }
    }

    private static ClientConfig config(boolean tls, String... addresses) {
        return new ClientConfig().setAppName("local-test").setPassword("local-test-password").setProtocol(tls ? Protocol.HTTPS : Protocol.HTTP)
                .setAddressList(Arrays.asList(addresses)).setConnectionTimeout(2).setReadTimeout(2).setWriteTimeout(2);
    }
    private static final class Endpoint implements AutoCloseable {
        final HttpServer server;
        final ExecutorService executor = Executors.newCachedThreadPool();
        final AtomicInteger authCalls = new AtomicInteger();
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger credentialCalls = new AtomicInteger();
        volatile String mode = "normal";
        volatile String redirect;
        Endpoint(String certificate) throws Exception {
            if (certificate == null) server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            else {
                KeyStore keys = KeyStore.getInstance("JKS");
                try (InputStream in = Files.newInputStream(directory.resolve(certificate + ".jks"))) { keys.load(in, STORE_PASSWORD.toCharArray()); }
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); kmf.init(keys, STORE_PASSWORD.toCharArray());
                SSLContext context = SSLContext.getInstance("TLS"); context.init(kmf.getKeyManagers(), null, null);
                HttpsServer https = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                https.setHttpsConfigurator(new HttpsConfigurator(context)); server = https;
            }
            server.setExecutor(executor); server.createContext("/openApi/", this::handle); server.start();
        }
        String address() { return "localhost:" + server.getAddress().getPort(); }
        void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestHeaders().containsKey("X-POWERJOB-ACCESS-TOKEN")) credentialCalls.incrementAndGet();
            while (exchange.getRequestBody().read() != -1) { /* Never print credentials or request bodies. */ }
            boolean auth = exchange.getRequestURI().getPath().endsWith("/authApp");
            if (auth) { authCalls.incrementAndGet(); send(exchange, 200, AUTH, "true"); return; }
            int attempt = calls.incrementAndGet();
            if (mode.equals("reject-once") && attempt == 1) { send(exchange, 200, bytes("{\"success\":false}"), "false"); return; }
            if (mode.equals("503")) { send(exchange, 503, bytes("{}"), "true"); return; }
            commits.incrementAndGet();
            if (mode.equals("redirect")) {
                exchange.getResponseHeaders().set("Location", redirect);
                send(exchange, 302, bytes("{}"), "true"); return;
            }
            if (mode.equals("503-after-commit")) {
                exchange.getResponseHeaders().set("Retry-After", "0");
                send(exchange, 503, bytes("{}"), "true"); return;
            }
            if (mode.equals("drop")) { exchange.close(); return; }
            byte[] result = exchange.getRequestURI().getPath().endsWith("/fetchInstanceStatus") ? bytes("{\"success\":true,\"data\":4}") : ID;
            send(exchange, 200, result, mode.equals("missing-header") ? null : "true");
        }
        void send(HttpExchange exchange, int code, byte[] body, String authStatus) throws IOException {
            // HttpServer canonicalizes this legal HTTP header to X-powerjob-auth-passed.
            if (authStatus != null) exchange.getResponseHeaders().set("X-POWERJOB-AUTH-PASSED", authStatus);
            exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(code, body.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
        }
        @Override public void close() { server.stop(0); executor.shutdownNow(); }
    }
}
