# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.conreo.couchytv.**$$serializer { *; }
-keepclassmembers class com.conreo.couchytv.** {
    *** Companion;
}
-keepclasseswithmembers class com.conreo.couchytv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SSHD explicitly guards these JMX wrappers with !OsUtils.isAndroid().
-dontwarn javax.management.MBeanException
-dontwarn javax.management.ReflectionException
# The SSH key provider and NIO/SFTP providers use reflection.
-keep class org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory { public <init>(); }
-keep class org.apache.sshd.** { *; }

# Only password authentication and locally generated RSA host keys are enabled.
# Kerberos/GSS and encrypted PEM login wrappers are not used on Android.
-dontwarn org.ietf.jgss.**
-dontwarn javax.security.auth.login.CredentialException
-dontwarn javax.security.auth.login.FailedLoginException

# SSHD optional desktop/native crypto and Kerberos providers are disabled/unused.
# Retain SSHD reflection metadata; the app only enables NIO2/password/RSA.
-dontwarn org.bouncycastle.**
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn org.apache.tomcat.jni.**
-dontwarn javax.security.auth.login.**

# Desktop SSH client/JAAS APIs are never enabled by this server.
-dontwarn java.rmi.**
-dontwarn javax.security.auth.callback.NameCallback
