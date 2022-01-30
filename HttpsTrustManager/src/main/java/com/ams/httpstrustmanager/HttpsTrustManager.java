package com.ams.httpstrustmanager;

import android.content.res.Resources;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class HttpsTrustManager {

    public static void useSSLCer(final Resources resources, final int ... rawCertificateResourceIds) {
        final CertificateFactory certificateFactory;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (final CertificateException exception) {
            Log.e("useSSLCer", "Failed to get an instance of the CertificateFactory.", exception);
            return;
        }
        int i = 0;
        final Certificate[] certificates = new Certificate[rawCertificateResourceIds.length];
        for (final int rawCertificateResourceId : rawCertificateResourceIds) {
            final Certificate certificate;
            try (final InputStream certificateInputStream = resources.openRawResource(rawCertificateResourceId)) {
                certificate = certificateFactory.generateCertificate(certificateInputStream);
            } catch (final IOException | CertificateException exception) {
                Log.e("useSSLCer", "Failed to retrieve the Certificate.", exception);
                return;
            }


            certificates[i] = certificate;
            i++;
        }

        final KeyStore keyStore;
        try {
            keyStore = buildKeyStore(certificates);
        } catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException exception) {
            Log.e("useSSLCer", "Failed to build the KeyStore with the Certificate.", exception);
            return;
        }

        final TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = buildTrustManager(keyStore);
        } catch (final KeyStoreException | NoSuchAlgorithmException exception) {
            Log.e("TAG", "Failed to build the TrustManagerFactory with the KeyStore.", exception);
            return;
        }

        final SSLContext sslContext;
        try {
            sslContext = buildSSLContext(trustManagerFactory);
        } catch (final KeyManagementException | NoSuchAlgorithmException exception) {
            Log.e("useSSLCer", "Failed to build the SSLContext with the TrustManagerFactory.", exception);
            return;
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    public static KeyStore buildKeyStore(final Certificate[] certificates) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final String keyStoreType = KeyStore.getDefaultType();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);

        int i = 0;
        for (final Certificate certificate : certificates) {
            keyStore.setCertificateEntry("ca" + i, certificate);
            i++;
        }

        return keyStore;
    }

    public static TrustManagerFactory buildTrustManager(final KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        final String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
    }
    public static SSLContext buildSSLContext(final TrustManagerFactory trustManagerFactory) throws KeyManagementException, NoSuchAlgorithmException {
        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }

}
