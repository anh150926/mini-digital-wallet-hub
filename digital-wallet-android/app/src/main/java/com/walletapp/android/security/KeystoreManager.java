package com.walletapp.android.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

public class KeystoreManager {

    private static final String TAG = "KeystoreManager";
    private static final String KEY_ALIAS = "wallet_tx_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    public static KeyPair getOrCreateKeyPair() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
                return new KeyPair(publicKey, privateKey);
            }

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
            );

            KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY
            )
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build();

            keyPairGenerator.initialize(parameterSpec);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            Log.e(TAG, "Error generating or retrieving KeyPair", e);
            throw new RuntimeException("Không thể khởi tạo khóa bảo mật phần cứng", e);
        }
    }

    public static String getPublicKeyBase64() {
        KeyPair keyPair = getOrCreateKeyPair();
        return Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);
    }

    public static String signData(String data) {
        try {
            KeyPair keyPair = getOrCreateKeyPair();
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error signing transaction payload", e);
            throw new RuntimeException("Ký giao dịch thất bại", e);
        }
    }
}
