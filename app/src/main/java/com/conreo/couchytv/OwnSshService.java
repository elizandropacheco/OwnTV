package com.conreo.couchytv;

import android.app.*;
import android.content.*;
import android.os.IBinder;
import android.util.Base64;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.sshd.server.*;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.apache.sshd.common.util.OsUtils;

/** Authenticated network access as the launcher UID. No credentials in public config. */
public final class OwnSshService extends Service {
    private SshServer server;
    public static volatile String status = "Desativado";

    public static void start(Context context) {
        android.content.SharedPreferences p = context.getSharedPreferences("own_ssh", 0);
        Intent intent =
                new Intent(context, OwnSshService.class)
                        .putExtra("enabled", p.getBoolean("enabled", false))
                        .putExtra("user", p.getString("user", ""))
                        .putExtra("hash", p.getString("hash", ""))
                        .putExtra("salt", p.getString("salt", ""))
                        .putExtra("port", p.getInt("port", 2222));
        context.startForegroundService(intent);
    }

    public static boolean isRunning(Context context) {
        android.app.ActivityManager manager =
                (android.app.ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo info : manager.getRunningServices(100))
            if (info.service.getClassName().equals(OwnSshService.class.getName())) return true;
        return false;
    }

    public static String status(Context context) {
        try {
            return new String(
                    java.nio.file.Files.readAllBytes(
                            new File(context.getFilesDir(), "ssh-status.txt").toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "Desativado";
        }
    }

    private void writeStatus(String value) {
        status = value;
        try {
            File temporary = new File(getFilesDir(), "ssh-status.new");
            try (OutputStream out = new FileOutputStream(temporary)) {
                out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            temporary.renameTo(new File(getFilesDir(), "ssh-status.txt"));
        } catch (IOException ignored) {
        }
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public static String hash(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        try {
            return Base64.encodeToString(
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec)
                            .getEncoded(),
                    Base64.NO_WRAP);
        } finally {
            spec.clearPassword();
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.getBooleanExtra("enabled", false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(
                new NotificationChannel(
                        "own_ssh", "Acesso SSH à TV", NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this, EpActivity.class).putExtra("page", "ssh");
        Notification notification =
                new Notification.Builder(this, "own_ssh")
                        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                        .setContentTitle("OwnTV · acesso SSH ativo")
                        .setContentText("Acesso autenticado pela rede local")
                        .setContentIntent(
                                PendingIntent.getActivity(
                                        this,
                                        8104,
                                        open,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                                | PendingIntent.FLAG_IMMUTABLE))
                        .setOngoing(true)
                        .build();
        startForeground(8104, notification);
        writeStatus("Iniciando servidor…");
        worker.execute(
                () -> {
                    try {
                        if (server != null) server.stop(true);
                        String user = intent.getStringExtra("user"),
                                expected = intent.getStringExtra("hash"),
                                encoded = intent.getStringExtra("salt");
                        int port = intent.getIntExtra("port", 2222);
                        if (user.isEmpty() || expected.isEmpty() || encoded.isEmpty())
                            throw new IOException("configure credentials");
                        OsUtils.setAndroid(true);
                        System.setProperty("org.apache.sshd.security.provider.BC.enabled", "false");
                        File home = new File(getFilesDir(), "remote");
                        home.mkdirs();
                        org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver(
                                () -> home.toPath());
                        OsUtils.setCurrentWorkingDirectoryResolver(() -> home.toPath());
                        OsUtils.setCurrentUser(user);
                        SshServer next = SshServer.setUpDefaultServer();
                        next.setIoServiceFactoryFactory(
                                new org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory());
                        next.setPort(port);
                        // The listener stays available across LAN interface changes, but public
                        // peers are rejected before authentication. This is not a router firewall.
                        OwnSshAccessPolicy accessPolicy = new OwnSshAccessPolicy();
                        Set<org.apache.sshd.common.session.Session> connections =
                                Collections.synchronizedSet(new HashSet<>());
                        next.addSessionListener(
                                new org.apache.sshd.common.session.SessionListener() {
                                    @Override
                                    public void sessionCreated(
                                            org.apache.sshd.common.session.Session session) {
                                        java.net.SocketAddress remote = session.getRemoteAddress();
                                        java.net.InetAddress peer =
                                                remote instanceof java.net.InetSocketAddress
                                                        ? ((java.net.InetSocketAddress) remote)
                                                                .getAddress()
                                                        : null;
                                        synchronized (connections) {
                                            if (!OwnSshAccessPolicy.isPrivatePeer(peer)
                                                    || connections.size() >= 8) {
                                                session.close(true);
                                                return;
                                            }
                                            connections.add(session);
                                        }
                                    }

                                    @Override
                                    public void sessionClosed(
                                            org.apache.sshd.common.session.Session session) {
                                        connections.remove(session);
                                    }
                                });
                        org.apache.sshd.core.CoreModuleProperties.MAX_AUTH_REQUESTS.set(next, 3);
                        org.apache.sshd.core.CoreModuleProperties.MAX_CONCURRENT_SESSIONS.set(
                                next, 2);
                        org.apache.sshd.core.CoreModuleProperties.MAX_CONCURRENT_CHANNELS.set(
                                next, 4);
                        org.apache.sshd.core.CoreModuleProperties.NIO_WORKERS.set(next, 2);
                        org.apache.sshd.core.CoreModuleProperties.AUTH_TIMEOUT.set(
                                next, java.time.Duration.ofSeconds(30));
                        org.apache.sshd.core.CoreModuleProperties.IDLE_TIMEOUT.set(
                                next, java.time.Duration.ofMinutes(10));
                        SimpleGeneratorHostKeyProvider hostKey =
                                new SimpleGeneratorHostKeyProvider(
                                        new File(getFilesDir(), "ssh-hostkey.ser").toPath());
                        hostKey.setAlgorithm("RSA");
                        hostKey.setKeySize(3072);
                        next.setKeyPairProvider(hostKey);
                        next.setUserAuthFactories(
                                Collections.singletonList(
                                        org.apache.sshd.server.auth.password.UserAuthPasswordFactory
                                                .INSTANCE));
                        next.setPasswordAuthenticator(
                                (username, password, session) -> {
                                    java.net.SocketAddress remote = session.getRemoteAddress();
                                    java.net.InetAddress peer =
                                            remote instanceof java.net.InetSocketAddress
                                                    ? ((java.net.InetSocketAddress) remote)
                                                            .getAddress()
                                                    : null;
                                    if (!accessPolicy.allowAttempt(
                                            peer,
                                            password == null ? 0 : password.length(),
                                            System.nanoTime() / 1_000_000L)) return false;
                                    if (!user.equals(username)) return false;
                                    try {
                                        return MessageDigest.isEqual(
                                                Base64.decode(expected, Base64.NO_WRAP),
                                                Base64.decode(
                                                        hash(
                                                                password,
                                                                Base64.decode(
                                                                        encoded, Base64.NO_WRAP)),
                                                        Base64.NO_WRAP));
                                    } catch (Exception e) {
                                        return false;
                                    }
                                });
                        next.setPublickeyAuthenticator((keyUser, key, session) -> false);
                        next.setForwardingFilter(
                                org.apache.sshd.server.forward.RejectAllForwardingFilter.INSTANCE);
                        next.setFileSystemFactory(new VirtualFileSystemFactory(home.toPath()));
                        next.setSubsystemFactories(
                                Collections.singletonList(
                                        new SftpSubsystemFactory.Builder().build()));
                        next.setShellFactory(
                                channel -> new LocalCommand(home, "/system/bin/sh", "-i"));
                        next.setCommandFactory(
                                (channel, command) ->
                                        new LocalCommand(home, "/system/bin/sh", "-c", command));
                        next.start();
                        server = next;
                        writeStatus("Ativo · porta " + port);
                    } catch (Exception | LinkageError e) {
                        writeStatus("Falha ao iniciar · " + e.getClass().getSimpleName());
                        android.util.Log.e("OwnTVSSH", "Startup failure", e);
                        stopSelf();
                    }
                });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (status.startsWith("Ativo") || status.startsWith("Iniciando")) writeStatus("Desativado");
        worker.execute(
                () -> {
                    try {
                        if (server != null) server.stop(true);
                    } catch (Exception ignored) {
                    }
                });
        worker.shutdown();
        super.onDestroy();
    }

    private static final class LocalCommand implements Command {
        private InputStream input;
        private OutputStream output, error;
        private ExitCallback callback;
        private Process process;
        private final String[] command;
        private final File home;
        private final ExecutorService streams = Executors.newFixedThreadPool(4);

        LocalCommand(File home, String... command) {
            this.home = home;
            this.command = command;
        }

        @Override
        public void setInputStream(InputStream in) {
            input = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            output = out;
        }

        @Override
        public void setErrorStream(OutputStream out) {
            error = out;
        }

        @Override
        public void setExitCallback(ExitCallback value) {
            callback = value;
        }

        @Override
        public void start(ChannelSession channel, Environment environment) throws IOException {
            ProcessBuilder builder = new ProcessBuilder(command).directory(home);
            builder.environment().put("HOME", home.getPath());
            builder.environment().put("TERM", "xterm-256color");
            process = builder.start();
            streams.execute(() -> copy(input, process.getOutputStream(), true));
            Future<?> stdout = streams.submit(() -> copy(process.getInputStream(), output, false));
            Future<?> stderr = streams.submit(() -> copy(process.getErrorStream(), error, false));
            streams.execute(
                    () -> {
                        int code = 1;
                        try {
                            code = process.waitFor();
                            try {
                                stdout.get(3, TimeUnit.SECONDS);
                                stderr.get(3, TimeUnit.SECONDS);
                            } catch (Exception ignored) {
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        try {
                            output.flush();
                            error.flush();
                        } catch (Exception ignored) {
                        }
                        callback.onExit(code);
                        streams.shutdown();
                    });
        }

        private void copy(InputStream from, OutputStream to, boolean close) {
            try {
                byte[] b = new byte[8192];
                int n;
                while ((n = from.read(b)) >= 0) {
                    to.write(b, 0, n);
                    to.flush();
                }
            } catch (IOException ignored) {
            } finally {
                if (close)
                    try {
                        to.close();
                    } catch (IOException ignored) {
                    }
            }
        }

        @Override
        public void destroy(ChannelSession channel) {
            if (process != null) process.destroy();
            streams.shutdownNow();
        }
    }
}
